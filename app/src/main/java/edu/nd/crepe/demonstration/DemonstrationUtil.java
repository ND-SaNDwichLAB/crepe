package edu.nd.crepe.demonstration;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityNodeInfo;

//import edu.nd.crepe.graphquery.ontology.CombinedOntologyQuery;
//import edu.nd.crepe.graphquery.ontology.LeafOntologyQuery;
//import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import androidx.annotation.RequiresApi;

import edu.nd.crepe.servicemanager.CrepeAccessibilityService;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.graphquery.automation.AutomatorUtil;
import edu.nd.crepe.graphquery.model.Node;
import edu.nd.crepe.graphquery.ontology.CombinedOntologyQuery;
import edu.nd.crepe.graphquery.ontology.LeafOntologyQuery;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import edu.nd.crepe.graphquery.ontology.SugiliteEntity;
import edu.nd.crepe.graphquery.ontology.SugiliteRelation;
import edu.nd.crepe.graphquery.ontology.SugiliteTriple;
import edu.nd.crepe.graphquery.ontology.UISnapshot;

import org.apache.commons.lang3.StringUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author toby
 * @date 1/7/19
 * @time 2:44 PM
 */
public class DemonstrationUtil {
    /**
     * initiate a demonstration recording -> need to call endRecording() when the recording ends
     * @param context
     * @param fullScreenOverlayManager
     * @param widgetDisplay
     */
    public static void initiateDemonstration(Context context, FullScreenOverlayManager fullScreenOverlayManager, WidgetDisplay widgetDisplay){

        // turn on the cat overlay to prepare for demonstration
        if(fullScreenOverlayManager != null){
            // if it's not currently showing the overlay
            if(!fullScreenOverlayManager.getShowingOverlay()) {
                fullScreenOverlayManager.enableOverlay(widgetDisplay);
            } else {
                fullScreenOverlayManager.disableOverlay();
            }

        }
    }

    /**
     * traverse a tree from the root, and return all the nodes in the tree
     * @param root
     * @return
     */
    public static List<AccessibilityNodeInfo> preOrderTraverse(AccessibilityNodeInfo root){
        if(root == null)
            return null;
        List<AccessibilityNodeInfo> list = new ArrayList<>();
        list.add(root);
        int childCount = root.getChildCount();
        for(int i = 0; i < childCount; i++){
            AccessibilityNodeInfo node = root.getChild(i);
            if(node != null)
                list.addAll(preOrderTraverse(node));
        }
        return list;
    }

    /**
     * Find the clicked node based on screen position
     * @param allNodeList
     * @param clickX
     * @param clickY
     * @return
     */
    public static List<AccessibilityNodeInfo> findMatchingNodeFromClick(List<AccessibilityNodeInfo> allNodeList, float clickX, float clickY){
        if(allNodeList != null && allNodeList.size() == 0 || allNodeList == null) return null;

        List<AccessibilityNodeInfo> matchingList = new ArrayList<>();


            for (AccessibilityNodeInfo node : allNodeList) {
                Rect nodeBoundingBox = new Rect();
                node.getBoundsInScreen(nodeBoundingBox);
                if (nodeBoundingBox.contains(Math.round(clickX), Math.round(clickY))) {
                    matchingList.add(node);
                }
            }

        return matchingList;
    }

    /**
     * Get the sibling node closest to the matched node from the above function
     * @param matchedNode
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static AccessibilityNodeInfo findClosestSiblingNode(AccessibilityNodeInfo matchedNode) {
        // get the text of the matching node
        String matchedNodeText = String.valueOf(matchedNode.getText());
        List<String> siblingNodeTextList = new ArrayList<>();

        // get information about matched node's sibling nodes
        AccessibilityNodeInfo parentNode = matchedNode.getParent();
        // use a variable to store the current closest node and its distance to the matched node
        // initialize it as infinity so the first comparison is easier
        double closestDistToMatchedNode = Double.POSITIVE_INFINITY;
        AccessibilityNodeInfo closestNode = new AccessibilityNodeInfo();

        if (parentNode != null) {
            int siblingCnt = parentNode.getChildCount() - 1;
            if (siblingCnt > 0) {
                // get the center position of the matched node
                Rect matchedNodeRect = new Rect();
                matchedNode.getBoundsInScreen(matchedNodeRect);
                int matchedCenterX = matchedNodeRect.centerX();
                int matchedCenterY = matchedNodeRect.centerY();

                // the loop here calls for siblingCnt + 1, because the matched node itself is in the tree
                for(int i = 0; i < siblingCnt + 1; i++) {
                    AccessibilityNodeInfo siblingNode = parentNode.getChild(i);

                    // get the center position of the sibling node
                    Rect siblingNodeRect = new Rect();
                    siblingNode.getBoundsInScreen(siblingNodeRect);
                    CharSequence siblingNodeTextChar = siblingNode.getText();
                    // we only compare if the current sibling node is not the matched node itself
                    if (!siblingNodeRect.equals(matchedNodeRect) && siblingNodeTextChar != null && !siblingNodeTextChar.toString().isEmpty()) {
                        // ge the position of the sibling node
                        int siblingCenterX = siblingNodeRect.centerX();
                        int siblingCenterY = siblingNodeRect.centerY();
                        // get the distance
                        double currentSiblingDist = Math.hypot(siblingCenterX - matchedCenterX, siblingCenterY - matchedCenterY);

                        if (currentSiblingDist < closestDistToMatchedNode) {
                            closestNode = siblingNode;
                        }
                        Log.d("uisnapshot", "Sibling node text: " + siblingNode.getText().toString());
                        siblingNodeTextList.add(siblingNode.getText().toString());
                    }
                }
                if(siblingNodeTextList.size() > 0) {
                    Log.d("uisnapshot", siblingNodeTextList.toString());
                    Log.d("uisnapshot", "The closest node contains text: " + closestNode.getText());
                } else {
                    // If all sibling nodes' text fields are empty, we also arrive at the no sibling situation
                    // TODO if there's no sibling, just use the screen position of the node
                    Log.d("uisnapshot", "All siblings' text fields are empty, same as no sibling");
                }
            } else {
                // if there's no sibling, just use the screen position of the node to construct query
                Log.d("uisnapshot", "No sibling");
            }
        } else {
            Log.e("uisnapshot", "Parent of the matching node is null, cannot find siblings");
        }

        return closestNode;
    }

    public static Rect getBoundingBoxOfClickedItem(float clickX, float clickY) {
        Rect resultBounds = new Rect();
        // create uiSnapshot for current screen
        UISnapshot uiSnapshot = CrepeAccessibilityService.getsSharedInstance().generateUISnapshot();

        List<AccessibilityNodeInfo> matchedAccessibilityNodeList = CrepeAccessibilityService.getsSharedInstance().getMatchingNodeFromClickWithText(clickX, clickY);

        SugiliteEntity<Node> targetEntity = new SugiliteEntity<>();
        AccessibilityNodeInfo matchedNode;

        if(matchedAccessibilityNodeList != null && matchedAccessibilityNodeList.size() == 1) {
            matchedNode = matchedAccessibilityNodeList.get(0);
            matchedNode.getBoundsInScreen(resultBounds);
            return resultBounds;
        } else {
            return null;
        }
    }

    public static List<Pair<OntologyQuery, Double>> processOverlayClick(float clickX, float clickY) {

        // create uiSnapshot for current screen
        UISnapshot uiSnapshot = CrepeAccessibilityService.getsSharedInstance().generateUISnapshot();
        // get the matched node

        List<AccessibilityNodeInfo> matchedAccessibilityNodeList = CrepeAccessibilityService.getsSharedInstance().getMatchingNodeFromClickWithText(clickX, clickY);
        // this matchedAccessibilityNode is an AccessibilityNodeInfo, which is not exactly the node stored in the screen's nodeSugiliteEntityMap.
        // We retrieved that stored node from this screen's uisnapshot

        SugiliteEntity<Node> targetEntity = new SugiliteEntity<>();
        AccessibilityNodeInfo matchedNode;

        if(matchedAccessibilityNodeList != null && matchedAccessibilityNodeList.size() == 1) {
            matchedNode = matchedAccessibilityNodeList.get(0);
            targetEntity = uiSnapshot.getEntityWithAccessibilityNode(matchedNode);
        } else {
            // TODO: Find the node that we actually need
        }


        List<Pair<OntologyQuery, Double>> defaultQueries = null;
        // temporarily change this to a list to store duplicate values from different query results
        List<Set<SugiliteEntity>> results = new ArrayList<>();
        if(targetEntity != null) {
            SugiliteRelation[] relationsToExclude = new SugiliteRelation[1];
            relationsToExclude[0] = SugiliteRelation.HAS_TEXT;
            defaultQueries = generateDefaultQueries(uiSnapshot, targetEntity, relationsToExclude);
        } else {
            Log.e("generate queries", "Cannot find the tapped entity!");
        }

        Log.i("defaultQueries", defaultQueries.stream()
                .map(pair -> pair.first.toString())
                .collect(Collectors.joining("\n")));

        // test if the queries can retrieve components on screen
        if(defaultQueries != null) {
            for(Pair<OntologyQuery, Double> query : defaultQueries) {
                Set<SugiliteEntity> queryResult = query.first.executeOn(uiSnapshot);
                results.add(queryResult);
            }
        }

        return defaultQueries;

    }

    public static String removeScriptExtension (String scriptName) {
        if (scriptName.endsWith(".SugiliteScript")) {
            return scriptName.replace(".SugiliteScript", "");
        } else {
            return scriptName;
        }
    }

    public static String addScriptExtension (String scriptName) {
        if (scriptName.endsWith(".SugiliteScript")) {
            return scriptName;
        } else {
            return scriptName + ".SugiliteScript";
        }
    }

    public static String joinListGrammatically(final List<String> list, String lastWordSeparator) {
        if (list.size() == 0) {
            return "";
        }
        return list.size() > 1
                ? StringUtils.join(list.subList(0, list.size() - 1), ", ")
                .concat(String.format("%s %s ", list.size() > 2 ? "," : "", lastWordSeparator))
                .concat(list.get(list.size() - 1))
                : list.get(0);
    }

    public static String boldify(String string){
        return "<b>" + string + "</b>";
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }
    public static boolean isInputMethodPackageName (String packageName) {
        Set<String> inputMethodNames = new HashSet<>(Arrays.asList(Const.INPUT_METHOD_PACKAGE_NAMES));
        return inputMethodNames.contains(packageName);
    }


    public static List<Pair<OntologyQuery, Double>> generateDefaultQueries(UISnapshot uiSnapshot, SugiliteEntity<Node> targetEntity, SugiliteRelation... relationsToExcludeArray){
        Set<SugiliteRelation> relationsToExclude = new HashSet<>(Arrays.asList(relationsToExcludeArray));
        //generate parent query
        List<Pair<OntologyQuery, Double>> queries = new ArrayList<>();
        CombinedOntologyQuery andQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
        CombinedOntologyQuery prevQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.PREV);
        boolean hasNonBoundingBoxFeature = false;
        boolean hasNonChildFeature = false;

        SugiliteEntity<Node> foundEntity = targetEntity;


        // generate sub queries -- add the packageName and className constraints to q

        if (! relationsToExclude.contains(SugiliteRelation.HAS_PACKAGE_NAME)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME)) != null) {
                //add packageName
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_PACKAGE_NAME);
                andQuery.addSubQuery(subQuery); // note that here instead of adding to the function's output variable queries, we add to the andQuery,
                                                // because for each query candidate, we want to always add the packageName and className constraints
                                                // this is only true for HAS_PACKAGE_NAME and HAS_CLASS_NAME
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_CLASS_NAME)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CLASS_NAME)) != null) {
                //add className
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CLASS_NAME))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_CLASS_NAME);
                andQuery.addSubQuery(subQuery); // note that here instead of adding to the function's output variable queries, we add to the andQuery,
                                                // because for each query candidate, we want to always add the packageName and className constraints
                                                // this is only true for HAS_PACKAGE_NAME and HAS_CLASS_NAME
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_TEXT)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_TEXT)) != null) {
                //add a text query
                CombinedOntologyQuery clonedQuery = andQuery.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_TEXT))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_TEXT);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;
                queries.add(Pair.create(clonedQuery, 0.1));
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_PARENT_TEXT)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PARENT_TEXT)) != null) {
                CombinedOntologyQuery clonedQuery = andQuery.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PARENT_TEXT))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_TEXT);
//                andQuery.addSubQuery(subQuery);
                queries.add(Pair.create(clonedQuery, 1.0));
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_CONTENT_DESCRIPTION)) {
            if ((getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CONTENT_DESCRIPTION)) != null) &&
                    (!getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CONTENT_DESCRIPTION)).equals(getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_TEXT))))) {
                //add content description
                CombinedOntologyQuery clonedQuery = andQuery.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CONTENT_DESCRIPTION))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_CONTENT_DESCRIPTION);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;
                queries.add(Pair.create(clonedQuery, 1.2));
            }
        }

//        if (! relationsToExclude.contains(SugiliteRelation.HAS_SIBLING_TEXT)) {
//            if (getValueIfOnlyOneObject(uiSnapshot.getListValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_SIBLING_TEXT)) != null) {
//                CombinedOntologyQuery clonedQuery = andQuery.clone();
//                LeafOntologyQuery subQuery = new LeafOntologyQuery();
//                Set<SugiliteEntity> object = new HashSet<>();
//                object.add(new SugiliteEntity(-1, List.class, getValueIfOnlyOneObject(uiSnapshot.getListValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_SIBLING_TEXT))));
//                subQuery.setObjectSet(object);
//                subQuery.setQueryFunction(SugiliteRelation.HAS_SIBLING_TEXT);
//                clonedQuery.addSubQuery(subQuery);
//                queries.add(Pair.create(clonedQuery, 1.0));
//            }
//        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_VIEW_ID)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_VIEW_ID)) != null) {
                //add view id
                CombinedOntologyQuery clonedQuery = andQuery.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_VIEW_ID))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_VIEW_ID);
                clonedQuery.addSubQuery(subQuery);
                hasNonBoundingBoxFeature = true;
                hasNonChildFeature = true;

                if (targetEntity.getEntityValue().getEditable()) {
                    //prioritize view id for text boxes
                    queries.add(Pair.create(clonedQuery, 1.0));
                } else {
                    queries.add(Pair.create(clonedQuery, 3.2));
                }
            }
        }


        if(foundEntity != null && uiSnapshot != null){
            //add list order
            if (! relationsToExclude.contains(SugiliteRelation.HAS_LIST_ORDER)) {
                Set<SugiliteTriple> triples = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(foundEntity.getEntityId(), SugiliteRelation.HAS_LIST_ORDER.getRelationId()));
                if (triples != null) {
                    for (SugiliteTriple triple : triples) {
                        String order = triple.getObject().getEntityValue().toString();

                        CombinedOntologyQuery clonedQuery = andQuery.clone();
                        LeafOntologyQuery subQuery = new LeafOntologyQuery();
                        Set<SugiliteEntity> object = new HashSet<>();
                        object.add(new SugiliteEntity(-1, String.class, order));
                        subQuery.setObjectSet(object);
                        subQuery.setQueryFunction(SugiliteRelation.HAS_LIST_ORDER);
                        clonedQuery.addSubQuery(subQuery);
                        hasNonBoundingBoxFeature = true;
                        hasNonChildFeature = true;
                        queries.add(Pair.create(clonedQuery, 3.0));
                    }
                }
            }

            if (! relationsToExclude.contains(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER)) {
                Set<SugiliteTriple> triples2 = uiSnapshot.getSubjectPredicateTriplesMap().get(new AbstractMap.SimpleEntry<>(foundEntity.getEntityId(), SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER.getRelationId()));
                if (triples2 != null) {
                    for (SugiliteTriple triple : triples2) {
                        Double order = (Double) triple.getObject().getEntityValue();

                        CombinedOntologyQuery clonedQuery = andQuery.clone();
                        LeafOntologyQuery subQuery = new LeafOntologyQuery();
                        Set<SugiliteEntity> object = new HashSet<>();
                        object.add(new SugiliteEntity(-1, Double.class, order));
                        subQuery.setObjectSet(object);
                        subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_WITH_LIST_ORDER);
                        clonedQuery.addSubQuery(subQuery);
                        hasNonBoundingBoxFeature = true;
                        hasNonChildFeature = true;
                        queries.add(Pair.create(clonedQuery, 0.1));
                    }
                }
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_CHILD_TEXT)) {
            //add child text
            List<String> childTexts = new ArrayList<>(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_CHILD_TEXT));
            if (childTexts != null ) {
                if (childTexts.size() > 0) {
                    int count = 0;
                    double score = 2.01 + (((double) (count++)) / (double) childTexts.size());
                    //TODO: in case of multiple childText queries, get all possible combinations

                    for (String childText : childTexts) {
                        if (childText != null && !childText.equals(relationsToExclude.contains(SugiliteRelation.HAS_TEXT))) {
                            CombinedOntologyQuery clonedQuery = andQuery.clone();
                            LeafOntologyQuery subQuery = new LeafOntologyQuery();
                            Set<SugiliteEntity> object = new HashSet<>();
                            object.add(new SugiliteEntity(-1, String.class, childText));
                            subQuery.setObjectSet(object);
                            subQuery.setQueryFunction(SugiliteRelation.HAS_CHILD_TEXT);
                            clonedQuery.addSubQuery(subQuery);
                            double newScore = score;
                            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME)) != null
                                    && AutomatorUtil.isHomeScreenPackage(getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PACKAGE_NAME)))) {
                                newScore = score - 1;
                            }
                            queries.add(Pair.create(clonedQuery, newScore));
                            hasNonBoundingBoxFeature = true;
                        }
                    }
                }

            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_SCREEN_LOCATION)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_SCREEN_LOCATION)) != null) {
                CombinedOntologyQuery clonedQuery = andQuery.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_SCREEN_LOCATION))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_SCREEN_LOCATION);
                clonedQuery.addSubQuery(subQuery);
                queries.add(Pair.create(clonedQuery, 100.0));
            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.HAS_PARENT_LOCATION)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PARENT_LOCATION)) != null) {
                CombinedOntologyQuery clonedQuery = andQuery.clone();
                LeafOntologyQuery subQuery = new LeafOntologyQuery();
                Set<SugiliteEntity> object = new HashSet<>();
                object.add(new SugiliteEntity(-1, String.class, getValueIfOnlyOneObject(uiSnapshot.getStringValuesForObjectEntityAndRelation(targetEntity, SugiliteRelation.HAS_PARENT_LOCATION))));
                subQuery.setObjectSet(object);
                subQuery.setQueryFunction(SugiliteRelation.HAS_PARENT_LOCATION);
                clonedQuery.addSubQuery(subQuery);
                queries.add(Pair.create(clonedQuery, 101.0));
            }
        }

        // For recursively generate subQueries for the PREV combinedQueries,
        // we need to exclude spatial relations, otherwise it's a never-ending recursion
        SugiliteRelation[] spatialRelationsToExclude = new SugiliteRelation[4];
        spatialRelationsToExclude[0] = SugiliteRelation.BELOW;
        spatialRelationsToExclude[1] = SugiliteRelation.ABOVE;
        spatialRelationsToExclude[2] = SugiliteRelation.LEFT;
        spatialRelationsToExclude[3] = SugiliteRelation.RIGHT;

        // for the outer addCombinedQuery, for now we just use has_class_name and has_package attributes, ignore all others
        SugiliteRelation[] addOuterQueryRelations = new SugiliteRelation[2];
        addOuterQueryRelations[0] = SugiliteRelation.HAS_CLASS_NAME;
        addOuterQueryRelations[1] = SugiliteRelation.HAS_PACKAGE_NAME;

        // spatial relations
        if (! relationsToExclude.contains(SugiliteRelation.LEFT)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.LEFT)) != null) {

                // we will create a nested query, more specifically, a double nested query
                // the outer query is the andQuery (for adding class and package attributes of our target query),
                // the inner query is the prevQuery (for left relations). Within the prevQuery, we will add the subQuery
                // example: (conj (has_class_name "android.widget.TextView") (has_package_name "com.android.settings") (left ( conj (has_text "xxx") (has_package_name "com.android.settings") ))


                CombinedOntologyQuery clonedAndQuery = andQuery.clone();
                CombinedOntologyQuery clonedPrevQuery = prevQuery.clone();
                OntologyQuery subQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
                Set<OntologyQuery> object = new HashSet<>();

                Node targetNode = getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.LEFT));
                if (uiSnapshot.getEntityWithNode(targetNode) != null) {

                    // First, add info for the subQuery of prevQuery
                    // the following lines creates an empty array of SugiliteRelation that the generateDefaultQueries function can take in
                    // using the empty array is because for the subQuery, we assume the restriction for the outer query doesn't apply.
                    // e.g. if we exclude HAS_TEXT for the outer query, we still can use (PREV (LEFT (HAS_TEXT "xxx")))
                    List<Pair<OntologyQuery, Double>> subQueryCandidates = generateDefaultQueries(uiSnapshot, uiSnapshot.getEntityWithNode(targetNode), spatialRelationsToExclude);
                    // parse the result, for now just take the first query, later we can change this and take a few different reasonable queries
                    subQuery = subQueryCandidates.get(0).first;
                    Double subQueryValue = subQueryCandidates.get(0).second;

                    // add info for clonedPrevQuery
                    clonedPrevQuery.setQueryRelation(SugiliteRelation.LEFT);
                    clonedPrevQuery.addSubQuery(subQuery);

                    // Second, add info for the outer query, i.e. clondedAndQuery
                    // the above clonedPrevQuery is the subQuery of the outer query
                    clonedAndQuery.addSubQuery(clonedPrevQuery);
                    // HAS_CLASS_NAME and HAS_PACKAGE_NAME are already attributes of the outer query, see the first 2 if statements of this function

                    queries.add(Pair.create(clonedAndQuery, subQueryValue + 0.1));
                } else {
                    Log.e("generateDefaultQueries", "Error generate recursive queries: cannot find the corresponding entity for target node");
                }

            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.RIGHT)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.RIGHT)) != null) {

                // we will create a nested query, more specifically, a double nested query
                // the outer query is the andQuery (for adding class and package attributes of our target query),
                // the inner query is the prevQuery (for right relations). Within the prevQuery, we will add the subQuery
                // example: (conj (has_class_name "android.widget.TextView") (has_package_name "com.android.settings") (right ( conj (has_text "xxx") (has_package_name "com.android.settings") ))


                CombinedOntologyQuery clonedAndQuery = andQuery.clone();
                CombinedOntologyQuery clonedPrevQuery = prevQuery.clone();
                OntologyQuery subQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
                Set<OntologyQuery> object = new HashSet<>();

                Node targetNode = getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.RIGHT));
                if (uiSnapshot.getEntityWithNode(targetNode) != null) {

                    // First, add info for the subQuery of prevQuery
                    // the following lines creates an empty array of SugiliteRelation that the generateDefaultQueries function can take in
                    // using the empty array is because for the subQuery, we assume the restriction for the outer query doesn't apply.
                    // e.g. if we exclude HAS_TEXT for the outer query, we still can use (PREV (LEFT (HAS_TEXT "xxx")))
                    List<Pair<OntologyQuery, Double>> subQueryCandidates = generateDefaultQueries(uiSnapshot, uiSnapshot.getEntityWithNode(targetNode), spatialRelationsToExclude);
                    // parse the result, for now just take the first query, later we can change this and take a few different reasonable queries
                    subQuery = subQueryCandidates.get(0).first;
                    Double subQueryValue = subQueryCandidates.get(0).second;

                    // add info for clonedPrevQuery
                    clonedPrevQuery.setQueryRelation(SugiliteRelation.RIGHT);
                    clonedPrevQuery.addSubQuery(subQuery);

                    // Second, add info for the outer query, i.e. clondedAndQuery
                    // the above clonedPrevQuery is the subQuery of the outer query
                    clonedAndQuery.addSubQuery(clonedPrevQuery);
                    // HAS_CLASS_NAME and HAS_PACKAGE_NAME are already attributes of the outer query, see the first 2 if statements of this function

                    queries.add(Pair.create(clonedAndQuery, subQueryValue + 0.1));
                } else {
                    Log.e("generateDefaultQueries", "Error generate recursive queries: cannot find the corresponding entity for target node");
                }

            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.ABOVE)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.ABOVE)) != null) {

                // we will create a nested query, more specifically, a double nested query
                // the outer query is the andQuery (for adding class and package attributes of our target query),
                // the inner query is the prevQuery (for right relations). Within the prevQuery, we will add the subQuery
                // example: (conj (has_class_name "android.widget.TextView") (has_package_name "com.android.settings") (right ( conj (has_text "xxx") (has_package_name "com.android.settings") ))


                CombinedOntologyQuery clonedAndQuery = andQuery.clone();
                CombinedOntologyQuery clonedPrevQuery = prevQuery.clone();
                OntologyQuery subQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
                Set<OntologyQuery> object = new HashSet<>();

                Node targetNode = getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.ABOVE));
                if (uiSnapshot.getEntityWithNode(targetNode) != null) {

                    // First, add info for the subQuery of prevQuery
                    // the following lines creates an empty array of SugiliteRelation that the generateDefaultQueries function can take in
                    // using the empty array is because for the subQuery, we assume the restriction for the outer query doesn't apply.
                    // e.g. if we exclude HAS_TEXT for the outer query, we still can use (PREV (LEFT (HAS_TEXT "xxx")))
                    List<Pair<OntologyQuery, Double>> subQueryCandidates = generateDefaultQueries(uiSnapshot, uiSnapshot.getEntityWithNode(targetNode), spatialRelationsToExclude);
                    // parse the result, for now just take the first query, later we can change this and take a few different reasonable queries
                    subQuery = subQueryCandidates.get(0).first;
                    Double subQueryValue = subQueryCandidates.get(0).second;

                    // add info for clonedPrevQuery
                    clonedPrevQuery.setQueryRelation(SugiliteRelation.ABOVE);
                    clonedPrevQuery.addSubQuery(subQuery);

                    // Second, add info for the outer query, i.e. clondedAndQuery
                    // the above clonedPrevQuery is the subQuery of the outer query
                    clonedAndQuery.addSubQuery(clonedPrevQuery);
                    // HAS_CLASS_NAME and HAS_PACKAGE_NAME are already attributes of the outer query, see the first 2 if statements of this function

                    queries.add(Pair.create(clonedAndQuery, subQueryValue + 0.1));
                } else {
                    Log.e("generateDefaultQueries", "Error generate recursive queries: cannot find the corresponding entity for target node");
                }

            }
        }

        if (! relationsToExclude.contains(SugiliteRelation.BELOW)) {
            if (getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.BELOW)) != null) {

                // we will create a nested query, more specifically, a double nested query
                // the outer query is the andQuery (for adding class and package attributes of our target query),
                // the inner query is the prevQuery (for right relations). Within the prevQuery, we will add the subQuery
                // example: (conj (has_class_name "android.widget.TextView") (has_package_name "com.android.settings") (right ( conj (has_text "xxx") (has_package_name "com.android.settings") ))


                CombinedOntologyQuery clonedAndQuery = andQuery.clone();
                CombinedOntologyQuery clonedPrevQuery = prevQuery.clone();
                OntologyQuery subQuery = new CombinedOntologyQuery(CombinedOntologyQuery.RelationType.AND);
                Set<OntologyQuery> object = new HashSet<>();

                Node targetNode = getValueIfOnlyOneObject(uiSnapshot.getNodeValuesForSubjectEntityAndRelation(targetEntity, SugiliteRelation.BELOW));
                if (uiSnapshot.getEntityWithNode(targetNode) != null) {

                    // First, add info for the subQuery of prevQuery
                    // the following lines creates an empty array of SugiliteRelation that the generateDefaultQueries function can take in
                    // using the empty array is because for the subQuery, we assume the restriction for the outer query doesn't apply.
                    // e.g. if we exclude HAS_TEXT for the outer query, we still can use (PREV (LEFT (HAS_TEXT "xxx")))
                    List<Pair<OntologyQuery, Double>> subQueryCandidates = generateDefaultQueries(uiSnapshot, uiSnapshot.getEntityWithNode(targetNode), spatialRelationsToExclude);
                    // parse the result, for now just take the first query, later we can change this and take a few different reasonable queries
                    subQuery = subQueryCandidates.get(0).first;
                    Double subQueryValue = subQueryCandidates.get(0).second;

                    // add info for clonedPrevQuery
                    clonedPrevQuery.setQueryRelation(SugiliteRelation.BELOW);
                    clonedPrevQuery.addSubQuery(subQuery);

                    // Second, add info for the outer query, i.e. clondedAndQuery
                    // the above clonedPrevQuery is the subQuery of the outer query
                    clonedAndQuery.addSubQuery(clonedPrevQuery);
                    // HAS_CLASS_NAME and HAS_PACKAGE_NAME are already attributes of the outer query, see the first 2 if statements of this function

                    queries.add(Pair.create(clonedAndQuery, subQueryValue + 0.1));
                } else {
                    Log.e("generateDefaultQueries", "Error generate recursive queries: cannot find the corresponding entity for target node");
                }

            }
        }


        Collections.sort(queries, new Comparator<Pair<OntologyQuery, Double>>() {
            @Override
            public int compare(Pair<OntologyQuery, Double> o1, Pair<OntologyQuery, Double> o2) {
                if(o1.second > o2.second) return 1;
                else if (o1.second.equals(o2.second)) return 0;
                else return -1;
            }
        });
        // serialize the query
        return queries;
    }

    private static  <T> T getValueIfOnlyOneObject (Collection<T> collection) {
        if (collection != null && collection.size() == 1) {
            List<T> list = new ArrayList<>(collection);
            return list.get(0);
        }
        return null;
    }


}