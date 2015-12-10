package com.example.jying.androidannotations.textbox;

import android.graphics.Point;
import android.graphics.Rect;

import com.example.jying.androidannotations.support.Subview;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by mreat on 7/9/15.
 */
public class TimestampRectangle extends TextRectangle {

    public TimestampRectangle(Subview subview){
        super(new Point(0, 0), new Point(0, 0), "", subview);

        SimpleDateFormat df = new SimpleDateFormat("MMM-dd-yyyy hh:mm", Locale.US);
        String dateString = df.format(Calendar.getInstance().getTime());
        this.setText(dateString);

        minimumWidth = (int) getModel().measureText(dateString);
        minimumHeight = 0;

        enforceBounds = true;
        fixBounds(null, false);
        resizeVerticallyToFitText(true);
        resetModifiedMemory(); // Don't show red outline for timestamps.
    }

    public TimestampRectangle(TextRectangleModel model, Subview subview) {
        super(model, subview);
        minimumWidth = (int) getModel().measureText(getText());
        minimumHeight = 0;
    }

    public void applyAction(int dx, int dy, Point position){
        child.offset(dx, dy);
        Rect unmodifiedRect = new Rect(child);
        if (enforceBounds) {
            fixBounds(unmodifiedRect, false);
        }
    }



}
