package com.example.crepe.graphquery.ontology;

import android.view.accessibility.AccessibilityNodeInfo;

import com.example.crepe.graphquery.ontology.SugiliteRelation;

public class ScreenRelationGraphQuery extends GraphQuery{

    private String queryString;

    public ScreenRelationGraphQuery(String queryString) {
        this.queryString = queryString;
    }

    public ScreenRelationGraphQuery(AccessibilityNodeInfo matchedNode, AccessibilityNodeInfo refereceNode) {

    }



    // infer the spatial relationship of two nodes
    private SugiliteRelation inferSpatialRelation(AccessibilityNodeInfo nodeOne, AccessibilityNodeInfo nodeTwo) {
        SugiliteRelation spatialRelation;



        return spatialRelation;
    }

    private String getReferenceNodeProperty(AccessibilityNodeInfo referenceNode) {

        return "";
    }

    public static void parseQuery(String query) {

    }
}
