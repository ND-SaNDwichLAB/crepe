package com.example.crepe.graphquery.recording;

import android.os.Message;

import com.example.crepe.graphquery.ontology.UISnapshot;

public class GraphQueryThreadWrapper {
    private GraphQueryThread graphQueryThread;
    private String graphQuery;

    // constructor
    public GraphQueryThreadWrapper(String graphQuery, UISnapshot uiSnapshot) {
        this.graphQuery = graphQuery;
        this.graphQueryThread = new GraphQueryThread(graphQuery);
    }

    // method for starting the graph query thread
    public void startThread() {
        // Start the thread
        graphQueryThread.start();
    }

    // method for stopping the graph query thread
    public void stopThread() {
        // Stop the thread
        graphQueryThread.looper.quit();
    }

    // check if the ui snapshot is updated
    public boolean isUISnapshotUpdated() {

    }

    // method for task to be executed in the graph query thread
    public void runTask() {
        graphQueryThread.handler.post(new Runnable() {
            @Override
            public void run() {



            }
        });
    }
}
