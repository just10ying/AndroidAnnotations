package com.example.jying.androidannotations.textbox;

import android.widget.EditText;

import com.example.jying.androidannotations.support.LayerDelegate;

/**
 * Created by jying on 6/26/2015.
 */
public interface TextboxLayerDelegate extends LayerDelegate {
    void onTextRectangleStartEditing(EditText editText, String content); // Raise keyboard
    void onTextRectangleStopEditing(EditText editText); // Raise keyboard
    void onTextRectangleFullEditing(TextboxOverlay overlay); // Show modal
}
