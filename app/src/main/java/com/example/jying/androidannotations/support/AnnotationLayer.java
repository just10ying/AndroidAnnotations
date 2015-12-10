package com.example.jying.androidannotations.support;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.jying.androidannotations.R;

/**
 * Created by jying on 6/26/2015.
 */
public class AnnotationLayer {

    private static final String ACTIVE_KEY = "active";
    private static final int MENU_ACTION_DISPLAY = MenuItem.SHOW_AS_ACTION_ALWAYS;
    private static final int TOAST_OFFSET_DP = 5;
    private int ACTION_BAR_HEIGHT;
    private int toastOffsetPx;
    private int iconResourceID;

    protected Toast toast;
    protected float activeResolutionRatio = .7f;
    protected float backgroundResolutionRatio = .4f;
    protected LayerDelegate delegate;
    protected Overlay overlay;
    protected ViewGroup toolbar;
    protected boolean active;
    protected String name;

    public AnnotationLayer(Context context, ViewGroup parent, int toolbarResourceID, int iconResourceID, String name, Overlay overlay, LayerDelegate delegate) {
        this.active = false; // Layers by default start as inactive.
        this.name = name;
        this.overlay = overlay;
        this.iconResourceID = iconResourceID;
        this.delegate = delegate;

        // Get the layout inflater to inflate the toolbar.
        if (toolbarResourceID != 0) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            toolbar = (ViewGroup) inflater.inflate(toolbarResourceID, parent, false);
            toolbar.setVisibility(View.GONE);
            setToolbarHandlers(context); // Set handlers before toolbar is added, since some buttons may fire immediately when the toolbar is added.
            parent.addView(toolbar);
        }

        // Create toast to show when the mode is active:
        ACTION_BAR_HEIGHT = context.getResources().getDimensionPixelSize(R.dimen.abc_action_bar_default_height_material);
        toastOffsetPx = (int) (TOAST_OFFSET_DP * context.getResources().getDisplayMetrics().density + 0.5f);
        toast = Toast.makeText(context, name, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, ACTION_BAR_HEIGHT + toastOffsetPx);
    }

    public void setActive(boolean active) {
        this.active = active;
        showToolbar(active);
        if (active) {
            toast.show();
            overlay.getBackingCanvas().setResizeRatio(activeResolutionRatio);
        }
        else {
            overlay.getBackingCanvas().setResizeRatio(backgroundResolutionRatio);
        }
        overlay.onLayerActivationChanged(active);
        delegate.onOverlayBackingChanged();
    }

    public boolean isActive() {
        return active;
    }

    public void removeToolbar() {
        ViewGroup viewGroup = (ViewGroup)(toolbar.getParent());
        if (viewGroup != null) {
            viewGroup.removeView(toolbar);
        }
    }

    public void showToolbar(boolean show) {
        if (toolbar != null) {
            if (show) {
                toolbar.setVisibility(View.VISIBLE);
            }
            else {
                toolbar.setVisibility(View.GONE);
            }
        }
    }

    public boolean isToolbarVisible() {
        return toolbar.getVisibility() == View.VISIBLE;
    }

    public Overlay getOverlay() {
        return overlay;
    }

    public void addMenuItem(Menu menu) {
        MenuItem item = menu.add(name);
        item.setIcon(iconResourceID);
        item.setShowAsAction(MENU_ACTION_DISPLAY);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                delegate.onLayerRequestSoleActivation();
                delegate.onOverlayBackingChanged();
                setActive(true);
                return false;
            }
        });
    }

    protected void setToolbarHandlers(Context context) {

    }

    public void saveToBundle(Bundle bundle) {
        overlay.saveToBundle(bundle, name);
        bundle.putBoolean(name + ACTIVE_KEY, active);
    }

    public void restoreFromBundle(Bundle bundle, boolean restoreActive) {
        overlay.restoreFromBundle(bundle, name);
        if (restoreActive) {
            setActive(bundle.getBoolean(name + ACTIVE_KEY));
        }
    }
}
