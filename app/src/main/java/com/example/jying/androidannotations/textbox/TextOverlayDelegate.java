package com.example.jying.androidannotations.textbox;

/**
 * Created by jying on 6/11/2015.
 */
public interface TextOverlayDelegate {
    void onTextRectangleSelect();
    void onTextRectangleDeselect();
    void onTextRectangleModify();
    void onTextRectangleTextEdit();
    void onZoom();
    void onPan();
    void onTimeStampSelect();
    void onTimeStampExistenceChanged(boolean hasTimeStamp);
}
