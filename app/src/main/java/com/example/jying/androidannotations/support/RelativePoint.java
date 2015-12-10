package com.example.jying.androidannotations.support;

import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jying on 7/9/2015.
 */
public class RelativePoint implements Parcelable {

    private double relativeX, relativeY;

    public RelativePoint(Point point, int dimensionX, int dimensionY) {
        this.relativeX = (double) point.x / dimensionX;
        this.relativeY = (double) point.y / dimensionY;
    }

    public RelativePoint(double relativeX, double relativeY) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    public Point toPoint(int dimensionX, int dimensionY) {
        double coordX = relativeX * dimensionX;
        double coordY = relativeY * dimensionY;
        return new Point((int) coordX, (int) coordY);
    }

    public int getX(int dimensionX) {
        return (int) (relativeX * dimensionX);
    }

    public int getY(int dimensionY) {
        return (int) (relativeY * dimensionY);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(relativeX);
        out.writeDouble(relativeY);
    }

    public static final Parcelable.Creator<RelativePoint> CREATOR = new Parcelable.Creator<RelativePoint>() {
        public RelativePoint createFromParcel(Parcel in) {
            double x = in.readDouble();
            double y = in.readDouble();
            return new RelativePoint(x, y);
        }

        public RelativePoint[] newArray(int size) {
            return new RelativePoint[size];
        }
    };
}
