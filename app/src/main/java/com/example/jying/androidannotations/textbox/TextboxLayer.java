package com.example.jying.androidannotations.textbox;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.jying.androidannotations.support.AnnotationLayer;
import com.example.jying.androidannotations.EditActivity;
import com.example.jying.androidannotations.support.KeyboardActionListener;
import com.example.jying.androidannotations.R;

/**
 * Created by jying on 6/26/2015.
 */
public class TextboxLayer extends AnnotationLayer {

    // Note: the KeyboardActionListener delegate fires too late for a smooth hiding of the delegate to occur when the keyboard is opening.
    // As such, it's the Layer's job to hide the toolbar whenever text editing occurs.

    private static final int TOOLBAR_RESOURCE_ID = R.layout.textbox_layer_toolbar;
    private static final int ICON_RESOURCE_ID = R.drawable.ic_text_format_white_24dp;
    private static final float TEXT_RESOLUTION_RATIO = .7f;

    private EditText invisibleEditText;

    public TextboxLayer(Context context, ViewGroup parent, String name, final TextboxLayerDelegate delegate) {
        super(context, parent, TOOLBAR_RESOURCE_ID, ICON_RESOURCE_ID, name, new TextboxOverlay(), delegate);

        enableToolbarButtons(false); // By default, toolbar buttons are disabled:

        // Overwrite the resolution ratios because text should look sharp.
        activeResolutionRatio = TEXT_RESOLUTION_RATIO ;
        backgroundResolutionRatio = TEXT_RESOLUTION_RATIO;

        ((TextboxOverlay) overlay).setDelegate(new TextOverlayDelegate() {
            @Override
            public void onTimeStampSelect(){
                enableToolbarButtons(false);
                toolbar.findViewById(R.id.delete_selected_annotation).setEnabled(true);
            }
            @Override
            public void onTimeStampExistenceChanged(boolean hasTimeStamp){
                toolbar.findViewById(R.id.add_timestamp).setEnabled(!hasTimeStamp);
            }
            @Override
            public void onTextRectangleSelect() {
                enableToolbarButtons(true);
                delegate.onTextRectangleStopEditing(invisibleEditText);
            }

            @Override
            public void onTextRectangleDeselect() {
                enableToolbarButtons(false);
                delegate.onTextRectangleStopEditing(invisibleEditText);
            }

            @Override
            public void onTextRectangleModify() {
                showToolbar(true);
                delegate.onTextRectangleStopEditing(invisibleEditText);
            }

            @Override
            public void onTextRectangleTextEdit() {
                delegate.onTextRectangleStartEditing(invisibleEditText, ((TextboxOverlay) overlay).getText());
                showToolbar(false);
            }

            @Override
            public void onZoom() {
                delegate.onTextRectangleStopEditing(invisibleEditText);
            }

            @Override
            public void onPan() {
                delegate.onTextRectangleStopEditing(invisibleEditText);
            }
        });

        ((EditActivity) context).subscribeKeyboardActionListener(new KeyboardActionListener() {
            @Override
            public void onKeyboardStartOpen() {
                if (active) {
                    ((TextboxOverlay) overlay).focusSubviewOnSelectedTextRectangle();
                }
            }

            @Override
            public void onKeyboardStartClose() {
                if (active) {
                    showToolbar(true);
                }
            }

            @Override
            public void onKeyboardFinishOpen() {}

            @Override
            public void onKeyboardFinishClose() {
                if (active) {
                    if (((TextboxOverlay) overlay).isTextRectangleSelected()) {
                        toolbar.setVisibility(View.VISIBLE);
                    }
                    else {
                        toolbar.setVisibility(View.GONE);
                    }
                    ((TextboxOverlay) overlay).restoreSubviewBeforeFocus();
                }
            }
        });

        // Create the invisible keyboard required by this view.
        invisibleEditText = new EditText(context);
        invisibleEditText.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        invisibleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ((TextboxOverlay) overlay).setText(s.toString());
                delegate.onOverlayBackingChanged();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });
        parent.addView(invisibleEditText);
    }

    @Override
    protected void setToolbarHandlers(Context context) {
        final TextboxOverlay textboxOverlay = (TextboxOverlay) overlay; // Cast the overlay to the correct type so we don't have to in all the on-click listeners

        toolbar.findViewById(R.id.text_edit_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToolbar(false);
                ((TextboxLayerDelegate) delegate).onTextRectangleStartEditing(invisibleEditText, ((TextboxOverlay) overlay).getText());
            }
        });

        toolbar.findViewById(R.id.font_size_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textboxOverlay.increaseFontSize();
                delegate.onOverlayBackingChanged();
            }
        });

        toolbar.findViewById(R.id.font_size_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textboxOverlay.decreaseFontSize();
                delegate.onOverlayBackingChanged();
            }
        });

        toolbar.findViewById(R.id.show_full_editor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToolbar(false);
                ((TextboxLayerDelegate) delegate).onTextRectangleFullEditing((TextboxOverlay) overlay);
            }
        });

        toolbar.findViewById(R.id.delete_selected_annotation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textboxOverlay.deleteSelectedTextRectangle();
                delegate.onOverlayBackingChanged();
            }
        });

        toolbar.findViewById(R.id.add_timestamp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textboxOverlay.addTimestampToCanvas();
                delegate.onOverlayBackingChanged();
            }
        });

    }
    protected void enableToolbarButtons(boolean selected){
        toolbar.findViewById(R.id.text_edit_button).setEnabled(selected);
        toolbar.findViewById(R.id.font_size_up).setEnabled(selected);
        toolbar.findViewById(R.id.font_size_down).setEnabled(selected);
        toolbar.findViewById(R.id.show_full_editor).setEnabled(selected);
        toolbar.findViewById(R.id.delete_selected_annotation).setEnabled(selected);
    }

}
