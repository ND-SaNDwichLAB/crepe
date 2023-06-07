package edu.nd.crepe.database;

import java.util.Calendar;

public class Data {

    private String dataId;
    private String dataFieldId;
    private String userId;
    private long timestamp;
    private String dataContent;

    public Data(String dataId, String dataFieldId, String userId, String dataContent) {
        this.dataId = dataId;
        this.dataFieldId = dataFieldId;
        this.userId = userId;
        this.timestamp = Calendar.getInstance().getTimeInMillis();
        this.dataContent = dataContent;
    }

    public Data(String dataId, String dataFieldId, String userId, long timeStamp, String dataContent) {
        this.dataId = dataId;
        this.dataFieldId = dataFieldId;
        this.userId = userId;
        this.timestamp = timeStamp;
        this.dataContent = dataContent;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getDataFieldId() {
        return dataFieldId;
    }

    public void setDataFieldId(String dataFieldId) {
        this.dataFieldId = dataFieldId;
    }

    public String getUserId() {
        return userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDataContent() {
        return dataContent;
    }

    public void setDataContent(String dataContent) {
        this.dataContent = dataContent;
    }
}
