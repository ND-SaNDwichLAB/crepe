package com.example.crepe.graphquery.recording;

import android.os.Message;

import com.example.crepe.database.Collector;
import com.example.crepe.graphquery.ontology.UISnapshot;

import java.util.List;

public class GraphQueryThreadWrapper {
    private GraphQueryThread graphQueryThread;
    private List<Collector> collectors;

    // constructor
    public GraphQueryThreadWrapper(List<Collector> collectors) {
        this.collectors = collectors;
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

    // method for task to be executed in the graph query thread
    public void runTask() {
        graphQueryThread.handler.post(new Runnable() {
            @Override
            public void run() {
                for (Collector collector : collectors) {
                    // Get the graph query
                    List<String> dataFields = collector.getDataFields();

                    // Get the UI snapshot
                    UISnapshot uiSnapshot = new UISnapshot();

                    // Send the graph query and the UI snapshot to the server



            }
        });
    }

    // method for checking if the graph query thread is running
    public boolean isThreadRunning() {
        return graphQueryThread.isAlive();
    }
}
