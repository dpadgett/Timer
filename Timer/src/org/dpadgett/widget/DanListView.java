package org.dpadgett.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Subclassing ListView, but will override and patch broken methods.
 *
 * @author dan
 */
public class DanListView extends android.widget.ListView {

	private boolean overrideGetChildCount = false;

	/**
	 * @param context
	 */
	public DanListView(Context context) {
		super(context);
	}

	/**
	 * @param context
	 * @param attrs
	 */
	public DanListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public DanListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
    /**
     * Returns the number of children in the group.
     *
     * @return a positive integer representing the number of children in
     *         the group
     */
    public int getChildCount() {
    	if (overrideGetChildCount) {
    		overrideGetChildCount = false;
    		return Math.max(0, super.getChildCount() - 1);
    	}
        return super.getChildCount();
    }

    /**
     * Smoothly scroll by distance pixels over duration milliseconds.
     * @param distance Distance to scroll in pixels.
     * @param duration Duration of the scroll animation in milliseconds.
     */
    public void smoothScrollBy(int distance, int duration) {
		overrideGetChildCount = true;
		super.smoothScrollBy(distance, duration);
		overrideGetChildCount = false;
    }

}
