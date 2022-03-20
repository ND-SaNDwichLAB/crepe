package com.example.crepe.database;

import java.util.Calendar;

public class User {

    private String userId;
    private String name;
    private long timeCreated;
    private long timeLastEdited;

    public User(String userId, String name, long timeCreated, long timeLastEdited) {
        this.userId = userId;
        this.name = name;
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
