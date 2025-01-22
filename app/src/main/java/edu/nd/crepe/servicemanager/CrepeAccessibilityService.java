package edu.nd.crepe.servicemanager;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_GESTURE_DETECTION_END;
import static android.view.accessibility.AccessibilityEvent.TYPE_GESTURE_DETECTION_START;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END;
import static android.view.accessibility.AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START;
import static android.view.accessibility.AccessibilityEvent.TYPE_TOUCH_INTERACTION_START;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_ENTER;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_HOVER_EXIT;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_LONG_CLICKED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TARGETED_BY_SCROLL;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

import static edu.nd.crepe.MainActivity.currentUser;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.navigation.Navigation;

import org.json.JSONException;
import org.json.JSONObject;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.Data;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.demonstration.DemonstrationUtil;
import edu.nd.crepe.demonstration.NavigationBarUtil;
import edu.nd.crepe.demonstration.OverlayViewManager;
import edu.nd.crepe.graphquery.model.Node;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import edu.nd.crepe.graphquery.ontology.SugiliteEntity;
import edu.nd.crepe.graphquery.ontology.UISnapshot;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
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
    private AtomicLong lastSavedResultTimestamp = new AtomicLong(0);
    private OverlayViewManager overlayViewManager;

    private List<Integer> targetEventTypes = Arrays.asList(
            TYPE_VIEW_TEXT_CHANGED,
            TYPE_WINDOW_STATE_CHANGED,
            TYPE_NOTIFICATION_STATE_CHANGED,
            TYPE_WINDOW_CONTENT_CHANGED,
            // interaction events
//            TYPE_VIEW_CLICKED,
//            TYPE_VIEW_LONG_CLICKED,
//            TYPE_VIEW_SCROLLED,
//            TYPE_VIEW_HOVER_ENTER,
//            TYPE_VIEW_HOVER_EXIT,
//            TYPE_VIEW_TARGETED_BY_SCROLL,
//            TYPE_TOUCH_INTERACTION_START,
//            TYPE_TOUCH_EXPLORATION_GESTURE_START,
//            TYPE_TOUCH_EXPLORATION_GESTURE_END,
//            TYPE_GESTURE_DETECTION_START,
//            TYPE_GESTURE_DETECTION_END,

            TYPE_VIEW_TEXT_SELECTION_CHANGED,
            TYPE_ANNOUNCEMENT);

    private List<Integer> interactionEventTypes = Arrays.asList(
            TYPE_VIEW_CLICKED,
            TYPE_VIEW_LONG_CLICKED,
            TYPE_VIEW_SCROLLED,
            TYPE_VIEW_HOVER_ENTER,
            TYPE_VIEW_HOVER_EXIT,
            TYPE_VIEW_TARGETED_BY_SCROLL,
            TYPE_TOUCH_INTERACTION_START,
            TYPE_TOUCH_EXPLORATION_GESTURE_START,
            TYPE_TOUCH_EXPLORATION_GESTURE_END,
            TYPE_GESTURE_DETECTION_START,
            TYPE_GESTURE_DETECTION_END
    );

    // used for setting a foreground notification channel
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "CrepeAccessibilityServiceChannel";

    private Handler handler = new Handler();
    private Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            updateHeartbeatTimestamp();
            // implement a heartbeat mechanism to check the service running status
            // 10 minute
            int HEARTBEAT_INTERVAL = 1000 * 600;
            handler.postDelayed(this, HEARTBEAT_INTERVAL);
        }
    };

    public UISnapshot getCurrentUiSnapshot() {
        return uiSnapshot;
    }

    private ScheduledExecutorService scheduler;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        dbManager = DatabaseManager.getInstance(this.getApplicationContext());
        firebaseCommunicationManager = new FirebaseCommunicationManager(this.getApplicationContext());
        overlayViewManager = new OverlayViewManager(this.getApplicationContext());

        // refresh the collector status for current collectors every REFRESH_INTERVAL minutes
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                refreshAllCollectorStatus();
            }
        }, 0, REFRESH_INTERVAL, TimeUnit.MINUTES);
        refreshAllCollectorStatus();

        // clear up the prevResults every 10 seconds, so that we do not miss collecting reoccurring data in the same screen
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                prevResults.clear();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CrepeAccessibilityService getService() {
            // Return this instance of CrepeAccessibilityService so clients can call public methods
            return CrepeAccessibilityService.this;
        }
    }   // however, we cannot override the onBind method because it's declared final for accessibilityServices


    // the below 3 functions are used to get around not being able to override onBind for accessibilityServices
    @Override
    public void onServiceConnected() {
        sSharedInstance = this;
        // for crepe foreground service

        Notification notification = CrepeNotificationManager.showNotification(this, "Crepe successfully running...");

        // Set the foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        handler.post(heartbeatRunnable);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedInstance = null;
        scheduler.shutdown();
        return true;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(heartbeatRunnable);
        super.onDestroy();
    }

    public static CrepeAccessibilityService getsSharedInstance() {
        return sSharedInstance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

        collectors = dbManager.getActiveCollectors();

        if (!targetEventTypes.contains(accessibilityEvent.getEventType())) {
            Log.i("accessibilityEvent", accessibilityEvent.getEventType() + " not in the target event list");
        } else if (collectors.isEmpty()) {
            Log.i("accessibilityEvent", "No collectors to monitor");
        } else {
            Log.i("accessibilityEvent", "Accessibility Event Type: " + AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType()) + " currently checking...");
            // update the list of apps we need to monitor
            List<String> monitoredAppPackages = new ArrayList<>();
            for (Collector collector : collectors) {
                monitoredAppPackages.add(collector.getAppPackage());
            }

            try {
                // get the current package
                currentPackageName = accessibilityEvent.getPackageName().toString();
                // if we are not monitoring this app, return
                if (!monitoredAppPackages.contains(currentPackageName)) {
                    Log.i("accessibilityEvent", "Current package " + currentPackageName + " not in the monitored app list");
                    return;
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Null pointer exception when getting package name");
                return;
            }

            // update a UISnapshot and all nodes on screen
            uiSnapshot = generateUISnapshot(accessibilityEvent);
            allNodeList = getAllNodesOnScreen();

            Log.i("accessibilityEvent", "Accessibility Event Type: " + accessibilityEvent.getEventType() + ", opening a new thread, current count: " + threadPool.getActiveCount());

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

                    // for each datafield, run the graph query on the uiSnapshot
                    for (Datafield datafield : datafieldsToStart) {
                        Log.i("query execution", "starting datafield: " + datafield.toString());
                        // Start a new graph query thread and execute the graph query
                        // 1. convert the graph query string to a graph query object
                        OntologyQuery currentQuery = OntologyQuery.deserialize(datafield.getGraphQuery());
                        // 2. run the graph query on the uiSnapshot
                        assert currentQuery != null;
                        Set<SugiliteEntity> currentResults = currentQuery.executeOn(uiSnapshot);
                        Log.i("query execution", "currentResults: " + currentResults);

                        // 3. store the new results in the database
                        for (SugiliteEntity result : currentResults) {
                            Log.i("query execution", "result: " + result);
                            Log.i("query execution", "prevResults: " + prevResults);
                            Log.i("query execution", "prevResults.contains(result): " + prevResults.contains(result));
//                            if (!prevResults.contains(result) && System.currentTimeMillis() - lastSavedResultTimestamp.get() > 4000) {  // prevent duplicate results from being saved, with the interval of 4 seconds
//                            if (!prevResults.contains(result)) {  // prevent duplicate results from being saved, prevResults is cleared every 10 seconds (see code on the top of this file)
                            Log.i("Timestamps", "System.currentTimeMillis(): " + System.currentTimeMillis() + ", lastSavedResultTimestamp: " + lastSavedResultTimestamp.get() + ", difference: " + (System.currentTimeMillis() - lastSavedResultTimestamp.get()));


                            try {
                                // show overlay
                                Rect overlayLocation = new Rect();
                                Node resultNode = (Node) result.getEntityValue();
                                if (resultNode == null) {
                                    Log.e("query execution", "cannot convert result to a Node, null");
                                }
                                overlayLocation = Rect.unflattenFromString(resultNode.getBoundsInScreen());
                                if (overlayLocation == null) {
                                    Log.e("query execution", "overlay location is null");
                                }
                                // adjust for the status bar height
                                NavigationBarUtil navigationBarUtil = new NavigationBarUtil();
                                int statusBarHeight = navigationBarUtil.getStatusBarHeight(getApplicationContext());
                                overlayLocation.offset(0, (-1) * statusBarHeight);
                                overlayViewManager.showRectOverlay(overlayLocation, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, 0x80FF0000, 5);
                            } catch (Exception e) {
                                Log.e("query execution", "failed to show overlay");
                                e.printStackTrace();
                            }

                            // if the result is not in the previous results, add it to the database
                            long timestamp = System.currentTimeMillis();
                            String dataString = processDataString(accessibilityEvent, result);
                            // the data id is the collector id + "%" + timestamp
                            Data resultData = new Data(datafield.getCollectorId() + "%" + timestamp, datafield.getDatafieldId(), currentUser.getUserId(), dataString);

                            try {
                                dbManager.addData(resultData);
                                Log.i("database", "added data: " + resultData);

                                // send the data to firebase
                                firebaseCommunicationManager.putData(resultData).addOnSuccessListener(suc -> {
                                    Log.i("Firebase", "Successfully added collector " + resultData.getDataContent() + " to firebase.");
                                }).addOnFailureListener(er -> {
                                    Log.e("Firebase", "Failed to add collector " + resultData.getDataContent() + " to firebase. Error: " + er.getMessage());
                                });

                                // update the last saved result timestamp
                                lastSavedResultTimestamp.set(System.currentTimeMillis());

                            } catch (Exception e) {
                                Log.i("database", "failed to add data: " + resultData.toString());
                                e.printStackTrace();
                            }
//                            }
                        }
                        prevResults.addAll(currentResults);
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
                Log.e(TAG, "Failed to get the activity name, with an accessibility event, for: " + componentName);
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
            Log.e(TAG, "Failed to get the activity name, no accessibility event, for: " + componentName);
        }

        uiSnapshot = new UISnapshot(windowManager.getDefaultDisplay(), rootNode, true, currentPackageName, currentAppActivityName);
        return uiSnapshot;

    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted");
    }

    public List<AccessibilityNodeInfo> getMatchingNodeFromClickWithContent(float clickX, float clickY) {

        this.allNodeList = getAllNodesOnScreen();

        List<AccessibilityNodeInfo> matchingNodeInfoList = DemonstrationUtil.findMatchingNodeFromClick(this.allNodeList, clickX, clickY);

        List<AccessibilityNodeInfo> resultNodeList = new ArrayList<>();

        if (matchingNodeInfoList != null && matchingNodeInfoList.size() > 0) {
            for (AccessibilityNodeInfo matchingNode : matchingNodeInfoList) {
                // if the target element contains text, we add it
                if (matchingNode.getText() != null && !matchingNode.getText().toString().isEmpty()) {
                    resultNodeList.add(matchingNode);
                }
                // if the target element contains content description, we also add it
                if (matchingNode.getContentDescription() != null && !matchingNode.getContentDescription().toString().isEmpty()) {
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

    public static boolean isAccessibilityServiceEnabled(Context context, Class accessibilityService) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString != null && prefString.contains(context.getPackageName() + "/" + accessibilityService.getName());
    }


    public void refreshAllCollectorStatus() {
        // retrieve all stored collectors and datafields
        collectors = dbManager.getActiveCollectors();
        datafields = dbManager.getAllDatafields();

        // refresh collector status based on current time
        for (Collector collector : collectors) {
            Boolean collectorUpdated = collector.autoSetCollectorStatus();
            if (collectorUpdated) {
                dbManager.updateCollectorStatus(collector);
            }
        }

        if (collectors != null && !collectors.isEmpty()) {
            firebaseCommunicationManager.updateAllCollectors();
        }

    }

    private void updateHeartbeatTimestamp() {
        // Update timestamp in local database
        long currentTime = System.currentTimeMillis();
        if (currentUser == null) {
            return;
        }
        currentUser.setLastHeartBeat(currentTime);
        dbManager.updateUser(currentUser);
        // send the updated timestamp in the updated user info to firebase
        HashMap<String, Object> userUpdates = new HashMap<>();
        ArrayList<String> updatedUserCollectors = currentUser.getCollectorsForCurrentUser();
        userUpdates.put("lastHeartBeat", currentTime);
        firebaseCommunicationManager.updateUser(currentUser.getUserId(), userUpdates);
    }

    private String processDataString(AccessibilityEvent event, SugiliteEntity result) {
        JSONObject jsonObject = new JSONObject();

        Date date = new Date(System.currentTimeMillis());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String dateString = dateFormat.format(date);

        try {
            jsonObject.put("eventType", AccessibilityEvent.eventTypeToString(event.getEventType()));
            jsonObject.put("timestamp", dateString);
            if (result.getType() == Node.class) {
                jsonObject.put("text", ((Node) result.getEntityValue()).getText());
                jsonObject.put("contentDescription", ((Node) result.getEntityValue()).getContentDescription());
            } else {
                Log.i("process results", "result node type: " + result.getType());
                jsonObject.put("value", ((Node) result.getEntityValue()).toString());
            }
            if (interactionEventTypes.contains(event.getEventType())) {
                jsonObject.put("interaction", event.getEventType());
                jsonObject.put("interactionSource", event.getSource());
                jsonObject.put("interactionTargetClass", event.getClassName());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
