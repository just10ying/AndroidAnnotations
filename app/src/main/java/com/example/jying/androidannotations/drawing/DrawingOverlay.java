package com.example.jying.androidannotations.drawing;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;

import com.example.jying.androidannotations.support.Overlay;
import com.example.jying.androidannotations.support.RelativePoint;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * Created by jying on 5/27/2015.
 */
public class DrawingOverlay extends Overlay {

    private static final String BUNDLE_PREFIX = "DrawingOverlay";
    private static final String BUNDLE_STORED_PATHS = "StoredPaths";
    private static final String BUNDLE_UNDONE_PATHS = "UndonePaths";
    private static final float SIZE_DIFFERENCE_MODIFIER = 1.3f; // Increasing this modifier means that changing sizes will change the stroke width more.
    private static final int BG_WIDTH_DIVISIONS = 300; // Affects the base size of lines.  A larger number means smaller lines.

    private DrawablePath currentPath; // This is the path that's currently being drawn.  This is created when touchDown is called and pushed onto the stack when touchUp is called.
    private Stack<DrawablePath> storedPaths; // A list of paths that the user has drawn
    private Stack<DrawablePath> undonePaths; // A list of paths that the user has undone.  This is used for the redo action.

    private int currentColor; // The color that the next DrawablePath should be:
    private int currentDrawSize; // The draw size of the next DrawablePath, not scaled.

    private boolean erase; // Whether or not we're currently erasing
    private boolean quadAdded; // Whether or not a quadratic bezier curve was added to the current path.  If it's not, we still want to register the dot as a path.

    public DrawingOverlay() {
        super();
        // Initialize the stacks for undo and redo.
        storedPaths = new Stack<DrawablePath>();
        undonePaths = new Stack<DrawablePath>();

        annotationSets.add((List) storedPaths);
    }

    public void setColor(int color) {
        this.currentColor = color;
    }

    public void setDrawSize(int size) {
        this.currentDrawSize = (int) (Math.pow(size, SIZE_DIFFERENCE_MODIFIER) * subview.getBgWidth() / BG_WIDTH_DIVISIONS);
    }

    public void enableEraser() {
        erase = true;
    }

    public void disableEraser() {
        erase = false;
    }

    public boolean isErasing() {
        return erase;
    }

    // Undoes the last path, putting it onto the stack.
    public void undo() {
        if (!storedPaths.empty()) {
            undonePaths.push(storedPaths.pop());
            backingCanvas.redrawAll();
        }
    }

    // Takes an action from undonePaths and redoes it.
    public void redo() {
        if (!undonePaths.empty()) {
            DrawablePath redoPath = undonePaths.pop();
            storedPaths.push(redoPath);
            backingCanvas.redrawAll();
        }
    }

    @Override
    public void clearAnnotations() {
        // Reset the stacks for undo and redo.
        storedPaths.clear();
        undonePaths.clear();
        super.clearAnnotations();
    }

    @Override
    public void onLayerActivationChanged(boolean active) {

    }

    @Override
    public void saveToBundle(Bundle bundle, String key) {
        bundle.putParcelableArray(key + BUNDLE_PREFIX + BUNDLE_STORED_PATHS, Arrays.copyOf(storedPaths.toArray(), storedPaths.size(), Parcelable[].class));
        bundle.putParcelableArray(key + BUNDLE_PREFIX + BUNDLE_UNDONE_PATHS, Arrays.copyOf(undonePaths.toArray(), undonePaths.size(), Parcelable[].class));
    }

    @Override
    public void restoreFromBundle(Bundle bundle, String key) {
        Parcelable stored[] = bundle.getParcelableArray(key + BUNDLE_PREFIX + BUNDLE_STORED_PATHS);
        Parcelable undone[] = bundle.getParcelableArray(key + BUNDLE_PREFIX + BUNDLE_UNDONE_PATHS);
        for (Parcelable path : stored) {
            storedPaths.push((DrawablePath) path);
        }
        for (Parcelable path : undone) {
            undonePaths.push((DrawablePath) path);
        }
    }

    // Called when the user starts a gesture.
    @Override
    protected void gestureStart(Point position) {
        quadAdded = false;  // Reset path added.

        currentPath = new DrawablePath(currentColor, currentDrawSize, erase);
        storedPaths.push(currentPath);

        currentPath.moveTo(new RelativePoint((double) position.x / subview.getBgWidth(), (double) position.y / subview.getBgHeight()));
    }

    // Called when the user is performing a one-finger drawing gesture (moving one finger on the screen)
    @Override
    protected void gestureDraw(Point position, int dx, int dy, boolean possiblyAccidental) {
        double pointAX = (double) (position.x - dx);
        double pointAY = (double) (position.y - dy);
        RelativePoint pointA = new RelativePoint(pointAX / subview.getBgWidth(), pointAY / subview.getBgHeight());

        double pointBX = (double) (2 * position.x - dx) / 2;
        double pointBY = (double) (2 * position.y - dy) / 2;
        RelativePoint pointB = new RelativePoint(pointBX / subview.getBgWidth(), pointBY / subview.getBgHeight());

        currentPath.quadTo(pointA, pointB);
        quadAdded = true;
        if (erase && (backingCanvas.getNumLayers() != 1)) {
            backingCanvas.redrawAll();
        }
        else {
            backingCanvas.quickdrawAnnotation(currentPath, this);
        }
    }

    // Called when the user has started a one-finger draw but has transitioned to a two-finger gesture within a specified draw distance
    @Override
    protected void unintendedDraw() {

    }

    // Called when the user finishes a gesture.
    @Override
    protected void gestureFinish(Point position, int maxFingers) {
        if (maxFingers == 1) {
            RelativePoint point;
            if (quadAdded) {
                // If there's a quadratic bezier curve that's been added, just finish off the line.
                point = new RelativePoint((double) position.x / subview.getBgWidth(), (double) position.y / subview.getBgHeight());

            } else {
                // Otherwise, draw a line to one above that point.
                point = new RelativePoint((double) (position.x + 1) / subview.getBgWidth(), (double) (position.y + 1) / subview.getBgHeight());
            }
            currentPath.lineTo(point);
            backingCanvas.redrawAll();
        }
        else {
            storedPaths.remove(currentPath);
        }
    }

    // Called when the user has longpressed a point
    @Override
    protected void longPress(Point point) {}
}
