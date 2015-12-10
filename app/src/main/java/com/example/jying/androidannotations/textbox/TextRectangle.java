package com.example.jying.androidannotations.textbox;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.StaticLayout;

import com.example.jying.androidannotations.support.Annotation;
import com.example.jying.androidannotations.support.BoundedRectangle;
import com.example.jying.androidannotations.support.Subview;
import com.example.jying.androidannotations.AnnotationView;

/**
 * Created by Justin on 6/16/2015.
 */
public class TextRectangle extends BoundedRectangle implements Annotation {

    private static final int MINIMUM_TEXT_SIZE = 12;
    private static final int DEFAULT_TEXT_SIZE_INCREMENT = 4;
    
    private static final int RECT_EDGE_TOLERANCE_DP = 20;
    private static final int RECT_CORNER_TOLERANCE_DP = 40;
    private static final int TEXT_SIZE_DECREMENT = 4;

    // Above constants in px units
    protected int DEFAULT_MINIMUM_WIDTH_DP = 160;
    protected int DEFAULT_MINIMUM_HEIGHT_DP = 90;
    protected int minimumWidth = AnnotationView.convertDpToPx(DEFAULT_MINIMUM_WIDTH_DP);
    protected int minimumHeight = AnnotationView.convertDpToPx(DEFAULT_MINIMUM_HEIGHT_DP);
    private static int edgeTolerance = AnnotationView.convertDpToPx(RECT_EDGE_TOLERANCE_DP);
    private static int cornerTolerance = AnnotationView.convertDpToPx(RECT_CORNER_TOLERANCE_DP);

    // Resize directions: describes what direction the user is trying to resize
    private boolean modifyBoundLeft;
    private boolean modifyBoundTop;
    private boolean modifyBoundRight;
    private boolean modifyBoundBottom;

    protected boolean enforceBounds; // Whether or not to enforce valid bounds on this TextRectangle.
    private Subview subview; // It's necessary to know the state of the subview (for magnification purposes) to adjust the edge tolerances for selection.
    private TextRectangleModel model; // This is an object that holds the actual representation of the text rectangle.  TextRectangleManager simply makes sure that any changes to that data are valid.

    public TextRectangle(Point origin, Point endPoint, String text, Subview subview) {
        // Set up the bounds of the BoundedRectangle.
        super(new Rect(0, 0, subview.getBgWidth(), subview.getBgHeight()), new Rect(origin.x, origin.y, endPoint.x, endPoint.y));
        this.subview = subview;
        this.model = new TextRectangleModel(child, text);

        // On creation, the text rectangle can be resized by default in the bottom right direction.
        resetModifierBooleans();
        modifyBoundRight = true;
        modifyBoundBottom = true;

        // By default, don't enforce bounds until it's enabled (on gestureFinish)
        enforceBounds = false;
    }

    public TextRectangleModel getModel() {
        return model;
    }

    public Rect getBoundingRect() {
        return child;
    }

    public void increaseFontSize() {
        model.setFontSize(model.getFontSize() + DEFAULT_TEXT_SIZE_INCREMENT);
        adjustBoundsForFontChange();
    }

    public void decreaseFontSize() {
        int requestedTextSize = model.getFontSize() - DEFAULT_TEXT_SIZE_INCREMENT;
        if (requestedTextSize < MINIMUM_TEXT_SIZE) {
            requestedTextSize = MINIMUM_TEXT_SIZE;
        }
        model.setFontSize(requestedTextSize);
        adjustBoundsForFontChange();
    }

    public void adjustBoundsForFontChange() {
        if (enforceBounds) {
            fixBounds(new Rect(child), false);
        }
        resizeFontToFitHeight();
        resizeVerticallyToFitText(true);
        enforceParentBounds();

        resetModifiedMemory(); // Don't show red outlines on resize
    }

    public String getText() {
        return model.text;
    }
    public void setText(String newText) {
        // Save the bound modifiers:
        boolean leftBoundModifierPrevious = modifyBoundLeft;
        boolean topBoundModifierPrevious = modifyBoundTop;
        boolean rightBoundModifierPrevious = modifyBoundRight;
        boolean bottomBoundModifierPrevious = modifyBoundBottom;

        // Text boxes expand upward.
        modifyBoundLeft = false;
        modifyBoundTop = true;
        modifyBoundRight = false;
        modifyBoundBottom = false;

        model.text = newText;
        resizeVerticallyToFitText(false);
        resizeFontToFitHeight();

        // Restore modifiers.
        modifyBoundLeft = leftBoundModifierPrevious;
        modifyBoundTop = topBoundModifierPrevious;
        modifyBoundRight = rightBoundModifierPrevious;
        modifyBoundBottom = bottomBoundModifierPrevious;
    }

    public boolean contains(Point point) {
        int tolerance = (int) (cornerTolerance / subview.getMagnification());
        Rect toleranceRect = new Rect(child);
        toleranceRect.inset(-1 * tolerance, -1 * tolerance);
        return toleranceRect.contains(point.x, point.y);
    }

    public void enforceBounds(boolean enforce) {
        enforceBounds = enforce;
        if (enforce) {
            child.sort(); // Fix any inverted bounds
            fixBounds(null, true);
        }
    }

    public void setDefaultOutline() {
        resetModifiedMemory();
    }

    public void setActionPoint(Point point) {
        int boundaryDistance = (int) (edgeTolerance / subview.getMagnification());
        int cornerDimension = (int) (cornerTolerance / subview.getMagnification());

        // Use the padded boundaries for calculating resize regions
        Rect paddedRect = model.getPaddedBoundaries();

        // Create rectangles for resizing from edges
        Rect leftEdgeBound = new Rect(paddedRect.left - boundaryDistance / 2, paddedRect.top, paddedRect.left + boundaryDistance / 2, paddedRect.bottom);
        Rect topEdgeBound = new Rect(paddedRect.left, paddedRect.top - boundaryDistance / 2, paddedRect.right, paddedRect.top + boundaryDistance / 2);
        Rect rightEdgeBound = new Rect(paddedRect.right - boundaryDistance / 2, paddedRect.top, paddedRect.right + boundaryDistance / 2, paddedRect.bottom);
        Rect bottomEdgeBound = new Rect(paddedRect.left, paddedRect.bottom - boundaryDistance / 2, paddedRect.right, paddedRect.bottom + boundaryDistance / 2);

        // Create rectangles for resizing from corners.  These are initially size 0 but are centered at the corners of the rectangle.
        Rect bottomLeftBound = new Rect(paddedRect.left, paddedRect.bottom, paddedRect.left, paddedRect.bottom);
        Rect bottomRightBound = new Rect(paddedRect.right, paddedRect.bottom, paddedRect.right, paddedRect.bottom);
        Rect topLeftBound = new Rect(paddedRect.left, paddedRect.top, paddedRect.left, paddedRect.top);
        Rect topRightBound = new Rect(paddedRect.right, paddedRect.top, paddedRect.right, paddedRect.top);

        bottomLeftBound.inset(cornerDimension / -2, cornerDimension / -2);
        bottomRightBound.inset(cornerDimension / -2, cornerDimension / -2);
        topLeftBound.inset(cornerDimension / -2, cornerDimension / -2);
        topRightBound.inset(cornerDimension / -2, cornerDimension / -2);

        resetModifierBooleans();

        if (topLeftBound.contains(point.x, point.y)) {
            modifyBoundTop = true;
            modifyBoundLeft = true;
        }
        else if (bottomLeftBound.contains(point.x, point.y)) {
            modifyBoundBottom = true;
            modifyBoundLeft = true;
        }
        else if (topRightBound.contains(point.x, point.y)) {
            modifyBoundTop = true;
            modifyBoundRight = true;
        }
        else if (bottomRightBound.contains(point.x, point.y)) {
            modifyBoundBottom = true;
            modifyBoundRight = true;
        }
        else if (leftEdgeBound.contains(point.x, point.y)) {
            modifyBoundLeft = true;
        }
        else if (topEdgeBound.contains(point.x, point.y)) {
            modifyBoundTop = true;
        }
        else if (rightEdgeBound.contains(point.x, point.y)) {
            modifyBoundRight = true;
        }
        else if (bottomEdgeBound.contains(point.x, point.y)) {
            modifyBoundBottom = true;
        }
        else if (paddedRect.contains(point.x, point.y)) {
            // A move is represented by setting all bound modifiers to true
            modifyBoundLeft = true;
            modifyBoundTop = true;
            modifyBoundRight = true;
            modifyBoundBottom = true;
        }
    }

    public void applyAction(int dx, int dy, Point position) {
        Rect unmodifiedRect = new Rect(child);

        if (modifyBoundLeft && modifyBoundTop && modifyBoundRight && modifyBoundBottom) {
            // The action is a move:
            child.offset(dx, dy);
        }
        else {
            if (modifyBoundLeft) {
                child.left = position.x;
            }
            if (modifyBoundTop) {
                child.top = position.y;
            }
            if (modifyBoundRight) {
                child.right = position.x;
            }
            if (modifyBoundBottom) {
                child.bottom = position.y;
            }
        }

        if (enforceBounds) {
            fixBounds(unmodifiedRect, false);
        }
    }

    // Ensures that the bounds of the rectangle are valid and aren't cutting off text.
    // The Rect argument is the rectangle to revert to if the resize won't fit in the requested boundary.  This can be null if you don't want to revert.
    // The boolean argument specifies whether or not to center the rectangle with fixed bounds at the old rectangle's position or to respect the bound modifier booleans.
    protected void fixBounds(Rect oldRect, boolean retainCenter) {
        // Assume that we fix no boundaries:
        resetModifiedMemory();

        // Ensure that the width is at least the minimum width.

        if (retainCenter) {
            enforceMinWidth(minimumWidth, .5f);
            enforceMinHeight(minimumHeight, .5f);
        }
        else {
            enforceMinWidth(minimumWidth, modifyBoundLeft ? 1 : 0);
            enforceMinHeight(minimumHeight, modifyBoundTop ? 1 : 0);
        }

        // Ensure that the rect is entirely within the viewable area, accounting for the padding and outline:
        enforceParentPaddedBounds();
        
        // If we had to resize the text, and we have a rectangle to revert to:
        if (resizeVerticallyToFitText(false)) {
            if (oldRect != null) {
                // Reset the previous rectangle
                setChild(oldRect);
                // Have the vertical bounds fit the rectangle snugly.
                resizeVerticallyToFitText(true);
            }
        }
    }

    // Resizes the bounding rectangle if necessary to fit the text.
    // Returns true if a resize had to occur.  Returns false otherwise.
    protected boolean resizeVerticallyToFitText(boolean forceFit) {
        // Check to ensure that text is not overflowing vertically.
        StaticLayout textRegion = model.getTextLayout();
        if ((child.height() < textRegion.getHeight()) || forceFit) {
            if (modifyBoundTop) {
                child.top = child.bottom - textRegion.getHeight();
                enforceMinHeight(minimumHeight, 1);
            }
            // By default, overflow off the bottom.
            else {
                child.bottom = child.top + textRegion.getHeight();
                enforceMinHeight(minimumHeight, 0);
            }
            return true;
        }
        return false;
    }

    private void resizeFontToFitHeight() {
        // While the bounding rectangle is too large, keep decreasing the font until the font fits.
        while (model.getOutlineBoundaries().height() > subview.getBgHeight()) {
            model.setFontSize(model.getFontSize() - TEXT_SIZE_DECREMENT);
            resizeVerticallyToFitText(true);
        }
    }

    protected void enforceParentPaddedBounds() {
        Rect baseBounds = new Rect(child);
        child.set(model.getOutlineBoundaries());
        // We have to reset the boundaries of the child to what they were before we accounted for the padding and outline.
        int dx = (child.width() - baseBounds.width()) / 2;
        int dy = (child.height() - baseBounds.height()) / 2;
        enforceParentBounds();
        child.inset(dx, dy);
    }

    // Set all modifiers to false.
    private void resetModifierBooleans() {
        modifyBoundLeft = false;
        modifyBoundTop = false;
        modifyBoundRight = false;
        modifyBoundBottom = false;
    }

    public void drawOnCanvas(Canvas canvas, float resizeRatio) {
        model.drawOnCanvas(canvas, subview, getModifiedbounds());
    }

    public TextRectangle(TextRectangleModel data, Subview subview) {
        this(new Point(data.backingRect.left, data.backingRect.top), new Point(data.backingRect.right, data.backingRect.bottom), data.text, subview);
        model.setFontSize(data.fontSize);
    }
}