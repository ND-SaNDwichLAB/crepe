package edu.nd.crepe.demonstration;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OverlayViewManager {
    private Context context;
    private WindowManager windowManager;
    private Map<String, View> overlays;
    private Map<String, Handler> handlers;

    public OverlayViewManager(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.overlays = new HashMap<>();
        this.handlers = new HashMap<>();
    }

    /**
     * Shows an overlay and returns its unique identifier
     * @return String identifier for the created overlay
     */
    public String showRectOverlay(Rect overlayLocation, int flag, int color) {
        return showRectOverlay(overlayLocation, flag, color, 0);
    }

    /**
     * Shows an overlay with automatic dissolution after specified time and returns its unique identifier
     * @return String identifier for the created overlay
     */
    public String showRectOverlay(Rect overlayLocation, int flag, int color, int lapseTimeInSeconds) {
        String overlayId = UUID.randomUUID().toString();

        View overlayView = new View(context);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flag,
                PixelFormat.TRANSLUCENT);

        layoutParams.width = overlayLocation.width();
        layoutParams.height = overlayLocation.height();
        layoutParams.x = overlayLocation.left;
        layoutParams.y = overlayLocation.top;
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        overlayView.setLayoutParams(layoutParams);
        overlayView.setBackgroundColor(color);

        windowManager.addView(overlayView, layoutParams);
        overlays.put(overlayId, overlayView);

        if (lapseTimeInSeconds > 0) {
            Handler handler = new Handler();
            handlers.put(overlayId, handler);
            handler.postDelayed(() -> dissolveOverlay(overlayId), lapseTimeInSeconds * 1000);
        }

        return overlayId;
    }

    /**
     * Dissolves a specific overlay with animation
     * @param overlayId the identifier of the overlay to dissolve
     */
    private void dissolveOverlay(String overlayId) {
        View overlayView = overlays.get(overlayId);
        if (overlayView != null) {
            overlayView.animate()
                    .alpha(0.0f)
                    .setDuration(1000)
                    .withEndAction(() -> {
                        removeOverlay(overlayId);
                    });
        }
    }

    /**
     * Removes a specific overlay immediately
     * @param overlayId the identifier of the overlay to remove
     * @return boolean indicating whether the removal was successful
     */
    public boolean removeOverlay(String overlayId) {
        View overlayView = overlays.get(overlayId);
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlays.remove(overlayId);

            Handler handler = handlers.get(overlayId);
            if (handler != null) {
                handler.removeCallbacksAndMessages(null);
                handlers.remove(overlayId);
            }
            return true;
        }
        return false;
    }

    /**
     * Removes all active overlays
     */
    public void removeAllOverlays() {
        for (String overlayId : new HashMap<>(overlays).keySet()) {
            removeOverlay(overlayId);
        }
    }

    /**
     * Gets the current count of active overlays
     * @return int number of active overlays
     */
    public int getOverlayCount() {
        return overlays.size();
    }
}