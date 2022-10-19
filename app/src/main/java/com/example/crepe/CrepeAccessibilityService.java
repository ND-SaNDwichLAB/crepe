package com.example.crepe;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.crepe.graphquery.DemonstrationUtil;
import com.example.crepe.graphquery.model.Node;
import com.example.crepe.graphquery.ontology.UISnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class CrepeAccessibilityService extends AccessibilityService {

    private static final String TAG = "crepeAccessibilityService: ";

    private static CrepeAccessibilityService sSharedInstance;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    private WindowManager windowManager;
    private String currentAppActivityName;
    private String currentPackageName;

    public UISnapshot getCurrentUiSnapshot() {
        return uiSnapshot;
    }


    private UISnapshot uiSnapshot;

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
        Log.e(TAG, "An accessibility event just happened");
        int eventType = accessibilityEvent.getEventType();
        Log.e(TAG, "Event type: " + String.valueOf(eventType));


        //update currentAppActivityName and currentPackageName on TYPE_WINDOW_STATE_CHANGED events
        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            uiSnapshot = generateUISnapshot(accessibilityEvent);
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

    public List<AccessibilityNodeInfo> getMatchingNodeFromClick(float clickX, float clickY) {

        AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> allNodeList = DemonstrationUtil.preOrderTraverse(rootNodeInfo);

        List<String> stringList = new ArrayList<>();
        for (int i = 0; i < allNodeList.size(); i++) {
            CharSequence nodeText = allNodeList.get(i).getText();
            if (nodeText != null) {
                stringList.add(nodeText.toString());
            }
        }

        List<AccessibilityNodeInfo> matchingNodeInfoList = DemonstrationUtil.findMatchingNodeFromClick(rootNodeInfo, clickX, clickY);

        List<AccessibilityNodeInfo> resultNodeList = null;

        if (matchingNodeInfoList != null) {
            for (AccessibilityNodeInfo matchingNode: matchingNodeInfoList) {
                if(matchingNode.getText() != null) {
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
