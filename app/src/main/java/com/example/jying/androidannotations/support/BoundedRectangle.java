package com.example.jying.androidannotations.support;

import android.graphics.Rect;

import com.example.jying.androidannotations.textbox.BoundsModificationRecord;

/**
 * Created by jying on 6/25/2015.
 */
public class BoundedRectangle {

    protected Rect parent, child;

    protected boolean leftBoundModified;
    protected boolean topBoundModified;
    protected boolean rightBoundModified;
    protected boolean bottomBoundModified;

    public BoundedRectangle(Rect parent, Rect child) {
        this.parent = parent;
        this.child = child;

        resetModifiedMemory();
    }

    public Rect getChild() {
        return new Rect(child);
    }

    public void setChild(Rect newChild) {
        if (newChild != null) {
            recordModifications(child, newChild);
            child.set(newChild);
            child.sort();
        }
    }

    public void resetModifiedMemory() {
        leftBoundModified = false;
        topBoundModified = false;
        rightBoundModified = false;
        bottomBoundModified = false;
    }

    // Ensures that the size of child is not bigger than the parent.
    public void enforceParentSize() {
        Rect modified = new Rect(child);
        if (child.width() > parent.width()) {
            modified.left = parent.left;
            modified.right = parent.right;
        }
        if (child.height() > parent.height()) {
            modified.top = parent.top;
            modified.bottom = parent.bottom;
        }
        recordModifications(child, modified);
        child.set(modified);
    }

    public void enforceParentBounds() {
        Rect modified = new Rect(child); // The rectangle that results after any bounds modifications
        int dx = 0;
        int dy = 0;

        if (child.right > parent.right) {
            dx = parent.right - child.right;
        }
        if (child.left < parent.left) {
            dx = parent.left - child.left;
        }
        if (child.bottom > parent.bottom) {
            dy = parent.bottom - child.bottom;
        }
        if (child.top < parent.top) {
            dy = parent.top - child.top;
        }
        modified.offset(dx, dy);

        recordModifications(child, modified);
        child.set(modified);
    }

    public void enforceMinWidth(int minWidth, float minWidthModifier) {
        Rect modified = new Rect(child); // The rectangle that results after any bounds modifications
        if (child.width() < minWidth) {
            int dx = minWidth - child.width();
            modified.left -= dx * minWidthModifier;
            modified.right += dx * (1 - minWidthModifier);
        }

        recordModifications(child, modified);
        child.set(modified);
    }

    public void enforceMinHeight(int minHeight, float minHeightModifier) {
        Rect modified = new Rect(child); // The rectangle that results after any bounds modifications
        if (child.height() < minHeight) {
            int dy = minHeight - child.height();
            modified.top -= dy * minHeightModifier;
            modified.bottom += dy * (1 - minHeightModifier);
        }

        recordModifications(child, modified);
        child.set(modified);
    }

    public BoundsModificationRecord getModifiedbounds() {
        BoundsModificationRecord record = new BoundsModificationRecord();
        record.leftModified = leftBoundModified;
        record.topModified = topBoundModified;
        record.rightModified = rightBoundModified;
        record.bottomModified = bottomBoundModified;
        return record;
    }

    private void recordModifications(Rect original, Rect modified) {
        if ((original == null) && (modified == null)) {
            leftBoundModified = true;
            topBoundModified = true;
            rightBoundModified = true;
            bottomBoundModified = true;
        }
        else if ((original == null) || (modified == null)) {
            leftBoundModified = false;
            topBoundModified = false;
            rightBoundModified = false;
            bottomBoundModified = false;
        }
        else {
            leftBoundModified = leftBoundModified || (original.left != modified.left);
            topBoundModified = topBoundModified || (original.top != modified.top);
            rightBoundModified = rightBoundModified || (original.right != modified.right);
            bottomBoundModified = bottomBoundModified || (original.bottom != modified.bottom);
        }
    }

}
