package org.dpadgett.compat;

import java.util.ArrayList;
import java.util.List;

import org.dpadgett.timer.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

// using this also requires using it in the appropriate layout xml.
public class LinearLayout extends android.widget.LinearLayout {

	private static final boolean COMPAT_NEEDED = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

	private int mShowDividers;
	private Drawable mDivider;
	private int mDividerWidth;
	private int mDividerHeight;
	private int mDividerPadding;
	private List<OnLayoutChangeListener> mOnLayoutChangeListeners;

	public LinearLayout(Context context) {
		super(context);
	}

	public LinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, 0);
	}

	public LinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	private void init(Context context, AttributeSet attrs, int defStyle) {
		TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.CompatLinearLayout, defStyle, 0);
        setDividerDrawable(a.getDrawable(R.styleable.CompatLinearLayout_compat_divider));
        mShowDividers = a.getInt(R.styleable.CompatLinearLayout_compat_showDividers, SHOW_DIVIDER_NONE);
	}

	@Override
	public void setShowDividers(int showDividers) {
		if (!COMPAT_NEEDED) {
			super.setShowDividers(showDividers);
			return;
		}
		if (showDividers != mShowDividers) {
			requestLayout();
		}
		mShowDividers = showDividers;
	}

	@Override
	public int getShowDividers() {
		if (!COMPAT_NEEDED) {
			return super.getShowDividers();
		}
		return mShowDividers;
	}

	@Override
	public void setDividerDrawable(Drawable divider) {
		if (!COMPAT_NEEDED) {
			super.setDividerDrawable(divider);
			return;
		}
		if (divider == mDivider) {
			return;
		}
		mDivider = divider;
		if (divider != null) {
			mDividerWidth = divider.getIntrinsicWidth();
			mDividerHeight = divider.getIntrinsicHeight();
		} else {
			mDividerWidth = 0;
			mDividerHeight = 0;
		}
		setWillNotDraw(divider == null);
		requestLayout();
	}

	@Override
	public void setDividerPadding(int padding) {
		if (!COMPAT_NEEDED) {
			super.setDividerPadding(padding);
			return;
		}
		mDividerPadding = padding;
	}

	@Override
	public int getDividerPadding() {
		if (!COMPAT_NEEDED) {
			return super.getDividerPadding();
		}
		return mDividerPadding;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (!COMPAT_NEEDED) {
			super.onDraw(canvas);
			return;
		}
		if (mDivider == null) {
			return;
		}

		if (getOrientation() == VERTICAL) {
			drawDividersVertical(canvas);
		} else {
			drawDividersHorizontal(canvas);
		}
	}

	void drawDividersVertical(Canvas canvas) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);

			if (child != null && child.getVisibility() != GONE) {
				if (hasDividerBeforeChildAt(i)) {
					final LayoutParams lp = (LayoutParams) child
							.getLayoutParams();
					final int top = child.getTop() - lp.topMargin;
					drawHorizontalDivider(canvas, top);
				}
			}
		}

		if (hasDividerBeforeChildAt(count)) {
			final View child = getChildAt(count - 1);
			int bottom = 0;
			if (child == null) {
				bottom = getHeight() - getPaddingBottom() - mDividerHeight;
			} else {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				bottom = child.getBottom() + lp.bottomMargin;
			}
			drawHorizontalDivider(canvas, bottom);
		}
	}

	void drawDividersHorizontal(Canvas canvas) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);

			if (child != null && child.getVisibility() != GONE) {
				if (hasDividerBeforeChildAt(i)) {
					final LayoutParams lp = (LayoutParams) child
							.getLayoutParams();
					final int left = child.getLeft() - lp.leftMargin;
					drawVerticalDivider(canvas, left);
				}
			}
		}

		if (hasDividerBeforeChildAt(count)) {
			final View child = getChildAt(count - 1);
			int right = 0;
			if (child == null) {
				right = getWidth() - getPaddingRight() - mDividerWidth;
			} else {
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				right = child.getRight() + lp.rightMargin;
			}
			drawVerticalDivider(canvas, right);
		}
	}

	void drawHorizontalDivider(Canvas canvas, int top) {
		mDivider.setBounds(getPaddingLeft() + mDividerPadding, top, getWidth()
				- getPaddingRight() - mDividerPadding, top + mDividerHeight);
		mDivider.draw(canvas);
	}

	void drawVerticalDivider(Canvas canvas, int left) {
		mDivider.setBounds(left, getPaddingTop() + mDividerPadding, left
				+ mDividerWidth, getHeight() - getPaddingBottom()
				- mDividerPadding);
		mDivider.draw(canvas);
	}

	protected boolean hasDividerBeforeChildAt(int childIndex) {
		if (childIndex == 0) {
			return (mShowDividers & SHOW_DIVIDER_BEGINNING) != 0;
		} else if (childIndex == getChildCount()) {
			return (mShowDividers & SHOW_DIVIDER_END) != 0;
		} else if ((mShowDividers & SHOW_DIVIDER_MIDDLE) != 0) {
			boolean hasVisibleViewBefore = false;
			for (int i = childIndex - 1; i >= 0; i--) {
				if (getChildAt(i).getVisibility() != GONE) {
					hasVisibleViewBefore = true;
					break;
				}
			}
			return hasVisibleViewBefore;
		}
		return false;
	}

	// TODO(dpadgett): still need to override measure

	public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
		if (!COMPAT_NEEDED) {
			super.addOnLayoutChangeListener(new WrappedOnLayoutChangeListener(listener));
			return;
		}
		if (mOnLayoutChangeListeners == null) {
			mOnLayoutChangeListeners = new ArrayList<OnLayoutChangeListener>();
		}
		if (!mOnLayoutChangeListeners.contains(listener)) {
			mOnLayoutChangeListeners.add(listener);
		}
	}

	public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
		if (!COMPAT_NEEDED) {
			super.removeOnLayoutChangeListener(new WrappedOnLayoutChangeListener(listener));
			return;
		}
		if (mOnLayoutChangeListeners == null) {
			return;
		}
		mOnLayoutChangeListeners.remove(listener);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (!COMPAT_NEEDED) {
			super.onLayout(changed, l, t, r, b);
			return;
		}
        int oldL = getLeft();
        int oldT = getTop();
        int oldB = getBottom();
        int oldR = getRight();
		super.onLayout(changed, l, t, r, b);
		if (mOnLayoutChangeListeners != null) {
            ArrayList<OnLayoutChangeListener> listenersCopy = new ArrayList<OnLayoutChangeListener>();
            listenersCopy.addAll(mOnLayoutChangeListeners);
            int numListeners = listenersCopy.size();
            for (int i = 0; i < numListeners; ++i) {
                listenersCopy.get(i).onLayoutChange(this, l, t, r, b, oldL, oldT, oldR, oldB);
            }
        }
	}

    /**
     * Interface definition for a callback to be invoked when the layout bounds of a view
     * changes due to layout processing.
     */
    public interface OnLayoutChangeListener {
        /**
         * Called when the focus state of a view has changed.
         *
         * @param v The view whose state has changed.
         * @param left The new value of the view's left property.
         * @param top The new value of the view's top property.
         * @param right The new value of the view's right property.
         * @param bottom The new value of the view's bottom property.
         * @param oldLeft The previous value of the view's left property.
         * @param oldTop The previous value of the view's top property.
         * @param oldRight The previous value of the view's right property.
         * @param oldBottom The previous value of the view's bottom property.
         */
        void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom);
    }
    
    public static class WrappedOnLayoutChangeListener implements android.view.View.OnLayoutChangeListener {

    	private final OnLayoutChangeListener delegate;

		private WrappedOnLayoutChangeListener(OnLayoutChangeListener delegate) {
    		this.delegate = delegate;
    	}

		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			delegate.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
		}
    	
		@Override
		public boolean equals(Object o) {
			return (o instanceof WrappedOnLayoutChangeListener) &&
					((WrappedOnLayoutChangeListener) o).delegate == delegate;
		}
    }
}
