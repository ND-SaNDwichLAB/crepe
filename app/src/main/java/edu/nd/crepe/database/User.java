package edu.nd.crepe.database;

import java.util.ArrayList;

public class User {

    private String userId;
    private String name;
    private String photoUrl;
    private long timeCreated;
    private long lastHeartBeat; // sent every interval to keep track of when the user was last active, in CrepeAccessibilityService

    // store collectorId for the collectors that the user is participating in
    // NOTE: this does not contain collectors that the user has created. That information is stored in the collectors themselves under the field "creatorUserId"
    private ArrayList<String> userCollectors = new ArrayList<>();

    public User(String userId, String name, String photoUrl, long timeCreated, long lastHeartBeat) {
        this.userId = userId;
        this.name = name;
        this.photoUrl = photoUrl;
        this.timeCreated = timeCreated;
        this.lastHeartBeat = lastHeartBeat;
        this.userCollectors = new ArrayList<>();
    }

    public User(String userId, String name, String photoUrl, long timeCreated, long lastHeartBeat, ArrayList<String> userCollectors) {
        this.userId = userId;
        this.name = name;
        this.photoUrl = photoUrl;
        this.timeCreated = timeCreated;
        this.lastHeartBeat = lastHeartBeat;
        this.userCollectors.addAll(userCollectors);
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

    public long getLastHeartBeat() {
        return lastHeartBeat;
    }

    public void setLastHeartBeat(long lastHeartBeat) {
        this.lastHeartBeat = lastHeartBeat;
    }

    public ArrayList<String> getCollectorsForCurrentUser() {
        return userCollectors;
    }

    public void overrideCollectorsForCurrentUser(ArrayList<String> collectorIds) {
        this.userCollectors = collectorIds;
    }

    public void addCollectorForCurrentUser(String collectorId) {
        this.userCollectors.add(collectorId);
    }

    public void removeCollectorForCurrentUser(Collector collector) {
        this.userCollectors.remove(collector);
    }

    public void removeAllCollectorsForCurrentUser() {
        this.userCollectors.clear();
    }

}
