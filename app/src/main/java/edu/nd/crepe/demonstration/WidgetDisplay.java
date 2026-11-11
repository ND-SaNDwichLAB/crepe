package edu.nd.crepe.demonstration;

import android.util.Log;
import android.view.View;
import android.view.WindowManager;

public class WidgetDisplay {
    private static final String TAG = "WidgetDisplay";

    WindowManager.LayoutParams layoutParams;
    View mFloatingView;
    WindowManager windowManager;
    // whether mFloatingView is currently attached to the window manager. Adding a view twice or removing
    // one that is not attached throws, so every add/remove is guarded by this flag.
    private boolean showing = false;

    WidgetDisplay(WindowManager.LayoutParams layoutParams, View mFloatingView, WindowManager windowManager){
        this.layoutParams = layoutParams;
        this.mFloatingView = mFloatingView;
        this.windowManager = windowManager;
    }

    /**
     * @return whether the widget is showing afterwards. False means it could not be added, which typically
     * is a missing "display over other apps" permission.
     */
    public boolean showWidget(){
        if (showing) {
            return true;
        }
        if (mFloatingView == null) {
            return false;
        }
        try {
            windowManager.addView(mFloatingView,layoutParams);
            mFloatingView.setVisibility(View.VISIBLE);
            showing = true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not show the floating widget", e);
        }
        return showing;
    }

    public void removeWidget(){
        if (!showing || mFloatingView == null) {
            return;
        }
        try {
            windowManager.removeView(mFloatingView);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "The floating widget was already removed", e);
        } finally {
            showing = false;
        }
    }
    public void refreshWidget(){
        removeWidget();
        showWidget();
    }
}
