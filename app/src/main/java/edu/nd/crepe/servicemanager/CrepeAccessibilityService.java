package edu.nd.crepe.servicemanager;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import edu.nd.crepe.MainActivity;
import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.Data;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.database.User;
import edu.nd.crepe.demonstration.DemonstrationUtil;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import edu.nd.crepe.graphquery.ontology.SugiliteEntity;
import edu.nd.crepe.graphquery.ontology.UISnapshot;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class CrepeAccessibilityService extends AccessibilityService {

    private static final String TAG = "crepeAccessibilityService";
    private static final long REFRESH_INTERVAL = 60;

    // the accessibility events that may carry data worth collecting
    private static final List<Integer> TARGET_EVENT_TYPES = Arrays.asList(
            TYPE_VIEW_TEXT_CHANGED,
            TYPE_WINDOW_STATE_CHANGED,
            TYPE_NOTIFICATION_STATE_CHANGED,
            TYPE_WINDOW_CONTENT_CHANGED,
            TYPE_VIEW_SCROLLED,
            TYPE_VIEW_TEXT_SELECTION_CHANGED,
            TYPE_ANNOUNCEMENT);

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
    // used to check if the retrieved data exists in the previous frame. It is touched by the pool threads
    // that run the queries and by the scheduler thread that clears it, so it has to be a concurrent set.
    private final Set<SugiliteEntity> prevResults = Collections.newSetFromMap(new ConcurrentHashMap<SugiliteEntity, Boolean>());
    private volatile UISnapshot uiSnapshot;
    // these two are replaced as a whole by the refresh thread and read by the event and pool threads
    private volatile List<Collector> collectors = new ArrayList<>();
    private volatile List<Datafield> datafields = new ArrayList<>();
    private List<AccessibilityNodeInfo> allNodeList;
    private AtomicLong lastSavedResultTimestamp = new AtomicLong(0);
    // used for setting a foreground notification channel
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "CrepeAccessibilityServiceChannel";

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                updateHeartbeatTimestamp();
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to update the heartbeat", e);
            } finally {
                // implement a heartbeat mechanism to check the service running status
                // 10 minute. Always schedule the next beat, a single failure must not stop the heartbeat for good.
                int HEARTBEAT_INTERVAL = 1000 * 600;
                handler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
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
        firebaseCommunicationManager = new FirebaseCommunicationManager(this);

        // refresh the collector status for current collectors every REFRESH_INTERVAL minutes
        // NOTE: an exception that escapes a scheduled task silently cancels every later run, so the tasks catch everything
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    refreshAllCollectorStatus();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to refresh the collector status", e);
                }
            }
        }, 0, REFRESH_INTERVAL, TimeUnit.MINUTES);
        try {
            refreshAllCollectorStatus();
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to load the collectors", e);
        }

        // clear up the prevResults every 10 seconds, so that we do not miss collecting reoccurring data in the same screen
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                prevResults.clear();
            }
        }, 0, 10, TimeUnit.SECONDS);

        // Create a notification channel
        createNotificationChannel();
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
        super.onServiceConnected();
        sSharedInstance = this;
        // for crepe foreground service
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Crepe")
                .setContentText("Crepe successfully running...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();


        // Set the foreground service type. The system may refuse to promote the service to the foreground
        // (missing permission, background start restrictions); the service still works without it.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not start the service in the foreground", e);
        }
        // the service can be connected more than once, make sure there is only one heartbeat loop
        handler.removeCallbacks(heartbeatRunnable);
        handler.post(heartbeatRunnable);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sSharedInstance = null;
        shutdownExecutors();
        return true;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(heartbeatRunnable);
        sSharedInstance = null;
        shutdownExecutors();
        super.onDestroy();
    }

    private void shutdownExecutors() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        threadPool.shutdownNow();
    }

    public static CrepeAccessibilityService getsSharedInstance() {
        return sSharedInstance;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        // This runs on the main thread of a service bound by the system, and the windows we look at belong to
        // arbitrary third party apps. Whatever they throw at us, it must not crash the service.
        try {
            handleAccessibilityEvent(accessibilityEvent);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to process an accessibility event", e);
        }
    }

    private void handleAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        if (accessibilityEvent == null) {
            return;
        }

        if (!TARGET_EVENT_TYPES.contains(accessibilityEvent.getEventType())) {
            Log.i("accessibilityEvent", accessibilityEvent.getEventType() + " not in the target event list");
            return;
        }

        // work on a consistent view of the collectors and datafields, they may be refreshed at any moment
        final List<Collector> currentCollectors = collectors;
        final List<Datafield> currentDatafields = datafields;
        if (currentCollectors == null || currentCollectors.isEmpty()) {
            Log.i("accessibilityEvent", "No collectors to monitor");
            return;
        }

        // get the current package
        CharSequence eventPackageName = accessibilityEvent.getPackageName();
        if (eventPackageName == null) {
            Log.e(TAG, "Accessibility event without a package name");
            return;
        }
        currentPackageName = eventPackageName.toString();

        // if we are not monitoring this app, return
        boolean isMonitored = false;
        for (Collector collector : currentCollectors) {
            if (currentPackageName.equals(collector.getAppPackage())) {
                isMonitored = true;
                break;
            }
        }
        if (!isMonitored) {
            Log.i("accessibilityEvent", "Current package " + currentPackageName + " not in the monitored app list");
            return;
        }

        // update a UISnapshot and all nodes on screen
        final UISnapshot snapshot = generateUISnapshot(accessibilityEvent);
        uiSnapshot = snapshot;
        allNodeList = getAllNodesOnScreen();

        Log.i("accessibilityEvent", "Accessibility Event Type: " + accessibilityEvent.getEventType() + ", opening a new thread, current count: " + threadPool.getActiveCount());

        // Submit a task to the thread pool. The task gets the snapshot of this event, not whatever
        // snapshot happens to be current by the time it runs.
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    collectData(snapshot, currentCollectors, currentDatafields);
                } catch (RuntimeException e) {
                    // exceptions thrown inside submit() are swallowed by the returned Future, so log them here
                    Log.e(TAG, "Failed to collect data", e);
                }
            }
        });
    }

    private void collectData(UISnapshot snapshot, List<Collector> activeCollectors, List<Datafield> allDatafields) {
        User user = getUserForCollection();
        if (user == null) {
            Log.w(TAG, "There is no signed in user to attribute the collected data to");
            return;
        }

        ArrayList<Datafield> datafieldsToStart = new ArrayList<>();
        for (Collector collector : activeCollectors) {
            // also add the datafields that are associated with this collector
            for (Datafield datafield : allDatafields) {
                if (Objects.equals(datafield.getCollectorId(), collector.getCollectorId())) {
                    datafieldsToStart.add(datafield);
                }
            }
        }

        // for each datafield, run the graph query on the uiSnapshot
        for (Datafield datafield : datafieldsToStart) {
            // a broken datafield must not keep the other ones from being collected
            try {
                Log.i("query execution", "starting datafield: " + datafield.getDatafieldId());
                // Start a new graph query thread and execute the graph query
                // 1. convert the graph query string to a graph query object
                OntologyQuery currentQuery = OntologyQuery.deserialize(datafield.getGraphQuery());
                if (currentQuery == null) {
                    Log.w(TAG, "Skipping datafield " + datafield.getDatafieldId() + ": its graph query could not be parsed");
                    continue;
                }
                // 2. run the graph query on the uiSnapshot
                Set<SugiliteEntity> currentResults = currentQuery.executeOn(snapshot);

                // 3. store the new results in the database
                for (SugiliteEntity result : currentResults) {
                    // prevent duplicate results from being saved, with the interval of 4 seconds
                    if (!prevResults.contains(result) && System.currentTimeMillis() - lastSavedResultTimestamp.get() > 4000) {
                        saveResult(user, datafield, result);
                    }
                }
                prevResults.addAll(currentResults);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to run datafield " + datafield.getDatafieldId(), e);
            }
        }
    }

    // if the result is not in the previous results, add it to the database
    private void saveResult(User user, Datafield datafield, SugiliteEntity result) {
        long timestamp = System.currentTimeMillis();
        Data resultData = null;
        try {
            // the data id is the collector id + "%" + timestamp
            resultData = new Data(datafield.getCollectorId() + "%" + timestamp, datafield.getDatafieldId(), user.getUserId(), result.saveToDatabaseAsString());
            final Data dataToSend = resultData;

            dbManager.addData(dataToSend);
            Log.i("database", "added data: " + dataToSend.getDataId());

            // send the data to firebase
            firebaseCommunicationManager.putData(dataToSend).addOnSuccessListener(suc -> {
                Log.i("Firebase", "Successfully added data " + dataToSend.getDataId() + " to firebase.");
            }).addOnFailureListener(er -> {
                Log.e("Firebase", "Failed to add data " + dataToSend.getDataId() + " to firebase. Error: " + er.getMessage());
            });

            // update the last saved result timestamp
            lastSavedResultTimestamp.set(System.currentTimeMillis());

        } catch (RuntimeException e) {
            Log.e("database", "failed to add data " + (resultData != null ? resultData.getDataId() : "(not created)"), e);
        }
    }

    /**
     * The user that collected data belongs to. MainActivity.currentUser is only set once the app has been opened,
     * but the service can be running without it (for example after the process was restarted), so fall back to the
     * profile in the local database.
     */
    @Nullable
    private User getUserForCollection() {
        User user = MainActivity.currentUser;
        if (user != null) {
            return user;
        }

        List<User> users = dbManager.getAllUsers();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            for (User candidate : users) {
                if (firebaseUser.getUid().equals(candidate.getUserId())) {
                    MainActivity.currentUser = candidate;
                    return candidate;
                }
            }
            return null;
        }
        if (users.size() == 1) {
            MainActivity.currentUser = users.get(0);
            return users.get(0);
        }
        return null;
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
                // the class of an event is often a view class rather than an activity, so this is expected
                Log.e(TAG, "Failed to get the activity name for: " + componentName);
            }
        }

        UISnapshot snapshot = new UISnapshot(windowManager.getDefaultDisplay(), rootNode, true, currentPackageName, currentAppActivityName);
        uiSnapshot = snapshot;
        return snapshot;

    }

    // if we are calling from another class and don't have some needed parameters
    public UISnapshot generateUISnapshot() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        try {
            ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am != null ? am.getRunningTasks(1) : null;
            if (taskInfo != null && !taskInfo.isEmpty() && taskInfo.get(0).topActivity != null) {
                ComponentName componentName = taskInfo.get(0).topActivity;
                try {
                    ActivityInfo activityInfo = getPackageManager().getActivityInfo(componentName, 0);
                    Log.i("CurrentActivity", activityInfo.packageName + " : " + activityInfo.name);
                    currentAppActivityName = activityInfo.name;
                    currentPackageName = activityInfo.packageName;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(this.getClass().getName(), "Failed to get the activity name for: " + componentName);
                }
            } else {
                // getRunningTasks only reports this app's own tasks on modern Android, and may report none
                Log.w(TAG, "Could not determine the top activity, keeping the previous one");
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Not allowed to query the running tasks, keeping the previous top activity", e);
        }

        UISnapshot snapshot = new UISnapshot(windowManager.getDefaultDisplay(), rootNode, true, currentPackageName, currentAppActivityName);
        uiSnapshot = snapshot;
        return snapshot;

    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted");
    }

    public List<AccessibilityNodeInfo> getMatchingNodeFromClickWithText(float clickX, float clickY) {

        this.allNodeList = getAllNodesOnScreen();

        List<AccessibilityNodeInfo> matchingNodeInfoList = DemonstrationUtil.findMatchingNodeFromClick(this.allNodeList, clickX, clickY);

        List<AccessibilityNodeInfo> resultNodeList = new ArrayList<>();

        if (matchingNodeInfoList != null) {
            for (AccessibilityNodeInfo matchingNode : matchingNodeInfoList) {
                // change here
                if (matchingNode.getText() != null && !matchingNode.getText().toString().isEmpty()) {
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
        if (context == null) {
            return false;
        }
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString != null && prefString.contains(context.getPackageName() + "/" + accessibilityService.getName());
    }


    public void refreshAllCollectorStatus() {
        // retrieve all stored collectors and datafields
        List<Collector> storedCollectors = dbManager.getActiveCollectors();
        List<Datafield> storedDatafields = dbManager.getAllDatafields();

        // refresh collector status based on current time, and only keep collecting for the ones that are still active.
        // (without the filter, a collector that just expired would keep collecting until the next refresh)
        List<Collector> stillActiveCollectors = new ArrayList<>();
        for (Collector collector : storedCollectors) {
            Boolean collectorUpdated = collector.autoSetCollectorStatus();
            if (collectorUpdated) {
                dbManager.updateCollectorStatus(collector);
            }
            if (Collector.ACTIVE.equals(collector.getCollectorStatus())) {
                stillActiveCollectors.add(collector);
            }
        }

        // publish both lists together
        datafields = storedDatafields;
        collectors = stillActiveCollectors;

        if (!storedCollectors.isEmpty()) {
            firebaseCommunicationManager.updateAllCollectors();
        }

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void updateHeartbeatTimestamp() {
        User user = MainActivity.currentUser;
        if (user == null) {
            return;
        }
        // Update timestamp in local database
        long currentTime = System.currentTimeMillis();
        user.setLastHeartBeat(currentTime);
        dbManager.updateUser(user);
        // send the updated timestamp in the updated user info to firebase
        HashMap<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("lastHeartBeat", currentTime);
        firebaseCommunicationManager.updateUser(user.getUserId(), userUpdates)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to send the heartbeat to firebase: " + e.getMessage()));
    }

}
