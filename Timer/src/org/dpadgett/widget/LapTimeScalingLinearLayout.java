package org.dpadgett.widget;

import org.dpadgett.timer.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Extension of LinearLayout to scale down lap time texts when they're too wide for the window,
 * to keep them from wrapping.  This has to be in a subclass since it should happen at measure time.
 *
 * @author dpadgett
 */
public class LapTimeScalingLinearLayout extends LinearLayout {

	public LapTimeScalingLinearLayout(Context context) {
		super(context);
	}

	public LapTimeScalingLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LapTimeScalingLinearLayout(Context context, AttributeSet attrs,
			int defStyle) {
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
			
			// scale the textviews so they don't wrap (looks ugly)
			int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
			if (totalWidth > maxWidth) {
				TextView lapLabel = (TextView) findViewById(R.id.lapLabel);
				TextView lapTimeView = (TextView) findViewById(R.id.lapTime);
				
				// Log.i(getClass().getName(), "Lap time text too wide, shrinking...");
				lapLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX,
						lapLabel.getTextSize() * maxWidth / totalWidth);
				lapTimeView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
						lapTimeView.getTextSize() * maxWidth / totalWidth);
				// Log.i(getClass().getName(), "Changed textWidth from " + totalWidth
				// 		+ " with max of " + MeasureSpec.toString(widthMeasureSpec));
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
