package com.example.crepe.graphquery.recording;

import android.os.Looper;
import android.os.Handler;
import android.util.Log;

import com.example.crepe.CrepeAccessibilityService;
import com.example.crepe.graphquery.ontology.UISnapshot;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

// This class is responsible for retrieving the data from the UI and sending it to the server

public class GraphQueryThread extends Thread {
    private static final String TAG = "GraphQueryThread";
    private CrepeAccessibilityService service;
    private String graphQuery;
    public Handler handler;
    public Looper looper;


    public GraphQueryThread() {
    }


    @Override
    public void run() {
        Looper.prepare();
        looper = Looper.myLooper();
        handler = new Handler();
        Looper.loop();
        Log.d(TAG, "GraphQueryThread is running");
        };

    }
}
