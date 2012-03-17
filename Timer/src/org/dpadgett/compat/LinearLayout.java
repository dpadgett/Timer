package org.dpadgett.compat;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

// using this also requires using it in the appropriate layout xml.
public class LinearLayout extends android.widget.LinearLayout {

	private static final boolean COMPAT_NEEDED =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

	private static class CompatLinearLayout extends LinearLayout {
		private int mShowDividers;
		private Drawable mDivider;
		private int mDividerWidth;
		private int mDividerHeight;
		private int mDividerPadding;
		private List<OnLayoutChangeListener> mOnLayoutChangeListeners;

		private CompatLinearLayout(Context context) {
			super(context);
		}

		private CompatLinearLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		private CompatLinearLayout(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		@Override
		public void setShowDividers(int showDividers) {
	        if (showDividers != mShowDividers) {
	            requestLayout();
	        }
	        mShowDividers = showDividers;
		}

		@Override
		public int getShowDividers() {
	        return mShowDividers;
		}

		@Override
		public void setDividerDrawable(Drawable divider) {
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
	        mDividerPadding = padding;
		}

		@Override
	    public int getDividerPadding() {
	        return mDividerPadding;
	    }

		@Override
		protected void onDraw(Canvas canvas) {
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
	                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
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
	                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
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
	        mDivider.setBounds(getPaddingLeft() + mDividerPadding, top,
	                getWidth() - getPaddingRight() - mDividerPadding, top + mDividerHeight);
	        mDivider.draw(canvas);
	    }

	    void drawVerticalDivider(Canvas canvas, int left) {
	        mDivider.setBounds(left, getPaddingTop() + mDividerPadding,
	                left + mDividerWidth, getHeight() - getPaddingBottom() - mDividerPadding);
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

	    //TODO(dpadgett): still need to override measure, layout

	    @Override
		public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
	        if (mOnLayoutChangeListeners == null) {
	            mOnLayoutChangeListeners = new ArrayList<OnLayoutChangeListener>();
	        }
	        if (!mOnLayoutChangeListeners.contains(listener)) {
	            mOnLayoutChangeListeners.add(listener);
	        }
		}

		@Override
		public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
	        if (mOnLayoutChangeListeners == null) {
	            return;
	        }
	        mOnLayoutChangeListeners.remove(listener);
		}

	}

	private LinearLayout(Context context) {
		super(context);
	}

	private LinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	private LinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setShowDividers(int showDividers) {
		super.setShowDividers(showDividers);
	}

	@Override
	public int getShowDividers() {
		return super.getShowDividers();
	}

	@Override
	public void setDividerDrawable(Drawable divider) {
		super.setDividerDrawable(divider);
	}

	@Override
	public void setDividerPadding(int padding) {
		super.setDividerPadding(padding);
	}

	@Override
	public int getDividerPadding() {
		return super.getDividerPadding();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	@Override
	public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
		super.addOnLayoutChangeListener(listener);
	}

	@Override
	public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
		super.removeOnLayoutChangeListener(listener);
	}

	public static LinearLayout newLinearLayout(Context context) {
		return COMPAT_NEEDED ?
				new CompatLinearLayout(context) :
				new LinearLayout(context);
	}

	public static LinearLayout newLinearLayout(Context context, AttributeSet attrs) {
		return COMPAT_NEEDED ?
				new CompatLinearLayout(context, attrs) :
				new LinearLayout(context, attrs);
	}

	public static LinearLayout newLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		return COMPAT_NEEDED ?
				new CompatLinearLayout(context, attrs, defStyle) :
				new LinearLayout(context, attrs, defStyle);
	}

}
