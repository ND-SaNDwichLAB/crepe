package com.example.crepe.database;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import java.util.ListIterator;
import java.util.TimeZone;

public class Collector {
    private String collectorId;
    private String creatorUserId;
    private String appName;
    private String appPackage;
    private String description;
    private String mode;
    private String targetServerIp;
    private long collectorStartTime;
    private long collectorEndTime;
    private List<Pair<String, String>> dataFields;
    private String collectorStatus;

    // some constants for collector status
    public static final String DELETED = "deleted";
    public static final String DISABLED = "disabled";
    public static final String ACTIVE = "active";
    public static final String NOTYETSTARTED = "notYetStarted";
    public static final String EXPIRED = "expired";


    public Collector(String collectorId, String creatorUserID, String appName, String appPackage, String description, String mode, String targetServerIp, long collectorStartTime, long collectorEndTime, String collectorGraphQuery, String collectorAppDataFields) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.appPackage = appPackage;
        this.description = description;
        this.mode = mode;
        this.targetServerIp = targetServerIp;
        this.collectorStartTime = collectorStartTime;
        this.collectorEndTime = collectorEndTime;
        this.dataFields = new ArrayList<>();
        this.dataFields.add(new Pair<String, String>(collectorGraphQuery,collectorAppDataFields));

        // auto generate the status of the collector based on current time
        this.autoSetCollectorStatus();

    }

    public Collector(String collectorId, String creatorUserID, String appName, String appPackage, String description, String mode, String targetServerIp, long collectorStartTime, long collectorEndTime, String collectorGraphQuery, String collectorAppDataFields, String collectorStatus) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.appPackage = appPackage;
        this.description = description;
        this.mode = mode;
        this.targetServerIp = targetServerIp;
        this.collectorStartTime = collectorStartTime;
        this.collectorEndTime = collectorEndTime;
        this.dataFields = new ArrayList<>();
        this.dataFields.add(new Pair<String, String>(collectorGraphQuery,collectorAppDataFields));
        this.collectorStatus = collectorStatus;
    }

    public Collector(String collectorId, String creatorUserID, String appName, String appPackage, String description, String mode, long collectorStartTime, long collectorEndTime, List<Pair<String, String>> collectorDataFields, String collectorStatus) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.appPackage = appPackage;
        this.description = description;
        this.mode = mode;
        this.collectorStartTime = collectorStartTime;
        this.collectorEndTime = collectorEndTime;
        this.dataFields = collectorDataFields;
        this.collectorStatus = collectorStatus;
    }

    public Collector(String collectorId) {
        this.collectorId = collectorId;
        this.dataFields = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Collector{" +
                "collectorId='" + collectorId + '\'' +
                ", creatorUserId='" + creatorUserId + '\'' +
                ", appName='" + appName + '\'' +
                ", appPackage='" + appPackage + '\'' +
                ", description='" + description + '\'' +
                ", collectorStartTime=" + collectorStartTime +
                ", collectorEndTime=" + collectorEndTime +
                ", collectorDataFields= " + getDataFieldsToString() +
                ", mode='" + mode + '\'' +
                ", targetServerIP='" + targetServerIp + '\'' +
                ", collectorStatus='" + collectorStatus + '\'' +
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

    public String getAppPackage() {
        return appPackage;
    }

    public void setAppPackage(String appPackage) {
        this.appPackage = appPackage;
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
        // force the timezone to be utc because of bug in material design. All time operations will be in utc
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
//        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSSZ");
        // force the timezone to be utc because of bug in material design. All time operations will be in utc
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(date);
    }

    public void setCollectorEndTime(long collectorEndTime) {
        this.collectorEndTime = collectorEndTime;
    }

    public List<Pair<String,String>> getDataFields() { return dataFields; }

    public String findDataField(String graphQuery){
        for (Pair<String,String> i : this.dataFields){
            if (i.first.equals(graphQuery)) {
                return i.second;
            }
        }
        return null;
    }

    public boolean removeDataField(String dataField){
        for (Pair<String,String> i : this.dataFields){
            if (i.second.equals(dataField)) {
                dataFields.remove(i);
                return true;
            }
        }
        return false;
    }

    public void putNewGraphQueryAndDataField(String collectorGraphQuery, String collectorAppDataFields){
        this.dataFields.add(new Pair<String, String>(collectorGraphQuery,collectorAppDataFields));
    }

    public String getDataFieldsToJson(){
        String dataFieldString = "{";
        for (Pair i : this.dataFields) {
            dataFieldString += i.first + ":" + i.second + ",";
        }
        if (!dataFieldString.equals("")){dataFieldString = dataFieldString.substring(0, dataFieldString.length() - 2);} // remove last ", "
        dataFieldString += "}";
        return dataFieldString;
    }

    public String getDataFieldsToString(){
        String dataFieldString = "";
        for (Pair i : this.dataFields) {
            dataFieldString += i.second + ",";
        }
        if (!dataFieldString.equals("")){ dataFieldString = dataFieldString.substring(0, dataFieldString.length() - 1);} // remove last "\n"
        return dataFieldString;
    }

    public String getCollectorStatus() {return collectorStatus;}




    // The collectorStatus will be set based on current time and the collector's start and end time
    // The return value indicates if the status is changed: true for changed, false for unchanged
    public Boolean autoSetCollectorStatus() {

        long currentTime = System.currentTimeMillis();
        // There are 5 statuses for the collector:
        // 1. deleted (will not be displayed on client phones)
        // 2. not deleted (will be displayed on phones) -- not yet started, active, expired based on time, disabled
        //                      the statuses are represented in camel case (e.g. notYetStarted)
        // by default, the collectors won't be deleted or disabled,
        // so we only need to allocate it based on current time
        String newStatus;
        if (currentTime < this.collectorStartTime) {
            newStatus = NOTYETSTARTED;
        } else if (currentTime <= this.collectorEndTime) {
            newStatus = ACTIVE;
        } else {
            newStatus = EXPIRED;
        }
        this.collectorStatus = newStatus;

        return collectorStatus != newStatus;
    }

    // We also provide a set status function to manually set the status to an arbitrary value
    public void setCollectorStatus(String collectorStatus) {
        if (collectorStatus == DELETED || collectorStatus == DISABLED || collectorStatus == NOTYETSTARTED || collectorStatus == ACTIVE || collectorStatus == EXPIRED) {
            this.collectorStatus = collectorStatus;
        } else {
            Log.e("collector", "The input status is not valid (must be deleted, disabled, notYetStarted, active, or expired)");
        }
    }

    public void activateCollector() {
        this.collectorStatus = ACTIVE;
    }

    public void deleteCollector() {
        this.collectorStatus = DELETED;
    }

    public void disableCollector() {
        this.collectorStatus = DISABLED;
    }

    public Boolean isDeleted() {
        return this.collectorStatus.equals(DELETED);
    }

}
