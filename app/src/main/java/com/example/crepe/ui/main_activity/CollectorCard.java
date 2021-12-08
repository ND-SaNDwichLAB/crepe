package com.example.crepe.ui.main_activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.crepe.R;

public class CollectorCard {
    private String collectorID;
//    private String creatorUserID;
    private String appName;
//    private String name;
    private long timeCreated;
    private long timeLastEdited;
//    private String mode;
//    private String targetServerIP;


    public CollectorCard(String collectorID, String appName, long timeCreated, long timeLastEdited) {
        this.collectorID = collectorID;
        this.appName = appName;
        this.timeCreated = timeCreated;
        this.timeLastEdited = timeLastEdited;
    }

    public String getCollectorID() {
        return collectorID;
    }

    public void setCollectorID(String collectorID) {
        this.collectorID = collectorID;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
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

    public Boolean inflateCollectorCard (Context context) {

        // TODO: inflate a new collector card with the parameters


        return true;
    }
}
