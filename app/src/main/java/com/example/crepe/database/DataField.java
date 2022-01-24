package com.example.crepe.database;

import java.util.Calendar;

public class DataField {

    private String dataFieldId;
    private String collectorId; // foreign key
    private String graphQuery;
    private String name;
    // TODO: database schema: do we need the following two?
    private long timeCreated;
    private long timelastEdited;

    private Boolean isDemonstrated;

    public DataField(String dataFieldId, String collectorId, String graphQuery, String name, Boolean isDemonstrated) {
        this.dataFieldId = dataFieldId;
        this.collectorId = collectorId;
        this.graphQuery = graphQuery;
        this.name = name;
        this.timeCreated = Calendar.getInstance().getTimeInMillis();
        this.timelastEdited = timeCreated;
        this.isDemonstrated = isDemonstrated;
    }

    public DataField(String dataFieldId, String collectorId, String graphQuery, String name, long timeCreated, long timelastEdited, Boolean isDemonstrated) {
        this.dataFieldId = dataFieldId;
        this.collectorId = collectorId;
        this.graphQuery = graphQuery;
        this.name = name;
        this.timeCreated = timeCreated;
        this.timelastEdited = timelastEdited;
        this.isDemonstrated = isDemonstrated;
    }

    public String getDataFieldId() {
        return dataFieldId;
    }

    public void setDataFieldId(String dataFieldId) {
        this.dataFieldId = dataFieldId;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String getGraphQuery() {
        return graphQuery;
    }

    public void setGraphQuery(String graphQuery) {
        this.graphQuery = graphQuery;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public long getTimelastEdited() {
        return timelastEdited;
    }

    public void setTimelastEdited(long timelastEdited) {
        this.timelastEdited = timelastEdited;
    }

    public Boolean getDemonstrated() {
        return isDemonstrated;
    }

    public void setDemonstrated(Boolean demonstrated) {
        isDemonstrated = demonstrated;
    }
}
