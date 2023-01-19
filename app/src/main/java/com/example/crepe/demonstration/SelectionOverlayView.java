package com.example.crepe.demonstration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;


// This class is to draw the overlay on the screen when there's a selection made during graph query demonstration
public class SelectionOverlayView extends View {

    private Paint paint;
    private float x;
    private float y;
    private float radius;

    public SelectionOverlayView(Context context, float x, float y, float radius) {
        super(context);
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        canvas.drawCircle(x, y, radius, paint);
    }
}
