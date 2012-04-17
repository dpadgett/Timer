package org.dpadgett.widget;

import android.content.Context;
import android.graphics.Rect;
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
			String text = "00:00:00 am";
			int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
			Rect textBounds = new Rect();
			getPaint().getTextBounds(text, 0, text.length(), textBounds);
			int textWidth = textBounds.width();
			if (textWidth > maxWidth) {
				Log.i(getClass().getName(), "Time text too wide, shrinking...");
				setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() * maxWidth / textWidth - 2);
				getPaint().getTextBounds(text, 0, text.length(), textBounds);
				int newTextWidth = textBounds.width();
				Log.i(getClass().getName(), "Changed textWidth from " + textWidth + " to " + newTextWidth + " with max of " + maxWidth);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
