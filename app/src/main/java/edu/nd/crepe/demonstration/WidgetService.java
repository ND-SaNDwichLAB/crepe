package edu.nd.crepe.demonstration;

import static edu.nd.crepe.demonstration.DemonstrationUtil.initiateDemonstration;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import edu.nd.crepe.R;
import edu.nd.crepe.ui.dialog.GraphQueryCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class WidgetService extends Service {
    private static final String TAG = "WidgetService";

    int LAYOUT_FLAG;
    View mFloatingView;
    WindowManager windowManager;
    float height, width;
    Context c = WidgetService.this;
    FullScreenOverlayManager fullScreenOverlayManager;
    WidgetDisplay widgetDisplay;

    GraphQueryCallback graphQueryCallback;

    public IBinder onBind(Intent intent){
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        // The system hands us a null intent when it restarts a service that it killed. The demonstration
        // callback only travels inside the intent, so there is nothing to resume: just stop.
        if (intent == null) {
            Log.w(TAG, "Started without an intent, stopping");
            stopSelf();
            return START_NOT_STICKY;
        }

        // the service may be started again while the widget is still showing, start from a clean slate
        // instead of stacking a second widget and overlay on top of the first
        tearDown();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        try {
            this.graphQueryCallback = (GraphQueryCallback) intent.getSerializableExtra("graphQueryCallback");
        } catch (ClassCastException e) {
            Log.e(TAG, "The demonstration callback has an unexpected type", e);
            this.graphQueryCallback = null;
        }

        try {
            showWidget();
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not set up the demonstration widget", e);
            Toast.makeText(c, "Could not start the demonstration. Please check the permissions of Crepe.", Toast.LENGTH_LONG).show();
            tearDown();
            stopSelf();
        }

        // there is no way to rebuild the widget after the system killed us (see above), so do not ask for a restart
        return START_NOT_STICKY;
    }

    private void showWidget() {
        // inflate widget layout
        mFloatingView = LayoutInflater.from(c).inflate(R.layout.demonstration_float_widget, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        // initialize position
        layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        layoutParams.x = 0;
        layoutParams.y = 100;

        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        widgetDisplay = new WidgetDisplay(layoutParams, mFloatingView, windowManager);
        if (!widgetDisplay.showWidget()) {
            // handled in onStartCommand: tell the user and stop the service
            throw new IllegalStateException("The floating widget could not be added to the window manager");
        }
        height = getResources().getDisplayMetrics().heightPixels;
        width = getResources().getDisplayMetrics().widthPixels;

        // initialize fullScreenOverlayManager
        fullScreenOverlayManager = new FullScreenOverlayManager(c, windowManager, getResources().getDisplayMetrics(), this.graphQueryCallback);
        final WidgetDisplay display = widgetDisplay;
        final FullScreenOverlayManager overlayManager = fullScreenOverlayManager;

        FloatingActionButton closeFltBtn = (FloatingActionButton) mFloatingView.findViewById(R.id.floating_close);
        FloatingActionButton drawFltBtn = (FloatingActionButton) mFloatingView.findViewById(R.id.floating_draw_frame);

        closeFltBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // turn off the overlay before leaving the app
                if(overlayManager.getShowingOverlay()) {
                    overlayManager.disableOverlay();
                }

                stopSelf();
                Intent launchIntent = c.getPackageManager().getLaunchIntentForPackage("edu.nd.crepe");
                if (launchIntent != null) {
                    c.startActivity(launchIntent);
                } else {
                    Toast.makeText(c, "There is no package available in android", Toast.LENGTH_LONG).show();
                }

                display.removeWidget();
            }
        });

        drawFltBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                initiateDemonstration(c, overlayManager, display);
                Toast.makeText(c, "Please tap on the data to collect", Toast.LENGTH_LONG).show();
            }
        });

        mFloatingView.setOnTouchListener(new View.OnTouchListener() {
            int initialX, initialY;
            float initialTouchX, initialTouchY;
            long startClickTime;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        startClickTime = Calendar.getInstance().getTimeInMillis();
                        mFloatingView.setVisibility(View.VISIBLE);
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        // touch position
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                        mFloatingView.setVisibility(View.VISIBLE);
                        layoutParams.x = initialX + (int)(initialTouchX - motionEvent.getRawX());
                        layoutParams.y = initialY + (int)(motionEvent.getRawY() - initialTouchY);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        mFloatingView.setVisibility(View.VISIBLE);
                        layoutParams.x = initialX + (int)(initialTouchX - motionEvent.getRawX());
                        layoutParams.y = initialY + (int)(motionEvent.getRawY() - initialTouchY);
                        try {
                            windowManager.updateViewLayout(mFloatingView,layoutParams);
                        } catch (IllegalArgumentException e) {
                            // the widget was removed while it was being dragged
                            Log.w(TAG, "Could not move the widget", e);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // remove the overlay and the widget, whatever state they are in
    private void tearDown() {
        if (fullScreenOverlayManager != null && fullScreenOverlayManager.getShowingOverlay()) {
            fullScreenOverlayManager.disableOverlay();
        }
        if (widgetDisplay != null) {
            widgetDisplay.removeWidget();
        }
        fullScreenOverlayManager = null;
        widgetDisplay = null;
        mFloatingView = null;
    }

    // remove widget
    public void onDestroy() {
        super.onDestroy();
        tearDown();
    }

    public void registerCallback(GraphQueryCallback dataGraphQueryCallback) {
        graphQueryCallback = dataGraphQueryCallback;
    }


}
