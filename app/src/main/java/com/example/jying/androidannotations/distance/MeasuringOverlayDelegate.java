package com.example.jying.androidannotations.distance;

/**
 * Created by jying on 7/17/2015.
 */
public interface MeasuringOverlayDelegate {
    void onContourSelectionChanged(boolean selected);
    void onMeasuringModeChanged(int mode);
    void onRequestContourDetection();
    void onMeasuringLineSelected(boolean selected);
    boolean isActive();
}
