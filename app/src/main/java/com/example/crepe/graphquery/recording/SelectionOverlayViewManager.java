package com.example.crepe.graphquery.recording;


import android.content.Context;
import android.widget.Toast;

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
