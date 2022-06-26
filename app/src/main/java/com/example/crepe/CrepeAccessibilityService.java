package com.example.crepe;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;


public class CrepeAccessibilityService extends AccessibilityService {

    private static final String TAG = "crepeAccessibilityService: ";

    private static CrepeAccessibilityService sSharedInstance;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();


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
        Log.e(TAG, "An accessibility event going on");
        int eventType = accessibilityEvent.getEventType();
        Log.e(TAG, "Event type: " + String.valueOf(eventType));
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted");
    }

    public String getString() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        return nodeInfo.toString();
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
