package com.example.jying.androidannotations.drawing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.jying.androidannotations.support.AnnotationLayer;
import com.example.jying.androidannotations.support.LayerDelegate;
import com.example.jying.androidannotations.R;

/**
 * Created by jying on 6/26/2015.
 */
public class DrawingLayer extends AnnotationLayer {

    // Bundle keys:
    private static final String COLOR_KEY = "color";
    private static final String SIZE_KEY = "size";
    private static final String ERASE_KEY = "erase";

    private static final int TOOLBAR_RESOURCE_ID = R.layout.drawing_layer_toolbar;
    private static final int ICON_RESOURCE_ID = R.drawable.ic_edit_white_24dp;
    private static final int DEFAULT_COLOR_INDEX = 0; // Black is default.
    private static final int DEFAULT_DRAW_SIZE_INDEX = 4;
    private static final Integer[] DRAW_SIZES = new Integer[]{1, 2, 3, 4, 5, 6, 7};

    public DrawingLayer(Context context, ViewGroup parent, String name, LayerDelegate delegate) {
        super(context, parent, TOOLBAR_RESOURCE_ID, ICON_RESOURCE_ID, name, new DrawingOverlay(), delegate);
    }

    @Override
    protected void setToolbarHandlers(Context context) {
        final DrawingOverlay drawingOverlay = (DrawingOverlay) overlay; // Cast the overlay to the correct type so we don't have to in all the on-click listeners
        final Resources resources = context.getResources(); // Get a copy of resources so we don't accidentally hang onto context

        toolbar.findViewById(R.id.undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingOverlay.undo();
                delegate.onOverlayBackingChanged();
            }
        });

        toolbar.findViewById(R.id.redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingOverlay.redo();
                delegate.onOverlayBackingChanged();
            }
        });

        toolbar.findViewById(R.id.eraser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawingOverlay.enableEraser();
                toolbar.findViewById(R.id.eraser).setBackgroundColor(resources.getColor(R.color.ButtonSelected));
                toolbar.findViewById(R.id.color_spinner).setBackgroundColor(resources.getColor(R.color.ButtonUnselected));

            }
        });

        // Initialize spinner for picking colors.
        final Spinner colorSpinner = (Spinner) toolbar.findViewById(R.id.color_spinner);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.color_strings, R.layout.color_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapter);
        colorSpinner.setBackgroundColor(context.getResources().getColor(R.color.ButtonSelected)); // Color spinnner is selected by default
        colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedColor = resources.getStringArray(R.array.color_strings)[position];
                int colorInt; // This is the color that the user selected.  It is populated below.
                if (selectedColor.equals(resources.getString(R.string.white))) {
                    colorInt = Color.WHITE;
                } else if (selectedColor.equals(resources.getString(R.string.blue))) {
                    colorInt = Color.BLUE;
                } else if (selectedColor.equals(resources.getString(R.string.red))) {
                    colorInt = Color.RED;
                } else if (selectedColor.equals(resources.getString(R.string.green))) {
                    colorInt = Color.GREEN;
                } else {
                    colorInt = Color.BLACK;
                }

                // On activity restart, selectItemView may be null.
                if (selectedItemView != null) {
                    selectedItemView.getBackground().setColorFilter(colorInt, PorterDuff.Mode.MULTIPLY);
                }
                adapter.notifyDataSetChanged();
                drawingOverlay.setColor(colorInt);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        colorSpinner.setSelection(DEFAULT_COLOR_INDEX);

        // Enable draw mode as soon as the spinner is clicked.
        colorSpinner.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                drawingOverlay.disableEraser();
                toolbar.findViewById(R.id.eraser).setBackgroundColor(resources.getColor(R.color.ButtonUnselected));
                toolbar.findViewById(R.id.color_spinner).setBackgroundColor(resources.getColor(R.color.ButtonSelected));
                return false;
            }
        });

        // Initialize spinner for picking draw width
        final Spinner sizeSpinner = (Spinner) toolbar.findViewById(R.id.size_spinner);
        ArrayAdapter<Integer> sizeAdapter = new ArrayAdapter<Integer>(context, android.R.layout.simple_spinner_item, DRAW_SIZES){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position,convertView,parent);
                view.setText("");
                view.setBackground(resources.getDrawable(R.drawable.ic_create_white_24dp));
                return view;
            }
        };
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);
        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                int selectedSize = Integer.parseInt(sizeSpinner.getSelectedItem().toString());
                drawingOverlay.setDrawSize(selectedSize);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        sizeSpinner.setSelection(sizeAdapter.getPosition(DEFAULT_DRAW_SIZE_INDEX)); // Sets the default width
    }

    @Override
    public void saveToBundle(Bundle bundle) {
        super.saveToBundle(bundle);
        int colorIndex = ((Spinner) toolbar.findViewById(R.id.color_spinner)).getSelectedItemPosition();
        int sizeIndex = ((Spinner) toolbar.findViewById(R.id.size_spinner)).getSelectedItemPosition();
        boolean erase = ((DrawingOverlay) overlay).isErasing();
        bundle.putInt(COLOR_KEY, colorIndex);
        bundle.putInt(SIZE_KEY, sizeIndex);
        bundle.putBoolean(ERASE_KEY, erase);
    }

    @Override
    public void restoreFromBundle(Bundle bundle, boolean restoreActive) {
        super.restoreFromBundle(bundle, restoreActive);
        ((Spinner) toolbar.findViewById(R.id.color_spinner)).setSelection(bundle.getInt(COLOR_KEY));
        ((Spinner) toolbar.findViewById(R.id.size_spinner)).setSelection(bundle.getInt(SIZE_KEY));
        if (bundle.getBoolean(ERASE_KEY)) {
            toolbar.findViewById(R.id.eraser).performClick();
        }
    }
}
