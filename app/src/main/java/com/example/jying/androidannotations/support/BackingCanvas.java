package com.example.jying.androidannotations.support;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jying on 7/9/2015.
 */
public class BackingCanvas {

    private Bitmap originalBitmap; // The original bitmap content at the original size
    private int width, height; // If the image is initially blank, we only need to save width and height to recreate the backing Bitmap.  Save these dimensions to conserve memory.

    float resizeRatio; // How much larger or smaller backingBitmap should be than the original.

    private Bitmap backingBitmap; // The bitmap on which drawings occur.  Does not necessarily have to be the original size.
    private Canvas backingCanvas; // Used to draw on the backingBitmap.
    private ArrayList<Overlay> overlays; // A list of lists of annotations that should be drawn.

    public BackingCanvas() {
        overlays = new ArrayList<Overlay>();
        resizeRatio = 1;
    }

    public BackingCanvas(Bitmap originalBitmap) {
        this();
        setOriginalBitmap(originalBitmap);
    }

    public BackingCanvas(int width, int height) {
        this();
        setBlankBitmap(width, height);
    }

    public void setResizeRatio(float resizeRatio) {
        this.resizeRatio = resizeRatio;

        // Recalculate backing canvas based on the new resizeRatio.
        if (originalBitmap == null) {
            setBlankBitmap(width, height);
        }
        else {
            setOriginalBitmap(originalBitmap);
        }

        redrawAll(); // Redraw everything because we just erased it.
    }

    public int getNumLayers() {
        return overlays.size();
    }

    public Canvas getCanvas() {
        return backingCanvas;
    }

    public void setOriginalBitmap(Bitmap originalBitmap) {
        this.originalBitmap = originalBitmap; // Save the original bitmap.
        this.backingBitmap = Bitmap.createScaledBitmap(originalBitmap, (int) (originalBitmap.getWidth() * resizeRatio), (int) (originalBitmap.getHeight() * resizeRatio), false);
        this.backingCanvas = new Canvas(backingBitmap);
    }

    public void setBlankBitmap(int width, int height) {
        this.originalBitmap = null;
        this.width = width;
        this.height = height;

        this.backingBitmap = Bitmap.createBitmap((int) (width * resizeRatio), (int) (height * resizeRatio), Bitmap.Config.ARGB_8888);
        this.backingCanvas = new Canvas(backingBitmap);
    }

    public void resetBackground() {
        if (originalBitmap == null) {
            clearAnnotations();
        }
        else {
            setOriginalBitmap(originalBitmap);
        }
    }

    public void clearAnnotations() {
        backingCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
    }

    public void drawOnCanvas(Canvas canvas, Subview subview) {
        Rect adjustedRect = subview.getAdjustedRectangle(backingCanvas, subview.getSubviewRectangle());
        canvas.drawBitmap(backingBitmap, adjustedRect, subview.getViewRectangle(), null);
    }

    public Bitmap getBitmap() {
        return backingBitmap;
    }

    // Draws an annotation without disturbing any layers below.  Layers above are redrawn.
    public void quickdrawAnnotation(Annotation newAnnotation, Overlay currentLayer) {
        // If any above layers require transparency, we must redraw all.
        for (int index = overlays.indexOf(currentLayer) + 1; index < overlays.size(); index++) {
            if (overlays.get(index).requiresTransparency()) {
                redrawAll();
                return;
            }
        }

        // Otherwise, quickdraw.
        newAnnotation.drawOnCanvas(backingCanvas, resizeRatio);
        for (int index = overlays.indexOf(currentLayer) + 1; index < overlays.size(); index++) {
            for (List<Annotation> annotations : overlays.get(index).getAnnotationSets()) {
                for (Annotation annotation : annotations) {
                    annotation.drawOnCanvas(backingCanvas, resizeRatio);
                }
            }
        }
    }

    public void addAnnotationSet(Overlay overlay) {
        overlays.add(overlay);
    }

    public void removeAnnotationSet(Overlay overlay) {
        overlays.remove(overlay);
    }

    public void redrawAll() {
        resetBackground(); // Reset the background to the original at the correct size.:
        for (Overlay overlay : overlays) {
            for (List<Annotation> annotations : overlay.getAnnotationSets()) {
                for (Annotation annotation : annotations) {
                    annotation.drawOnCanvas(backingCanvas, resizeRatio);
                }
            }
        }
    }

}
