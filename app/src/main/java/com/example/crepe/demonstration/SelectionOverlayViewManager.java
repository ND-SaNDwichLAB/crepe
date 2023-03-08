package com.example.crepe.demonstration;


import android.content.Context;
import android.graphics.Rect;

public class SelectionOverlayViewManager {

    private Context context;

    public SelectionOverlayViewManager(Context context) {
        this.context = context;
    }

    public SelectionOverlayView getRectOverlay(Rect clickedItemBounds) {
        SelectionOverlayView selectionOverlayView = new SelectionOverlayView(context, clickedItemBounds);
        return selectionOverlayView;
    }
}
