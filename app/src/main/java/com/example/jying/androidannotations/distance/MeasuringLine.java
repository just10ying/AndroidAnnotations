package com.example.jying.androidannotations.distance;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

import com.example.jying.androidannotations.AnnotationView;
import com.example.jying.androidannotations.support.Annotation;
import com.example.jying.androidannotations.support.GestureHandler;
import com.example.jying.androidannotations.support.Subview;

import java.util.ArrayList;

/**
 * Created by jying on 7/6/2015.
 * This class represents the lines that the user can draw
 */
public class MeasuringLine implements Annotation {
    public static final int ENDPOINT_GRAB_TOLERANCE_DP = 20;
    public static final int CENTER_GRAB_TOLERANCE_DP = 15;
    private static final int MINIMUM_DISTANCE_DP = 5;
    private static int endpointGrabTolerancePx = AnnotationView.convertDpToPx(ENDPOINT_GRAB_TOLERANCE_DP);
    private static int minimumDistancePx = AnnotationView.convertDpToPx(MINIMUM_DISTANCE_DP);
    private static int centerGrabTolerancePx = AnnotationView.convertDpToPx(CENTER_GRAB_TOLERANCE_DP);

    private ContourRectangle contourRectangle;
    private Subview subview;
    private MeasuringLineModel model;
    private ArrayList<Point> selectedPoints;

    public MeasuringLine(Point start, Point end, Subview subview) {
        this.subview = subview;
        selectedPoints = new ArrayList<Point>();
        model = new MeasuringLineModel(start);
        model.setEnd(end);
    }

    public MeasuringLine(MeasuringLineModel model, Subview subview) {
        this.subview = subview;
        this.model = model;
        selectedPoints = new ArrayList<Point>();
    }

    public MeasuringLineModel getModel() {
        return model;
    }

    public void setScalingContourRectangle(ContourRectangle contourRectangle) {
        this.contourRectangle = contourRectangle;
    }

    // Canvas parameter required for size.  Each overlay has a reference to the backing canvas, so they can pass that in.
    public boolean contains(Point position) {
        return isPointInTextBox(position) || getPointNearPosition(position) != null;
    }

    public boolean isTooSmall() {
        return GestureHandler.getTotalDistance(model.getPoints()) < minimumDistancePx;
    }

    public void setActionPointToEndpoint() {
        selectedPoints.add(model.getPoints().get(model.getPoints().size() - 1));
    }

    public void setActionPoint(Point position) {
        selectedPoints.clear();

        if (isPointInTextBox(position)) {
            selectedPoints.addAll(model.getPoints());
        }
        else {
            Point selectedPoint = getPointNearPosition(position);
            if (selectedPoint != null) {
                selectedPoints.add(selectedPoint);
            }
        }
    }

    public void setSelected(boolean selected) {
        model.setSelected(selected);
    }

    public void applyAction(int dx, int dy, Point position) {
        // Apply the move to the selected endpoint.
        for (Point point : selectedPoints) {
            point.offset(dx, dy);
        }
    }

    public void drawOnCanvas(Canvas canvas, float resizeRatio) {
        model.drawOnCanvas(canvas, subview, contourRectangle);
    }

    private boolean isPointInTextBox(Point position) {
        Rect boundary = model.getTextBounds(contourRectangle);
        int centerTolerance = (int) (centerGrabTolerancePx / subview.getMagnification());
        boundary.inset(-centerTolerance, -centerTolerance);
        if (boundary.contains(position.x, position.y)) {
            return true;
        }
        return false;
    }

    private Point getPointNearPosition(Point position) {
        int endpointTolerance = (int) (endpointGrabTolerancePx / subview.getMagnification());
        ArrayList<Point> points = model.getPoints();
        for (Point point : points) {
            if (GestureHandler.getDistanceTo(point, position) < endpointTolerance) {
                return point;
            }
        }
        return null;
    }
}
