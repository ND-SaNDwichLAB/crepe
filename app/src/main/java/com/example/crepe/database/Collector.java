package com.example.crepe.database;

public class Collector {

    // TODO: database schema - changed String name to description

    private String collectorId;
    private String creatorUserId;
    private String appName;
    private String description;
    private long timeCreated;
    private long timeLastEdited;
    private String mode;
    private String targetServerIp;

    public Collector(String collectorId, String creatorUserID, String appName, String description, long timeCreated, long timeLastEdited, String mode, String targetServerIp) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.description = description;
        this.timeCreated = timeCreated;
        this.timeLastEdited = timeLastEdited;
        this.mode = mode;
        this.targetServerIp = targetServerIp;
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
                ", timeCreated=" + timeCreated +
                ", timeLastEdited=" + timeLastEdited +
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

    public String getTargetServerIp() {
        return targetServerIp;
    }

    public void setTargetServerIp(String targetServerIp) {
        this.targetServerIp = targetServerIp;
    }
}
