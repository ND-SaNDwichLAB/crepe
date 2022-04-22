package com.example.crepe.demosntration;

import static android.app.Service.START_STICKY;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.crepe.R;

import java.security.Provider;
import java.util.List;
import java.util.Map;

public class WidgetService extends Service {
    int LAYOUT_FLAG;
    View mFloatingView;
    ImageView imageClose;
    WindowManager windowManager;
    Context c;

//    public WidgetService(Provider provider, String type, String algorithm, String className, List<String> aliases, Map<String, String> attributes) {
//        super(provider, type, algorithm, className, aliases, attributes);
//    }

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

        // layout parameters for closed button
        WindowManager.LayoutParams imageParameter = new WindowManager.LayoutParams(140,
                140,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        imageParameter.gravity = Gravity.BOTTOM | Gravity.CENTER;
        imageParameter.y = 100;
        windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        imageClose = new ImageView(c);
        imageClose.setImageResource(R.drawable.ic_baseline_close_24);
        imageClose.setVisibility(View.INVISIBLE);
        windowManager.addView(imageClose,imageParameter);
        windowManager.addView(mFloatingView,layoutParams);
        mFloatingView.setVisibility(View.VISIBLE);


        return START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
    }

}
