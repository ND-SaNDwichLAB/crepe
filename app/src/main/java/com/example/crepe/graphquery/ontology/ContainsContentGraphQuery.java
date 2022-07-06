package com.example.crepe.graphquery.ontology;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;

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

    // the constructor that takes a query string, used after retrieving from the database
    // TODO yuwen: support recursive operations
    public ContainsContentGraphQuery(String queryString) {
        this.queryString = queryString;
        // take out the parenthesis outside
        if(queryString.startsWith("(") && queryString.endsWith(")")) {
            queryString = queryString.substring(1, queryString.length() - 1);
            String[] parsedResults = queryString.split(" ");
            if(parsedResults.length == 2) {
                this.relation = SugiliteRelation.getRelationFromString(parsedResults[0]);
                this.property = parsedResults[1];
            } else {
                Log.d("parse query", "Parse query error, contains more than 2 fields");
            }

        }
    }

    private String generateQueryString(SugiliteRelation relation, String property) {
        return "(" + relation.getRelationName() + " " + property + ")";
    }

    public SugiliteRelation getRelation() {
        return relation;
    }

    public void setRelation(SugiliteRelation relation) {
        this.relation = relation;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
}
