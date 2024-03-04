package edu.nd.crepe.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.Data;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.database.User;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseCommunicationManager {
    private Context context;
    private FirebaseDatabase db;

    private static final String CREATOR = "creator";
    private static final String PARTICIPANT = "participant";
    private static final String NONE = "none";

    public FirebaseCommunicationManager(Context c) {
        this.db = FirebaseDatabase.getInstance();
        this.context = c;
    }

    public Task<Void> putCollector(Collector collector) {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        return databaseReference.child(collector.getCollectorId()).setValue(collector);

    }

    public Task<Void> putUser(User user) {
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(user.getUserId()).setValue(user);
    }

    public Task<Void> putData(Data data) {
        DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
        return databaseReference.child(data.getDataId()).setValue(data);
    }

    public Task<Void> putDatafield(Datafield datafield) {
        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        return databaseReference.child(datafield.getDatafieldId()).setValue(datafield);
    }

    public Task<Void> updateCollector(String key, HashMap<String, Object> hashMap) {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    public Task<Void> updateUser(String key, HashMap<String, Object> hashMap) {
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    // write a function to update the userName for a given user
    public Task<Void> updateUserName(String key, String userName) {
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(key).child("userName").setValue(userName);
    }

    public Task<Void> updateData(String key, HashMap<String, Object> hashMap) {
        DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    public Task<Void> updateDatafield(String key, HashMap<String, Object> hashMap) {
        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    // NOTE: this does not actually "remove" the collector, it just sets the status to deleted.
    // This is consistent with local database behavior and helps to preserve the collector info even after it's deleted.
    public Task<Void> setCollectorStatusDeleted(String key) {
        // instead of removing the collector from firebase, we will just set the status to deleted
        // this is consistent with local database behavior, so we do not lose existing collector data
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());

        // Create a map to hold updates regarding the deletion
        Map<String, Object> updates = new HashMap<>();
        // Set the collectorStatus field to DELETED
        updates.put("collectorStatus", Collector.DELETED);

        // Update the child node
        return databaseReference.child(key).updateChildren(updates);
    }

    public Task<Void> removeUser(String key) {
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }

    public Task<Void> removeData(String key) {
        DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }

    public Task<Void> removeDatafield(String key) {

        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }

    public void retrieveUser(String key, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        databaseReference.child(key).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        DataSnapshot dataSnapshot = task.getResult();
                        String userId = String.valueOf(dataSnapshot.child("userId").getValue());
                        String userName = String.valueOf(dataSnapshot.child("name").getValue());
                        String photoUrl = String.valueOf(dataSnapshot.child("photoUrl").getValue());
                        long timeCreated = (long) dataSnapshot.child("timeCreated").getValue();
                        long lastHeartBeat = (long) dataSnapshot.child("lastHeartBeat").getValue();

                        GenericTypeIndicator<List<String>> genericTypeIndicator = new GenericTypeIndicator<List<String>>() {
                        };
                        List<String> userCollectorsList = dataSnapshot.child("userCollectors").getValue(genericTypeIndicator);
                        ArrayList<String> userCollectors;
                        if (userCollectorsList == null) {
                            userCollectors = new ArrayList<>();
                        } else {
                            userCollectors = new ArrayList<>(userCollectorsList);
                        }

                        User user = new User(userId, userName, photoUrl, timeCreated, lastHeartBeat, userCollectors);
                        // call firebase callback to update user
                        firebaseCallback.onResponse(user);
                    } else {
                        Log.e("Firebase", "retrieve user: Failed to find the user firebase.");
                        Toast.makeText(context, "retrieve user: Failed to find the user firebase.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("Firebase", "retrieve user: Failed to launch connection to firebase.");
                    Toast.makeText(context, "retrieve user: Failed to launch connection to firebase.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void retrieveCollector(String collectorId, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        databaseReference.child(collectorId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    DataSnapshot dataSnapshot = task.getResult();
                    String collectorId1 = String.valueOf(dataSnapshot.child("collectorId").getValue());
                    String creatorUserId = String.valueOf(dataSnapshot.child("creatorUserId").getValue());
                    String appName = String.valueOf(dataSnapshot.child("appName").getValue());
                    String appPackage = String.valueOf(dataSnapshot.child("appPackage").getValue());
                    String description = String.valueOf(dataSnapshot.child("description").getValue());
                    String mode = String.valueOf(dataSnapshot.child("mode").getValue());
                    String targetServerIp = String.valueOf(dataSnapshot.child("targetServerIp").getValue());
                    String collectorStatus = String.valueOf(dataSnapshot.child("collectorStatus").getValue());
                    long collectorStartTime = (long) dataSnapshot.child("collectorStartTime").getValue();
                    long collectorEndTime = (long) dataSnapshot.child("collectorEndTime").getValue();

                    Collector collector = new Collector(collectorId1, creatorUserId, appName, appPackage, description, mode, targetServerIp, collectorStartTime, collectorEndTime, collectorStatus);

                    // Call firebase callback to update collector
                    firebaseCallback.onResponse(collector);

                } else {
                    Log.e("Firebase", "Failed to retrieve collector, it does not exist.");
                    firebaseCallback.onErrorResponse(new Exception("Failed to retrieve collector, it does not exist."));
                }
            } else {
                Log.e("Firebase", "retrieve collector: Failed to launch connection to firebase. Error: ", task.getException());
                firebaseCallback.onErrorResponse(task.getException());
            }
        });


    }

    public void retrieveCollectorWithCreatorUserId(String userId, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        Query query = databaseReference.orderByChild("creatorUserId").equalTo(userId);
        query.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    List<Collector> collectors = new ArrayList<>();
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        String collectorId = String.valueOf(snapshot.child("collectorId").getValue());
                        String creatorUserId = String.valueOf(snapshot.child("creatorUserId").getValue());
                        String appName = String.valueOf(snapshot.child("appName").getValue());
                        String appPackage = String.valueOf(snapshot.child("appPackage").getValue());
                        String description = String.valueOf(snapshot.child("description").getValue());
                        String mode = String.valueOf(snapshot.child("mode").getValue());
                        String targetServerIp = String.valueOf(snapshot.child("targetServerIp").getValue());
                        String collectorStatus = String.valueOf(snapshot.child("collectorStatus").getValue());
                        long collectorStartTime = (long) snapshot.child("collectorStartTime").getValue();
                        long collectorEndTime = (long) snapshot.child("collectorEndTime").getValue();

                        Collector collector = new Collector(collectorId, creatorUserId, appName, appPackage, description, mode, targetServerIp, collectorStartTime, collectorEndTime, collectorStatus);
                        // TODO Yuwen we would want to show non-active collectors in other tabs in the future
                        if (collectorStatus.equals(Collector.ACTIVE)) {
                            collectors.add(collector);
                        }
                    }

                    // Call firebase callback to update collectors
                    firebaseCallback.onResponse(collectors);
                } else {
                    Log.e("Firebase", "retrieve collector with creator user id: Failed to launch connection to firebase.");
                    firebaseCallback.onErrorResponse(task.getException());
                }
            }
        });
    }

    public void updateAllCollectors() {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());

        databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {

                // get current time
                long currentTime = System.currentTimeMillis();

                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        DataSnapshot dataSnapshot = task.getResult();
                        for (DataSnapshot collectorSnapshot : dataSnapshot.getChildren()) {

                            // if the collector is already expired, it will have only one field
                            if (collectorSnapshot.getChildrenCount() == 1
                                    && String.valueOf(collectorSnapshot.child("collectorStatus").getValue()).equals(Collector.EXPIRED)) {
                                continue;
                            }

                            // retrieve relevant attributes
                            String collectorId = String.valueOf(collectorSnapshot.child("collectorId").getValue());
                            String collectorStatus = String.valueOf(collectorSnapshot.child("collectorStatus").getValue());
                            long collectorStartTime = (long) collectorSnapshot.child("collectorStartTime").getValue();
                            long collectorEndTime = (long) collectorSnapshot.child("collectorEndTime").getValue();

                            // check if the collector time frame contains the current time
                            if (collectorStatus != Collector.DELETED) {
                                String currentStatus = "";
                                if (currentTime >= collectorStartTime && currentTime <= collectorEndTime) {
                                    currentStatus = Collector.ACTIVE;
                                } else if (currentTime < collectorStartTime) {
                                    currentStatus = Collector.NOTYETSTARTED;
                                } else if (currentTime > collectorEndTime) {
                                    currentStatus = Collector.EXPIRED;
                                }

                                if (!currentStatus.equals(collectorStatus)) {
                                    // update collector status
                                    databaseReference.child(collectorId).child("collectorStatus").setValue(currentStatus);
                                }
                            }
                        }
                    } else {
                        Log.i("Firebase", "Collector list is empty");
                    }
                } else {
                    Log.e("Firebase", "retrieve collector: Failed to launch connection to firebase.");
                }
            }
        });
    }


    public void retrieveDatafieldsWithCollectorId(String collectorId, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        Query query = databaseReference.orderByChild("collectorId").equalTo(collectorId);
        query.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    List<Datafield> datafields = new ArrayList<>();
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        String datafieldId = String.valueOf(snapshot.child("datafieldId").getValue());
                        String name = String.valueOf(snapshot.child("name").getValue());
                        String graphQuery = String.valueOf(snapshot.child("graphQuery").getValue());
                        boolean demonstrated = (boolean) snapshot.child("demonstrated").getValue();
                        long timeCreated = (long) snapshot.child("timeCreated").getValue();
                        long timelastEdited = (long) snapshot.child("timelastEdited").getValue();

                        Datafield datafield = new Datafield(datafieldId, collectorId, graphQuery, name, timeCreated, timelastEdited, demonstrated);
                        datafields.add(datafield);
                    }

                    // Call firebase callback to update datafields
                    firebaseCallback.onResponse(datafields);
                } else {
                    Log.e("Firebase", "retrieve datafield with collector id: Failed to launch connection to firebase.");
                    firebaseCallback.onErrorResponse(task.getException());
                }
            }
        });
    }

    // retrieve all collector ids on firebase
    public void retrieveAllCollectors(FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        databaseReference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    List<Collector> collectors = new ArrayList<>();
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        Collector collector = snapshot.getValue(Collector.class);
                        collectors.add(collector);
                    }

                    // Call firebase callback to update collectors
                    firebaseCallback.onResponse(collectors);
                } else {
                    Log.e("Firebase", "retrieve all collectors: Failed to launch connection to firebase.");
                    firebaseCallback.onErrorResponse(task.getException());
                }
            }
        });
    }

    // retrieve data with datafieldId?
    public void retrieveDataWithDatafieldId(String datafieldId, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
        // get the access to the data table
        Query query = databaseReference.orderByChild("datafieldId").equalTo(datafieldId);
        query.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    List<Data> datas = new ArrayList<>();
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        String dataId = String.valueOf(snapshot.child("dataId").getValue());
                        String userId = String.valueOf(snapshot.child("userId").getValue());
                        String datafieldId = String.valueOf(snapshot.child("datafieldId").getValue());
                        String dataContent = String.valueOf(snapshot.child("dataContent").getValue());
                        long timestamp = (long) snapshot.child("timestamp").getValue();
                        Data data = new Data(dataId, datafieldId, userId, timestamp, dataContent);
//                        // if creator, add all data
//                        if (checkDataAccessRule(userId,dbManager.getAllUsers().get(0)).equals(PARTICIPANT)) {
//                            datas.add(data);
//                        }
                        // TODO: if creator, add all data when data.datafieldId.creatorId == self.userId

                        datas.add(data);
                    }

                    // Call firebase callback to update datas
                    firebaseCallback.onResponse(datas);
                } else {
                    Log.e("Firebase", "retrieve data with datafield id: Failed to launch connection to firebase.");
                    firebaseCallback.onErrorResponse(task.getException());
                }
            }
        });
    }

    /* description of firebase access rule (set on the web portal)
    WARNING: This is actually not in use, the access rule is set on the web portal
    User
    - creator: read & write access to only userId = self.userId
    - participant: read & write access to userId = self.userId
    Data
    - creator: read rows where data.collectorId.creatorId == creator.userId
    - participant:
    read: rows where data.userId == self.userId
    write:
    for writing to the server, check on server side: verify collectorId is in collector table, collectorStatus == active. participant.userId == the id of the account sending the data (you can’t pretend to be someone else)
    Collector
    - creator: read & write rows where collector.creatorId == self.userId
    - participant: read rows when they know the collectorId (ofc the collectorId has to exist in current table, status == active)
    Datafield
    - creator: read & write rows where collector.creatorId == self.userId
    - participant: read rows when they know the collectorId (ofc the collectorId has to exist in current table, status == active)
     */


}
