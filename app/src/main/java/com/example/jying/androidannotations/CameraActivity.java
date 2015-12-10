package com.example.jying.androidannotations;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;


public class CameraActivity extends ActionBarActivity {

    private final float MAX_WIDTH_RATIO = 2.0f; // The bitmap's width should be at most screenWidth * MAX_WIDTH_RATIO, but it can be less.  This is here for memory and performance reasons.
    private final float MAX_WIDTH_PX = 2048;

    private CameraPreview mPreview;
    private FrameLayout cameraFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreview = new CameraPreview(this);
        cameraFrame = (FrameLayout) findViewById(R.id.camera_preview);
        cameraFrame.addView(mPreview);

        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                decorView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
    }

    public void focusCamera(View view) {
        mPreview.getCamera().autoFocus(null);
    }

    public void takePicture(View view) {
        mPreview.getCamera().autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                mPreview.getCamera().takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        // Create background image:
                        Bitmap bgBitmap = BitmapFactory.decodeByteArray(data, 0, data.length).copy(Bitmap.Config.ARGB_8888, true);

                        // Downscale background image if necessary
                        Display display = getWindowManager().getDefaultDisplay();
                        int screenWidth = display.getWidth();

                        int maxAllowableWidth = (int) Math.min(screenWidth * MAX_WIDTH_RATIO, MAX_WIDTH_PX);
                        if (bgBitmap.getWidth() > maxAllowableWidth) {
                            int maxAllowableHeight = (int) ((float) maxAllowableWidth / ((float) bgBitmap.getWidth() / (float) bgBitmap.getHeight()));
                            bgBitmap = Bitmap.createScaledBitmap(bgBitmap, maxAllowableWidth, maxAllowableHeight, false); // Downscale bitmap
                        }

                        JotActivity.bgBitmap = bgBitmap;
                        Intent intent = new Intent(getApplicationContext(), JotActivity.class);
                        camera.release();
                        startActivity(intent);
                    }
                });
            }
        });
    }
}
