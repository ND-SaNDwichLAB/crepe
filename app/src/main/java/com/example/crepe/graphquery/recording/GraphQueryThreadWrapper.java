package com.example.crepe.graphquery.recording;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import com.example.crepe.CrepeAccessibilityService;
import com.example.crepe.database.Collector;
import com.example.crepe.database.Data;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.demonstration.DemonstrationUtil;
import com.example.crepe.graphquery.ontology.OntologyQuery;
import com.example.crepe.graphquery.ontology.UISnapshot;
import com.example.crepe.network.FirebaseCommunicationManager;

import java.util.List;

public class GraphQueryThreadWrapper {
    private GraphQueryThread graphQueryThread;
    private List<Collector> collectors;
    private Context context;

    // constructor
    public GraphQueryThreadWrapper(List<Collector> collectors, Context c) {
        this.collectors = collectors;
        this.context = c;
        graphQueryThread = new GraphQueryThread();
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
        taskSender();
    }

    // method for task to be executed in the graph query thread
    public void runTask() {
        graphQueryThread.handler.post(new Runnable() {
            @Override
            public void run() {
                // Get the UI snapshot
                UISnapshot uiSnapshot = CrepeAccessibilityService.getsSharedInstance().generateUISnapshot();
                for (Collector collector : collectors) {
                    // get collector id
                    String collectorId = collector.getCollectorId();
                    // get creator id
                    String creatorId = collector.getCreatorUserId();
                    // Get the graph query. data type: list of pairs of strings
                    List<Pair<String, String>> dataFields = collector.getDataFields();
                    // for each graph query, send the graph query and the UI snapshot to the
                    for (Pair<String, String> dataField : dataFields) {
                        String graphQuery = dataField.first;
                        // TODO: YUWEN run the graph query on the UI snapshot; use method ExecuteOn
                        // TODO: need to convert the graph query from a string to a graph query object
                        String result = graphQuery.executeOn(uiSnapshot);
                        // TODO: get datafield id
                        String dataFieldId = "";

                        // create data object
                        long timestamp = System.currentTimeMillis();
                        Data data = new Data(collectorId, dataFieldId, creatorId, timestamp, result);
                        // add data to the database
                        // check the mode of the collector. if local, add to local database. if remote, add to remote database
                        if (collector.getMode().equals("Local")) {
                            // add new data to the database
                            DatabaseManager databaseManager = new DatabaseManager(context);
                            databaseManager.addData(data);
                        } else if (collector.getMode().equals("Remote")) {
                            // add new data to the database
                            DatabaseManager databaseManager = new DatabaseManager(context);
                            FirebaseCommunicationManager firebaseCommunicationManager = new FirebaseCommunicationManager();
                            firebaseCommunicationManager.putData(data).addOnSuccessListener(suc->{
                                Log.i("Firebase","Successfully added data " + data.getDataId() + " to firebase.");
                            }).addOnFailureListener(er->{
                                Log.e("Firebase","Failed to add data " + data.getDataId() + " to firebase.");
                            });
                            databaseManager.addData(data);
                        }
                    }
                }

            }
        });
    }

    // method for checking if the graph query thread is running
    public boolean isThreadRunning() {
        return graphQueryThread.isAlive();
    }

    // check if the accessibilityEvent is update
    // TODO: YUWEN implement this method
    public boolean isAccessibilityEventUpdate() {
        return true;
    }

    // always run this method in the graph query thread
    public void taskSender() {
        // check if the graph query thread is running
        while (isThreadRunning()) {
            // check if the accessibility event is update
            if (isAccessibilityEventUpdate()) {
                // run the task
                runTask();
            }
            // sleep for 1 second
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}