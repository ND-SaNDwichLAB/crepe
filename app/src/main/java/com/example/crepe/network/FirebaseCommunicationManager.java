package com.example.crepe.network;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.crepe.database.Collector;
import com.example.crepe.database.Data;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.database.Datafield;
import com.example.crepe.database.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseCommunicationManager {
    private Context context;
    private FirebaseDatabase db;
    private Collector collector;

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

    public void retrieveCollector(String key, FirebaseCallback firebaseCallback){
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
        databaseReference.child(key).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().exists()){
                        DataSnapshot dataSnapshot = task.getResult();
                        String collectorId = String.valueOf(dataSnapshot.child("collectorId").getValue());
                        //String creatorUserId = String.valueOf(dataSnapshot.child("creatorUserId").getValue());
                        String appName = String.valueOf(dataSnapshot.child("appName").getValue());
                        String description = String.valueOf(dataSnapshot.child("description").getValue());
                        String mode = String.valueOf(dataSnapshot.child("mode").getValue());
                        String status = String.valueOf(dataSnapshot.child("collectorStatus").getValue());
                        long collectorStartTime = (long) dataSnapshot.child("collectorStartTime").getValue();
                        long collectorEndTime = (long) dataSnapshot.child("collectorEndTime").getValue();
                        List<HashMap<String,String>> dataFieldsRaw = (List<HashMap<String,String>>)dataSnapshot.child("dataFields").getValue();
                        List<Pair<String,String>> dataFields = new ArrayList<>();
                        for (HashMap i : dataFieldsRaw){
                            System.out.println(i.get("first").toString());
                            System.out.println(i.get("second").toString());
                            dataFields.add(new Pair<String,String>(i.get("first").toString(), i.get("second").toString()));
                        }
                        Collector collector = new Collector(collectorId,"1",appName,description,mode,collectorStartTime,collectorEndTime,dataFields,status);
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

    public Collector getCollector(){
        return this.collector;
    }

    public void setCollector(Collector collector){
        this.collector = collector;
    }





}
