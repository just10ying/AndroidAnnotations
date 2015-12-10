package com.example.jying.androidannotations.drawing;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Parcel;
import android.os.Parcelable;

import com.example.jying.androidannotations.support.Annotation;
import com.example.jying.androidannotations.support.RelativePoint;

import java.util.ArrayList;

/**
 * Created by jying on 6/24/2015.
 */
public class DrawablePath extends Path implements Annotation, Parcelable {

    // Multipliers that control how thick the path should be drawn on canvas
    private static final int DRAW_SIZE_MODIFIER = 1;
    private static final int ERASE_SIZE_MODIFIER = 4;

    private int savedDimX, savedDimY;
    private ArrayList<RelativePoint> pathPoints;
    private Paint drawPaint;
    private float strokeSize;
    private boolean transparent;

    public DrawablePath(int color, float strokeSize, boolean transparent) {
        super();

        this.strokeSize = strokeSize;

        // Create paint associated with this object:
        drawPaint = new Paint();
        drawPaint.setAntiAlias(true);
        drawPaint.setDither(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setColor(color);

        if (transparent) {
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        this.savedDimX = 0;
        this.savedDimY = 0;

        this.pathPoints = new ArrayList<RelativePoint>();
        this.transparent = transparent;
    }

    public void drawOnCanvas(Canvas canvas, float resizeRatio) {
        if ((savedDimX != canvas.getWidth()) || (savedDimY != canvas.getHeight())) {
            savedDimX = canvas.getWidth();
            savedDimY = canvas.getHeight();

            // Clear the existing path.
            this.rewind();

            // Recreate the path, because our dimensions have changed.
            for (int index = 0; index < pathPoints.size(); index++) {
                RelativePoint pointA = pathPoints.get(index);
                if (index == 0) {
                    super.moveTo(pointA.getX(savedDimX), pointA.getY(savedDimY));
                }
                else if (index == (pathPoints.size() - 1)) {
                    super.lineTo(pointA.getX(savedDimX), pointA.getY(savedDimY));
                }
                else {
                    RelativePoint pointB = pathPoints.get(index + 1);
                    super.quadTo(pointA.getX(savedDimX), pointA.getY(savedDimY), pointB.getX(savedDimX), pointB.getY(savedDimY));
                    index++;
                }
            }

            // Make sure the path is long enough to get drawn the given resolution:
            if (new PathMeasure(this, false).getLength() < 1) {
                // If it's too small, draw a line that is visible.
                RelativePoint lastPoint = pathPoints.get(pathPoints.size() - 1);
                super.lineTo(lastPoint.getX(savedDimX) + 1, lastPoint.getY(savedDimY) + 1);
            }
        }

        // Fix stroke width for new resolution
        float strokeWidthPx = transparent ? (strokeSize * ERASE_SIZE_MODIFIER) : (strokeSize * DRAW_SIZE_MODIFIER);
        drawPaint.setStrokeWidth(strokeWidthPx * resizeRatio);
        canvas.drawPath(this, drawPaint);
    }

    public void moveTo(RelativePoint point) {
        pathPoints.add(point);
        super.moveTo(point.getX(savedDimX), point.getY(savedDimY));
    }

    public void quadTo(RelativePoint a, RelativePoint b) {
        pathPoints.add(a);
        pathPoints.add(b);
        super.quadTo(a.getX(savedDimX), a.getY(savedDimY), b.getX(savedDimX), b.getY(savedDimY));
    }

    public void lineTo(RelativePoint point) {
        pathPoints.add(point);
        super.lineTo(point.getX(savedDimX), point.getY(savedDimY));
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        // Write out paint data
        out.writeInt(drawPaint.getColor());
        out.writeFloat(strokeSize);
        out.writeInt(transparent ? 1 : 0);

        // Write out path data
        out.writeList(pathPoints);
    }

    public static final Parcelable.Creator<DrawablePath> CREATOR = new Parcelable.Creator<DrawablePath>() {
        public DrawablePath createFromParcel(Parcel in) {
            // Read in paint data
            int color = in.readInt();
            float strokeWidth = in.readFloat();
            boolean transparent = (in.readInt() == 1);

            // Read in path data
            ArrayList<RelativePoint> pointsList = in.readArrayList(RelativePoint.class.getClassLoader());

            DrawablePath recreatedPath = new DrawablePath(color, strokeWidth, transparent);
            for (int index = 0; index < pointsList.size(); index++) {
                if (index == 0) {
                    recreatedPath.moveTo(pointsList.get(index));
                }
                else if (index == (pointsList.size() - 1)) {
                    recreatedPath.lineTo(pointsList.get(index));
                }
                else {
                    recreatedPath.quadTo(pointsList.get(index), pointsList.get(index + 1));
                    index++;
                }
            }
            return recreatedPath;
        }

        public DrawablePath[] newArray(int size) {
            return new DrawablePath[size];
        }
    };
}
