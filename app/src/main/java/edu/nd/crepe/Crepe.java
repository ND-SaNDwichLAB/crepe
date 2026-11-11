package edu.nd.crepe;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.FirebaseDatabase;

public class Crepe extends Application {
    private static final String TAG = "Crepe";

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);
        // Enable Firebase offline persistence only once and before any usage of the database instance.
        // Firebase throws if the database was already used in this process; in that case it simply
        // keeps running with whatever persistence setting it already has.
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (DatabaseException e) {
            Log.w(TAG, "Could not enable Firebase offline persistence: " + e.getMessage());
        }
    }
}
