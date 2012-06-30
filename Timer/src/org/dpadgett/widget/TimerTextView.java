package org.dpadgett.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

public class TimerTextView extends TextView {

	// the timestamp from which we started timing
	private long startingTime = 0;
	
	// whether or not we are paused
	private boolean isPaused = true;
	
	// a prefix to prepend to the timer text
	private String textPrefix = "";

	public TimerTextView(Context context) {
		super(context);
	}

	public TimerTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimerTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setStartingTime(long startingTime) {
		this.startingTime  = startingTime;
	}
	
	public void pause() {
		pause(System.currentTimeMillis());
	}
	
	public void pause(long pausingTimestamp) {
		isPaused = true;
		setTimerText(pausingTimestamp);
	}
	
	public void resume() {
		isPaused = false;
	}
	
	public void reset() {
		startingTime = System.currentTimeMillis();
		setTimerText(startingTime);
	}
	
	public void setTextPrefix(String textPrefix) {
		this.textPrefix = textPrefix;
	}
	
	public void forceUpdate(long timestamp) {
		setTimerText(timestamp);
	}
	
	private void setTimerText(long timestamp) {
		long elapsedTime = timestamp - startingTime;
		setText(textPrefix + getTimerText(elapsedTime));
		invalidate();
	}

	private void setTimerText() {
		setTimerText(System.currentTimeMillis());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (!isPaused) {
			setTimerText();
		}
		super.onDraw(canvas);
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
				// Log.i(getClass().getName(), "Timer text too wide, shrinking...");
				setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() * maxWidth / totalWidth);
				// Log.i(getClass().getName(), "Changed textWidth from " + totalWidth + " with max of " + maxWidth);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	private static String getTimerText(long elapsedTime) {
		long millis = elapsedTime % 1000;
		elapsedTime /= 1000;
		long secs = elapsedTime % 60;
		elapsedTime /= 60;
		long mins = elapsedTime % 60;
		elapsedTime /= 60;
		long hours = elapsedTime % 60;
		return String.format("%02d:%02d:%02d.%03d", hours, mins, secs, millis);
	}
}
