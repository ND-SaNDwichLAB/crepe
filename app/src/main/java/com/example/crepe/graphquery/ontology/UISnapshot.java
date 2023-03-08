package com.example.crepe.graphquery.ontology;

import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.example.crepe.demonstration.DemonstrationUtil;
import com.example.crepe.graphquery.model.Node;
import com.example.crepe.graphquery.ontology.helper.ListOrderResolver;
//import com.example.crepe.graphquery.automation.AutomatorUtil;
//import com.example.crepe.graphquery.ontology.helper.ListOrderResolver;
//import com.example.crepe.graphquery.ontology.helper.TextStringParseHelper;
//import com.example.crepe.graphquery.ontology.helper.annotator.SugiliteNodeAnnotator;
//import com.example.crepe.graphquery.ontology.helper.annotator.SugiliteTextParentAnnotator;

import org.apache.commons.lang3.StringUtils;

/**
 * @author toby
 * @date 9/25/17
 * @time 5:57 PM
 */
public class UISnapshot {
    private final Set<SugiliteTriple> triples;
    private static boolean TO_ADD_SPATIAL_RELATIONS = true;

    //indexes for triples
    private final Map<Integer, Set<SugiliteTriple>> subjectTriplesMap;
    private final Map<Integer, Set<SugiliteTriple>> objectTriplesMap;
    private final Map<Integer, Set<SugiliteTriple>> predicateTriplesMap;

    private final transient Map<Map.Entry<Integer, Integer>, Set<SugiliteTriple>> subjectPredicateTriplesMap;

    //indexes for entities and relations
    private Map<Integer, SugiliteEntity> sugiliteEntityIdSugiliteEntityMap;
    private Map<Integer, SugiliteRelation> sugiliteRelationIdSugiliteRelationMap;


    private transient int entityIdCounter;
    private final transient Map<Node, SugiliteEntity<Node>> nodeSugiliteEntityMap;
    private final transient Map<String, SugiliteEntity<String>> stringSugiliteEntityMap;
    private final transient Map<List<String>, SugiliteEntity<List>> stringListSugiliteEntityMap;
    private final transient Map<Double, SugiliteEntity<Double>> doubleSugiliteEntityMap;
    private final transient Map<Boolean, SugiliteEntity<Boolean>> booleanSugiliteEntityMap;
    private final transient Map<Node, AccessibilityNodeInfo> nodeAccessibilityNodeInfoMap;
    private final transient UISnapshot uiSnapshot;

    private boolean stringEntitiesAreAnnotated = false;

    protected static final String TAG = UISnapshot.class.getSimpleName();
    private transient Display display;

    private String activityName;
    private String packageName;


    //construct an empty UI snapshot
    public UISnapshot(Display display){
        //empty
        this.display = display;
        this.uiSnapshot = this;
        this.triples = new LinkedHashSet<>();
        this.subjectTriplesMap = new HashMap<>();
        this.objectTriplesMap = new HashMap<>();
        this.predicateTriplesMap = new HashMap<>();
        this.subjectPredicateTriplesMap = new HashMap<>();
        this.sugiliteEntityIdSugiliteEntityMap = new HashMap<>();
        this.sugiliteRelationIdSugiliteRelationMap = new HashMap<>();
        this.nodeSugiliteEntityMap = new HashMap<>();
        this.stringSugiliteEntityMap = new HashMap<>();
        this.stringListSugiliteEntityMap = new HashMap<List<String>, SugiliteEntity<List>>();
        this.doubleSugiliteEntityMap = new HashMap<>();
        this.booleanSugiliteEntityMap = new HashMap<>();
        this.nodeAccessibilityNodeInfoMap = new HashMap<>();
        this.entityIdCounter = 0;
    }

    //construct a UISnapshot for a list of windows
    public UISnapshot(Display display, List<AccessibilityWindowInfo> windows, boolean toConstructNodeAccessibilityNodeInfoMap, String activePackageName, String activeActivityName) {
        this(display);
        this.packageName = activePackageName;
        this.activityName = activeActivityName;

        List<Node> allNodes = new ArrayList<>();
        for(AccessibilityWindowInfo window : windows){
            AccessibilityNodeInfo rootNode = window.getRoot();
            if(rootNode != null) {
                allNodes.addAll(preOrderNodeTraverseWithZIndex(rootNode, toConstructNodeAccessibilityNodeInfoMap, window.getLayer(), new ArrayList<>(), activePackageName, activeActivityName));
            }
        }

        long startTime = System.currentTimeMillis();
        constructFromListOfNodes(allNodes);
        long stopTime = System.currentTimeMillis();
        Log.v(TAG, String.format("Constructed from List of %d Nodes! -- Takes %s ms", allNodes.size(), String.valueOf(stopTime - startTime)));
    }

    //construct a UISnapshot from a rootNode
    public UISnapshot(Display display, AccessibilityNodeInfo rootNode, boolean toConstructNodeAccessibilityNodeInfoMap, String activePackageName, String activeActivityName) {
        this(display);
        this.activityName = activeActivityName;
        this.packageName = activePackageName;

        List<AccessibilityNodeInfo> allOldNodes = DemonstrationUtil.preOrderTraverse(rootNode);
        List<Node> allNodes = new ArrayList<>();
        if(allOldNodes != null) {
            for (AccessibilityNodeInfo oldNode : allOldNodes) {
                if (activePackageName != null) {
                    Node node = new Node(oldNode, activePackageName.equals(oldNode.getPackageName()) ? activeActivityName : null);
                    if (node.getPackageName() != null && (node.getPackageName().contains("com.android.systemui") || node.getPackageName().contains("crepe"))) {
                        continue;
                    }
                    allNodes.add(node);
                    if (toConstructNodeAccessibilityNodeInfoMap) {
                        nodeAccessibilityNodeInfoMap.put(node, oldNode);
                    }
                }
            }
        }
        constructFromListOfNodes(allNodes);
    }

    /*
    private void setActivityNameAndPackageNameFromAllNodes(List<Node> allNodes) {
        for(Node node : allNodes) {
            if (node.getPackageName() != null) {
                if (this.packageName == null) {
                    this.packageName = node.getPackageName();
                } else {
                    if (! node.getPackageName().equals(this.packageName)){
                        Log.e("UISnapshot", String.format("Inconsistent package name! Had %s, now get %s.", this.packageName, node.getPackageName()));
                    }
                }

                if (node.getClassName() != null) {
                    ComponentName componentName = new ComponentName(
                            node.getPackageName().toString(),
                            node.getClassName().toString()
                    );
                    try {
                        ActivityInfo activityInfo = SugiliteData.getAppContext().getPackageManager().getActivityInfo(componentName, 0);
                        if (activityInfo.name != null) {
                            if (this.activityName == null) {
                                this.activityName = activityInfo.name;
                            } else {
                                if (! node.getPackageName().equals(this.packageName)){
                                    Log.e("UISnapshot", String.format("Inconsistent activity name! Had %s, now get %s.", this.activityName, activityInfo.name));
                                }
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("UISnapshot", String.format("Can't find activity name for %s", componentName.flattenToString()));
                    }
                }
            }
        }
    }
    */


    /**
     * contruct a UI snapshot from a list of all nodes
     * @param allNodes
     */
    private void constructFromListOfNodes(List<Node> allNodes){

        if(allNodes != null){
            for(Node node : allNodes) {
                if(node.getPackageName() != null && (node.getPackageName().contains("com.android.systemui") || node.getPackageName().contains("crepe"))){
                    continue;
                }

                //get the corresponding entity for the node
                SugiliteEntity<Node> currentEntity = null;
                if (nodeSugiliteEntityMap.containsKey(node)) {
                    currentEntity = nodeSugiliteEntityMap.get(node);
                } else {
                    //create a new entity for the node
                    SugiliteEntity<Node> entity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, node);
                    nodeSugiliteEntityMap.put(node, entity);
                    currentEntity = entity;
                }


                if (node.getClassName() != null) {
                    //class
                    String className = node.getClassName();
                    addEntityStringTriple(currentEntity, className, SugiliteRelation.HAS_CLASS_NAME);
                }

                if (node.getText() != null) {
                    //text
                    String text = node.getText();
                    addEntityStringTriple(currentEntity, text, SugiliteRelation.HAS_TEXT);
                }

                if (node.getViewIdResourceName() != null) {
                    //view id
                    String viewId = node.getViewIdResourceName();
                    addEntityStringTriple(currentEntity, viewId, SugiliteRelation.HAS_VIEW_ID);
                }

                if (node.getPackageName() != null) {
                    //package name
                    String packageName = node.getPackageName();
                    addEntityStringTriple(currentEntity, packageName, SugiliteRelation.HAS_PACKAGE_NAME);
                }

                if (node.getActivityName() != null) {
                    //activity name
                    String activityName = node.getActivityName();
                    addEntityStringTriple(currentEntity, activityName, SugiliteRelation.HAS_ACTIVITY_NAME);
                }

                if (node.getContentDescription() != null) {
                    //content description
                    String contentDescription = node.getContentDescription();
                    addEntityStringTriple(currentEntity, contentDescription, SugiliteRelation.HAS_CONTENT_DESCRIPTION);
                }


                //isClickable
                addEntityBooleanTriple(currentEntity, node.getClickable(), SugiliteRelation.IS_CLICKABLE);

                //isEditable
                addEntityBooleanTriple(currentEntity, node.getEditable(), SugiliteRelation.IS_EDITABLE);

                //isScrollable
                addEntityBooleanTriple(currentEntity, node.getScrollable(), SugiliteRelation.IS_SCROLLABLE);

                //isCheckable
                addEntityBooleanTriple(currentEntity, node.getCheckable(), SugiliteRelation.IS_CHECKABLE);

                //isChecked
                addEntityBooleanTriple(currentEntity, node.getChecked(), SugiliteRelation.IS_CHECKED);

                //isSelected
                addEntityBooleanTriple(currentEntity, node.getSelected(), SugiliteRelation.IS_SELECTED);

                //isFocused
                addEntityBooleanTriple(currentEntity, node.getFocused(), SugiliteRelation.IS_FOCUSED);


                //screen location
                addEntityStringTriple(currentEntity, node.getBoundsInScreen(), SugiliteRelation.HAS_SCREEN_LOCATION);

                //parent location
                addEntityStringTriple(currentEntity, node.getBoundsInParent(), SugiliteRelation.HAS_PARENT_LOCATION);


                //*** temporarily disable HAS_PARENT and HAS_CHILD relations ***


                //has_parent relation
                if (node.getParent() != null) {
                    //parent
                    Node parentNode = node.getParent();
                    SugiliteEntity<Node> parentEntity = null;
                    if(nodeSugiliteEntityMap.containsKey(parentNode)) {
                        parentEntity = nodeSugiliteEntityMap.get(parentNode);
                        SugiliteTriple triple1 = new SugiliteTriple(parentEntity, SugiliteRelation.HAS_CHILD, currentEntity);
                        addTriple(triple1);
                        SugiliteTriple triple2 = new SugiliteTriple(currentEntity, SugiliteRelation.HAS_PARENT, parentEntity);
                        addTriple(triple2);
                    }
                    else {
                        SugiliteEntity<Node> newEntity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, parentNode);
                        parentEntity = newEntity;
                        nodeSugiliteEntityMap.put(parentNode, newEntity);
                        SugiliteTriple triple1 = new SugiliteTriple(newEntity, SugiliteRelation.HAS_CHILD, currentEntity);
                        addTriple(triple1);
                        SugiliteTriple triple2 = new SugiliteTriple(currentEntity, SugiliteRelation.HAS_PARENT, newEntity);
                        addTriple(triple2);
                    }

                    AccessibilityNodeInfo parentAccessibilityNode = nodeAccessibilityNodeInfoMap.get(parentNode);
                    // add parent text relation
                    if (parentAccessibilityNode != null && parentAccessibilityNode.getText()!= null && !parentAccessibilityNode.getText().toString().isEmpty()) {
                        addEntityStringTriple(currentEntity, parentAccessibilityNode.getText().toString(), SugiliteRelation.HAS_PARENT_TEXT);
                    }

                    // add sibling text relation
                    Integer siblingCount = parentAccessibilityNode.getChildCount();
                    List<String> resultSiblingStringList = new ArrayList<>();
                    if (siblingCount > 1) {  // if the node has siblings
                        for (int i = 0; i < siblingCount; i++) {
                            AccessibilityNodeInfo siblingNode = parentAccessibilityNode.getChild(i);
                            if (siblingNode != null && !nodeAccessibilityNodeInfoMap.get(node).equals(siblingNode)) {   // the second condition makes sure we don't collect the string of current node
                                if (siblingNode.getText() != null && !siblingNode.getText().toString().isEmpty()) {
                                    resultSiblingStringList.add(siblingNode.getText().toString());
                                }
                            }
                        }

                        addEntityStringListTriple(currentEntity, resultSiblingStringList, SugiliteRelation.HAS_SIBLING_TEXT);
                    }
                }




                //has_child_text relation
                if (node.getParent() != null && node.getText() != null){
                    String text = node.getText();
                    Set<Node> parentNodes = new LinkedHashSet<>();
                    Node currentParent = node;
                    while(currentParent.getParent() != null){
                        currentParent = currentParent.getParent();
                        parentNodes.add(currentParent);
                    }
                    for(Node parentNode : parentNodes){
                        if(nodeSugiliteEntityMap.containsKey(parentNode)) {
                            addEntityStringTriple(nodeSugiliteEntityMap.get(parentNode), text, SugiliteRelation.HAS_CHILD_TEXT);
                        }
                        else {
                            SugiliteEntity<Node> newEntity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, parentNode);
                            nodeSugiliteEntityMap.put(parentNode, newEntity);
                            addEntityStringTriple(newEntity, text, SugiliteRelation.HAS_CHILD_TEXT);
                        }
                    }
                }

                // spatial relations
                if (TO_ADD_SPATIAL_RELATIONS) {
                    // loop through all other nodes, find the ones closest to the current node in each of the 4 directions, and add them to the UISnapshot

                    // maintain 2 hashmaps
                    // 1. spatialRelationMap: key – relation to current node, value – all other nodes
                    // the reason for relation being the key and node being the value: for each current node, in UISnapshot, we only store the 4 nodes closest to it in 4 directions.
                    // As a result, if we find a closer node in 1 direction, we replace the previous closest node in spatialRelationMap with the new closest node
                    Map<SugiliteRelation, Node> spatialRelationMap = new HashMap<>();
                    // 2. spatialDistanceMap: key – all other nodes, value – distance to current node
                    Map<Node, Double> spatialDistanceMap = new HashMap<>();

                    for(Node objectNode: allNodes) {
                        if (!objectNode.equals(node)) {   // ensure it's not the same node
                            if (objectNode.getIsVisibleToUser()) { // if the node is visible to user
                                // 1. figure out the relationship between the nodes, store in spatialRelationMap
                                // Note there can be 1 - 2 spatial relationships between nodes: e.g. above and right
                                List<SugiliteRelation> spatialRelationList = getSpatialRelationBetweenNodes(node, objectNode);

                                //  2. calculate the distance, store in spatialDistanceMap
                                Double spatialDistance = getSpatialDistanceBetweenNodes(node, objectNode);
                                if (spatialDistance != null) {
                                    spatialDistanceMap.put(objectNode, spatialDistance);
                                } else {
                                    // the two nodes intersect, skip
                                }

                                if (spatialRelationList != null && spatialRelationList.size() > 0) {
                                    for (SugiliteRelation spatialRelation : spatialRelationList) {
                                        if (spatialRelationMap.containsKey(spatialRelation)) {  // if there has been a node added to this direction
                                            Double currentClosestDistance = spatialDistanceMap.get(spatialRelationMap.get(spatialRelation));
                                            if (currentClosestDistance != null && currentClosestDistance >= spatialDistance) {  // see if the new spatial distance is smaller
                                                spatialRelationMap.put(spatialRelation, objectNode);  // if so, update
                                            }
                                        } else {
                                            spatialRelationMap.put(spatialRelation, objectNode);  // if there has not been a node in this direction, add it to the map directly
                                        }
                                    }
                                }
                            }
                        }
                    }
                    //  3. after calculating with all other nodes, we can store the spatial relations for current node to uisnapshot
                    if (!spatialRelationMap.isEmpty()) {
                        for (SugiliteRelation spatialRelation : spatialRelationMap.keySet()) {
                            addEntityNodeTriple(currentEntity, spatialRelationMap.get(spatialRelation), spatialRelation);
                        }
                    }

                }

            }

            for(Map.Entry<Node, SugiliteEntity<Node>> entry : nodeSugiliteEntityMap.entrySet()){
                SugiliteEntity currentEntity = entry.getValue();
                // TODO: add order in list info
                ListOrderResolver listOrderResolver = new ListOrderResolver();
                Set<SugiliteTriple> triples = subjectPredicateTriplesMap.get(new AbstractMap.SimpleEntry<>(currentEntity.getEntityId(), SugiliteRelation.HAS_CHILD.getRelationId()));
                Set<Node> childNodes = new LinkedHashSet<>();
                if(triples != null) {
                    for (SugiliteTriple triple : triples) {
                        Node child = (Node)triple.getObject().getEntityValue();
                        Rect rect = Rect.unflattenFromString(child.getBoundsInScreen());
                        int size = rect.width() * rect.height();
                        if (size > 0) {
                            childNodes.add(child);
                        }

                    }

                    if (listOrderResolver.isAList(entry.getKey(), childNodes)) {
                        addEntityBooleanTriple(currentEntity, true, SugiliteRelation.IS_A_LIST);
                        addOrderForChildren(childNodes);
                    }
                }
            }




        }

    }

    private void addOrderForChildren(Iterable<Node> children){
        //add list order for list items
        List<Map.Entry<Node, Integer>> childNodeYValueList = new ArrayList<>();
        for(Node childNode : children){
            childNodeYValueList.add(new AbstractMap.SimpleEntry<>(childNode, Integer.valueOf(childNode.getBoundsInScreen().split(" ")[1])));
        }
        childNodeYValueList.sort(new Comparator<Map.Entry<Node, Integer>>() {
            @Override
            public int compare(Map.Entry<Node, Integer> o1, Map.Entry<Node, Integer> o2) {
                return o1.getValue() - o2.getValue();
            }
        });
        int counter = 0;
        for(Map.Entry<Node, Integer> entry : childNodeYValueList){
            counter ++;
            Node childNode = entry.getKey();

            //addEntityStringTriple(nodeSugiliteEntityMap.get(childNode), String.valueOf(counter), SugiliteRelation.HAS_LIST_ORDER);
            addEntityNumericTriple(nodeSugiliteEntityMap.get(childNode), Double.valueOf(counter), SugiliteRelation.HAS_LIST_ORDER);

            SugiliteEntity<Node> childEntity = nodeSugiliteEntityMap.get(childNode);
            if(childEntity != null){
                for(SugiliteEntity<Node> entity : getAllChildEntities(childEntity, new LinkedHashSet<>())){
                    //addEntityStringTriple(entity, String.valueOf(counter), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                    addEntityNumericTriple(entity, Double.valueOf(counter), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                }
            }
        }
    }


    private Set<SugiliteEntity<Node>> getAllChildEntities(SugiliteEntity<Node> node, Set<SugiliteEntity<Node>> coveredNodes){
        Set<SugiliteEntity<Node>> results = new LinkedHashSet<>();
        Set<SugiliteTriple> triples = subjectPredicateTriplesMap.get(new AbstractMap.SimpleEntry<>(node.getEntityId(), SugiliteRelation.HAS_CHILD.getRelationId()));
        if(triples != null) {
            for (SugiliteTriple triple : triples) {
                if (triple.getObject().getEntityValue() instanceof Node && (!results.contains(triple.getObject()))) {
                    if(coveredNodes.contains(triple.getObject())){
                        continue;
                    }
                    results.add(triple.getObject());
                    coveredNodes.add(triple.getObject());
                    results.addAll(getAllChildEntities(triple.getObject(), coveredNodes));
                }
            }
        }
        coveredNodes.addAll(results);
        return results;
    }

    private static String cleanUpString(String string){
        return string.replace("(", "").replace(")", "").replace("\"", "");
    }


    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<String>, SugiliteRelation) triple
     * @param currentEntity
     * @param string
     * @param relation
     */
    public void addEntityStringTriple(SugiliteEntity currentEntity, String string, SugiliteRelation relation){
        //class
        SugiliteEntity<String> objectEntity = null;

        //clean up the string
        string = cleanUpString(string);

        if (stringSugiliteEntityMap.containsKey(string)) {
            objectEntity = stringSugiliteEntityMap.get(string);
        } else {
            //create a new entity for the class name
            SugiliteEntity<String> entity = new SugiliteEntity<>(entityIdCounter++, String.class, string);
            synchronized (stringSugiliteEntityMap) {
                stringSugiliteEntityMap.put(string, entity);
            }
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringValue(string);
        synchronized (this) {
            addTriple(triple);
        }
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<List<String>>, SugiliteRelation) triple
     * @param currentEntity
     * @param stringList
     * @param relation
     */
    public void addEntityStringListTriple(SugiliteEntity currentEntity, List<String> stringList, SugiliteRelation relation){
        //class
        SugiliteEntity<List> objectEntity = null;

        //clean up the strings
        for (int i = 0; i < stringList.size(); i++) {
            stringList.set(i, cleanUpString(stringList.get(i)));
        }

        if (stringListSugiliteEntityMap.containsKey(stringList)) {
            objectEntity = stringListSugiliteEntityMap.get(stringList);
        } else {
            //create a new entity for the class name
            SugiliteEntity<List> entity = new SugiliteEntity<>(entityIdCounter++, List.class, stringList);
            synchronized (stringListSugiliteEntityMap) {
                stringListSugiliteEntityMap.put(stringList, entity);
            }
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringListValue(stringList);
        synchronized (this) {
            addTriple(triple);
        }
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Double>, SugiliteRelation) triple
     * @param currentEntity
     * @param numeric
     * @param relation
     */
    private void addEntityNumericTriple(SugiliteEntity currentEntity, Double numeric, SugiliteRelation relation){
        //class
        SugiliteEntity<Double> objectEntity = null;

        if (doubleSugiliteEntityMap.containsKey(numeric)) {
            objectEntity = doubleSugiliteEntityMap.get(numeric);
        } else {
            //create a new entity for the class name
            SugiliteEntity<Double> entity = new SugiliteEntity<>(entityIdCounter++, Double.class, numeric);
            doubleSugiliteEntityMap.put(numeric, entity);
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringValue(numeric.toString());
        addTriple(triple);
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Boolean>, SugiliteRelation) triple
     * @param currentEntity
     * @param bool
     * @param relation
     */
    private void addEntityBooleanTriple(SugiliteEntity currentEntity, Boolean bool, SugiliteRelation relation){
        //class
        SugiliteEntity<Boolean> objectEntity = null;

        if (booleanSugiliteEntityMap.containsKey(bool)) {
            objectEntity = booleanSugiliteEntityMap.get(bool);
        } else {
            //create a new entity for the class name
            SugiliteEntity<Boolean> entity = new SugiliteEntity<>(entityIdCounter++, Boolean.class, bool);
            booleanSugiliteEntityMap.put(bool, entity);
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        triple.setObjectStringValue(bool.toString());
        addTriple(triple);
    }

    /**
     * helper function used for adding a <SugiliteEntity, SugiliteEntity<Node>, SugiliteRelation) triple
     * @param currentEntity
     * @param node
     * @param relation
     */
    private void addEntityNodeTriple(SugiliteEntity currentEntity, Node node, SugiliteRelation relation){
        //class
        SugiliteEntity<Node> objectEntity = null;

        if (nodeSugiliteEntityMap.containsKey(node)) {
            objectEntity = nodeSugiliteEntityMap.get(node);
        } else {
            //create a new entity for the class name
            SugiliteEntity<Node> entity = new SugiliteEntity<Node>(entityIdCounter++, Node.class, node);
            nodeSugiliteEntityMap.put(node, entity);
            objectEntity = entity;
        }

        SugiliteTriple triple = new SugiliteTriple(currentEntity, relation, objectEntity);
        addTriple(triple);
    }


    /**
     * helper function to figure out the spatial relation between a pair of Nodes
     * @param subjectNode
     * @param objectNode
     */
    private List<SugiliteRelation> getSpatialRelationBetweenNodes(Node subjectNode, Node objectNode) {
        Rect subjectRect = Rect.unflattenFromString(subjectNode.getBoundsInScreen());
        Rect objectRect = Rect.unflattenFromString(objectNode.getBoundsInScreen());

        // check for intersect first
        if (subjectRect.intersect(objectRect)) {
            return null;
        } else {
            // make sure the top/bottom or left/right are not flipped
            subjectRect.sort();
            objectRect.sort();

            Integer subjectLeft = subjectRect.left;
            Integer subjectRight = subjectRect.right;
            Integer subjectBottom = subjectRect.bottom;
            Integer subjectTop = subjectRect.top;

            Integer objectLeft = objectRect.left;
            Integer objectRight = objectRect.right;
            Integer objectBottom = objectRect.bottom;
            Integer objectTop = objectRect.top;

            List<SugiliteRelation> resultRelationList = new ArrayList<>();
            if ( subjectLeft >= objectRight ) resultRelationList.add(SugiliteRelation.RIGHT);   // subject is to the right of object
            if ( subjectRight <= objectLeft ) resultRelationList.add(SugiliteRelation.LEFT);    // subject is to the left of object
            if ( subjectBottom <= objectTop ) resultRelationList.add(SugiliteRelation.ABOVE);   // subject is above the object
            if ( subjectTop >= objectBottom ) resultRelationList.add(SugiliteRelation.BELOW);   // subject is below the object

            return resultRelationList;
        }

    }

    /**
     * helper function to figure out the spatial distance between a pair of Nodes
     * @param referenceNode
     * @param secondNode
     */
    private Double getSpatialDistanceBetweenNodes(Node referenceNode, Node secondNode) {
        Rect referenceRect = Rect.unflattenFromString(referenceNode.getBoundsInScreen());
        Rect secondRect = Rect.unflattenFromString(secondNode.getBoundsInScreen());

        if (referenceRect.intersect(secondRect)) {
            return null;
        } else {
         Integer referenceXCenter = referenceRect.centerX();
         Integer referenceYCenter = referenceRect.centerY();
         Integer secondXCenter = secondRect.centerX();
         Integer secondYCenter = secondRect.centerY();

         return Math.sqrt( (referenceXCenter - secondXCenter) * (referenceXCenter - secondXCenter) + (referenceYCenter - secondYCenter) * (referenceYCenter - secondYCenter) );
        }
    }


    public void update(AccessibilityEvent event){
        // TODO: update the UI snapshot based on an event
    }

    public void update(AccessibilityNodeInfo rootNode){
        //TODO: update the UI snapshot basd on a rootNode
    }

    //add a triple to the UI snapshot
    private void addTriple(SugiliteTriple triple){
        triples.add(triple);

        //fill in the indexes for triples
        if(!subjectTriplesMap.containsKey(triple.getSubject().getEntityId())){
            subjectTriplesMap.put(triple.getSubject().getEntityId(), new LinkedHashSet<>());
        }
        if(!predicateTriplesMap.containsKey(triple.getPredicate().getRelationId())){
            predicateTriplesMap.put(triple.getPredicate().getRelationId(), new LinkedHashSet<>());
        }
        if(!objectTriplesMap.containsKey(triple.getObject().getEntityId())){
            objectTriplesMap.put(triple.getObject().getEntityId(), new LinkedHashSet<>());
        }

        if(!subjectPredicateTriplesMap.containsKey(new AbstractMap.SimpleEntry<>(triple.getSubject().getEntityId(), triple.getPredicate().getRelationId()))){
            subjectPredicateTriplesMap.put(new AbstractMap.SimpleEntry<>(triple.getSubject().getEntityId(), triple.getPredicate().getRelationId()), new LinkedHashSet<>());
        }

        subjectTriplesMap.get(triple.getSubject().getEntityId()).add(triple);
        predicateTriplesMap.get(triple.getPredicate().getRelationId()).add(triple);
        objectTriplesMap.get(triple.getObject().getEntityId()).add(triple);
        subjectPredicateTriplesMap.get(new AbstractMap.SimpleEntry<>(triple.getSubject().getEntityId(), triple.getPredicate().getRelationId())).add(triple);

        //fill in the two indexes for entities and relations
        if(!sugiliteEntityIdSugiliteEntityMap.containsKey(triple.getSubject().getEntityId())){
            sugiliteEntityIdSugiliteEntityMap.put(triple.getSubject().getEntityId(), triple.getSubject());
        }
        if(!sugiliteRelationIdSugiliteRelationMap.containsKey(triple.getPredicate().getRelationId())){
            sugiliteRelationIdSugiliteRelationMap.put(triple.getPredicate().getRelationId(), triple.getPredicate());
        }
        if(!sugiliteEntityIdSugiliteEntityMap.containsKey(triple.getObject().getEntityId())){
            sugiliteEntityIdSugiliteEntityMap.put(triple.getObject().getEntityId(), triple.getObject());
        }

    }

    public void removeTriple(SugiliteTriple triple){
        triples.remove(triple);

        if(subjectTriplesMap.get(triple.getSubject().getEntityId()) != null){
            subjectTriplesMap.get(triple.getSubject().getEntityId()).remove(triple);
        }

        if(predicateTriplesMap.get(triple.getPredicate().getRelationId()) != null){
            predicateTriplesMap.get(triple.getPredicate().getRelationId()).remove(triple);
        }

        if(objectTriplesMap.get(triple.getObject().getEntityId()) != null){
            objectTriplesMap.get(triple.getObject().getEntityId()).remove(triple);
        }

    }

    public Map<Integer, SugiliteEntity> getSugiliteEntityIdSugiliteEntityMap() {
        return sugiliteEntityIdSugiliteEntityMap;
    }

    public Map<Integer, SugiliteRelation> getSugiliteRelationIdSugiliteRelationMap() {
        return sugiliteRelationIdSugiliteRelationMap;
    }

    public Map<Integer, Set<SugiliteTriple>> getSubjectTriplesMap() {
        return subjectTriplesMap;
    }

    public Map<Integer, Set<SugiliteTriple>> getObjectTriplesMap() {
        return objectTriplesMap;
    }

    public Set<SugiliteTriple> getTriples() {
        return triples;
    }

    public Map<Integer, Set<SugiliteTriple>> getPredicateTriplesMap() {
        return predicateTriplesMap;
    }

    public Integer getEntityIdCounter() {
        return entityIdCounter;
    }

    public Map<Node, SugiliteEntity<Node>> getNodeSugiliteEntityMap() {
        return nodeSugiliteEntityMap;
    }

    public Map<String, SugiliteEntity<String>> getStringSugiliteEntityMap() {
        return stringSugiliteEntityMap;
    }

    public Map<Boolean, SugiliteEntity<Boolean>> getBooleanSugiliteEntityMap() {
        return booleanSugiliteEntityMap;
    }

    public Map<Node, AccessibilityNodeInfo> getNodeAccessibilityNodeInfoMap() {
        return nodeAccessibilityNodeInfoMap;
    }

    public SugiliteEntity<Node> getEntityWithAccessibilityNode(AccessibilityNodeInfo matchedAccessibilityNode) {
        String stringDifferences = "";
        Node matchedNode = null;
        for(Map.Entry<Node, AccessibilityNodeInfo> accessibilityNodeInfo: nodeAccessibilityNodeInfoMap.entrySet()) {
            if(Objects.equals(accessibilityNodeInfo.getValue().toString().trim(), matchedAccessibilityNode.toString().trim())) {
                matchedNode = accessibilityNodeInfo.getKey();
            } else {
                stringDifferences = StringUtils.difference(accessibilityNodeInfo.toString().trim(), matchedAccessibilityNode.toString().trim());
            }
        }
        if (matchedNode != null) {
            return nodeSugiliteEntityMap.get(matchedNode);
        } else {
            return null;
        }

    }

    public SugiliteEntity<Node> getEntityWithNode(Node targetNode) {
        if (targetNode != null) {
            return nodeSugiliteEntityMap.get(targetNode);
        } else {
            return null;
        }

    }

    public Map<Map.Entry<Integer, Integer>, Set<SugiliteTriple>> getSubjectPredicateTriplesMap() {
        return subjectPredicateTriplesMap;
    }


    /**
     *
     * @param parent parent node
     * @param toConstructNodeAccessibilityNodeInfoMap whether to populate nodeAccessibilityNodeInfoMap while traversing
     * @param windowZIndex the z index of the window
     * @param parentNodeZIndexSequence the z index sequence of the parent node
     * @return
     */
    private List<Node> preOrderNodeTraverseWithZIndex(AccessibilityNodeInfo parent, boolean toConstructNodeAccessibilityNodeInfoMap, Integer windowZIndex, List<Integer> parentNodeZIndexSequence, String externalPackageName, String externalActivityName){
        //fill in the Z-index for nodes recursively
        if(parent == null) {
            return null;
        }
        List<Node> list = new ArrayList<>();
        Node node = new Node(parent, windowZIndex, parentNodeZIndexSequence, externalPackageName != null && externalPackageName.equals(parent.getPackageName()) ? externalActivityName : null);
        if(toConstructNodeAccessibilityNodeInfoMap){
            nodeAccessibilityNodeInfoMap.put(node, parent);
        }
        list.add(node);
        int childCount = parent.getChildCount();
        for(int i = 0; i < childCount; i ++){
            AccessibilityNodeInfo childNode = parent.getChild(i);
            if(childNode != null) {
                list.addAll(preOrderNodeTraverseWithZIndex(childNode, toConstructNodeAccessibilityNodeInfoMap, windowZIndex, node.getNodeZIndexSequence(), externalPackageName, externalActivityName));
            }
        }
        return list;
    }

    public Set<String> getStringValuesForObjectEntityAndRelation(SugiliteEntity objectEntity, SugiliteRelation relation) {
        Set<String> result = new LinkedHashSet<>();
        Integer subjectEntityId = objectEntity.getEntityId();
        if (subjectTriplesMap.get(subjectEntityId) != null) {
            Set<SugiliteTriple> triples = new HashSet<>(subjectTriplesMap.get(subjectEntityId));    // here we get a copy of the original Map to avoid changing the original stored values
            triples.removeIf(t -> !t.getPredicate().equals(relation));
            for (SugiliteTriple t : triples) {
                if (t.getObject() != null & t.getObject().getEntityValue() != null) {
                    if (t.getObject().getEntityValue() instanceof String) {
                        result.add((String) t.getObject().getEntityValue());
                    }
                }
            }
        } else {
            Log.e("getStringValuesForObjectEntityAndRelation", "subjectTriplesMap.get(subjectEntityId) is null");
        }

        return result;
    }

    public Set<List> getListValuesForObjectEntityAndRelation(SugiliteEntity objectEntity, SugiliteRelation relation) {
        Set<List> result = new LinkedHashSet<>();
        Integer subjectEntityId = objectEntity.getEntityId();
        if (subjectTriplesMap.get(subjectEntityId) != null) {
            Set<SugiliteTriple> triples = new HashSet<>(subjectTriplesMap.get(subjectEntityId));
            triples.removeIf(t -> !t.getPredicate().equals(relation));
            for (SugiliteTriple t : triples) {
                if (t.getObject() != null & t.getObject().getEntityValue() != null) {
                    if (t.getObject().getEntityValue() instanceof List) {
                        result.add((List) t.getObject().getEntityValue());
                    }
                }
            }
        } else {
            Log.e("getStringValuesForObjectEntityAndRelation", "subjectTriplesMap.get(subjectEntityId) is null");
        }

        return result;
    }

    public Set<Node> getNodeValuesForSubjectEntityAndRelation(SugiliteEntity subjectEntity, SugiliteRelation relation) {
        Set<Node> result = new LinkedHashSet<>();
        Integer subjectEntityId = subjectEntity.getEntityId();
        if (subjectTriplesMap.get(subjectEntityId) != null) {
            Set<SugiliteTriple> triples = new HashSet<>(subjectTriplesMap.get(subjectEntityId));
            triples.removeIf(t -> !t.getPredicate().equals(relation));
            for (SugiliteTriple t : triples) {
                if (t.getObject() != null & t.getObject().getEntityValue() != null) {
                    if (t.getObject().getEntityValue() instanceof Node) {
                        result.add((Node) t.getObject().getEntityValue());
                    }
                }
            }
        } else {
            Log.e("getStringValuesForObjectEntityAndRelation", "subjectTriplesMap.get(subjectEntityId) is null");
        }

        return result;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getActivityName() {
        return activityName;
    }
}
