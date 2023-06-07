package com.example.crepe.database;

import java.util.Calendar;

public class User {

    private String userId;
    private String name;
    private String photoUrl;
    private long timeCreated;
    private long timeLastEdited;

    public User(String userId, String name, String photoUrl, long timeCreated, long timeLastEdited) {
        this.userId = userId;
        this.name = name;
        this.photoUrl = photoUrl;
        this.timeCreated = timeCreated;
        this.timeLastEdited = timeLastEdited;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    // no setter for time created because it should not be modified after creation

    public long getTimeLastEdited() {
        return timeLastEdited;
    }

    public void setTimeLastEdited(long timeLastEdited) {
        this.timeLastEdited = timeLastEdited;
    }
}
