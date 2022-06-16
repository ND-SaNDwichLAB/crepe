package com.example.crepe.demonstration;

import static com.example.crepe.graphquery.DemonstrationUtil.initiateDemonstration;

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
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crepe.R;
import com.example.crepe.graphquery.recording.FullScreenOverlayManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

public class WidgetService extends Service {
    int LAYOUT_FLAG;
    View mFloatingView;
    ImageView imageClose;
    WindowManager windowManager;
    float height, width;
    Context c = WidgetService.this;
    FullScreenOverlayManager fullScreenOverlayManager;

    public IBinder onBind(Intent intent){
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

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
        windowManager.addView(mFloatingView,layoutParams);
        mFloatingView.setVisibility(View.VISIBLE);

        height = windowManager.getDefaultDisplay().getHeight();
        width = windowManager.getDefaultDisplay().getHeight();

        // initialize fullScreenOverlayManager
        fullScreenOverlayManager= new FullScreenOverlayManager(c, windowManager, getResources().getDisplayMetrics() );

        FloatingActionButton closeFltBtn = (FloatingActionButton) mFloatingView.findViewById(R.id.floating_close);
        FloatingActionButton drawFltBtn = (FloatingActionButton) mFloatingView.findViewById(R.id.floating_draw_frame);

        closeFltBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
                Intent launchIntent = c.getPackageManager().getLaunchIntentForPackage("com.example.crepe");
                if (launchIntent != null) {
                    c.startActivity(launchIntent);
                } else {
                    Toast.makeText(c, "There is no package available in android", Toast.LENGTH_LONG).show();
                }
            }
        });

        drawFltBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                initiateDemonstration(c, fullScreenOverlayManager);
            }
        });

        // drag movement
//        closeFltBtn.setOnTouchListener(new View.OnTouchListener() {
//            int initialX, initialY;
//            float initialTouchX, initialTouchY;
//            long startClickTime;
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                switch(motionEvent.getAction()){
//                    case MotionEvent.ACTION_DOWN:
//                        startClickTime = Calendar.getInstance().getTimeInMillis();
//                        mFloatingView.setVisibility(View.VISIBLE);
//                        initialX = layoutParams.x;
//                        initialY = layoutParams.y;
//                        // touch position
//                        initialTouchX = motionEvent.getRawX();
//                        initialTouchY = motionEvent.getRawY();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
//                        mFloatingView.setVisibility(View.VISIBLE);
//                        layoutParams.x = initialX + (int)(initialTouchX - motionEvent.getRawX());
//                        layoutParams.y = initialY + (int)(motionEvent.getRawY() - initialTouchY);
//                        return true;
//                    case MotionEvent.ACTION_MOVE:
//                        mFloatingView.setVisibility(View.VISIBLE);
//                        layoutParams.x = initialX + (int)(initialTouchX - motionEvent.getRawX());
//                        layoutParams.y = initialY + (int)(motionEvent.getRawY() - initialTouchY);
//                        windowManager.updateViewLayout(mFloatingView,layoutParams);
//                        return true;
//                }
//                return false;
//            }
//        });
//
//        drawFltBtn.setOnTouchListener(new View.OnTouchListener() {
//            int initialX, initialY;
//            float initialTouchX, initialTouchY;
//            long startClickTime;
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                switch(motionEvent.getAction()){
//                    case MotionEvent.ACTION_DOWN:
//                        startClickTime = Calendar.getInstance().getTimeInMillis();
//                        mFloatingView.setVisibility(View.VISIBLE);
//                        initialX = layoutParams.x;
//                        initialY = layoutParams.y;
//                        // touch position
//                        initialTouchX = motionEvent.getRawX();
//                        initialTouchY = motionEvent.getRawY();
//                        return true;
//                    case MotionEvent.ACTION_UP:
//                        long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
//                        mFloatingView.setVisibility(View.VISIBLE);
//                        layoutParams.x = initialX + (int)(initialTouchX - motionEvent.getRawX());
//                        layoutParams.y = initialY + (int)(motionEvent.getRawY() - initialTouchY);
//                        return true;
//                    case MotionEvent.ACTION_MOVE:
//                        mFloatingView.setVisibility(View.VISIBLE);
//                        layoutParams.x = initialX + (int)(initialTouchX - motionEvent.getRawX());
//                        layoutParams.y = initialY + (int)(motionEvent.getRawY() - initialTouchY);
//                        windowManager.updateViewLayout(mFloatingView,layoutParams);
//                        return true;
//                }
//                return false;
//            }
//        });

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
                        windowManager.updateViewLayout(mFloatingView,layoutParams);
                        return true;
                }
                return false;
            }
        });

        return START_STICKY;
    }

    // remove widget
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null){
            windowManager.removeView(mFloatingView);

        }
    }

    private void setVisibility(Boolean clicked, FloatingActionButton btn) {
        // if the fab icon is clicked, show the small buttons
        if(!clicked) {
            btn.setVisibility(View.VISIBLE);
        } else {
            // if the fab icon is clicked to be closed, set the visibilities to invisible
            btn.setVisibility(View.INVISIBLE);
        }
    }


}
