package com.example.crepe.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Collector {

    // TODO: database schema - changed String name to description

    private String collectorId;
    private String creatorUserId;
    private String appName;
    private String description;
    private String mode;
    private String targetServerIp;
    private long collectorStartTime;
    private long collectorEndTime;
    private String collectorGraphQuery;
    private String collectorAppDataFields;


    public Collector(String collectorId, String creatorUserID, String appName, String description, String mode, String targetServerIp, long collectorStartTime, long collectorEndTime, String collectorGraphQuery, String collectorAppDataFields) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.description = description;
        this.mode = mode;
        this.targetServerIp = targetServerIp;
        this.collectorStartTime = collectorStartTime;
        this.collectorEndTime = collectorEndTime;
        this.collectorGraphQuery = collectorGraphQuery;
        this.collectorAppDataFields = collectorAppDataFields;
    }

    public Collector(String collectorId) {
        this.collectorId = collectorId;
    }

    @Override
    public String toString() {
        return "Collector{" +
                "collectorId='" + collectorId + '\'' +
                ", creatorUserId='" + creatorUserId + '\'' +
                ", appName='" + appName + '\'' +
                ", description='" + description + '\'' +
                ", collectorStartTime=" + collectorStartTime +
                ", collectorEndTime=" + collectorEndTime +
                ", collectorGraphQuery=" + collectorGraphQuery +
                ", collectorAppDataFields= " + collectorAppDataFields +
                ", mode='" + mode + '\'' +
                ", targetServerIP='" + targetServerIp + '\'' +
                '}';
    }

    public String idToString() {
        return "Collector with id: " + collectorId;
    }

    public String getCollectorId() {
        return collectorId;
    }

    public void setCollectorId(String collectorId) {
        this.collectorId = collectorId;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTargetServerIp() {
        return targetServerIp;
    }

    public void setTargetServerIp(String targetServerIp) {
        this.targetServerIp = targetServerIp;
    }


    public long getCollectorStartTime() {
        return collectorStartTime;
    }

    public String getCollectorStartTimeString() {
        Date date = new Date(collectorStartTime);
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public void setCollectorStartTime(long collectorStartTime) {
        this.collectorStartTime = collectorStartTime;
    }

    public long getCollectorEndTime() {
        return collectorEndTime;
    }

    public String getCollectorEndTimeString() {
        Date date = new Date(collectorEndTime);
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        dateFormat.setTimeZone(TimeZone.getDefault());

        return dateFormat.format(date);
    }

    public void setCollectorEndTime(long collectorEndTime) {
        this.collectorEndTime = collectorEndTime;
    }



    public String getCollectorAppDataFields() {
        return collectorAppDataFields;
    }

    public void setCollectorAppDataFields(String collectorAppDataFields) {
        this.collectorAppDataFields = collectorAppDataFields;
    }

    public String getCollectorGraphQuery() {
        return collectorGraphQuery;
    }

    public void setCollectorGraphQuery(String collectorGraphQuery) {
        this.collectorGraphQuery = collectorGraphQuery;
    }
}
