package org.dpadgett.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * A {@link TextView} which displays a time in HH:MM:SS am format.  Ensures no line breaks.
 *
 * @author dpadgett
 */
public class TimeTextView extends TextView {

	public TimeTextView(Context context) {
		super(context);
	}

	public TimeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY ||
				MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {
			int totalWidth = 0;
			
			{
				int unspecifiedMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
				// find padding amount
				super.onMeasure(unspecifiedMeasureSpec, unspecifiedMeasureSpec);
				totalWidth = getMeasuredWidth();
			}
			
			// scale the textview so it don't wrap (looks ugly)
			int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
			if (totalWidth > maxWidth) {
				Log.i(getClass().getName(), "Time text too wide, shrinking...");
				setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() * maxWidth / totalWidth);
				Log.i(getClass().getName(), "Changed textWidth from " + totalWidth + " with max of " + maxWidth);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
