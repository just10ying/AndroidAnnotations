package com.example.jying.androidannotations.support;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by jying on 5/29/2015.
 * This class represents the portion of the background image and drawable portion that is currently in view.
 * This class handles keeping the view at the correct aspect ratio and within the specified boundaries (parent.width() and parent.height()).
 * It handles the pan and zoom events passed in through offsetViewLocation and adjustViewZoom.
 */
public class Subview extends BoundedRectangle {

    // Constants
    private static final float MAX_MAGNIFICATION_MODIFIER = 2; // The view can be zoomed in such that the ratio of pixels in the image to pixels on the screen is 1:MAX_MAGNIFICATION_MODIFIER
    private static final float NEXT_RESIZE_PADDING_PERCENT = .1f; // How much padding to leave on the edges of the screen when a rectangle is focused.  In this case, .1 padding would mean that 80% of the screen is rectangle and 20% is padding, with 10% on either side.

    private Rect rectangleToSet; // The rectangle to set after setViewDimensions gives the subview valid dimensions;
    private int viewWidth, viewHeight; // Stores the dimensions of the view.  This is used to generate an aspect ratio and also calculate the largest magnification allowable.
    private float viewAspectRatio; // This is the aspect ratio of the screen.
    private float zoomModifier; // Controls how quickly zoom actions occur.  For each pixel of distance change between the user's fingers, the distance of the childRectangle will change by the modifier;

    public Subview(int bgWidth, int bgHeight, int viewWidth, int viewHeight) {
        super(new Rect(0, 0, bgWidth, bgHeight), new Rect(0, 0, viewWidth, viewHeight));
        zoomModifier = (float) parent.width() / viewWidth;
    }

    public Rect getSubviewRectangle() {
        return child;
    }

    public void setSubviewRectangle(Rect newSubview) {
        // If the width isn't zero, then we already had the view dimensions set after inflation, and we can go ahead and set the new subview immediately.
        if (child.width() != 0) {
            setChild(newSubview);
            setViewDimensions(viewWidth, viewHeight); // Reset the view dimensions to ensure that the aspect ratio is preserved.
        }
        else {
            rectangleToSet = newSubview;
        }
    }

    public int getBgWidth() {
        return parent.width();
    }

    public int getBgHeight() {
        return parent.height();
    }

    // Adjusts point positions for different canvases.
    public Point getAdjustedPoint(Canvas canvas, Point point) {
        Point returnPoint = new Point();
        returnPoint.x = (int) (point.x * getResizeRatio(canvas));
        returnPoint.y = (int) (point.y * getResizeRatio(canvas));
        return returnPoint;
    }

    // Adjusts rectangles (position and length) for drawing on different canvases.
    public Rect getAdjustedRectangle(Canvas canvas, Rect input) {
        return getAdjustedRectangle(getResizeRatio(canvas), input);
    }

    // Adjusts rectangles (position and length) for drawing on different canvases.
    public Rect getAdjustedRectangle(float resizeRatio, Rect input) {
        int adjustedWidth = (int) (input.width() * resizeRatio);
        int adjustedHeight = (int) (input.height() * resizeRatio);
        int adjustedCenterX = (int) (input.centerX() * resizeRatio);
        int adjustedCenterY = (int) (input.centerY() * resizeRatio);

        Rect adjustedRect = new Rect(adjustedCenterX, adjustedCenterY, adjustedCenterX, adjustedCenterY);
        adjustedRect.inset(-adjustedWidth / 2, -adjustedHeight / 2);

        return adjustedRect;
    }

    public float getResizeRatio(Canvas canvas) {
        return (float) canvas.getWidth() / getBgWidth();
    }

    public float getMagnification() {
        return (float) viewWidth / (float) child.width();
    }

    public Rect getViewRectangle() { return new Rect(0, 0, viewWidth, viewHeight); }

    // Gets the true location on the image that the user has touched.
    public Point getTruePoint(Point original) {
        float widthModifier = (float) child.width() / viewWidth;
        float heightModifier = (float) child.height() / viewHeight;

        return new Point((int) (original.x * widthModifier + child.left), (int) (original.y * heightModifier + child.top));
    }

    // Gets the location on the AnnotationView that the user has touched.
    public Point getViewPoint(Point original) {
        float widthModifier = (float) child.width() / viewWidth;
        float heightModifier = (float) child.height() / viewHeight;

        return new Point((int) ((original.x - child.left) / widthModifier), (int) ((original.y - child.top) / heightModifier));
    }

    public RelativePoint convertToRelativePoint(Point point) {
        return new RelativePoint(point, getBgWidth(), getBgHeight());
    }

    public Point convertFromRelativePoint(RelativePoint point) {
        return point.toPoint(getBgWidth(), getBgHeight());
    }

    // Sets new view dimensions on the child rectangle.
    public void setViewDimensions(int newViewWidth, int newViewHeight) {
        int oldViewWidth = viewWidth; // Save old view width

        // Save new data:
        this.viewWidth = newViewWidth;
        this.viewHeight = newViewHeight;
        this.viewAspectRatio = (float) newViewWidth / (float) newViewHeight;
        this.zoomModifier = (float) parent.width() / viewWidth;

        if (rectangleToSet != null) {
            child.set(rectangleToSet);
            rectangleToSet = null;
        }
        // If we don't have a backing rectangle yet:
        else if ((child.width() == 0) || (child.height() == 0)) {
            // Setup the default child rectangle.
            restoreDefaultSubview();
        }
        // If we have backing rectangle, try to preserve the view using the top left corner
        else {
            // Retain the current top left corner of the backing rectangle.
            float zoomRatio = (float) child.width() / oldViewWidth; // Preserve the old zoom ratio:
            child.right = child.left + (int) (newViewWidth * zoomRatio);
            child.bottom = child.top + (int) (child.width() / viewAspectRatio);

            enforceParentBounds();
        }
    }

    public void offsetViewLocation(int dx, int dy) {
        child.offset(dx, dy);
        enforceParentBounds();
    }

    public void adjustViewZoom(float dDistance) {
        dDistance *= zoomModifier;

        // Calculate change in the bounds of the rect
        float dy = (float) (dDistance / Math.sqrt(Math.pow(viewAspectRatio, 2) + 1));
        float dx = viewAspectRatio * dy;

        insetSubview(dx, dy);
        enforceParentBounds();
    }

    public void focusOnRect(Rect focusRect) {
        float rectRatio = focusRect.width() / focusRect.height();
        int newWidth, newHeight;

        if (rectRatio > viewAspectRatio) {
            // Fit width:
            newWidth = (int) (focusRect.width() * (1 + 2 * NEXT_RESIZE_PADDING_PERCENT));
            newHeight = (int) (newWidth / viewAspectRatio);
        }
        else {
            // Fit height:
            newHeight = (int) (focusRect.height() * (1 + 2 * NEXT_RESIZE_PADDING_PERCENT));
            newWidth = (int) (newHeight * viewAspectRatio);
        }
        // Create a rectangle of 0 area at the center, and then inset it to the correct dimensions.
        child = new Rect(focusRect.centerX(), focusRect.centerY(), focusRect.centerX(), focusRect.centerY());
        insetSubview(-1 * newWidth / 2, -1 * newHeight / 2);

        enforceParentBounds();
    }

    // Generates a new rectangle with the area changed as requested.
    private void insetSubview(float dx, float dy) {
        Rect unmodified = new Rect(child);
        child.inset((int) dx, (int) dy);

        // Ensure that generated rectangle is within specified width and height.
        if ((child.width() > parent.width()) || (child.height() > parent.height())) {
            // Create a default child rectangle that we know is the correct dimensions

            int centerX = child.centerX();
            int centerY = child.centerY();

            // Restore the default subview, as this is the largest valid view.
            restoreDefaultSubview();
            // Center this rectangle at the old rectangle's center to preserve the view.  (If we don't do this and just use resizedRect, then the view will jump back to having a top of 0, 0)
            child.offset(centerX - child.centerX(), centerY - child.centerY());
        }

        if (child.width() < viewWidth / MAX_MAGNIFICATION_MODIFIER) {
            Rect resizedRect = new Rect(child.centerX(), child.centerY(), child.centerX(), child.centerY());
            int insetX = (int) (viewWidth / (-2 * MAX_MAGNIFICATION_MODIFIER));
            int insetY = (int) (viewHeight / (-2 * MAX_MAGNIFICATION_MODIFIER));
            resizedRect.inset(insetX, insetY);
            child.set(resizedRect);
            if ((child.width() > getBgWidth()) || child.height() > getBgHeight()) {
                child.set(unmodified);
            }
        }
    }

    private void restoreDefaultSubview() {
        float bgAspectRatio = (float) parent.width() / (float) parent.height();
        if (bgAspectRatio > viewAspectRatio) {
            // The bg has more horizontal pixels per vertical line than the view.
            // This means that we should use the background's height as the starting height of the rectangle.
            child = new Rect(0, 0, (int) (parent.height() * viewAspectRatio), parent.height());
        }
        else {
            // The bg has less horizontal pixels per vertical line than the view.
            // This means that we should use the background's width as the starting width of the rectangle.
            child = new Rect(0, 0, parent.width(), (int) (parent.width() / viewAspectRatio));
        }
    }
}