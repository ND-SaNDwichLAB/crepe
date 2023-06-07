package edu.nd.crepe.demonstration;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;


// This class is to draw the overlay on the screen when there's a selection made during graph query demonstration
public class SelectionOverlayView extends View {

    private Paint paint;
    private Rect bounds;
    private float radius;

    public SelectionOverlayView(Context context, Rect bounds) {
        super(context);
        this.bounds = bounds;
        this.paint = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // draw a rectangle with a half-transparent gray color
        paint.setColor(Color.argb(128, 128, 128, 128));
        canvas.drawRect(bounds, paint);
    }
}
