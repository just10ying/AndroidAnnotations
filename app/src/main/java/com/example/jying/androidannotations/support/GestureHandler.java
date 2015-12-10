package com.example.jying.androidannotations.support;

import android.graphics.Point;
import android.os.Handler;
import android.view.MotionEvent;

import com.example.jying.androidannotations.AnnotationView;

import java.util.ArrayList;

/**
 * Created by jying on 6/4/2015.
 * Provides handler functions for common
 */
public abstract class GestureHandler {

    protected final int LONGPRESS_DISTANCE_DP = 5; // The max distance in DP that the user's finger can travel before being disqualified as a long press.

    // When a user tries to two-finger touch, one finger is always on the screen a split second before the other, and this finger may move before before the second goes down.
    // We start recording a line right away when the first finger goes down, and we want to remove that line if the user actually wanted it to be a two-finger action.
    // This is the number of polled moves that can be registered before the initially drawn path is not deleted.
    // Helps protect against accidental draws when trying to zoom.
    protected int MAX_ACCIDENTAL_MOVES = 3;
    private int MAX_SUPPORTED_FINGERS = 2; // The number of fingers currently supported.  This describes how many fingers' coordinates that are saved between polls.
    protected int MINIMUM_DRAW_DP = 1; // Controls how far the user has to move their finger in the timespan of one polling for the action to be considered a draw.  Helps performance by making paths less lengthy.
    protected int MINIMUM_ZOOM_DP = 1; // Controls how far the user has to move their fingers apart in one polling for the action to be considered a zoom.  Helps protect against accidental zooms when trying to pan.
    private int actionMaxFingers = 0; // Remembers the highest number of fingers in this action
    private int numMoveActions = 0;

    private final Handler longpressHandler = new Handler(); // Handles long presses
    private Runnable longpressCallback; // The runnable that will call the longpress function once the timer is up.
    private Point longpressPoint; // The original longpress point
    private Point[] currentTouches = new Point[MAX_SUPPORTED_FINGERS];
    private Point[] previousTouches = new Point[MAX_SUPPORTED_FINGERS];

    protected int LONGPRESS_DURATION_MS = 500; // The number of milliseconds to wait for a long press
    protected Subview subview; // Represents the part of the canvas that the user is currently looking at.

    public GestureHandler() {

    }

    public void setSubview(Subview subview) {
        this.subview = subview;
    }

    // This requires a view for vibration as well as invalidation for long presses, which are on a timer.
    public void evaluateEvent(MotionEvent event, int maxX, int maxY) {
        int pointerCount = event.getPointerCount(); // Record the number of fingers in the event.

        // Save where the user is touching
        for (int fingerNum = 0; fingerNum < MAX_SUPPORTED_FINGERS; fingerNum++) {
            if (fingerNum < pointerCount) {
                // Ensure that touch is within bounds:
                int adjustedX = (int) event.getX(fingerNum);
                int adjustedY = (int) event.getY(fingerNum);

                if (adjustedX < 0) {
                    adjustedX = 0;
                }
                else if (adjustedX > maxX) {
                    adjustedX = maxX;
                }

                if (adjustedY < 0) {
                    adjustedY = 0;
                }
                else if (adjustedY > maxY) {
                    adjustedY = maxY;
                }
                currentTouches[fingerNum] = new Point(adjustedX, adjustedY);
            }
            else {
                currentTouches[fingerNum] = null;
            }
        }

        // Handle the user's action
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                numMoveActions = 0;
                actionMaxFingers = pointerCount;
                gestureStart(subview.getTruePoint(currentTouches[0]));

                longpressPoint = currentTouches[0]; // Save the current point where the finger went down
                // Start long press handler:
                longpressHandler.postDelayed(longpressCallback = new Runnable() {
                    public void run() {
                        longpressPoint = subview.getTruePoint(currentTouches[0]);
                        longPress(longpressPoint);
                    }
                }, LONGPRESS_DURATION_MS);
                break;
            case MotionEvent.ACTION_MOVE:
                numMoveActions++;
                actionMaxFingers = Math.max(actionMaxFingers, pointerCount);
                // If we're doing a two finger action and we have a previous two-finger action
                if ((pointerCount == 2) && (previousTouches[1] != null)) {
                    longpressHandler.removeCallbacks(longpressCallback); // It's no longer a long press
                    if (numMoveActions < MAX_ACCIDENTAL_MOVES) {
                        unintendedDraw();
                    }
                    float fingerDistanceChange = getTotalDistance(currentTouches) - getTotalDistance(previousTouches);
                    // If the user's action is drastic enough to be considered a zoom:
                    if (Math.abs(fingerDistanceChange) > AnnotationView.convertDpToPx(MINIMUM_ZOOM_DP)) {
                        gestureZoom(fingerDistanceChange);
                    }
                    // Panning is always on.
                    Point centerNew = getPointsCenter(currentTouches);
                    Point centerOld = getPointsCenter(previousTouches);
                    int dx = centerOld.x - centerNew.x;
                    int dy = centerOld.y - centerNew.y;
                    gesturePan(dx, dy);
                }
                else if (actionMaxFingers == 1) {
                    float distanceTraveled = getTotalDistance(new Point[]{currentTouches[0], previousTouches[0]}); // The distance between this touch and the last touch in pixels
                    // If the user's action is drastic enough to be considered a draw:
                    if (distanceTraveled > AnnotationView.convertDpToPx(MINIMUM_DRAW_DP)) {
                        int dx = 0;
                        int dy = 0;
                        if (previousTouches != null) {
                            Point currentTruePoint = subview.getTruePoint(currentTouches[0]);
                            Point lastTruePoint = subview.getTruePoint(previousTouches[0]);
                            dx = currentTruePoint.x - lastTruePoint.x;
                            dy = currentTruePoint.y - lastTruePoint.y;
                        }
                        gestureDraw(subview.getTruePoint(currentTouches[0]), dx, dy, numMoveActions < MAX_ACCIDENTAL_MOVES);
                    }
                    // If the user's finger has moved too far from its original position for it to be considered a long press:
                    if (getTotalDistance(new Point[]{longpressPoint, currentTouches[0]}) > AnnotationView.convertDpToPx(LONGPRESS_DISTANCE_DP)) {
                        longpressHandler.removeCallbacks(longpressCallback);
                    }
                }
                // Save the last coordinates.c
                previousTouches = currentTouches.clone();
                break;
            case MotionEvent.ACTION_UP:
                longpressHandler.removeCallbacks(longpressCallback);
                gestureFinish(subview.getTruePoint(currentTouches[0]), actionMaxFingers);
                previousTouches = new Point[MAX_SUPPORTED_FINGERS]; // Clear previous touches
                break;
        }
    }

    // Called when the user starts a gesture.
    protected abstract void gestureStart(Point position);

    // Called when the user is performing a one-finger drawing gesture (moving one finger on the screen)
    protected abstract void gestureDraw(Point position, int dx, int dy, boolean possiblyAccidental);

    // Called when the user has started a one-finger draw but has transitioned to a two-finger gesture within a specified draw distance
    protected abstract void unintendedDraw();

    // Called when the user finishes a gesture.
    protected abstract void gestureFinish(Point position, int maxFingers);

    // Called when the user has longpressed a point
    protected abstract void longPress(Point position);

    // Called when the user is gesturing with two fingers to pan the view
    protected void gesturePan(int dx, int dy) {
        subview.offsetViewLocation(dx, dy);
    }

    // Called when the user is gesturing with two fingers to zoom the view
    protected void gestureZoom(float dA) {
        subview.adjustViewZoom(dA);
    }

    public static float getDistanceTo(Point a, Point b) {
        Point[] points = {a, b};
        return getTotalDistance(points);
    }

    // Gets the total distance between each of the points in the array in order.  Does not wrap around.  Distance is 0 if elements are null.
    public static float getTotalDistance(Point[] points) {
        float totalDistance = 0;
        for (int index = 0; index < (points.length - 1); index++) {
            if ((points[index] == null) || (points[index + 1 ] == null)) {
                totalDistance += 0;
            }
            else {
                int dx = points[index].x - points[index + 1].x;
                int dy = points[index].y - points[index + 1].y;
                totalDistance += Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
            }
        }
        return totalDistance;
    }

    public static float getTotalDistance(ArrayList<Point> points) {
        return getTotalDistance(points.toArray(new Point[points.size()]));
    }

    // Returns the center of all the points.
    public static Point getPointsCenter(Point[] points) {
        int x = 0;
        int y = 0;
        for (Point p : points) {
            x += p.x;
            y += p.y;
        }
        return new Point(x / points.length, y / points.length);
    }

}
