package com.example.crepe.demonstration;


import android.content.Context;

public class SelectionOverlayViewManager {

    private Context context;

    public SelectionOverlayViewManager(Context context) {
        this.context = context;
    }

    public SelectionOverlayView getCircleOverlay(float x, float y, float radius) {
        SelectionOverlayView selectionOverlayView = new SelectionOverlayView(context, x, y, radius);
        return selectionOverlayView;
    }
}
