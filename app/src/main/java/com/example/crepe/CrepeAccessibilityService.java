package com.example.crepe;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_ENTER;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_EXIT;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.example.crepe.MainActivity.androidId;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.example.crepe.database.Collector;
import com.example.crepe.database.Data;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.database.Datafield;
import com.example.crepe.demonstration.DemonstrationUtil;
import com.example.crepe.graphquery.ontology.OntologyQuery;
import com.example.crepe.graphquery.ontology.SugiliteEntity;
import com.example.crepe.graphquery.ontology.UISnapshot;
import com.example.crepe.graphquery.thread.GraphQueryThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CrepeAccessibilityService extends AccessibilityService {

    private static final String TAG = "crepeAccessibilityService: ";

    private static CrepeAccessibilityService sSharedInstance;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private DatabaseManager dbManager = new DatabaseManager(this);

    // maintain a thread pool inside of the accessibility for running graph queries
     private ExecutorService threadPool = Executors.newFixedThreadPool(10);

    private WindowManager windowManager;
    private String currentAppActivityName;
    private String currentPackageName;
    private UISnapshot prevUiSnapshot;  // used to check if the retrieved data exists in the previous frame
    private UISnapshot uiSnapshot;
    private List<AccessibilityNodeInfo> allNodeList;

    public UISnapshot getCurrentUiSnapshot() {
        return uiSnapshot;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CrepeAccessibilityService getService() {
            // Return this instance of CrepeAccessibilityService so clients can call public methods
            return CrepeAccessibilityService.this;
        }
    }   // however, we cannot override the onBind method because it's declared final for accessibilityServices

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

        List<Integer> targetEventTypes = Arrays.asList(
                TYPE_VIEW_TEXT_CHANGED,
                TYPE_WINDOW_STATE_CHANGED,
                TYPE_NOTIFICATION_STATE_CHANGED,
                TYPE_VIEW_HOVER_ENTER,
                TYPE_VIEW_HOVER_EXIT,
                TYPE_WINDOW_CONTENT_CHANGED,
                TYPE_VIEW_SCROLLED,
                TYPE_VIEW_TEXT_SELECTION_CHANGED,
                TYPE_ANNOUNCEMENT,
                TYPE_WINDOWS_CHANGED,
                TYPE_VIEW_CONTEXT_CLICKED);

        if (!targetEventTypes.contains(accessibilityEvent.getEventType())) {
            Log.i("accessbilityEvent", accessibilityEvent.toString() + " not in the target event list");
        } else {

            // update a UISnapshot and all nodes on screen
            prevUiSnapshot = uiSnapshot;
            uiSnapshot = generateUISnapshot(accessibilityEvent);
            allNodeList = getAllNodesOnScreen();
            // Submit a task to the thread pool
            threadPool.submit(new Runnable() {
                @Override
                public void run() {


                    // retrieve all stored collectors and datafields
                    List<Collector> collectors = dbManager.getActiveCollectors();
                    List<Datafield> datafields = dbManager.getAllDatafields();

                    // update the list of apps we need to monitor
                    List<String> monitoredApps = new ArrayList<>();
                    for (Collector collector : collectors) {
                        monitoredApps.add(collector.getAppPackage());
                    }

                    // get the current package
                    currentPackageName = accessibilityEvent.getPackageName().toString();

                    // if the current package is in the monitored apps list, start the data collection for collectors that are monitoring this app
                    if (currentPackageName != null && monitoredApps.contains(currentPackageName)) {
                        ArrayList<String> collectorIdsToStart = new ArrayList<>();
                        ArrayList<Datafield> datafieldsToStart = new ArrayList<>();
                        if (collectors != null && datafields != null) {
                            for (Collector collector : collectors) {
                                if (collector.getAppPackage().equals(currentPackageName)) {
                                    // if the current package is the same as the collector's app name, start the data collection
                                    collectorIdsToStart.add(collector.getCollectorId());
                                    // also add the datafields that are associated with this collector
                                    for (Datafield datafield : datafields) {
                                        if (datafield.getCollectorId().equals(collector.getCollectorId())) {
                                            datafieldsToStart.add(datafield);
                                        }
                                    }

                                }
                            }
                        }

                        // for each datafield, run the graph query on the uiSnapshot
                        if (datafieldsToStart.size() > 0) {
                            for (Datafield datafield : datafieldsToStart) {
                                // Start a new graph query thread and execute the graph query
                                // 1. convert the graph query string to a graph query object
                                OntologyQuery currentQuery = OntologyQuery.deserialize(datafield.getGraphQuery());
                                // 2. run the graph query on the uiSnapshot
                                Set<SugiliteEntity> prevResults = currentQuery.executeOn(prevUiSnapshot);
                                Set<SugiliteEntity> currentResults = currentQuery.executeOn(uiSnapshot);
                                // 3. store the new results in the database
                                for (SugiliteEntity result : currentResults) {
                                    if (!prevResults.contains(result)) {
                                        // if the result is not in the previous results, add it to the database
                                        long timestamp = System.currentTimeMillis();
                                        // the data id is the collector id + "%" + timestamp
                                        Data resultData = new Data(datafield.getCollectorId() + "%" + String.valueOf(timestamp), datafield.getDataFieldId(), androidId, result.saveToDatabaseAsString());
                                        Boolean addDataResult = false;
                                        try {
                                            addDataResult = dbManager.addData(resultData);
                                            Log.i("dataset", "added data: " + resultData.toString());
                                        } catch (Exception e) {
                                            Log.i("dataset", "failed to add data: " + resultData.toString());
                                            e.printStackTrace();
                                        }
                                    }
                                }

                            }

                        }
                    }

                }
            });

        }



    }

    public UISnapshot generateUISnapshot(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (accessibilityEvent.getPackageName() != null && accessibilityEvent.getClassName() != null) {
            ComponentName componentName = new ComponentName(
                    accessibilityEvent.getPackageName().toString(),
                    accessibilityEvent.getClassName().toString()
            );

            try {
                ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);
                Log.i("CurrentActivity", activityInfo.packageName + " : " + activityInfo.name + " : " + AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType()));
                currentAppActivityName = activityInfo.name;
                currentPackageName = activityInfo.packageName;
            } catch (PackageManager.NameNotFoundException e) {
                //e.printStackTrace();
                Log.e(this.getClass().getName(), "Failed to get the activity name for: " + componentName);
            }
        }

        uiSnapshot = new UISnapshot(windowManager.getDefaultDisplay(), rootNode, true, currentPackageName, currentAppActivityName);
        return uiSnapshot;

    }

    // if we are calling from another class and don't have some needed parameters
    public UISnapshot generateUISnapshot() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        ComponentName componentName = taskInfo.get(0).topActivity;

        try {
            ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);
            Log.i("CurrentActivity", activityInfo.packageName + " : " + activityInfo.name);
            currentAppActivityName = activityInfo.name;
            currentPackageName = activityInfo.packageName;
        } catch (PackageManager.NameNotFoundException e) {
            //e.printStackTrace();
            Log.e(this.getClass().getName(), "Failed to get the activity name for: " + componentName);
        }

        uiSnapshot = new UISnapshot(windowManager.getDefaultDisplay(), rootNode, true, currentPackageName, currentAppActivityName);
        return uiSnapshot;

    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted");
    }

    public List<AccessibilityNodeInfo> getMatchingNodeFromClickWithText(float clickX, float clickY) {

        List<AccessibilityNodeInfo> matchingNodeInfoList = DemonstrationUtil.findMatchingNodeFromClick(this.allNodeList, clickX, clickY);

        List<AccessibilityNodeInfo> resultNodeList = new ArrayList<>();

        if (matchingNodeInfoList != null) {
            for (AccessibilityNodeInfo matchingNode: matchingNodeInfoList) {
                // change here
                if(matchingNode.getText() != null && !matchingNode.getText().toString().isEmpty()) {
                    resultNodeList.add(matchingNode);
                }
            }
        }
        if (!resultNodeList.isEmpty()) {
            return resultNodeList;
        } else {
            return null;
        }
    }

    public List<AccessibilityNodeInfo> getAllNodesOnScreen() {
        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        return DemonstrationUtil.preOrderTraverse(rootNodeInfo);
    }

    // the below 3 functions are used to get around not being able to override onBind for accessibilityServices
    @Override
    public void onServiceConnected() {
        sSharedInstance = this;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedInstance = null;
        return true;
    }

    public static CrepeAccessibilityService getsSharedInstance() {
        return sSharedInstance;
    }


}
