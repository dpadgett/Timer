/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dpadgett.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.RemoteViews.RemoteView;

/**
 * This widget display an analog clock with three hands for hours,
 * minutes, and seconds.
 */
@RemoteView
public class AnalogClockWithTimezone extends View {
    private Time mCalendar;

    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;
    private Drawable mDial;

    private int mDialWidth;
    private int mDialHeight;

    private boolean mAttached;

    private final Handler mHandler = new Handler();
    private float mSeconds;
    private float mMinutes;
    private float mHour;
    private boolean mChanged;
    
    private Runnable mUpdater;

    public AnalogClockWithTimezone(Context context) {
        this(context, null);
    }

    public AnalogClockWithTimezone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClockWithTimezone(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
        Resources r = getContext().getResources();
        int[] resources = {
        		Resources.getSystem().getIdentifier("AnalogClock_dial", "styleable", "android"),
        		Resources.getSystem().getIdentifier("AnalogClock_hand_hour", "styleable", "android"),
        		Resources.getSystem().getIdentifier("AnalogClock_hand_minute", "styleable", "android"),
        };
        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, resources, defStyle, 0);

        mDial = a.getDrawable(Resources.getSystem().getIdentifier("AnalogClock_dial", "styleable", "android"));
        if (mDial == null) {
            mDial = r.getDrawable(Resources.getSystem().getIdentifier("clock_dial", "drawable", "android"));
        }

        mHourHand = a.getDrawable(Resources.getSystem().getIdentifier("AnalogClock_hand_hour", "styleable", "android"));
        if (mHourHand == null) {
            mHourHand = r.getDrawable(Resources.getSystem().getIdentifier("clock_hand_hour", "drawable", "android"));
        }

        mMinuteHand = a.getDrawable(Resources.getSystem().getIdentifier("AnalogClock_hand_minute", "styleable", "android"));
        if (mMinuteHand == null) {
            mMinuteHand = r.getDrawable(Resources.getSystem().getIdentifier("clock_hand_minute", "drawable", "android"));
        }

        mSecondHand = a.getDrawable(Resources.getSystem().getIdentifier("AnalogClock_hand_minute", "styleable", "android"));
        if (mSecondHand == null) {
            mSecondHand = r.getDrawable(Resources.getSystem().getIdentifier("clock_hand_minute", "drawable", "android"));
        }

        mCalendar = new Time();

        mDialWidth = mDial.getIntrinsicWidth();
        mDialHeight = mDial.getIntrinsicHeight();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // Make sure we update to the current time
        onTimeChanged();
        
        mUpdater = new Runnable() {
			@Override
			public void run() {
				onTimeChanged();
				for (OnTickListener l : onTickListeners) {
					l.onTick();
				}
				invalidate();
                long now = SystemClock.uptimeMillis();
                long realNow = System.currentTimeMillis();
                long next = now + (1000 - (realNow % 1000));
                mHandler.postAtTime(this, next);
			}
        };
        mUpdater.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mAttached = false;
        }
        mHandler.removeCallbacks(mUpdater);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);

        float hScale = 1.0f;
        float vScale = 1.0f;

        if (widthMode != MeasureSpec.UNSPECIFIED && widthSize < mDialWidth) {
            hScale = (float) widthSize / (float) mDialWidth;
        }

        if (heightMode != MeasureSpec.UNSPECIFIED && heightSize < mDialHeight) {
            vScale = (float )heightSize / (float) mDialHeight;
        }

        float scale = Math.min(hScale, vScale);

        setMeasuredDimension(resolveSizeAndState((int) (mDialWidth * scale), widthMeasureSpec, 0),
                resolveSizeAndState((int) (mDialHeight * scale), heightMeasureSpec, 0));
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec.  Will take the desired size, unless a different size
     * is imposed by the constraints.  The returned value is a compound integer,
     * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
     * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the resulting
     * size is smaller than the size the view wants to be.
     *
     * @param size How big the view wants to be
     * @param measureSpec Constraints imposed by the parent
     * @return Size information bit mask as defined by
     * {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     */
    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            result = size;
            break;
        case MeasureSpec.AT_MOST:
            if (specSize < size) {
                result = specSize | MEASURED_STATE_TOO_SMALL;
            } else {
                result = size;
            }
            break;
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result | (childMeasuredState&MEASURED_STATE_MASK);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mChanged = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean changed = mChanged;
        if (changed) {
            mChanged = false;
        }

        int availableWidth = getRight() - getLeft();
        int availableHeight = getBottom() - getTop();

        int x = availableWidth / 2;
        int y = availableHeight / 2;

        final Drawable dial = mDial;
        int w = dial.getIntrinsicWidth();
        int h = dial.getIntrinsicHeight();

        boolean scaled = false;

        if (availableWidth < w || availableHeight < h) {
            scaled = true;
            float scale = Math.min((float) availableWidth / (float) w,
                                   (float) availableHeight / (float) h);
            canvas.save();
            canvas.scale(scale, scale, x, y);
        }

        if (changed) {
            dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        dial.draw(canvas);

        canvas.save();
        canvas.rotate(mHour / 12.0f * 360.0f, x, y);
        final Drawable hourHand = mHourHand;
        if (changed) {
            w = hourHand.getIntrinsicWidth();
            h = hourHand.getIntrinsicHeight();
            hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        hourHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);

        final Drawable minuteHand = mMinuteHand;
        if (changed) {
            w = minuteHand.getIntrinsicWidth();
            h = minuteHand.getIntrinsicHeight();
            minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        minuteHand.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.rotate(mSeconds / 60.0f * 360.0f, x, y);

        final Drawable secondHand = mSecondHand;
        if (changed) {
            w = secondHand.getIntrinsicWidth() / 2;
            h = secondHand.getIntrinsicHeight();
            secondHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
        }
        secondHand.draw(canvas);
        canvas.restore();

        if (scaled) {
            canvas.restore();
        }
    }

    private void onTimeChanged() {
        mCalendar.setToNow();

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;
        int second = mCalendar.second;
        
        mSeconds = second;
        mMinutes = minute + second / 60.0f;
        mHour = hour + mMinutes / 60.0f;
        mChanged = true;

        updateContentDescription(mCalendar);
    }

    public void setTimezone(String tz) {
    	mCalendar = new Time(tz);
    	onTimeChanged();
    	postInvalidate();
    }
    
    private List<OnTickListener> onTickListeners = new ArrayList<OnTickListener>();
    public void addOnTickListener(OnTickListener l) {
    	onTickListeners.add(l);
    }
    
    public void removeOnTickListener(OnTickListener l) {
    	onTickListeners.remove(l);
    }
    
    public interface OnTickListener {
    	void onTick();
    }

    private void updateContentDescription(Time time) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(getContext(),
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }
}
