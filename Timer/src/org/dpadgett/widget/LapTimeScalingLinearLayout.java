package org.dpadgett.widget;

import org.dpadgett.timer.R;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
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
			TextView lapLabel = (TextView) findViewById(R.id.lapLabel);
			TextView lapTimeView = (TextView) findViewById(R.id.lapTime);
			
			// scale the textviews so they don't wrap (looks ugly)
			int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
			int textWidth = 0;
			Rect textBounds = new Rect();
			lapLabel.getPaint().getTextBounds("lap 00", 0, 6, textBounds);
			textWidth += textBounds.width();
			lapTimeView.getPaint().getTextBounds(lapTimeView.getText().toString(),
					0, lapTimeView.getText().length(), textBounds);
			textWidth += textBounds.width();
			if (textWidth > maxWidth) {
				Log.i(getClass().getName(), "Lap time text too wide, shrinking...");
				lapLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, lapLabel.getTextSize() * maxWidth / textWidth - 2);
				lapTimeView.setTextSize(TypedValue.COMPLEX_UNIT_PX, lapTimeView.getTextSize() * maxWidth / textWidth - 2);
				lapLabel.getPaint().getTextBounds("lap 00", 0, 6, textBounds);
				int newTextWidth = textBounds.width();
				lapTimeView.getPaint().getTextBounds(lapTimeView.getText().toString(), 0, lapTimeView.getText().length(), textBounds);
				newTextWidth += textBounds.width();
				Log.i(getClass().getName(), "Changed textWidth from " + textWidth + " to " + newTextWidth + " with max of " + maxWidth);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

}
