package edu.nd.crepe;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.Data;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.demonstration.DemonstrationUtil;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import edu.nd.crepe.graphquery.ontology.SugiliteEntity;
import edu.nd.crepe.graphquery.ontology.UISnapshot;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class CrepeAccessibilityService extends AccessibilityService {

    private static final String TAG = "crepeAccessibilityService";
    private static final long REFRESH_INTERVAL = 60;

    private static CrepeAccessibilityService sSharedInstance;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private DatabaseManager dbManager;

    private FirebaseCommunicationManager firebaseCommunicationManager;

    // maintain a thread pool inside of the accessibility for running graph queries
     private ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

    private WindowManager windowManager;
    private String currentAppActivityName;
    private String currentPackageName;
    private Set<SugiliteEntity> prevResults = new HashSet<>(); // used to check if the retrieved data exists in the previous frame
    private UISnapshot uiSnapshot;
    private List<Collector> collectors;
    private List<Datafield> datafields;
    private List<AccessibilityNodeInfo> allNodeList;

    public UISnapshot getCurrentUiSnapshot() {
        return uiSnapshot;
    }
    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        dbManager = DatabaseManager.getInstance(this.getApplicationContext());
        firebaseCommunicationManager = new FirebaseCommunicationManager(this);

        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshCollector();
            }
        }, 0, REFRESH_INTERVAL, TimeUnit.MINUTES);


        refreshCollector();
    }

    public void refreshCollector() {
        // retrieve all stored collectors and datafields
        collectors = dbManager.getActiveCollectors();
        datafields = dbManager.getAllDatafields();

        // refresh collector status based on current time
        for (Collector collector : collectors) {
            collector.autoSetCollectorStatus();
            dbManager.updateCollectorStatus(collector);
        }

        if (collectors != null && !collectors.isEmpty()) {
            firebaseCommunicationManager.updateAllCollectors();
        }

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
                TYPE_WINDOW_CONTENT_CHANGED,
                TYPE_VIEW_SCROLLED,
                TYPE_VIEW_TEXT_SELECTION_CHANGED,
                TYPE_ANNOUNCEMENT);

        if (!targetEventTypes.contains(accessibilityEvent.getEventType())) {
            Log.i("accessibilityEvent", accessibilityEvent.getEventType() + " not in the target event list");
        } else {

            // update the list of apps we need to monitor
            List<String> monitoredApps = new ArrayList<>();
            for (Collector collector : collectors) {
                monitoredApps.add(collector.getAppPackage());
            }

            try {
                // get the current package
                currentPackageName = accessibilityEvent.getPackageName().toString();
            } catch (NullPointerException e) {
                Log.e(TAG, "Null pointer exception when getting package name");
                return;
            }

            // update a UISnapshot and all nodes on screen
            uiSnapshot = generateUISnapshot(accessibilityEvent);
            allNodeList = getAllNodesOnScreen();

            if (collectors.size() > 0) {
                Log.i("accessibilityEvent", "Accessibility Event Type: " + accessibilityEvent.getEventType() + ", opening a new thread, current count: " + threadPool.getActiveCount());

                // use this as a time window to determine if we should save the result
                AtomicLong lastSavedResultTimestamp = new AtomicLong(0);

                // Submit a task to the thread pool
                threadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> collectorIdsToStart = new ArrayList<>();
                        ArrayList<Datafield> datafieldsToStart = new ArrayList<>();
                        for (Collector collector : collectors) {
                            collectorIdsToStart.add(collector.getCollectorId());
                            // also add the datafields that are associated with this collector
                            for (Datafield datafield : datafields) {
                                if (datafield.getCollectorId().equals(collector.getCollectorId())) {
                                    datafieldsToStart.add(datafield);
                                }
                            }
                        }

                        Log.i("query execution", "collectorIdsToStart: " + collectorIdsToStart.toString());
                        Log.i("query execution", "datafieldsToStart: " + datafieldsToStart.toString());

                        // for each datafield, run the graph query on the uiSnapshot
                        for (Datafield datafield : datafieldsToStart) {
                            Log.i("query execution", "starting datafield: " + datafield.toString());
                            // Start a new graph query thread and execute the graph query
                            // 1. convert the graph query string to a graph query object
                            OntologyQuery currentQuery = OntologyQuery.deserialize(datafield.getGraphQuery());
                            // 2. run the graph query on the uiSnapshot
                            Set<SugiliteEntity> currentResults = currentQuery.executeOn(uiSnapshot);
                            Log.i("query execution", "currentResults: " + currentResults);

                            // 3. store the new results in the database
                            for (SugiliteEntity result : currentResults) {
                                Log.i("query execution", "result: " + result);
                                Log.i("query execution", "prevResults: " + prevResults);
                                Log.i("query execution", "prevResults.contains(result): " + prevResults.contains(result));
                                    if (!prevResults.contains(result) && System.currentTimeMillis() - lastSavedResultTimestamp.get() > 1000) {
                                        // if the result is not in the previous results, add it to the database
                                        long timestamp = System.currentTimeMillis();
                                        // the data id is the collector id + "%" + timestamp
                                        Data resultData = new Data(datafield.getCollectorId() + "%" + timestamp, datafield.getDataFieldId(), MainActivity.currentUser.getUserId(), result.saveToDatabaseAsString());

                                        try {
                                            dbManager.addData(resultData);
                                            Log.i("database", "added data: " + resultData);

                                            // send the data to firebase
                                            firebaseCommunicationManager.putData(resultData).addOnSuccessListener(suc -> {
                                                Log.i("Firebase", "Successfully added collector " + resultData.getDataContent() + " to firebase.");
                                            }).addOnFailureListener(er -> {
                                                Log.e("Firebase", "Failed to add collector " + resultData.getDataContent() + " to firebase.");
                                            });

                                            // update the last saved result timestamp
                                            lastSavedResultTimestamp.set(System.currentTimeMillis());

                                        } catch (Exception e) {
                                            Log.i("database", "failed to add data: " + resultData.toString());
                                            e.printStackTrace();
                                        }
                                    }
                            }
                            prevResults = currentResults;

                        }

                    }

                });
            }

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
                Log.e(TAG, "Failed to get the activity name for: " + componentName);
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
        scheduler.shutdown();
        return true;
    }

    public static CrepeAccessibilityService getsSharedInstance() {
        return sSharedInstance;
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class accessibilityService) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString != null && prefString.contains(context.getPackageName() + "/" + accessibilityService.getName());
    }


}
