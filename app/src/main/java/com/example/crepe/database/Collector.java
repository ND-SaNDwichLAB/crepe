package com.example.crepe.database;

public class Collector {

    private String collectorID;
    private String creatorUserID;
    private String appName;
    private String name;
    private long timeCreated;
    private long timeLastEdited;
    private String mode;
    private String targetServerIP;

    public Collector(String collectorID, String creatorUserID, String appName, String name, long timeCreated, long timeLastEdited, String mode, String targetServerIP) {
        this.collectorID = collectorID;
        this.creatorUserID = creatorUserID;
        this.appName = appName;
        this.name = name;
        this.timeCreated = timeCreated;
        this.timeLastEdited = timeLastEdited;
        this.mode = mode;
        this.targetServerIP = targetServerIP;
    }

    public Collector(String collectorID) {
        this.collectorID = collectorID;
    }

    @Override
    public String toString() {
        return "Collector{" +
                "collectorID='" + collectorID + '\'' +
                ", creatorUserID='" + creatorUserID + '\'' +
                ", appName='" + appName + '\'' +
                ", name='" + name + '\'' +
                ", timeCreated=" + timeCreated +
                ", timeLastEdited=" + timeLastEdited +
                ", mode='" + mode + '\'' +
                ", targetServerIP='" + targetServerIP + '\'' +
                '}';
    }

    public String idToString() {
        return "Collector with id: " + collectorID;
    }

    public String getCollectorID() {
        return collectorID;
    }

    public void setCollectorID(String collectorID) {
        this.collectorID = collectorID;
    }

    public String getCreatorUserID() {
        return creatorUserID;
    }

    public void setCreatorUserID(String creatorUserID) {
        this.creatorUserID = creatorUserID;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getTimeLastEdited() {
        return timeLastEdited;
    }

    public void setTimeLastEdited(long timeLastEdited) {
        this.timeLastEdited = timeLastEdited;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTargetServerIP() {
        return targetServerIP;
    }

    public void setTargetServerIP(String targetServerIP) {
        this.targetServerIP = targetServerIP;
    }
}
