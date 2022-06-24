package com.example.crepe.graphquery.recording;


import android.content.Context;
import android.widget.Toast;

public class SelectionOverlayViewManager {

    private Context context;

    public SelectionOverlayViewManager(Context context) {
        this.context = context;
    }

    public SelectionOverlayView getCircleOverlay(float x, float y, float radius) {
        Toast.makeText(context, "Hello Hello Hello, drawing overlay", Toast.LENGTH_SHORT).show();
        SelectionOverlayView selectionOverlayView = new SelectionOverlayView(context, x, y, radius);
        return selectionOverlayView;
    }
}
