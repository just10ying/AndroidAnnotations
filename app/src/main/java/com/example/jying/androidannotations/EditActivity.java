package com.example.jying.androidannotations;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.jying.androidannotations.distance.MeasuringLayerDelegate;
import com.example.jying.androidannotations.distance.MeasuringOverlay;
import com.example.jying.androidannotations.support.AnnotationLayer;
import com.example.jying.androidannotations.support.BackingCanvas;
import com.example.jying.androidannotations.support.LayerDelegate;
import com.example.jying.androidannotations.support.Subview;
import com.example.jying.androidannotations.distance.MeasuringLayer;
import com.example.jying.androidannotations.drawing.DrawingLayer;
import com.example.jying.androidannotations.distance.EdgeDetector;
import com.example.jying.androidannotations.textbox.TextboxLayer;
import com.example.jying.androidannotations.textbox.TextboxLayerDelegate;
import com.example.jying.androidannotations.textbox.TextboxOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public abstract class EditActivity extends KeyboardListenerActivity {
    // These keys are the strings used to save state data to the bundle
    private static final String SUBVIEW_KEY = "SubviewKey";

    private Menu menu; // Remember the menu; we need to add layers to it when the user creates the layers.
    private Bitmap bgBitmap;
    
    protected AnnotationView annotationView;
    protected HashMap<String, AnnotationLayer> annotationLayers;
    protected ArrayList<BackingCanvas> backingCanvases;

    // This bundle is the secureBundle that was passed into onCreateWithSession.  Use this secure bundle to restore the activity in onRestoreInstanceState
    private Bundle secureBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        super.setRootView(findViewById(R.id.edit_root));

        // Disable the back button on the action bar.
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);

        annotationLayers = new HashMap<String, AnnotationLayer>();
        backingCanvases = new ArrayList<BackingCanvas>();

        secureBundle = savedInstanceState; // Save off the secure bundle so it can be used in onRestoreInstanceState
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
            entry.getValue().addMenuItem(menu);
        }
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.discard_drawing) {
            showClearDrawingDialog();
        }
        else if (id == R.id.submit_drawing) {
            showSubmitDrawingDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        saveAnnotationDataToBundle(savedInstanceState);
        savedInstanceState.putParcelable(SUBVIEW_KEY, annotationView.getSubview().getSubviewRectangle()); // Save subview:
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        annotationView.getSubview().setSubviewRectangle((Rect) secureBundle.getParcelable(SUBVIEW_KEY));
        restoreAnnotationDataFromBundle(secureBundle, true);
        // Now that data has been restored, redraw the data.
        for (BackingCanvas canvas : backingCanvases) {
            canvas.redrawAll();
        }
    }

    protected void saveAnnotationDataToBundle(Bundle savedInstanceState) {
        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
            entry.getValue().saveToBundle(savedInstanceState);
        }
    }

    protected void restoreAnnotationDataFromBundle(Bundle savedInstanceState, boolean fullRestore) {
        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
            entry.getValue().restoreFromBundle(savedInstanceState, fullRestore);
        }
    }

    protected Bitmap getAnnotationBackground() {
        return bgBitmap;
    }

    protected void setAnnotationBackground(Bitmap image) {
        this.bgBitmap = image;

        ViewGroup root = (ViewGroup) findViewById(R.id.edit_root);
        root.removeAllViews();
        annotationView = new AnnotationView(this, image, annotationLayers, backingCanvases);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0);
        params.weight = 1;
        annotationView.setLayoutParams(params);

        root.addView(annotationView);

        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
            entry.getValue().getOverlay().setSubview(annotationView.getSubview());
        }
    }

    protected AnnotationLayer createLayer(Class<? extends AnnotationLayer> layerType, BackingCanvas backingCanvas, String key) {
        ViewGroup root = (ViewGroup) findViewById(R.id.edit_root);

        AnnotationLayer layer = null;
        if (layerType == DrawingLayer.class) {
            layer = new DrawingLayer(this, root, key, new LayerDelegate() {
                @Override
                public void onOverlayBackingChanged() {
                    annotationView.invalidate();
                }

                @Override
                public void onLayerRequestSoleActivation() {
                    deactivateAllLayers();
                }
            });
        }
        else if (layerType == TextboxLayer.class) {
            layer = new TextboxLayer(this, root, key, new TextboxLayerDelegate() {
                @Override
                public void onTextRectangleStartEditing(EditText editText, String content) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    editText.setText(content);
                    editText.requestFocus();
                    editText.setSelection(editText.getText().toString().length());
                    inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                }

                @Override
                public void onTextRectangleStopEditing(EditText editText) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }

                @Override
                public void onTextRectangleFullEditing(final TextboxOverlay overlay) {
                    LayoutInflater inflater = LayoutInflater.from(EditActivity.this);
                    View textEditPrompt = inflater.inflate(R.layout.annotation_text_edit, null);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
                    alertDialogBuilder.setView(textEditPrompt);
                    final EditText userInput = (EditText) textEditPrompt.findViewById(R.id.annotation_dialog_edit_text);
                    userInput.setText(overlay.getText());
                    userInput.setSelection(userInput.getText().toString().length());

                    alertDialogBuilder.setCancelable(false).setPositiveButton(getResources().getString(R.string.modal_accept), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            overlay.setText(userInput.getText().toString());
                            annotationView.invalidate();
                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                        }
                    }).setNegativeButton(getResources().getString(R.string.modal_decline), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                            dialog.cancel();
                        }
                    });

                    AlertDialog editTextDialog = alertDialogBuilder.create();
                    editTextDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    editTextDialog.show();
                }

                @Override
                public void onOverlayBackingChanged() {
                    annotationView.invalidate();
                }

                @Override
                public void onLayerRequestSoleActivation() {
                    deactivateAllLayers();
                }
            });
        }
        else if (layerType == MeasuringLayer.class) {
            layer = new MeasuringLayer(this, root, key, new MeasuringLayerDelegate() {
                @Override
                public void onOverlayBackingChanged() {
                    annotationView.invalidate();
                }

                @Override
                public void onLayerRequestSoleActivation() {
                    deactivateAllLayers();
                }

                @Override
                public void onCalculateContours(final MeasuringOverlay overlay, final float scale) {
                    final ProgressDialog progress = new ProgressDialog(EditActivity.this);
                    progress.setTitle(getResources().getString(R.string.image_recognition_progress_title));
                    progress.setMessage(getResources().getString(R.string.image_recognition_progress_content));
                    progress.setCancelable(false);
                    progress.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run()
                        {
                            final EdgeDetector detector = new EdgeDetector(bgBitmap, scale);
                            overlay.setContourRectangles(detector.getContourRectangles());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progress.dismiss();
                                    if (detector.getContourRectangles().size() == 0) {
                                        onNoContoursFound();
                                    }
                                }
                            });
                        }
                    }).start();
                }

                @Override
                public void onNoContoursFound() {
                    new AlertDialog.Builder(EditActivity.this)
                            .setTitle(getResources().getString(R.string.no_contours_found_title))
                            .setMessage(getResources().getString(R.string.no_contours_found_message))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(EditActivity.this, CameraActivity.class);
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {}
                            })
                            .setIcon(R.drawable.ic_camera_alt_white_18dp)
                            .show();
                }

                @Override
                public void onContourSelectedAsReference(final MeasuringOverlay overlay) {
                    LayoutInflater inflater = LayoutInflater.from(EditActivity.this);
                    View distancePrompt = inflater.inflate(R.layout.measuring_distance_modal, null);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EditActivity.this);
                    alertDialogBuilder.setView(distancePrompt);
                    final EditText userInput = (EditText) distancePrompt.findViewById(R.id.distance_edit_text);

                    alertDialogBuilder.setCancelable(false).setPositiveButton(getResources().getString(R.string.modal_accept), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                overlay.setLongEdgeDimension(Double.parseDouble(userInput.getText().toString()));
                            }
                            catch (NumberFormatException ex) {
                                overlay.setLongEdgeDimension(0);
                            }
                            // Send distance data back to overlay:
                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                        }
                    }).setNegativeButton(getResources().getString(R.string.modal_decline), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.hideSoftInputFromWindow(userInput.getWindowToken(), 0);
                            dialog.cancel();
                        }
                    });

                    AlertDialog editTextDialog = alertDialogBuilder.create();
                    editTextDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    editTextDialog.show();
                }
            });
        }

        if (menu != null) {
            layer.addMenuItem(menu);
        }

        // If the backing canvas hasn't been fully initialized yet and we have the image dimensions, finish the initialization by setting a blank bitmap of the correct size.
        if ((backingCanvas.getCanvas() == null) && (annotationView != null)){
            Subview subview = annotationView.getSubview();
            backingCanvas.setBlankBitmap(subview.getBgWidth(), subview.getBgHeight());
        }

        layer.getOverlay().setSubview(annotationView.getSubview());
        layer.getOverlay().setBackingCanvas(backingCanvas);
        backingCanvases.add(backingCanvas);

        annotationLayers.put(key, layer);
        return layer;
    }

    protected abstract void submitAnnotations();

    private void deactivateAllLayers() {
        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
            entry.getValue().setActive(false);
        }
    }

    private void showClearDrawingDialog() {
        //Confirm user input
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.discard_annotations))
                .setMessage(getResources().getString(R.string.discard_annotations_message))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        for (Map.Entry<String, AnnotationLayer> entry : annotationLayers.entrySet()) {
                            entry.getValue().getOverlay().clearAnnotations();
                        }
                        annotationView.invalidate();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showSubmitDrawingDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.submit_annotations))
                .setMessage(getResources().getString(R.string.submit_annotations_message))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        submitAnnotations();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(R.drawable.ic_file_upload_white_18dp)
                .show();
    }
}
