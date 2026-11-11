package edu.nd.crepe.network;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.Data;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.database.User;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads and writes the remote (Firebase) copy of the collectors, users, datafields and data.
 *
 * Snapshots are parsed by hand so that a record with a missing or oddly typed field (for example
 * an expired collector that was reduced to a single field) is reported through the callback instead
 * of throwing inside a Firebase listener.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FirebaseCommunicationManager {
    private static final String TAG = "Firebase";

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

    // ---------------------------------------------------------------------------------------
    // Snapshot parsing helpers
    // ---------------------------------------------------------------------------------------

    @Nullable
    private static String optString(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        return value == null ? null : String.valueOf(value);
    }

    private static long optLong(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Field " + key + " is not a number: " + value);
            }
        }
        return 0L;
    }

    private static boolean optBoolean(DataSnapshot snapshot, String key) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static Collector parseCollector(DataSnapshot snapshot) {
        // the key of a collector node is its collector id
        String collectorId = optString(snapshot, "collectorId");
        if (collectorId == null) {
            collectorId = snapshot.getKey();
        }
        Collector collector = new Collector(
                collectorId,
                optString(snapshot, "creatorUserId"),
                optString(snapshot, "appName"),
                optString(snapshot, "appPackage"),
                optString(snapshot, "description"),
                optString(snapshot, "mode"),
                optString(snapshot, "targetServerIp"),
                optLong(snapshot, "collectorStartTime"),
                optLong(snapshot, "collectorEndTime"),
                optString(snapshot, "collectorStatus"));
        if (collector.getCollectorStatus() == null) {
            collector.autoSetCollectorStatus();
        }
        return collector;
    }

    private static User parseUser(DataSnapshot snapshot) {
        String userId = optString(snapshot, "userId");
        if (userId == null) {
            userId = snapshot.getKey();
        }
        String userName = optString(snapshot, "name");
        String photoUrl = optString(snapshot, "photoUrl");

        ArrayList<String> userCollectors = new ArrayList<>();
        for (DataSnapshot collectorSnapshot : snapshot.child("userCollectors").getChildren()) {
            Object collectorId = collectorSnapshot.getValue();
            if (collectorId != null) {
                userCollectors.add(String.valueOf(collectorId));
            }
        }

        return new User(userId, userName != null ? userName : "", photoUrl != null ? photoUrl : "",
                optLong(snapshot, "timeCreated"), optLong(snapshot, "lastHeartBeat"), userCollectors);
    }

    private static Datafield parseDatafield(DataSnapshot snapshot, String collectorId) {
        String datafieldId = optString(snapshot, "datafieldId");
        if (datafieldId == null) {
            datafieldId = snapshot.getKey();
        }
        return new Datafield(
                datafieldId,
                collectorId,
                optString(snapshot, "graphQuery"),
                optString(snapshot, "name"),
                optLong(snapshot, "timeCreated"),
                optLong(snapshot, "timelastEdited"),
                optBoolean(snapshot, "demonstrated"));
    }

    private static Data parseData(DataSnapshot snapshot) {
        String dataId = optString(snapshot, "dataId");
        if (dataId == null) {
            dataId = snapshot.getKey();
        }
        return new Data(
                dataId,
                optString(snapshot, "datafieldId"),
                optString(snapshot, "userId"),
                optLong(snapshot, "timestamp"),
                optString(snapshot, "dataContent"));
    }

    private static Exception exceptionOf(@Nullable Exception e, String fallbackMessage) {
        return e != null ? e : new Exception(fallbackMessage);
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------------------------

    public void retrieveUser(String key, FirebaseCallback firebaseCallback) {
        try {
            DatabaseReference databaseReference = db.getReference(User.class.getSimpleName());
            databaseReference.child(key).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "retrieve user: Failed to launch connection to firebase.", task.getException());
                    showToast("retrieve user: Failed to launch connection to firebase.");
                    firebaseCallback.onErrorResponse(exceptionOf(task.getException(), "Failed to launch connection to firebase."));
                    return;
                }
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot == null || !dataSnapshot.exists()) {
                    Log.e(TAG, "retrieve user: Failed to find the user firebase.");
                    showToast("retrieve user: Failed to find the user firebase.");
                    firebaseCallback.onErrorResponse(new Exception("Failed to find the user in firebase."));
                    return;
                }

                User user;
                try {
                    user = parseUser(dataSnapshot);
                } catch (RuntimeException e) {
                    Log.e(TAG, "retrieve user: Failed to parse the user.", e);
                    firebaseCallback.onErrorResponse(e);
                    return;
                }
                // call firebase callback to update user
                firebaseCallback.onResponse(user);
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "retrieve user: invalid request for user " + key, e);
            firebaseCallback.onErrorResponse(e);
        }
    }

    public void retrieveCollector(String collectorId, FirebaseCallback firebaseCallback) {
        try {
            DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
            databaseReference.child(collectorId).get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "retrieve collector: Failed to launch connection to firebase. Error: ", task.getException());
                    firebaseCallback.onErrorResponse(exceptionOf(task.getException(), "Failed to launch connection to firebase."));
                    return;
                }
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot == null || !dataSnapshot.exists()) {
                    Log.e(TAG, "Failed to retrieve collector, it does not exist.");
                    firebaseCallback.onErrorResponse(new Exception("Failed to retrieve collector, it does not exist."));
                    return;
                }

                Collector collector;
                try {
                    collector = parseCollector(dataSnapshot);
                } catch (RuntimeException e) {
                    Log.e(TAG, "retrieve collector: Failed to parse the collector.", e);
                    firebaseCallback.onErrorResponse(e);
                    return;
                }
                // Call firebase callback to update collector
                firebaseCallback.onResponse(collector);
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "retrieve collector: invalid request for collector " + collectorId, e);
            firebaseCallback.onErrorResponse(e);
        }
    }

    public void retrieveCollectorWithCreatorUserId(String userId, FirebaseCallback firebaseCallback) {
        try {
            DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
            Query query = databaseReference.orderByChild("creatorUserId").equalTo(userId);
            query.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "retrieve collector with creator user id: Failed to launch connection to firebase.", task.getException());
                    firebaseCallback.onErrorResponse(exceptionOf(task.getException(), "Failed to launch connection to firebase."));
                    return;
                }

                ArrayList<Collector> collectors = new ArrayList<>();
                try {
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        collectors.add(parseCollector(snapshot));
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "retrieve collector with creator user id: Failed to parse collectors.", e);
                    firebaseCallback.onErrorResponse(e);
                    return;
                }
                // Call firebase callback to update collectors
                firebaseCallback.onResponse(collectors);
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "retrieve collector with creator user id: invalid request for user " + userId, e);
            firebaseCallback.onErrorResponse(e);
        }
    }

    public void updateAllCollectors() {
        DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());

        databaseReference.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "update all collectors: Failed to launch connection to firebase.", task.getException());
                return;
            }
            DataSnapshot dataSnapshot = task.getResult();
            if (dataSnapshot == null || !dataSnapshot.exists()) {
                Log.i(TAG, "Collector list is empty");
                return;
            }

            // get current time
            long currentTime = System.currentTimeMillis();

            for (DataSnapshot collectorSnapshot : dataSnapshot.getChildren()) {
                try {
                    String collectorStatus = String.valueOf(collectorSnapshot.child("collectorStatus").getValue());

                    // if the collector is already expired, it will have only one field
                    if (collectorSnapshot.getChildrenCount() == 1 && Collector.EXPIRED.equals(collectorStatus)) {
                        continue;
                    }

                    // a collector without a time frame cannot be classified, leave it alone
                    if (!collectorSnapshot.hasChild("collectorStartTime") || !collectorSnapshot.hasChild("collectorEndTime")) {
                        Log.w(TAG, "Skipping collector " + collectorSnapshot.getKey() + ": it has no time frame");
                        continue;
                    }

                    // retrieve relevant attributes, the key of the node is the collector id
                    String collectorId = collectorSnapshot.getKey();
                    long collectorStartTime = optLong(collectorSnapshot, "collectorStartTime");
                    long collectorEndTime = optLong(collectorSnapshot, "collectorEndTime");

                    // check if the collector time frame contains the current time
                    if (!Collector.DELETED.equals(collectorStatus)) {
                        String currentStatus;
                        if (currentTime >= collectorStartTime && currentTime <= collectorEndTime) {
                            currentStatus = Collector.ACTIVE;
                        } else if (currentTime < collectorStartTime) {
                            currentStatus = Collector.NOTYETSTARTED;
                        } else {
                            currentStatus = Collector.EXPIRED;
                        }

                        if (!currentStatus.equals(collectorStatus)) {
                            // update collector status
                            databaseReference.child(collectorId).child("collectorStatus").setValue(currentStatus);
                        }
                    }
                } catch (RuntimeException e) {
                    // one malformed collector must not prevent the others from being refreshed
                    Log.e(TAG, "update all collectors: skipping collector " + collectorSnapshot.getKey(), e);
                }
            }
        });
    }


    public void retrieveDatafieldsWithCollectorId(String collectorId, FirebaseCallback firebaseCallback) {
        try {
            DatabaseReference databaseReference = db.getReference(Datafield.class.getSimpleName());
            Query query = databaseReference.orderByChild("collectorId").equalTo(collectorId);
            query.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "retrieve datafield with collector id: Failed to launch connection to firebase.", task.getException());
                    firebaseCallback.onErrorResponse(exceptionOf(task.getException(), "Failed to launch connection to firebase."));
                    return;
                }

                ArrayList<Datafield> datafields = new ArrayList<>();
                try {
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        datafields.add(parseDatafield(snapshot, collectorId));
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "retrieve datafield with collector id: Failed to parse datafields.", e);
                    firebaseCallback.onErrorResponse(e);
                    return;
                }
                // Call firebase callback to update datafields
                firebaseCallback.onResponse(datafields);
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "retrieve datafield with collector id: invalid request for collector " + collectorId, e);
            firebaseCallback.onErrorResponse(e);
        }
    }

    // retrieve all collectors on firebase
    public void retrieveAllCollectors(FirebaseCallback firebaseCallback) {
        try {
            DatabaseReference databaseReference = db.getReference(Collector.class.getSimpleName());
            databaseReference.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "retrieve all collectors: Failed to launch connection to firebase.", task.getException());
                    firebaseCallback.onErrorResponse(exceptionOf(task.getException(), "Failed to launch connection to firebase."));
                    return;
                }

                ArrayList<Collector> collectors = new ArrayList<>();
                try {
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        collectors.add(parseCollector(snapshot));
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "retrieve all collectors: Failed to parse collectors.", e);
                    firebaseCallback.onErrorResponse(e);
                    return;
                }
                // Call firebase callback to update collectors
                firebaseCallback.onResponse(collectors);
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "retrieve all collectors: invalid request", e);
            firebaseCallback.onErrorResponse(e);
        }
    }

    // retrieve data with datafieldId
    public void retrieveDataWithDatafieldId(String datafieldId, FirebaseCallback firebaseCallback) {
        try {
            DatabaseReference databaseReference = db.getReference(Data.class.getSimpleName());
            // get the access to the data table
            Query query = databaseReference.orderByChild("datafieldId").equalTo(datafieldId);
            query.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "retrieve data with datafield id: Failed to launch connection to firebase.", task.getException());
                    firebaseCallback.onErrorResponse(exceptionOf(task.getException(), "Failed to launch connection to firebase."));
                    return;
                }

                ArrayList<Data> datas = new ArrayList<>();
                try {
                    for (DataSnapshot snapshot : task.getResult().getChildren()) {
                        // TODO: if creator, add all data when data.datafieldId.creatorId == self.userId
                        datas.add(parseData(snapshot));
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "retrieve data with datafield id: Failed to parse data.", e);
                    firebaseCallback.onErrorResponse(e);
                    return;
                }
                // Call firebase callback to update datas
                firebaseCallback.onResponse(datas);
            });
        } catch (RuntimeException e) {
            Log.e(TAG, "retrieve data with datafield id: invalid request for datafield " + datafieldId, e);
            firebaseCallback.onErrorResponse(e);
        }
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
