package com.example.jying.androidannotations.distance;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.example.jying.androidannotations.support.AnnotationLayer;
import com.example.jying.androidannotations.R;

/**
 * Created by jying on 7/6/2015.
 */
public class MeasuringLayer extends AnnotationLayer {

    private static final int TOOLBAR_RESOURCE_ID = R.layout.measuring_layer_toolbar;
    private static final int ICON_RESOURCE_ID = R.drawable.ic_space_bar_white_24dp;

    public MeasuringLayer(Context context, ViewGroup parent, String name, final MeasuringLayerDelegate delegate) {
        super(context, parent, TOOLBAR_RESOURCE_ID, ICON_RESOURCE_ID, name, new MeasuringOverlay(), delegate);

        ImageButton button = (ImageButton) toolbar.findViewById(R.id.accept_reference_object);
        button.setEnabled(false); // Disable this button by default.  This is done programmatically because there is no equivalent to android:enable="false" for ImageButtons (or at the very least it doesn't work).

        ((MeasuringOverlay) overlay).setDelegate(new MeasuringOverlayDelegate() {
            @Override
            public void onContourSelectionChanged(boolean selected) {
                ImageButton button = (ImageButton) toolbar.findViewById(R.id.accept_reference_object);
                button.setEnabled(selected);
            }

            @Override
            public void onMeasuringModeChanged(int mode) {
                switch (mode) {
                    case MeasuringOverlay.MODE_SELECT_REFERENCE:
                        toolbar.findViewById(R.id.calibration_layout).setVisibility(View.VISIBLE);
                        toolbar.findViewById(R.id.measuring_layout).setVisibility(View.GONE);
                        break;
                    case MeasuringOverlay.MODE_MEASURE:
                        toolbar.findViewById(R.id.calibration_layout).setVisibility(View.GONE);
                        toolbar.findViewById(R.id.measuring_layout).setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public void onRequestContourDetection() {
                delegate.onCalculateContours((MeasuringOverlay) overlay, MeasuringOverlay.IMAGE_PROCESSING_SCALE);
            }

            @Override
            public void onMeasuringLineSelected(boolean selected) {
                toolbar.findViewById(R.id.delete_measuring_line).setEnabled(selected);
            }
        });
    }

    @Override
    protected void setToolbarHandlers(Context context) {
        toolbar.findViewById(R.id.accept_reference_object).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MeasuringLayerDelegate) delegate).onContourSelectedAsReference((MeasuringOverlay) overlay);
            }
        });

        toolbar.findViewById(R.id.recalibrate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MeasuringOverlay) overlay).setMode(MeasuringOverlay.MODE_SELECT_REFERENCE);
            }
        });

        toolbar.findViewById(R.id.delete_measuring_line).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MeasuringOverlay) overlay).onDeleteButtonPressed();
                delegate.onOverlayBackingChanged();
            }
        });
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        MeasuringOverlay measuringOverlay = (MeasuringOverlay) overlay;
        if ((active) && (measuringOverlay.getContourRectangles() != null) && (measuringOverlay.getContourRectangles().size() == 0)) {
            ((MeasuringLayerDelegate) delegate).onNoContoursFound(); // Show message about taking a new picture to get contours.
        }
    }
}
