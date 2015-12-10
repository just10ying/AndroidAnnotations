package com.example.jying.androidannotations.distance;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.example.jying.androidannotations.AnnotationView;
import com.example.jying.androidannotations.support.Subview;

import java.util.ArrayList;

/**
 * Created by jying on 7/21/2015.
 */
public class MeasuringLineModel implements Parcelable {
    private static final String NOT_CALIBRATED_TEXT = "-"; // This is the string shown if the user draws measurements before calibration or if the user sets a 0 value for calibration.
    private static final int LINE_WIDTH_DP = 5;
    private static final int FILL_WIDTH_DP = 3;
    private static final float SELECT_RADIUS_ADJUST = .5f; // The calculated line radius is multiplied by this amount.  For some reason, the drawn radius seems to be twice the size of the actual touch area, so this corrects for that.

    private static final int DEFAULT_LINE_COLOR = Color.WHITE;
    private static final int SELECTED_LINE_COLOR = Color.parseColor("#33CC33");
    private static final int DEFAULT_TEXT_COLOR = Color.BLACK;
    private static final int SELECTED_TEXT_COLOR = Color.BLACK;
    private static final int DEFAULT_TEXT_SIZE = 20;
    private static final int HANDLE_LINE_WIDTH_DP = 0;
    private static int lineWidthPx = AnnotationView.convertDpToPx(LINE_WIDTH_DP);
    private static int fillWidthPx = AnnotationView.convertPxToDp(FILL_WIDTH_DP);

    private boolean selected;
    private Point start, end;
    private Paint linePaint, fillPaint, handlePaint;
    private TextPaint textPaint;
    private Rect textBoundary; // This rect is the boundary on the last drawn canvas that the text portion of the annotation resides in.

    public MeasuringLineModel(Point start) {
        this.start = start;
        setEnd(start);
        setSelected(false);

        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setDither(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineWidthPx);
        linePaint.setColor(DEFAULT_LINE_COLOR);

        fillPaint = new Paint(linePaint);
        fillPaint.setStrokeWidth(fillWidthPx);
        fillPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        handlePaint = new Paint(linePaint);
        handlePaint.setColor(SELECTED_LINE_COLOR);
        handlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        handlePaint.setAlpha(50);

        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setColor(DEFAULT_TEXT_COLOR);
        textPaint.setTextSize(DEFAULT_TEXT_SIZE);
    }

    public void drawOnCanvas(Canvas canvas, Subview subview, ContourRectangle contourRectangle) {
        // Setup paints:
        linePaint.setStrokeWidth(lineWidthPx * subview.getResizeRatio(canvas));
        linePaint.setColor(selected ? SELECTED_LINE_COLOR : DEFAULT_LINE_COLOR);
        fillPaint.setColor(selected ? SELECTED_LINE_COLOR : DEFAULT_LINE_COLOR);

        // Calculate the real coordinates of the line on the canvas of the given size:
        Point adjustedStart = subview.getAdjustedPoint(canvas, start);
        Point adjustedEnd = subview.getAdjustedPoint(canvas, end);

        if (selected) {
            // Draw handles such that they are a constant size regardless of zoom:
            handlePaint.setStrokeWidth(AnnotationView.convertDpToPx(HANDLE_LINE_WIDTH_DP) / subview.getMagnification());
            float handleRadius = AnnotationView.convertDpToPx(MeasuringLine.ENDPOINT_GRAB_TOLERANCE_DP) * SELECT_RADIUS_ADJUST / subview.getMagnification();
            canvas.drawCircle(adjustedStart.x, adjustedStart.y, handleRadius, handlePaint);
            canvas.drawCircle(adjustedEnd.x, adjustedEnd.y, handleRadius, handlePaint);
        }

        canvas.drawLine(adjustedStart.x, adjustedStart.y, adjustedEnd.x, adjustedEnd.y, linePaint);

        StaticLayout textLayout = getTextLayoutWithDistance(contourRectangle);
        Bitmap textBitmap = Bitmap.createBitmap(textLayout.getWidth(), textLayout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas textCanvas = new Canvas(textBitmap);
        textLayout.draw(textCanvas);

        int textLayoutAdjustedWidth = (int) (textLayout.getWidth() * subview.getResizeRatio(canvas));
        int textLayoutAdjustedHeight = (int) (textLayout.getHeight() * subview.getResizeRatio(canvas));

        textBoundary = getMidpointRect(adjustedStart, adjustedEnd, textLayoutAdjustedWidth, textLayoutAdjustedHeight);
        canvas.drawRect(textBoundary, fillPaint);

        Rect src = new Rect(0, 0, textBitmap.getWidth(), textBitmap.getHeight());
        canvas.drawBitmap(textBitmap, src, textBoundary, null);
    }

    public ArrayList<Point> getPoints() {
        ArrayList<Point> points = new ArrayList<Point>();
        points.add(start);
        points.add(end);
        return points;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    public void setEnd(Point end) {
        this.end = new Point(end);
    }

    public Rect getTextBounds(ContourRectangle contourRectangle) {
        StaticLayout textLayout = getTextLayoutWithDistance(contourRectangle);
        return getMidpointRect(start, end, textLayout.getWidth(), textLayout.getHeight());
    }

    private Rect getMidpointRect(Point a, Point b, int width, int height) {
        Rect midpointRect = new Rect();
        midpointRect.left = (a.x + b.x - width) / 2;
        midpointRect.top = (a.y + b.y - height) / 2;
        midpointRect.right = midpointRect.left + width;
        midpointRect.bottom = midpointRect.top + height;
        return midpointRect;
    }

    private StaticLayout getTextLayoutWithDistance(ContourRectangle contourRectangle) {
        double cmDistance = contourRectangle.getCmDistanceBetween(start, end); // Calculate meter distance:
        String annotationText = (cmDistance == 0) ? NOT_CALIBRATED_TEXT : (String.format("%1$,.2f", cmDistance) + "cm");
        return getStaticLayout(annotationText);
    }

    private StaticLayout getStaticLayout(String text) {
        textPaint.setColor(selected ? SELECTED_TEXT_COLOR : DEFAULT_TEXT_COLOR);
        return new StaticLayout(text, textPaint, (int) textPaint.measureText(text), Layout.Alignment.ALIGN_NORMAL, 1, 1, false);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(start, flags);
        out.writeParcelable(end, flags);
        out.writeInt(selected ? 1 : 0);
    }

    public static final Parcelable.Creator<MeasuringLineModel> CREATOR = new Parcelable.Creator<MeasuringLineModel>() {
        public MeasuringLineModel createFromParcel(Parcel in) {
            Point start = in.readParcelable(Point.class.getClassLoader());
            Point end = in.readParcelable(Point.class.getClassLoader());
            boolean selected = (in.readInt() == 1);
            MeasuringLineModel model = new MeasuringLineModel(start);
            model.setEnd(end);
            model.setSelected(selected);
            return model;
        }

        public MeasuringLineModel[] newArray(int size) {
            return new MeasuringLineModel[size];
        }
    };
}
