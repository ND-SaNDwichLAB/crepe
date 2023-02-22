package com.example.crepe;

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

import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.database.Datafield;
import com.example.crepe.demonstration.DemonstrationUtil;
import com.example.crepe.graphquery.ontology.OntologyQuery;
import com.example.crepe.graphquery.ontology.UISnapshot;
import com.example.crepe.graphquery.thread.GraphQueryThread;

import java.util.ArrayList;
import java.util.List;
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

        // for almost all accessibility events, we want to re-run existing stored graph queries to see if there are new results

        // 1. retrieve all stored graph queries and stored results
        // TODO maybe optimize: only re-retrieve graph queries if there are actually new queries being added to the database,
        // meaning we need to have a shared instance of such queries
        // TODO use only 1 dbManager for the activity

        List<Collector> collectors = dbManager.getAllCollectors();
        List<Datafield> datafields = dbManager.getAllDatafields();

        // get a UISnapshot and all nodes on screen
        uiSnapshot = generateUISnapshot(accessibilityEvent);
        allNodeList = getAllNodesOnScreen();

        // for each collector, run the graph query on the uiSnapshot
        for (Datafield datafield : datafields) {
            // Submit a task to the thread pool
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    // Start a new graph query thread and execute the graph query
                    // 1. convert the graph query string to a graph query object
                    OntologyQuery currentQuery = OntologyQuery.deserialize(datafield.getGraphQuery());
                    // TODO: 2. run the graph query on the uiSnapshot
                    // TODO: 3. if there are new results (by comparing new and old entries using dbManager), send the new results to the server
                }
            });

        }

        // TODO: check if there is an existing thread. Use a thread pool

        // 2. for each graph query, run the graph query on the uiSnapshot
        // 3. if there are new results (by comparing new and old entries using dbManager), send the new results to the server


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
//        Map<Node, AccessibilityNodeInfo> nodeInfoMap =  uiSnapshot.getNodeAccessibilityNodeInfoMap();
//        Log.i("uisnapshot", String.valueOf(uiSnapshot));
//        Log.i("uisnapshot", String.valueOf(nodeInfoMap));

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
