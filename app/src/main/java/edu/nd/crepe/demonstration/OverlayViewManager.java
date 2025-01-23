package edu.nd.crepe.demonstration;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

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
     * Shows a dot overlay and returns its unique identifier
     * @param x The x-coordinate of the dot center
     * @param y The y-coordinate of the dot center
     * @param radius The radius of the dot in pixels
     * @param flag Window manager flags
     * @param color The color of the dot
     * @param showRipple Whether to show a ripple effect around the dot
     * @return String identifier for the created overlay
     */
    public String showDotOverlay(int x, int y, int radius, int flag, int color, boolean showRipple) {
        return showDotOverlay(x, y, radius, flag, color, showRipple, 0);
    }

    /**
     * Shows a dot overlay with automatic dissolution after specified time
     * @param x The x-coordinate of the dot center
     * @param y The y-coordinate of the dot center
     * @param radius The radius of the dot in pixels
     * @param flag Window manager flags
     * @param color The color of the dot
     * @param showRipple Whether to show a ripple effect around the dot
     * @param lapseTimeInSeconds Time after which the overlay should dissolve
     * @return String identifier for the created overlay
     */
    public String showDotOverlay(int x, int y, int radius, int flag, int color, boolean showRipple, int lapseTimeInSeconds) {
        String overlayId = UUID.randomUUID().toString();

        // Create container for dot and ripple
        android.widget.FrameLayout container = new android.widget.FrameLayout(context);

        // Create main dot
        View dotView = new View(context);
        android.widget.FrameLayout.LayoutParams dotParams = new android.widget.FrameLayout.LayoutParams(
                radius * 2,
                radius * 2);
        dotParams.gravity = Gravity.CENTER;
        dotView.setLayoutParams(dotParams);

        // Create circular background for dot
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(color);
        dotView.setBackground(shape);

        container.addView(dotView);

        if (showRipple) {
            // Create ripple view
            View rippleView = new View(context);
            android.widget.FrameLayout.LayoutParams rippleParams = new android.widget.FrameLayout.LayoutParams(
                    radius * 2,
                    radius * 2);
            rippleParams.gravity = Gravity.CENTER;
            rippleView.setLayoutParams(rippleParams);

            // Create ripple background
            android.graphics.drawable.GradientDrawable rippleShape = new android.graphics.drawable.GradientDrawable();
            rippleShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            rippleShape.setColor(android.graphics.Color.argb(50,
                    android.graphics.Color.red(color),
                    android.graphics.Color.green(color),
                    android.graphics.Color.blue(color)));
            rippleView.setBackground(rippleShape);

            container.addView(rippleView, 0); // Add ripple behind dot

            // Create ripple animation
            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1000);
            animator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            animator.setRepeatMode(android.animation.ValueAnimator.RESTART);

            animator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                rippleView.setScaleX(1f + value);
                rippleView.setScaleY(1f + value);
                rippleView.setAlpha(1f - value);
            });

            // Start ripple animation
            animator.start();
        }

        // Set up window parameters
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flag,
                PixelFormat.TRANSLUCENT);

        // Container size needs to accommodate ripple
        layoutParams.width = radius * 4;  // Double size to allow for ripple expansion
        layoutParams.height = radius * 4;

        // Adjust position to center, accounting for container size
        layoutParams.x = x - (layoutParams.width / 2);
        layoutParams.y = y - (layoutParams.height / 2);

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        container.setLayoutParams(layoutParams);

        windowManager.addView(container, layoutParams);
        overlays.put(overlayId, container);

        if (lapseTimeInSeconds > 0) {
            Handler handler = new Handler();
            handlers.put(overlayId, handler);
            handler.postDelayed(() -> dissolveOverlay(overlayId), lapseTimeInSeconds * 1000);
        }

        return overlayId;
    }

    /**
     * Shows an overlay with automatic dissolution after specified time and returns its unique identifier
     * use lapseTimeInSeconds as 0 for no dissolution
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

    public String showTextOverlay(Rect overlayLocation, int flag, String text, int color, int lapseTimeInSeconds) {
        String overlayId = UUID.randomUUID().toString();

        TextView overlayView = new TextView(context);
        overlayView.setText(text);
        overlayView.setTextColor(color);
        overlayView.setGravity(Gravity.CENTER);
        overlayView.setPadding(20, 10, 20, 10);  // Add some padding for better readability

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flag,
                PixelFormat.TRANSLUCENT);

        // Measure the text size
        Paint textPaint = overlayView.getPaint();
        Rect textBounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textBounds);

        // Add padding to the measured size
        int textWidth = textBounds.width() + overlayView.getPaddingLeft() + overlayView.getPaddingRight();
        int textHeight = textBounds.height() + overlayView.getPaddingTop() + overlayView.getPaddingBottom();

        // Use the larger of either the original bounds or the text size
        layoutParams.width = Math.max(overlayLocation.width(), textWidth);
        layoutParams.height = Math.max(overlayLocation.height(), textHeight);

        // Center the overlay relative to the original location
        layoutParams.x = overlayLocation.left - (layoutParams.width - overlayLocation.width()) / 2;
        layoutParams.y = overlayLocation.top - (layoutParams.height - overlayLocation.height()) / 2;
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        overlayView.setLayoutParams(layoutParams);
        overlayView.setBackgroundColor(Color.argb(128, 0, 0, 0));

        windowManager.addView(overlayView, layoutParams);
        overlays.put(overlayId, overlayView);

        if (lapseTimeInSeconds > 0) {
            Handler handler = new Handler();
            handlers.put(overlayId, handler);
            handler.postDelayed(() -> dissolveOverlay(overlayId), lapseTimeInSeconds * 1000);
        }

        return overlayId;
    }
}