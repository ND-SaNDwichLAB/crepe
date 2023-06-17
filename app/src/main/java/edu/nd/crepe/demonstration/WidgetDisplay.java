package edu.nd.crepe.demonstration;

import android.view.View;
import android.view.WindowManager;

public class WidgetDisplay {
    WindowManager.LayoutParams layoutParams;
    View mFloatingView;
    WindowManager windowManager;

    WidgetDisplay(WindowManager.LayoutParams layoutParams, View mFloatingView, WindowManager windowManager){
        this.layoutParams = layoutParams;
        this.mFloatingView = mFloatingView;
        this.windowManager = windowManager;
    }

    public void showWidget(){
        windowManager.addView(mFloatingView,layoutParams);
        mFloatingView.setVisibility(View.VISIBLE);
    }

    public void removeWidget(){
        if (mFloatingView != null){
            windowManager.removeView(mFloatingView);
        }

    }
    public void refreshWidget(){
        removeWidget();
        showWidget();
    }
}
