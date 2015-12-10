package com.example.jying.androidannotations;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.example.jying.androidannotations.support.AnnotationLayer;
import com.example.jying.androidannotations.support.BackingCanvas;
import com.example.jying.androidannotations.support.Overlay;
import com.example.jying.androidannotations.support.Subview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jying on 5/21/2015.
 */
public class AnnotationView extends View {

    // Mode constants
    private static float density; // The screen density.  Used for converting dp to px

    private Subview subview; // Represents the part of the canvas that the user is currently looking at.
    private Bitmap bgBitmap; // The bitmap picture that the user took.  This is the background of this view.
    private HashMap<String, AnnotationLayer> layers; // The list of layers that are currently instantiated in the activity.
    private Set<BackingCanvas> canvasSet;
    private ArrayList<BackingCanvas> canvases;

    public static int convertDpToPx(int dpDimension) {
        return (int) (dpDimension * density + 0.5f);
    }

    public static int convertPxToDp(int pxDimension) {
        return (int) (pxDimension / density);
    }

    public AnnotationView(Context context, Bitmap background, HashMap<String, AnnotationLayer> layers, ArrayList<BackingCanvas> canvases) {
        super(context);
        this.bgBitmap = background;
        this.layers = layers;
        this.canvases = canvases;

        int bitmapWidth = bgBitmap.getWidth();
        int bitmapHeight = bgBitmap.getHeight();

        AnnotationView.density = getContext().getResources().getDisplayMetrics().density;
        canvasSet = new HashSet<BackingCanvas>(); // Allocate this object for onMeasure to save time when the activity is being drawn.

        // Create helper classes
        subview = new Subview(bitmapWidth, bitmapHeight, getMeasuredWidth(), getMeasuredHeight()); // Create subview rectangle with for the view initially, but these will be updated soon after in onMeasure.
    }

    public Subview getSubview() {
        return subview;
    }

    // This fires when the view first gets its dimensions.
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // On some versions of Android (such as 5.1 - API22), the measured dimensions will be different on each call of onMeasure.  Always accept and set the latest measured dimensions.
        subview.setViewDimensions(getMeasuredWidth(), getMeasuredHeight());

        canvasSet.clear(); // Reset the hashset (rather than reallocating it) so we can prevent redrawing the same canvas multiple times.

        // Check each overlay to see if its canvas needs to be redrawn on measure:
        Overlay overlay;
        for (Map.Entry<String, AnnotationLayer> entry : layers.entrySet()) {
            if ((overlay = entry.getValue().getOverlay()).isViewInflationRequired()) {
                canvasSet.add(overlay.getBackingCanvas());
            }
        }
        for (BackingCanvas canvas : canvasSet) {
            canvas.redrawAll();
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(bgBitmap, subview.getSubviewRectangle(), subview.getViewRectangle(), null); // Draw requested portion of background image
        for (BackingCanvas backingCanvas : canvases) {
            backingCanvas.drawOnCanvas(canvas, subview);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for (Map.Entry<String, AnnotationLayer> entry : layers.entrySet()) {
            if (entry.getValue().isActive()) {
                entry.getValue().getOverlay().evaluateEvent(event, getMeasuredWidth(), getMeasuredHeight());
            }
        }
        invalidate(); // Redraw the view.
        return true;
    }
}
