package com.example.crepe.graphquery.ontology;

import android.view.accessibility.AccessibilityNodeInfo;

// example: (HAS_TEXT Price)
public class ContainsContentGraphQuery extends GraphQuery {

    private SugiliteRelation relation;
    private String property;
    private String queryString;

    public ContainsContentGraphQuery(AccessibilityNodeInfo node) {
        this.property = node.getText().toString();
        if(this.property != null && !this.property.isEmpty()) {
            this.relation = SugiliteRelation.HAS_TEXT;
        }
        this.queryString = generateQueryString(this.relation, this.property);
    }

    private String generateQueryString(SugiliteRelation relation, String property) {
        return "(" + relation.getRelationName() + " " + property + ")";
    }
}
