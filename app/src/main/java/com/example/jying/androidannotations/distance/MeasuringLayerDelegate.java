package com.example.jying.androidannotations.distance;

import com.example.jying.androidannotations.support.LayerDelegate;

/**
 * Created by jying on 7/16/2015.
 */
public interface MeasuringLayerDelegate extends LayerDelegate {

    void onCalculateContours(MeasuringOverlay overlay, float scale);
    void onContourSelectedAsReference(MeasuringOverlay overlay);
    void onNoContoursFound();
}
