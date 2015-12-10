package com.example.jying.androidannotations;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.jying.androidannotations.support.KeyboardActionListener;

import java.util.ArrayList;

/**
 * Created by Justin on 6/19/2015.
 */
public class KeyboardListenerActivity extends ActionBarActivity {

    // If the height of the activity is less than the height of the screen by 1/constant, then we consider the keyboard to be up.
    // But no seriously, why is there no onKeyboardUp or onKeyboardDown events?  This is really awful.
    // I'm only doing this because there's a highly voted StackOverflow article where this is suggested.
    private static final int KEYBOARD_DETECTION_CONSTANT = 4;
    private boolean previousKeyboardState = false; // Remembers if the keyboard was open or closed.  Used for detecting onKeyboardUp and onKeyboardDown events.
    private boolean positiveDifference = false; // Assuming that the keyboard started closed, this will allow the activity to tell when the keyboard has changed directions
    private Integer previousHeightDifference; // Remembers the last height difference; initially null

    protected static final int KEYBOARD_OPEN = 0;
    protected static final int KEYBOARD_CLOSED = 1;
    private int keyboardState = KEYBOARD_CLOSED; // By default, assume the keyboard is closed.

    private ArrayList<KeyboardActionListener> subscriptions; // These are the action listeners that should be called when a keyboard event occurs.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        subscriptions = new ArrayList<KeyboardActionListener>();
    }

    protected void setRootView(final View activityRootView) {
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDifference = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                boolean isKeyboardVisible = (heightDifference > (activityRootView.getRootView().getHeight() / KEYBOARD_DETECTION_CONSTANT));
                if (isKeyboardVisible != previousKeyboardState) {
                    if (isKeyboardVisible) {
                        keyboardState = KEYBOARD_OPEN;
                        onKeyboardFinishOpen();
                    } else {
                        keyboardState = KEYBOARD_CLOSED;
                        onKeyboardFinishClose();
                    }
                }
                previousKeyboardState = isKeyboardVisible;
                if (previousHeightDifference != null) {
                    int dDifference = heightDifference - previousHeightDifference;
                    // If we're seeing a positive change in difference, and we had a negative change in difference before:
                    if ((dDifference > 0) && (!positiveDifference)) {
                        onKeyboardStartOpen();
                    }
                    // Else if we're seeing a negative change in difference, and we had a positive change in difference before:
                    else if ((dDifference < 0) && (positiveDifference)) {
                        onKeyboardStartClose();
                    }

                    // If dDifference is 0, remember the previous sign.
                    if (dDifference != 0) {
                        positiveDifference = (dDifference > 0);
                    }
                }
                previousHeightDifference = new Integer(heightDifference);
            }
        });
    }

    private void onKeyboardStartOpen() {
        for (KeyboardActionListener listener : subscriptions) {
            listener.onKeyboardStartOpen();
        }
    }

    private void onKeyboardStartClose() {
        for (KeyboardActionListener listener : subscriptions) {
            listener.onKeyboardStartClose();
        }
    }

    private void onKeyboardFinishOpen() {
        for (KeyboardActionListener listener : subscriptions) {
            listener.onKeyboardFinishOpen();
        }
    }

    private void onKeyboardFinishClose() {
        for (KeyboardActionListener listener : subscriptions) {
            listener.onKeyboardFinishClose();
        }
    }

    public void subscribeKeyboardActionListener(KeyboardActionListener listener) {
        subscriptions.add(listener);
    }

    public void unsubscribeKeyboardActionListener(KeyboardActionListener listener) {
        subscriptions.remove(listener);
    }

    protected int getKeyboardStatus() {
        return keyboardState;
    }
}
