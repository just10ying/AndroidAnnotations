package com.example.jying.androidannotations.support;

import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Justin on 6/8/2015.
 */
public abstract class Overlay extends GestureHandler {

    protected BackingCanvas backingCanvas;
    protected List<List<Annotation>> annotationSets;
    protected boolean requiresTransparency = false; // By default, assume that layers don't need transparency unless specifically stated.
    protected boolean requiresViewInflation = false; // By default, assume that layers don't need the view to be inflated (view dimensions) unless specifically stated.

    public Overlay() {
        super();
        annotationSets = new ArrayList<List<Annotation>>();
    }

    public void setBackingCanvas(BackingCanvas newBackingCanvas) {
        if (newBackingCanvas == null) {
            backingCanvas.removeAnnotationSet(this);
        }
        else {
            newBackingCanvas.addAnnotationSet(this);
        }
        this.backingCanvas = newBackingCanvas;
        this.backingCanvas.redrawAll();
    }

    public boolean isViewInflationRequired() {
        return requiresViewInflation;
    }

    public List<List<Annotation>> getAnnotationSets() {
        return annotationSets;
    }

    public boolean requiresTransparency() {
        return requiresTransparency;
    }

    public BackingCanvas getBackingCanvas() {
        return backingCanvas;
    }

    public void clearAnnotations() {
        backingCanvas.redrawAll();
    }

    public abstract void onLayerActivationChanged(boolean active);

    public abstract void saveToBundle(Bundle bundle, String key);

    public abstract void restoreFromBundle(Bundle bundle, String key);

}
