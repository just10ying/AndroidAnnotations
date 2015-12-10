package com.example.jying.androidannotations;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.example.jying.androidannotations.support.AnnotationLayer;
import com.example.jying.androidannotations.support.BackingCanvas;
import com.example.jying.androidannotations.distance.MeasuringLayer;
import com.example.jying.androidannotations.drawing.DrawingLayer;
import com.example.jying.androidannotations.textbox.TextboxLayer;

import java.util.Map;

/**
 * Created by jying on 7/3/2015.
 */
public class JotActivity extends EditActivity {

    public static Bitmap bgBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setAnnotationBackground(bgBitmap);
        BackingCanvas canvas = new BackingCanvas(bgBitmap.getWidth(), bgBitmap.getHeight());
        canvas.setResizeRatio(.5f);
        createLayer(DrawingLayer.class, canvas, "Draw Mode").setActive(savedInstanceState == null);
        createLayer(MeasuringLayer.class, canvas, "Measure Mode");

        BackingCanvas resizedCanvas = new BackingCanvas(bgBitmap.getWidth(), bgBitmap.getHeight());
        resizedCanvas.setResizeRatio(.5f);
        createLayer(TextboxLayer.class, resizedCanvas, "Text Mode");
    }

    @Override
    protected void submitAnnotations() {
        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
            // Do something to save off the image.
        }
        for (BackingCanvas canvas : backingCanvases) {
            canvas.setResizeRatio(1);
        }
        annotationView.invalidate();
    }
}
