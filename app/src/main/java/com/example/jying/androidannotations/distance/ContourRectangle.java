package com.example.jying.androidannotations.distance;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

import com.example.jying.androidannotations.AnnotationView;
import com.example.jying.androidannotations.support.Annotation;

import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

/**
 * Created by jying on 7/15/2015.
 */
public class ContourRectangle implements Comparable<ContourRectangle>, Annotation, Parcelable {

    private static final double CENTER_TOLERANCE = 1/30000d;
    private static final double EDGE_TOLERANCE = 1/30000d;
    private static final int SELECTED_OUTLINE_COLOR = Color.GREEN;
    private static final int DEFAULT_OUTLINE_COLOR = Color.RED;
    private static final int STROKE_WIDTH_DP = 5;

    private Point[] points;
    private double area;
    private Paint outlinePaint;
    private double longDimCm;
    private boolean selected;

    public ContourRectangle(MatOfPoint rectangle, float scale) {
        org.opencv.core.Point[] openCVPoints = rectangle.toArray();

        this.area = Math.abs(Imgproc.contourArea(rectangle));
        this.points = new Point[openCVPoints.length];
        this.selected = false;

        for (int index = 0; index < openCVPoints.length; index++) {
            points[index] = new Point((int) (openCVPoints[index].x / scale), (int) (openCVPoints[index].y / scale));
        }
        setContourPaint();
    }

    public void drawOnCanvas(Canvas canvas, float externalScale) {
        float startX, startY, stopX, stopY;
        for (int index = 0; index < points.length; index++) {
            startX = (float) points[index].x * externalScale;
            startY = (float) points[index].y * externalScale;
            stopX = (float) points[(index + 1) % points.length].x * externalScale;
            stopY = (float) points[(index + 1) % points.length].y * externalScale;

            outlinePaint.setStrokeWidth(AnnotationView.convertDpToPx(STROKE_WIDTH_DP) * externalScale);
            outlinePaint.setColor(selected ? SELECTED_OUTLINE_COLOR : DEFAULT_OUTLINE_COLOR);
            canvas.drawLine(startX, startY, stopX, stopY, outlinePaint);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setLongEdgeDimension(double longDimCm) {
        this.longDimCm = longDimCm;
    }

    public double getCmDistanceBetween(Point a, Point b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;

        double dxCm = dx * getCmPerPixel();
        double dyCm = dy * getCmPerPixel();

        float totalLength = (float) Math.sqrt(Math.pow(dxCm, 2) + Math.pow(dyCm, 2));

        return totalLength;
    }

    public double getShortEdge() {
        return getDistances()[1];
    }

    public double getLongEdge() {
        return getDistances()[2];
    }

    // Gets distances from smallest to largest.
    private double[] getDistances() {
        double distances[] = new double[4];
        Point startPoint = points[0];
        for (int index = 0; index < points.length; index++) {
            distances[index] = distanceBetween(startPoint, points[index]);
        }
        Arrays.sort(distances);
        return distances;
    }

    public double getCmPerPixel() {
        return longDimCm / getLongEdge();
    }

    public int compareTo(ContourRectangle other) {
        if (this.area > other.area) {
            return 1;
        }
        else if (this.area == other.area) {
            return 0;
        }
        else {
            return -1;
        }
    }

    // From http://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
    public boolean contains(Point test) {
        int i;
        int j;
        boolean result = false;
        for (i = 0, j = points.length - 1; i < points.length; j = i++) {
            if ((points[i].y > test.y) != (points[j].y > test.y) &&
                    (test.x < (points[j].x - points[i].x) * (test.y - points[i].y) / (points[j].y-points[i].y) + points[i].x)) {
                result = !result;
            }
        }
        return result;
    }

    public boolean equals(ContourRectangle other, int imageArea) {
        // Rectangles are equal if their center is approximately the same, and their dimensions are approximately the same.
        // Check centers
        double centerTolerance = imageArea * CENTER_TOLERANCE;
        if (distanceBetween(getCenter(this.points), getCenter(other.points)) > centerTolerance) {
            return false;
        }
        if (Math.abs(this.getShortEdge() - other.getShortEdge()) > imageArea * EDGE_TOLERANCE) {
            return false;
        }
        if (Math.abs(this.getLongEdge() - other.getLongEdge()) > imageArea * EDGE_TOLERANCE) {
            return false;
        }
        return true;
    }

    private double distanceBetween(Point a, Point b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
    }

    private Point getCenter(Point[] points) {
        double centerX = 0;
        double centerY = 0;
        for (Point point : points) {
            centerX += point.x;
            centerY += point.y;
        }
        return new Point((int) (centerX / points.length), (int) (centerY / points.length));
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeTypedArray(points, flags); // Write out path data
        out.writeDouble(area);
        out.writeInt(selected ? 1 : 0);
        out.writeDouble(longDimCm);
    }

    public static final Parcelable.Creator<ContourRectangle> CREATOR = new Parcelable.Creator<ContourRectangle>() {
        public ContourRectangle createFromParcel(Parcel in) {
            Point[] points = in.createTypedArray(Point.CREATOR);
            double area = in.readDouble();
            boolean selected = in.readInt() == 1;
            double longDimCm = in.readDouble();

            ContourRectangle rect = new ContourRectangle(points, area);
            rect.selected = selected;
            rect.longDimCm = longDimCm;
            return rect;
        }

        public ContourRectangle[] newArray(int size) {
            return new ContourRectangle[size];
        }
    };

    private ContourRectangle(Point[] corners, double area) {
        this.points = corners;
        this.area = area;
        setContourPaint();
    }

    private void setContourPaint() {
        outlinePaint = new Paint();
        outlinePaint.setAntiAlias(true);
        outlinePaint.setDither(true);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeJoin(Paint.Join.ROUND);
        outlinePaint.setStrokeCap(Paint.Cap.ROUND);
    }
}
