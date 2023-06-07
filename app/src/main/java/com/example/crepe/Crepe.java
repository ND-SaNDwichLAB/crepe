package com.example.crepe;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class Crepe extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Firebase persistence only once and before any usage of the database instance
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
