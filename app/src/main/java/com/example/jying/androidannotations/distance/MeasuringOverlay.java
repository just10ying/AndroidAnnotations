package com.example.jying.androidannotations.distance;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Parcelable;

import com.example.jying.androidannotations.support.Annotation;
import com.example.jying.androidannotations.support.Overlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jying on 7/6/2015.
 */
public class MeasuringOverlay extends Overlay {

    public static final float IMAGE_PROCESSING_SCALE = .5f;

    // Mode constants
    public static final int MODE_SELECT_REFERENCE = 0;
    public static final int MODE_MEASURE = 1;
    private int mode; // The overall mode of the overlay.

    // Bundle prefixes
    private static final String BUNDLE_PREFIX = "MeasuringOverlay";
    private static final String BUNDLE_MEASURED_LINES = "MeasuredLines";
    private static final String BUNDLE_CONTOUR_RECTANGLES = "ContourRectangles";
    private static final String BUNDLE_MODE = "Mode";

    private MeasuringLine currentLine;
    private ArrayList<ContourRectangle> contourRectangles;
    private ArrayList<MeasuringLine> measuredLines;
    private MeasuringOverlayDelegate delegate;

    public MeasuringOverlay() {
        super();

        MAX_ACCIDENTAL_MOVES = 6; // Allow some additional accidental moves in a zoom gesture.
        MINIMUM_DRAW_DP = 0;

        requiresViewInflation = true; // Because of the selection handles, this overlay requires view inflation to size the handles.

        delegate = new MeasuringOverlayDelegate() {
            @Override
            public void onContourSelectionChanged(boolean selected) {

            }

            @Override
            public void onMeasuringModeChanged(int mode) {

            }

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public void onRequestContourDetection() {

            }

            @Override
            public void onMeasuringLineSelected(boolean selected) {

            }
        };
        measuredLines = new ArrayList<>();

        setMode(MODE_SELECT_REFERENCE); // You must select a reference before you do anything else, so by default we start up in this mode.
        annotationSets.add((List) measuredLines); // Show measured lines by default
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        delegate.onMeasuringModeChanged(mode);

        if (delegate.isActive()) {
            switch(mode) {
                case MODE_MEASURE:
                    annotationSets.remove((List) contourRectangles);
                    break;
                case MODE_SELECT_REFERENCE:
                    deselectMeasuringLines();
                    if (contourRectangles == null) {
                        delegate.onRequestContourDetection();
                    }
                    else {
                        annotationSets.add((List) contourRectangles);
                    }
                    break;
            }
        }
        else {
            annotationSets.remove((List) contourRectangles);
            deselectMeasuringLines();
        }

        if (backingCanvas != null) {
            backingCanvas.redrawAll();
        }
    }

    public void setDelegate(MeasuringOverlayDelegate delegate) {
        this.delegate = delegate;
    }

    public void clearAnnotations() {
        measuredLines.clear();
        backingCanvas.redrawAll();
    }

    public void setLongEdgeDimension(double dimension) {
        ContourRectangle rect = getSelectedContourRectangle();
        rect.setLongEdgeDimension(dimension);
        // Set scale on all MeasuringLine objects:
        for (Annotation line : measuredLines) {
            if (line instanceof MeasuringLine) {
                ((MeasuringLine) line).setScalingContourRectangle(rect);
            }
        }
        setMode(MODE_MEASURE);
    }

    public void setContourRectangles(ArrayList<ContourRectangle> contourRectangles) {
        // Copy the new arraylist into this one.  We don't want to change up the reference and add additional layers.
        this.contourRectangles = contourRectangles;
        annotationSets.add((List) this.contourRectangles); // not that "this" is necessary, but...
        backingCanvas.redrawAll();
    }

    public ArrayList<ContourRectangle> getContourRectangles() {
        return contourRectangles;
    }

    public void onDeleteButtonPressed() {
        if (currentLine != null) {
            measuredLines.remove(currentLine);
            currentLine = null;
            backingCanvas.redrawAll();
            delegate.onMeasuringLineSelected(false);
        }
    }

    @Override
    public void onLayerActivationChanged(boolean active) {
        setMode(getMode());
    }

    @Override
    public void saveToBundle(Bundle bundle, String key) {
        // Save Contour Rectangles
        if (contourRectangles != null) {
            bundle.putParcelableArray(key + BUNDLE_PREFIX + BUNDLE_CONTOUR_RECTANGLES, Arrays.copyOf(contourRectangles.toArray(), contourRectangles.size(), Parcelable[].class));
        }

        // Save Measuring Lines
        MeasuringLineModel measuringLineModels[] = new MeasuringLineModel[measuredLines.size()];
        for (int index = 0; index < measuringLineModels.length; index++) {
            measuringLineModels[index] = measuredLines.get(index).getModel();
        }
        bundle.putParcelableArray(key + BUNDLE_PREFIX + BUNDLE_MEASURED_LINES, measuringLineModels);

        // Save mode:
        bundle.putInt(key + BUNDLE_PREFIX + BUNDLE_MODE, mode);

    }

    @Override
    public void restoreFromBundle(Bundle bundle, String key) {
        Parcelable storedCRectangles[] = bundle.getParcelableArray(key + BUNDLE_PREFIX + BUNDLE_CONTOUR_RECTANGLES);
        if (storedCRectangles != null) {
            if (contourRectangles == null) {
                contourRectangles = new ArrayList<>();
            }
            for (Parcelable rect : storedCRectangles) {
                contourRectangles.add((ContourRectangle) rect);
            }
        }

        Parcelable storedLines[] = bundle.getParcelableArray(key + BUNDLE_PREFIX + BUNDLE_MEASURED_LINES);
        for (Parcelable storedLineModel : storedLines) {
            MeasuringLineModel lineModel = (MeasuringLineModel) storedLineModel;
            MeasuringLine line = new MeasuringLine(lineModel, subview);
            ContourRectangle rect;
            if ((rect = getSelectedContourRectangle()) != null) {
                line.setScalingContourRectangle(rect);
            }
            measuredLines.add(line);
        }

        setMode(bundle.getInt(key + BUNDLE_PREFIX + BUNDLE_MODE)); // Restore mode:
    }

    // Called when the user starts a gesture.
    @Override
    protected void gestureStart(Point position) {
        switch(mode) {
            case MODE_MEASURE:
                MeasuringLine line = getMeasuringLineAtPosition(position);
                if (line != null) {
                    setLineSelection(line, true); // If the user clicked on a line, set it as selected.
                    line.setActionPoint(position);
                }
                else {
                    // Otherwise, create a new measuring line:
                    currentLine = new MeasuringLine(position, position, subview);
                    setLineSelection(currentLine, true);

                    // Set the scale of that measuring line, if we have a selected contour rectangle.
                    ContourRectangle rect;
                    if ((rect = getSelectedContourRectangle()) != null) {
                        currentLine.setScalingContourRectangle(rect);
                    }

                    currentLine.setActionPointToEndpoint();
                    measuredLines.add(currentLine);
                }
                break;
            case MODE_SELECT_REFERENCE:
                // Search from small to large, selecting the smallest and deselecting all others.
                boolean found = false;
                for (int index = contourRectangles.size() - 1; index >= 0; index--) {
                    ContourRectangle rect = contourRectangles.get(index);
                    if (rect.contains(position)) {
                        rect.setSelected(!found);
                        found = true;
                    }
                    else {
                        rect.setSelected(false);
                    }
                }
                delegate.onContourSelectionChanged(found);
                backingCanvas.redrawAll();
                break;
        }
    }

    // Called when the user is performing a one-finger drawing gesture (moving one finger on the screen)
    @Override
    protected void gestureDraw(Point position, int dx, int dy, boolean possiblyAccidental) {
        if (mode == MODE_MEASURE) {
            currentLine.applyAction(dx, dy, position);
            backingCanvas.redrawAll();
        }
    }

    // Called when the user has started a one-finger draw but has transitioned to a two-finger gesture within a specified draw distance
    @Override
    protected void unintendedDraw() {
        if (mode == MODE_MEASURE && currentLine != null && currentLine.isTooSmall()) {
            deleteCurrentLine();
        }
    }

    // Called when the user finishes a gesture.
    @Override
    protected void gestureFinish(Point position, int maxFingers) {
        if (maxFingers == 1) {
            if (mode == MODE_MEASURE) {
                if (currentLine != null && currentLine.isTooSmall()) {
                    deleteCurrentLine();
                }
                else if (currentLine != null) {
                    delegate.onMeasuringLineSelected(true);
                }
                backingCanvas.redrawAll();
            }
        }
    }

    // Called when the user has longpressed a point
    @Override
    protected void longPress(Point point) {}

    @Override
    protected void gestureZoom(float dA) {
        super.gestureZoom(dA);
        backingCanvas.redrawAll();
    }

    private ContourRectangle getSelectedContourRectangle() {
        for (ContourRectangle rect : contourRectangles) {
            if (rect.isSelected()) {
                return rect;
            }
        }
        return null;
    }

    private void deselectMeasuringLines() {
        for (MeasuringLine line : measuredLines) {
            line.setSelected(false);
        }
        setLineSelection(null, false);
    }

    private MeasuringLine getMeasuringLineAtPosition(Point position) {
        for (MeasuringLine line : measuredLines) {
            if (line.contains(position)) {
                return line;
            }
        }
        return null;
    }

    private void setLineSelection(MeasuringLine selectLine, boolean select) {
        currentLine = selectLine;
        if (currentLine != null) {
            currentLine.setSelected(select);
        }
        delegate.onMeasuringLineSelected(select);

        // Deselect all other lines.
        for (MeasuringLine line : measuredLines) {
            if (line != selectLine) {
                line.setSelected(false);
            }
        }
    }

    private void deleteCurrentLine() {
        measuredLines.remove(currentLine);
        currentLine = null;
        delegate.onMeasuringLineSelected(false);
    }
}
