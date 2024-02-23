package edu.nd.crepe.database;

import android.util.Log;

import com.google.firebase.database.core.view.Change;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.TimeZone;

public class Collector implements Serializable {
    private String collectorId;
    private String creatorUserId;
    private String appName;
    private String appPackage;
    private String description;
    private String mode;
    private String targetServerIp;  // not really in use, should be null most of the time. intended to be used when researchers want to collect data to their own specific server
    private long collectorStartTime;
    private long collectorEndTime;
    private String collectorStatus;

    // some constants for collector status
    public static final String DELETED = "deleted";
    public static final String ACTIVE = "active";
    public static final String NOTYETSTARTED = "notYetStarted";
    public static final String EXPIRED = "expired";

    // used in the collector comparison function (compareWith), we do not compare the collectorId, creatorId, appName, and appPackage, since we assume these are not changeable
    public enum ChangeStatus {
        NO_CHANGE, DESCRIPTION_CHANGE, COLLECTOR_START_TIME_CHANGE, COLLECTOR_END_TIME_CHANGE, COLLECTOR_STATUS_CHANGE
    }


    public Collector(String collectorId, String creatorUserID, String appName, String appPackage, String description, String mode, String targetServerIp, String collectorStartTime, String collectorEndTime) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.appPackage = appPackage;
        this.description = description;
        this.mode = mode;
        this.targetServerIp = targetServerIp;
        this.collectorStartTime = Long.parseLong(collectorStartTime);
        this.collectorEndTime = Long.parseLong(collectorEndTime);

        // auto generate the status of the collector based on current time
        this.autoSetCollectorStatus();

    }

    public Collector(String collectorId, String creatorUserID, String appName, String appPackage, String description, String mode, String targetServerIp, long collectorStartTime, long collectorEndTime, String collectorStatus) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.appPackage = appPackage;
        this.description = description;
        this.mode = mode;
        this.targetServerIp = targetServerIp;
        this.collectorStartTime = collectorStartTime;
        this.collectorEndTime = collectorEndTime;
        this.collectorStatus = collectorStatus;
    }

    public Collector(String collectorId, String creatorUserID, String appName, String appPackage, String description, String mode, long collectorStartTime, long collectorEndTime, String collectorStatus) {
        this.collectorId = collectorId;
        this.creatorUserId = creatorUserID;
        this.appName = appName;
        this.appPackage = appPackage;
        this.description = description;
        this.mode = mode;
        this.collectorStartTime = collectorStartTime;
        this.collectorEndTime = collectorEndTime;
        this.collectorStatus = collectorStatus;
    }

    public Collector(String collectorId) {
        this.collectorId = collectorId;
    }

    public Collector() {
    }

    @Override
    public String toString() {
        return "Collector{" + "collectorId='" + collectorId + '\'' + ", creatorUserId='" + creatorUserId + '\'' + ", appName='" + appName + '\'' + ", appPackage='" + appPackage + '\'' + ", description='" + description + '\'' + ", collectorStartTime=" + collectorStartTime + ", collectorEndTime=" + collectorEndTime + ", mode='" + mode + '\'' + ", targetServerIP='" + targetServerIp + '\'' + ", collectorStatus='" + collectorStatus + '\'' + '}';
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


    public String getCollectorStatus() {
        return collectorStatus;
    }


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
        if (newStatus.equals(this.collectorStatus)) {
            return false;
        } else {
            this.collectorStatus = newStatus;
            return true;
        }
    }

    // We also provide a set status function to manually set the status to an arbitrary value
    public void setCollectorStatus(String collectorStatus) {
        if (collectorStatus == DELETED || collectorStatus == NOTYETSTARTED || collectorStatus == ACTIVE || collectorStatus == EXPIRED) {
            this.collectorStatus = collectorStatus;
        } else {
            Log.e("collector", "The input status is not valid (must be deleted, notYetStarted, active, or expired)");
        }
    }

    public void activateCollector() {
        this.collectorStatus = ACTIVE;
    }

    public void setStatusDeleted() {
        this.collectorStatus = DELETED;
    }


    public Boolean isDeleted() {
        return this.collectorStatus.equals(DELETED);
    }

    public ChangeStatus compareWith(Collector updatedCollector) {
        if (this.collectorId.equals(updatedCollector.getCollectorId())
                && this.creatorUserId.equals(updatedCollector.getCreatorUserId())
                && this.appName.equals(updatedCollector.getAppName())
                && this.appPackage.equals(updatedCollector.getAppPackage())) {
            if (!this.description.equals(updatedCollector.getDescription())) {
                return ChangeStatus.DESCRIPTION_CHANGE;
            }
            if (this.collectorStartTime != updatedCollector.getCollectorStartTime()) {
                return ChangeStatus.COLLECTOR_START_TIME_CHANGE;
            }
            if (this.collectorEndTime != updatedCollector.getCollectorEndTime()) {
                return ChangeStatus.COLLECTOR_END_TIME_CHANGE;
            }
            if (!this.collectorStatus.equals(updatedCollector.getCollectorStatus())) {
                return ChangeStatus.COLLECTOR_STATUS_CHANGE;
            }
            return ChangeStatus.NO_CHANGE;
        } else {
            Log.e("collector", "The input collector is not the same as the current collector");
            return null;
        }
    }
}
