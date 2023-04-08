package com.example.crepe.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.crepe.database.Collector;
import com.example.crepe.database.Data;
import com.example.crepe.database.Datafield;
import com.example.crepe.database.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FirebaseCommunicationManager {
    private Context context;
    private FirebaseDatabase db;

    public FirebaseCommunicationManager(Context c) {
        this.db = FirebaseDatabase.getInstance();
        this.context = c;
    }

    public Task<Void> putCollector(Collector collector){
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
        return databaseReference.child(datafield.getDataFieldId()).setValue(datafield);
    }

    public Task<Void> updateCollector(String key, HashMap<String, Object> hashMap){
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    public Task<Void> updateUser(String key, HashMap<String, Object> hashMap){
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    // write a function to update the userName for a given user
    public Task<Void> updateUserName(String key, String userName){
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(key).child("userName").setValue(userName);
    }

    public Task<Void> updateData(String key, HashMap<String, Object> hashMap){
        DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    public Task<Void> updateDatafield(String key, HashMap<String, Object> hashMap){
        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        return databaseReference.child(key).updateChildren(hashMap);
    }

    public Task<Void> removeCollector(String key){
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }

    public Task<Void> removeUser(String key){
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }

    public Task<Void> removeData(String key){
        DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }

    public Task<Void> removeDatafield(String key){
        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        return databaseReference.child(key).removeValue();
    }
/*
    public void retrieveCollector(String key, FirebaseCallback firebaseCallback){   // TODO Meng key is collectorId
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        databaseReference.child(key).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().exists()){
                        DataSnapshot dataSnapshot = task.getResult();
                        String collectorId = String.valueOf(dataSnapshot.child("collectorId").getValue());
                        String creatorUserId = String.valueOf(dataSnapshot.child("creatorUserId").getValue());
                        String appName = String.valueOf(dataSnapshot.child("appName").getValue());
                        String appPackage = String.valueOf(dataSnapshot.child("appPackage").getValue());
                        String description = String.valueOf(dataSnapshot.child("description").getValue());
                        String mode = String.valueOf(dataSnapshot.child("mode").getValue());
                        String status = String.valueOf(dataSnapshot.child("collectorStatus").getValue());
                        long collectorStartTime = (long) dataSnapshot.child("collectorStartTime").getValue();
                        long collectorEndTime = (long) dataSnapshot.child("collectorEndTime").getValue();
                        Collector collector = new Collector(collectorId,creatorUserId,appName, appPackage, description,mode,String.valueOf(collectorStartTime),String.valueOf(collectorEndTime),status);
                        // call firebase callback to update collector
                        firebaseCallback.onResponse(collector);

                    } else {
                        Log.e("Firebase","Failed to find the collector firebase.");
                        Toast.makeText(context,"Failed to find the collector firebase.",Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e("Firebase", "Failed to launch connection to firebase.");
                    Toast.makeText(context,"Failed to launch connection to firebase.",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

 */

    public void retrieveUser(String key, FirebaseCallback firebaseCallback) {   // TODO Meng key is userId
        DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
        databaseReference.child(key).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        DataSnapshot dataSnapshot = task.getResult();
                        String userId = String.valueOf(dataSnapshot.child("userId").getValue());
                        String userName = String.valueOf(dataSnapshot.child("name").getValue());
                        long timeCreated = (long) dataSnapshot.child("timeCreated").getValue();
                        long timeLastEdited = (long) dataSnapshot.child("timeLastEdited").getValue();
                        User user = new User(userId, userName, timeCreated, timeLastEdited);
                        // call firebase callback to update user
                        firebaseCallback.onResponse(user);
                    } else {
                        Log.e("Firebase", "Failed to find the user firebase.");
                        Toast.makeText(context, "Failed to find the user firebase.", Toast.LENGTH_LONG).show();
                    }
            }   else{
                    Log.e("Firebase", "Failed to launch connection to firebase.");
                    Toast.makeText(context, "Failed to launch connection to firebase.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // chatGPT's code
    public void retrieveCollector(String collectorId, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        databaseReference.child(collectorId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().exists()) {
                        DataSnapshot dataSnapshot = task.getResult();
                        String collectorId = String.valueOf(dataSnapshot.child("collectorId").getValue());
                        String creatorUserId = String.valueOf(dataSnapshot.child("creatorUserId").getValue());
                        String appName = String.valueOf(dataSnapshot.child("appName").getValue());
                        String appPackage = String.valueOf(dataSnapshot.child("appPackage").getValue());
                        String description = String.valueOf(dataSnapshot.child("description").getValue());
                        String mode = String.valueOf(dataSnapshot.child("mode").getValue());
                        String targetServerIp = String.valueOf(dataSnapshot.child("targetServerIp").getValue());
                        String collectorStatus = String.valueOf(dataSnapshot.child("collectorStatus").getValue());
                        long collectorStartTime = (long) dataSnapshot.child("collectorStartTime").getValue();
                        long collectorEndTime = (long) dataSnapshot.child("collectorEndTime").getValue();

                        Collector collector = new Collector(collectorId, creatorUserId, appName, appPackage, description, mode, targetServerIp, collectorStartTime, collectorEndTime, collectorStatus);

                        // Call firebase callback to update collector
                        firebaseCallback.onResponse(collector);

                    } else {
                        Log.e("Firebase", "Failed to find the collector in firebase.");
                        Toast.makeText(context, "Failed to find the collector in firebase.", Toast.LENGTH_LONG).show();
                        firebaseCallback.onErrorResponse(task.getException());
                    }
                } else {
                    Log.e("Firebase", "Failed to launch connection to firebase.");
                    Toast.makeText(context, "Failed to launch connection to firebase.", Toast.LENGTH_LONG).show();
                    firebaseCallback.onErrorResponse(task.getException());
                }
            }
        });
    }



    public void retrieveDatafieldswithCollectorId(String collectorId, FirebaseCallback firebaseCallback) {
        DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
        Query query = databaseReference.orderByChild("collectorId").equalTo(collectorId);

        query.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()) {
                    List<Datafield> datafields = new ArrayList<>();
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        String dataFieldId = String.valueOf(snapshot.child("dataFieldId").getValue());
                        String name = String.valueOf(snapshot.child("name").getValue());
                        String graphQuery = String.valueOf(snapshot.child("graphQuery").getValue());
                        boolean demonstrated = (boolean) snapshot.child("demonstrated").getValue();
                        long timeCreated = (long) snapshot.child("timeCreated").getValue();
                        long timelastEdited = (long) snapshot.child("timelastEdited").getValue();

                        Datafield datafield = new Datafield(dataFieldId, collectorId, name, graphQuery, timeCreated, timelastEdited, demonstrated);
                        datafields.add(datafield);
                    }

                    // Call firebase callback to update datafields
                    firebaseCallback.onResponse(datafields);
                } else {
                    Log.e("Firebase", "Failed to launch connection to firebase.");
                    firebaseCallback.onErrorResponse(task.getException());
                }
            }
        });
    }




}
