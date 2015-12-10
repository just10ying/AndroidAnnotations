package com.example.jying.androidannotations.distance;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* Class: EdgeDetector
 * Author: Varun Ganesh
 *
 * Description:
 * Takes the input image and identifies the area of interest i.e. the borders
 * of the contact card and performs a cropping operation of the area
 * Algorithm uses a combination of Canny Detector and thresholding at multiple
 * levels to detect card edges.
 *
 * See original at: https://code.google.com/p/scope-ocr/source/browse/trunk/+scope-ocr+--username+aravindh.shankar.91@gmail.com/Prototype/Scope/src/com/example/scope/EdgeDetection.java?r=22
 */

public class EdgeDetector {

    private static final String TAG = "EdgeDetector.java";

    private float scale;
    private ArrayList<ContourRectangle> contourRectangles = new ArrayList<>();;
    private Mat srcMat = new Mat();
    private Bitmap srcImage = null;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Unable to initialize openCV!");
        }
    }

    public EdgeDetector(Bitmap img, float scale) {
        this.scale = scale;
        if (scale != 1){
            srcImage = Bitmap.createScaledBitmap(img, (int) (img.getWidth() * scale), (int) (img.getHeight() * scale), false);
        }
        else {
            srcImage = img;
        }

        srcMat.release();
        Utils.bitmapToMat(srcImage, srcMat);
        processEdges();
        srcImage = null; // Release image reference
    }

    public ArrayList<ContourRectangle> getContourRectangles() {
        return contourRectangles;
    }

    // This method was adapted from an OpenCV example.  I did not write this method, and it hasn't been brought up to coding standards.
    private void processEdges() {
        Mat blurred = new Mat();
        srcMat.copyTo(blurred);

        Imgproc.medianBlur(srcMat, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), blurred.type());
        Imgproc.cvtColor(gray0, gray0, Imgproc.COLOR_RGB2GRAY);
        Mat gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint> squares = new ArrayList<>();

        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {
            int ch[] = {c, 0};
            MatOfInt fromto = new MatOfInt(ch);
            List<Mat> blurredlist = new ArrayList<Mat>();
            List<Mat> graylist = new ArrayList<Mat>();
            blurredlist.add(0, blurred);
            graylist.add(0, gray0);
            Core.mixChannels(blurredlist, graylist, fromto);
            gray0 = graylist.get(0);
            // Try several threshold levels
            int threshold_level = 2;
            for (int thresholdLevel = 0; thresholdLevel < threshold_level; thresholdLevel++) {
                // Use Canny instead of zero threshold level!  Canny helps to catch squares with gradient shading

                if (thresholdLevel >= 0) {
                    Imgproc.Canny(gray0, gray, 20, 30);
                    Imgproc.dilate(gray, gray, Mat.ones(new Size(3, 3), 0)); // Dilate helps to remove potential holes between edge segments
                }
                else {
                    int thresh = (thresholdLevel + 1) * 255 / threshold_level;
                    Imgproc.threshold(gray0, gray, thresh, 255, Imgproc.THRESH_TOZERO);
                }

                // Find contours and store them in a list
                Imgproc.findContours(gray, contours, new Mat(), 1, 2);

                MatOfPoint2f approx = new MatOfPoint2f();
                MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
                MatOfPoint mMOP = new MatOfPoint();
                for (int i = 0; i < contours.size(); i++) {
                    contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
                    Imgproc.approxPolyDP(mMOP2f1, approx, Imgproc.arcLength(mMOP2f1, true) * 0.02, true);
                    approx.convertTo(mMOP, CvType.CV_32S);

                    if (approx.rows() == 4 && Math.abs(Imgproc.contourArea(approx)) > 1000 && Imgproc.isContourConvex(mMOP)) {
                        double maxCosine = 0;
                        Point[] list = approx.toArray();
                        for (int j = 2; j < 5; j++) {
                            double cosine =Math.abs(angle(list[j%4], list[j-2], list[j-1]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3 ) {
                            MatOfPoint temp = new MatOfPoint();
                            approx.convertTo(temp, CvType.CV_32S);
                            squares.add(temp);
                        }
                    }
                }
            }
        }

        // Save only unique contour rectangles:
        for (int index = 0; index < squares.size(); index++) {
            ContourRectangle newRect = new ContourRectangle(squares.get(index), scale);
            boolean add = true;
            for (ContourRectangle existingRect : contourRectangles) {
                if (newRect.equals(existingRect, srcImage.getWidth() * srcImage.getHeight())) {
                    add = false;
                }
            }
            if (add) {
                contourRectangles.add(newRect);
            }
        }
        Collections.sort(contourRectangles, Collections.reverseOrder());
    }

    private double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

}
