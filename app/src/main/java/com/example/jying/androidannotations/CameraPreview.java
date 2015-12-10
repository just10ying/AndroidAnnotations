package com.example.jying.androidannotations;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;
import java.util.List;

/**
 * Created by jying on 5/21/2015.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("Camera", "Error setting camera preview: " + e.getMessage());
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.release();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null){
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            Log.e("CameraPreview", "Camera preview could not be stopped: " + e.getMessage());
        }

        // Set camera orientation.
        int displayOrientation = getCameraRotation(this);
        mCamera.setDisplayOrientation(displayOrientation);

        // Change preview and picture size / aspect ratio to most closely match the surface:
        Camera.Parameters cameraParameters = mCamera.getParameters();
        cameraParameters.set("orientation", "portrait");
        cameraParameters.setRotation(displayOrientation);
        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);

        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        List<Camera.Size> pictureSizes = cameraParameters.getSupportedPictureSizes();

        Camera.Size bestPreviewSize = getOptimalSize(previewSizes, w, h);
        Camera.Size bestPictureSize = getOptimalSize(pictureSizes, w, h);

        cameraParameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        cameraParameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);
        mCamera.setParameters(cameraParameters);

        // Start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d("Camera", "Error starting camera preview: " + e.getMessage());
        }
    }

    // Given the current view, gets the angle in degrees that the camera should be rotated so that the user sees the picture right-side-up
    private int getCameraRotation(View view) {
        int degrees = 0;
        switch (((Activity) view.getContext()).getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        Camera.CameraInfo camInfo = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);
        return (camInfo.orientation - degrees + 360) % 360;
    }

    // From developer.android.com.
    // Gets the optimal size for this view, given the Camera's available preview dimensions
    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
