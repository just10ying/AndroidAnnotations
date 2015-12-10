package com.example.jying.androidannotations.textbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.example.jying.androidannotations.AnnotationView;
import com.example.jying.androidannotations.support.Subview;

/**
 * Created by Justin on 6/29/2015.
 */
public class TextRectangleModel implements Parcelable {

    private static final int DEFAULT_BG_COLOR = Color.argb(120, 0, 0, 0);
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_TEXT_SIZE = 42;
    private static final int PADDING_DP = 3;

    private static final int OUTLINE_COLOR_DEFAULT = Color.WHITE;
    private static final int OUTLINE_COLOR_ALERT = Color.RED;
    private static final int OUTLINE_WIDTH_DP = 2;

    private static int padding = AnnotationView.convertDpToPx(PADDING_DP);
    private static int outlineWidth = AnnotationView.convertDpToPx(OUTLINE_WIDTH_DP);

    private Paint defaultOutlinePaint, alertOutlinePaint; // Paints used to outline the selected TextRectangle.
    private Paint bgPaint;
    private Canvas textCanvas;
    private TextPaint textPaint;

    // Data required to reconstruct TextRectangleModel
    public Rect backingRect;
    public String text;
    public int fontSize;
    public boolean selected;

    public TextRectangleModel(Rect backingRect, String text) {
        this.backingRect = backingRect;
        this.text = text;
        this.fontSize = DEFAULT_TEXT_SIZE;
        this.selected = false;

        textCanvas = new Canvas();

        // Initialize paints:
        bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(DEFAULT_BG_COLOR);

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setColor(DEFAULT_TEXT_COLOR);
        textPaint.setTextSize(DEFAULT_TEXT_SIZE);

        // Set default paint for selection outlines:
        defaultOutlinePaint = new Paint();
        defaultOutlinePaint.setStyle(Paint.Style.STROKE);
        defaultOutlinePaint.setStrokeWidth(outlineWidth);
        defaultOutlinePaint.setColor(OUTLINE_COLOR_DEFAULT);

        alertOutlinePaint = new Paint(defaultOutlinePaint);
        alertOutlinePaint.setColor(OUTLINE_COLOR_ALERT);
    }

    public void setSelection(boolean selected) {
        this.selected = selected;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        textPaint.setTextSize(fontSize);
    }

    public int getFontSize() {
        return (int) textPaint.getTextSize();
    }

    public void drawOnCanvas(Canvas canvas, Subview subview, BoundsModificationRecord record) {
        float resizeRatio = subview.getResizeRatio(canvas);
        StaticLayout textRegion = getTextLayout();
        Rect savedRect = null;

        // If the canvas we're drawing on is not at native resolution, do a bit of extra work to adjust the rectangle such that it's in the right relative place on the new canvas.
        // Temporarily adjust the size of backingRect.  If the ratio is 1, there's no harm in adjusting the location.
        savedRect = new Rect(backingRect);
        Rect adjustedRect = subview.getAdjustedRectangle(canvas, backingRect);
        backingRect.set(adjustedRect);

        // Draw padded boundaries.  This is the background shading.
        Rect paddedRect = getPaddedAdjustedBoundaries(resizeRatio);
        canvas.drawRect(paddedRect, bgPaint);

        // Draw the text:
        if ((resizeRatio != 1) && (text.length() > 0)) {
            // Create a bitmap that is the text portion of the annotation at native resolution
            Bitmap textBitmap = Bitmap.createBitmap(textRegion.getWidth(), textRegion.getHeight(), Bitmap.Config.ARGB_8888);
            textCanvas.setBitmap(textBitmap);
            textRegion.draw(textCanvas);

            // Draw a downscaled version of that bitmap onto the canvas
            int right = (int) (backingRect.left + textBitmap.getWidth() * resizeRatio);
            int bottom = (int) (backingRect.top + textBitmap.getHeight() * resizeRatio);
            canvas.drawBitmap(textBitmap, new Rect(0, 0, textBitmap.getWidth(), textBitmap.getHeight()), new Rect(backingRect.left, backingRect.top, right, bottom), null);
        }
        else {
            canvas.save();
            canvas.translate(Math.min(backingRect.left, backingRect.right), Math.min(backingRect.top, backingRect.bottom));
            textRegion.draw(canvas);
            canvas.restore();
        }

        // Draw selection outlines.
        if (selected) {
            // Calculate new selection outline dimensions
            defaultOutlinePaint.setStrokeWidth(outlineWidth * resizeRatio);
            alertOutlinePaint.setStrokeWidth(outlineWidth * resizeRatio);

            Rect borderRect = getOutlineAdjustedBoundaries(resizeRatio);

            // Inset by 1/2 the line width to have the line in the center:
            int lineWidth = (int) (defaultOutlinePaint.getStrokeWidth() * resizeRatio);
            borderRect.inset(lineWidth / 2, lineWidth / 2);

            Rect leftLine = new Rect(borderRect.left, borderRect.top, borderRect.left, borderRect.bottom);
            Rect topLine = new Rect(borderRect.left, borderRect.top, borderRect.right, borderRect.top);
            Rect rightLine = new Rect(borderRect.right, borderRect.top, borderRect.right, borderRect.bottom);
            Rect bottomLine = new Rect(borderRect.left, borderRect.bottom, borderRect.right, borderRect.bottom);

            canvas.drawRect(leftLine, record.leftModified ? alertOutlinePaint : defaultOutlinePaint);
            canvas.drawRect(rightLine, record.rightModified ? alertOutlinePaint : defaultOutlinePaint);

            canvas.drawRect(topLine, record.topModified ? alertOutlinePaint : defaultOutlinePaint);
            canvas.drawRect(bottomLine, record.bottomModified ? alertOutlinePaint : defaultOutlinePaint);
        }

        // Restore the old backing rectangle.
        backingRect.set(savedRect);
    }

    public Rect getPaddedBoundaries() {
        return getPaddedAdjustedBoundaries(1);
    }

    private Rect getPaddedAdjustedBoundaries(float resizeRatio) {
        Rect paddedRect = new Rect(backingRect);
        paddedRect.inset((int) (-padding * resizeRatio), (int) (-padding * resizeRatio));
        return paddedRect;
    }

    public Rect getOutlineBoundaries() {
        return getOutlineAdjustedBoundaries(1);
    }

    private Rect getOutlineAdjustedBoundaries(float resizeRatio) {
        Rect outlinedRect = getPaddedAdjustedBoundaries(resizeRatio);
        int lineWidth = (int) (defaultOutlinePaint.getStrokeWidth() * resizeRatio);
        outlinedRect.inset(-lineWidth, -lineWidth);
        return outlinedRect;
    }

    // Gets the text layout that will be drawn to the canvas and is used for sizing purposes
    public StaticLayout getTextLayout() {
        return new StaticLayout(text, textPaint, Math.abs(backingRect.width()), Layout.Alignment.ALIGN_NORMAL, 1, 1, false);
    }

    public float measureText(String text) {
        return textPaint.measureText(text);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(backingRect, 0);
        out.writeString(text);
        out.writeInt(fontSize);
        out.writeInt(selected ? 1 : 0);
    }

    public static final Parcelable.Creator<TextRectangleModel> CREATOR = new Parcelable.Creator<TextRectangleModel>() {
        public TextRectangleModel createFromParcel(Parcel in) {
            Rect backingRect = in.readParcelable(Rect.class.getClassLoader());
            String text = in.readString();
            int fontSize = in.readInt();
            boolean selected = (in.readInt() == 1);
            TextRectangleModel model = new TextRectangleModel(backingRect, text);
            model.setSelection(selected);
            model.setFontSize(fontSize);
            return model;
        }

        public TextRectangleModel[] newArray(int size) {
            return new TextRectangleModel[size];
        }
    };
}
