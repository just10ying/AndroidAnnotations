package com.example.jying.androidannotations.textbox;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;

import com.example.jying.androidannotations.AnnotationView;
import com.example.jying.androidannotations.support.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Justin on 6/4/2015.
 */
public class TextboxOverlay extends Overlay {

    // Constants
    private static final String BUNDLE_PREFIX = "TEXTBOX_OVERLAY";
    private static final String TIMESTAMP_INDEX = "TimestampIndex";
    private static final String DEFAULT_ANNOTATION_TEXT = "";
    private static final int CREATION_LONGPRESS_MS = 300; // The number of milliseconds to wait for a long press
    private static final int MIN_DP_AREA = 500; // The minimum area in DP that a text rectangle must have in order to persist / not be deleted as accidental.

    // Modes
    private static final int MODE_DESELECT = 0;
    private static final int MODE_CREATE = 1;
    private static final int MODE_EDIT = 2;
    private int mode; // Describes what the user is currently doing:

    private Point fingerDownPoint; // The point at which the user's finger started touching the screen.  This is saved so we can create a text rectangle there if that's what the user is really doing.
    private ArrayList<TextRectangle> textRectangles; // List of all the textRectangles that have been created
    private TextRectangle selectedTextRectangle; // The current text rectangle being manipulated
    private TextOverlayDelegate delegate; // Delegate that exposes various activity-level functionality to TextOverlay.
    private Rect savedSubviewRect; // The subview rectangle that represented the view before the user focused on a specific rectangle.
    private boolean hasTimestamp;

    public TextboxOverlay () {
        super();
        this.hasTimestamp = false; // Initially, we start with no timestamp.
        this.requiresTransparency = true; // This overlay requires transparency.

        // Set a default delegate:
        delegate = new TextOverlayDelegate() {
            @Override
            public void onTimeStampSelect(){
            }
            @Override
            public void onTimeStampExistenceChanged(boolean hasTimeStamp){

            }
            @Override
            public void onTextRectangleSelect() {

            }

            @Override
            public void onTextRectangleDeselect() {

            }

            @Override
            public void onTextRectangleModify() {

            }

            @Override
            public void onTextRectangleTextEdit() {

            }

            @Override
            public void onZoom() {

            }

            @Override
            public void onPan() {

            }
        };

        textRectangles = new ArrayList<TextRectangle>(); // Make new arraylist of text rectangles:
        annotationSets.add((List) textRectangles);

        setSelectedTextRectangle(null);
        delegate.onTextRectangleDeselect();

        LONGPRESS_DURATION_MS = CREATION_LONGPRESS_MS; // Set custom longpress time:
    }

    public void setDelegate(TextOverlayDelegate delegate) {
        this.delegate = delegate;
    }

    public boolean isTextRectangleSelected() {
        return (selectedTextRectangle != null);
    }

    public void focusSubviewOnSelectedTextRectangle() {
        if (selectedTextRectangle != null) {
            savedSubviewRect = subview.getSubviewRectangle();
            subview.focusOnRect(selectedTextRectangle.getBoundingRect());
        }
    }

    public void restoreSubviewBeforeFocus() {
        if (savedSubviewRect != null) {
            subview.setSubviewRectangle(savedSubviewRect);
        }
    }

    public void increaseFontSize() {
        selectedTextRectangle.increaseFontSize();
        backingCanvas.redrawAll();
    }

    public void decreaseFontSize() {
        selectedTextRectangle.decreaseFontSize();
        backingCanvas.redrawAll();
    }

    public void removeSelection() {
        setSelectedTextRectangle(null);
        backingCanvas.redrawAll();
    }

    public void deleteSelectedTextRectangle() {
        if(selectedTextRectangle instanceof TimestampRectangle){
            hasTimestamp = false;
            delegate.onTimeStampExistenceChanged(false);
        }

        textRectangles.remove(selectedTextRectangle);
        setSelectedTextRectangle(null);
        delegate.onTextRectangleDeselect();
        backingCanvas.redrawAll();
    }

    public String getText() {
        return selectedTextRectangle.getText();
    }

    public void setText(String newText) {
        if (selectedTextRectangle != null) {
            selectedTextRectangle.setText(newText);
            backingCanvas.redrawAll();
        }
    }

    // Clears all existing drawings and reinitializes the backing bitmap and canvas.
    @Override
    public void clearAnnotations() {
        textRectangles.clear();
        super.clearAnnotations();

        // Cleanup within the overlay
        setSelectedTextRectangle(null);
        delegate.onTextRectangleDeselect();
    }

    @Override
    public void onLayerActivationChanged(boolean active) {
        removeSelection();
    }

    // Called when the user starts a gesture.
    @Override
    protected void gestureStart(Point position) {
        // Get the text rectangle that the user clicked on
        setSelectedTextRectangle(getTextRectangleAtPosition(position));
        // If the user clicked on a text rectangle:
        if (selectedTextRectangle != null) {
            // See if the point the user touched is a point for resizing or moving.
            selectedTextRectangle.setActionPoint(position);
            mode = MODE_EDIT;
        }
        // If the user clicked on blank space:
        else {
            fingerDownPoint = position;
            mode = MODE_DESELECT;
        }
        backingCanvas.redrawAll();
    }

    // Called when the user is performing a one-finger drawing gesture (moving one finger on the screen)
    @Override
    protected void gestureDraw(Point position, int dx, int dy, boolean possiblyAccidental) {
        if (selectedTextRectangle != null) {
            selectedTextRectangle.applyAction(dx, dy, position); // Modify the rectangle bounds accordingly.
            delegate.onTextRectangleModify();
            backingCanvas.redrawAll();
        }
        else if (!possiblyAccidental) {
            // If we know the draw definitely isn't accidental, create a TextRectangle where requested.
            setSelectedTextRectangle(new TextRectangle(fingerDownPoint, position, DEFAULT_ANNOTATION_TEXT, subview));
            textRectangles.add(selectedTextRectangle);
            mode = MODE_CREATE;
        }
    }

    // Called when the user finishes a gesture.
    @Override
    protected void gestureFinish(Point position, int maxFingers) {
        if (selectedTextRectangle != null) {
            Rect bounds = selectedTextRectangle.getBoundingRect();
            Point start = new Point(bounds.left, bounds.top);
            Point end = new Point(bounds.right, bounds.bottom);
            Point viewStart = subview.getViewPoint(start);
            Point viewEnd = subview.getViewPoint(end);
            int xPx = Math.abs(viewStart.x - viewEnd.x);
            int yPx = Math.abs(viewStart.y - viewEnd.y);
            int area = AnnotationView.convertPxToDp(xPx) * AnnotationView.convertPxToDp(yPx);
            if (area < MIN_DP_AREA) {
                // If the area is too small, delete the selected text rectangle.
                textRectangles.remove(selectedTextRectangle);
                setSelectedTextRectangle(null);
                backingCanvas.redrawAll();
            }
            else {
                selectedTextRectangle.enforceBounds(true); // Start enforcing bounds after this point, but...
                selectedTextRectangle.setDefaultOutline(); // ...remove any red outlining when the user removes their finger.
                backingCanvas.redrawAll();
            }
        }

        // If the gesture that just ended has created a text rectangle, show the keyboard.
        if ((mode == MODE_CREATE) && (selectedTextRectangle != null)) {
            delegate.onTextRectangleSelect();
            delegate.onTextRectangleTextEdit();
        }
    }

    // Called when the user has longpressed a point
    @Override
    protected void longPress(Point position) {
        if (selectedTextRectangle instanceof TimestampRectangle) {
            // Do nothing, as we want to disallow editing.
            return;
        }
        if (mode == MODE_EDIT) {
            delegate.onTextRectangleTextEdit();
        }
        // If we're not already editing a rectangle, create a new one on longpress.
        else {
            mode = MODE_CREATE;
            // Create a new text rectangle with empty text
            setSelectedTextRectangle(new TextRectangle(fingerDownPoint, position, DEFAULT_ANNOTATION_TEXT, subview));
            textRectangles.add(selectedTextRectangle);
            backingCanvas.redrawAll();
        }
    }

    @Override
    protected void gesturePan(int dx, int dy) {
        delegate.onPan();
        super.gesturePan(dx, dy);
    }

    @Override
    protected void gestureZoom(float dA) {
        delegate.onZoom();
        super.gestureZoom(dA);
    }

    // Called when the user has started a one-finger draw but has transitioned to a two-finger gesture within a specified draw distance
    @Override
    protected void unintendedDraw() {}

    // Use this function for setting a Text Rectangle as selected.  It handles a lot of activity things.
    private void setSelectedTextRectangle(TextRectangle rectangleToSelect) {
        // Delete the text rectangle if it has no text and is not being selected by the user.
        if ((selectedTextRectangle != null) && (selectedTextRectangle.getText().length() == 0) && (rectangleToSelect != selectedTextRectangle)) {
            textRectangles.remove(selectedTextRectangle);
        }

        // Deselect all text rectangles:
        for (TextRectangle textRectangle : textRectangles) {
            textRectangle.getModel().setSelection(false);
        }

        if (rectangleToSelect == null) {
            delegate.onTextRectangleDeselect();
        }
        else {
            delegate.onTextRectangleSelect();
            rectangleToSelect.getModel().setSelection(true);
            if (rectangleToSelect instanceof TimestampRectangle) {
                delegate.onTimeStampSelect();
            }
        }

        selectedTextRectangle = rectangleToSelect;
    }

    private TextRectangle getTextRectangleAtPosition(Point position) {
        for (int index = textRectangles.size() - 1; index >= 0; index--) {
            if (textRectangles.get(index).contains(position)) {
                return textRectangles.get(index);
            }
        }
        return null;
    }

    @Override
    public void saveToBundle(Bundle bundle, String key) {
        Parcelable textRectangleModels[] = new Parcelable[textRectangles.size()];
        for (int index = 0; index < textRectangles.size(); index++) {
            textRectangleModels[index] = textRectangles.get(index).getModel();
            if (textRectangles.get(index) instanceof TimestampRectangle) {
                bundle.putInt(TIMESTAMP_INDEX, index);
            }
        }
        bundle.putParcelableArray(key + BUNDLE_PREFIX, textRectangleModels);
    }

    @Override
    public void restoreFromBundle(Bundle bundle, String key) {
        int timestampIndex = bundle.getInt(TIMESTAMP_INDEX, -1);
        if (timestampIndex != -1) {
            hasTimestamp = true;
            delegate.onTimeStampExistenceChanged(hasTimestamp);
        }
        Parcelable parceledTextRectangleData[] = bundle.getParcelableArray(key + BUNDLE_PREFIX);
        for (int index = 0; index < parceledTextRectangleData.length; index++) {
            TextRectangleModel model = (TextRectangleModel) parceledTextRectangleData[index];

            // Construct the rectangle using the appropriate class.
            TextRectangle addRectangle = (index == timestampIndex) ? new TimestampRectangle(model, subview) : new TextRectangle(model, subview);
            addRectangle.enforceBounds(true);
            addRectangle.setDefaultOutline(); // remove any red outlining when the user removes their finger.

            textRectangles.add(addRectangle);
            // Restore selection
            if (model.selected) {
                setSelectedTextRectangle(addRectangle);
            }
            backingCanvas.redrawAll();
        }
    }

    public void addTimestampToCanvas(){
        // Ensure only one Timestamp can be added
        if (!hasTimestamp) {
            setSelectedTextRectangle(new TimestampRectangle(subview));
            textRectangles.add(selectedTextRectangle);
            backingCanvas.redrawAll();

            hasTimestamp = true;
            delegate.onTimeStampExistenceChanged(true);
        }
    }


}
