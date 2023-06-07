package edu.nd.crepe.graphquery.thread;

import android.util.Log;

// This class is responsible for retrieving the data from the UI and sending it to the server

public class GraphQueryThread extends Thread {
    private static final String TAG = "GraphQueryThread";


    public GraphQueryThread() {
    }


    @Override
    public void run() {
        Log.d(TAG, "GraphQueryThread is running");
    };

}
