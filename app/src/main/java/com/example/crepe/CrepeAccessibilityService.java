package com.example.crepe;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.Nullable;



public class CrepeAccessibilityService extends AccessibilityService {

    private static final String TAG = "crepeAccessibilityService: ";

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
}
