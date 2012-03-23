/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.util.Arrays;

import org.dpadgett.compat.R;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.LayoutInflater.Filter;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * A widget that enables the user to select a number form a predefined range.
 * The widget presents an input filed and up and down buttons for selecting the
 * current value. Pressing/long pressing the up and down buttons increments and
 * decrements the current value respectively. Touching the input filed shows a
 * scroll wheel, tapping on which while shown and not moving allows direct edit
 * of the current value. Sliding motions up or down hide the buttons and the
 * input filed, show the scroll wheel, and rotate the latter. Flinging is
 * also supported. The widget enables mapping from positions to strings such
 * that instead the position index the corresponding string is displayed.
 * <p>
 * For an example of using this widget, see {@link android.widget.TimePicker}.
 * </p>
 */
public class FasterNumberPicker extends LinearLayout {

	private static final boolean COMPAT_NEEDED = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

	/**
     * The default update interval during long press.
     */
    private static final long DEFAULT_LONG_PRESS_UPDATE_INTERVAL = 300 / 2;

    /**
     * The index of the middle selector item.
     */
    private int mSelectorMiddleItemIndex = 3;

    /**
     * The coefficient by which to adjust (divide) the max fling velocity.
     */
    private static final int SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT = 8;

    /**
     * The the duration for adjusting the selector wheel.
     */
    private static final int SELECTOR_ADJUSTMENT_DURATION_MILLIS = 800;

    /**
     * The duration of scrolling to the next/previous value while changing
     * the current value by one, i.e. increment or decrement.
     */
    private static final int CHANGE_CURRENT_BY_ONE_SCROLL_DURATION = 300;

    /**
     * The the delay for showing the input controls after a single tap on the
     * input text.
     */
    private static final int SHOW_INPUT_CONTROLS_DELAY_MILLIS = ViewConfiguration
            .getDoubleTapTimeout();

    /**
     * The strength of fading in the top and bottom while drawing the selector.
     */
    private static final float TOP_AND_BOTTOM_FADING_EDGE_STRENGTH = 0.9f;

    /**
     * The default unscaled height of the selection divider.
     */
    private static final int UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT = 2;

    /**
     * In this state the selector wheel is not shown.
     */
    private static final int SELECTOR_WHEEL_STATE_NONE = 0;

    /**
     * In this state the selector wheel is small.
     */
    private static final int SELECTOR_WHEEL_STATE_SMALL = 1;

    /**
     * In this state the selector wheel is large.
     */
    private static final int SELECTOR_WHEEL_STATE_LARGE = 2;

    /**
     * The alpha of the selector wheel when it is bright.
     */
    private static final int SELECTOR_WHEEL_BRIGHT_ALPHA = 255;

    /**
     * The alpha of the selector wheel when it is dimmed.
     */
    private static final int SELECTOR_WHEEL_DIM_ALPHA = 60;

    /**
     * The alpha for the increment/decrement button when it is transparent.
     */
    private static final int BUTTON_ALPHA_TRANSPARENT = 0;

    /**
     * The alpha for the increment/decrement button when it is opaque.
     */
    private static final int BUTTON_ALPHA_OPAQUE = 1;

    /**
     * The property for setting the selector paint.
     */
    private static final String PROPERTY_SELECTOR_PAINT_ALPHA = "selectorPaintAlpha";

    /**
     * The property for setting the increment/decrement button alpha.
     */
    private static final String PROPERTY_BUTTON_ALPHA = "alpha";

    /**
     * The numbers accepted by the input text's {@link Filter}
     */
    private static final char[] DIGIT_CHARACTERS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    /**
     * Constant for unspecified size.
     */
    private static final int SIZE_UNSPECIFIED = -1;

    /**
     * Use a custom NumberPicker formatting callback to use two-digit minutes
     * strings like "01". Keeping a static formatter etc. is the most efficient
     * way to do this; it avoids creating temporary objects on every call to
     * format().
     *
     * @hide
     */
    public static final FasterNumberPicker.Formatter TWO_DIGIT_FORMATTER = new FasterNumberPicker.Formatter() {
        final StringBuilder mBuilder = new StringBuilder();

        final java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);

        final Object[] mArgs = new Object[1];

        public String format(int value) {
            mArgs[0] = value;
            mBuilder.delete(0, mBuilder.length());
            mFmt.format("%02d", mArgs);
            return mFmt.toString();
        }
    };

    /**
     * The increment button.
     */
    private final ImageButton mIncrementButton;

    /**
     * The decrement button.
     */
    private final ImageButton mDecrementButton;

    /**
     * The text for showing the current value.
     */
    private final EditText mInputText;


	private boolean mInputTextVisible;

	/**
     * The min height of this widget.
     */
    private final int mMinHeight;

    /**
     * The max height of this widget.
     */
    private final int mMaxHeight;

    /**
     * The max width of this widget.
     */
    private final int mMinWidth;

    /**
     * The max width of this widget.
     */
    private int mMaxWidth;

    /**
     * Flag whether to compute the max width.
     */
    private final boolean mComputeMaxWidth;

    /**
     * The height of the text.
     */
    private final int mTextSize;

    /**
     * The height of the gap between text elements if the selector wheel.
     */
    private int mSelectorTextGapHeight;

    /**
     * The values to be displayed instead the indices.
     */
    private String[] mDisplayedValues;

    /**
     * Lower value of the range of numbers allowed for the NumberPicker
     */
    private int mMinValue;

    /**
     * Upper value of the range of numbers allowed for the NumberPicker
     */
    private int mMaxValue;

    /**
     * Current value of this NumberPicker
     */
    private int mValue;

    /**
     * Listener to be notified upon current value change.
     */
    private OnValueChangeListener mOnValueChangeListener;

    /**
     * Listener to be notified upon scroll state change.
     */
    private OnScrollListener mOnScrollListener;

    /**
     * Formatter for for displaying the current value.
     */
    private Formatter mFormatter;

    /**
     * The speed for updating the value form long press.
     */
    private long mLongPressUpdateInterval = DEFAULT_LONG_PRESS_UPDATE_INTERVAL;

    /**
     * Cache for the string representation of selector indices.
     */
    private final SparseArray<String> mSelectorIndexToStringCache = new SparseArray<String>();

    /**
     * The {@link Paint} for drawing the selector.
     */
    private final Paint mSelectorWheelPaint;

    /**
     * The height of a selector element (text + gap).
     */
    private int mSelectorElementHeight;

    /**
     * The initial offset of the scroll selector.
     */
    private int mInitialScrollOffset = Integer.MIN_VALUE;

    /**
     * The current offset of the scroll selector.
     */
    private int mCurrentScrollOffset;

    /**
     * The {@link Scroller} responsible for flinging the selector.
     */
    private final Scroller mFlingScroller;

    /**
     * The {@link Scroller} responsible for adjusting the selector.
     */
    private final Scroller mAdjustScroller;

    /**
     * The previous Y coordinate while scrolling the selector.
     */
    private int mPreviousScrollerY;

    /**
     * Handle to the reusable command for setting the input text selection.
     */
    private SetSelectionCommand mSetSelectionCommand;

    /**
     * Handle to the reusable command for adjusting the scroller.
     */
    private AdjustScrollerCommand mAdjustScrollerCommand;

    /**
     * Handle to the reusable command for changing the current value from long
     * press by one.
     */
    private ChangeCurrentByOneFromLongPressCommand mChangeCurrentByOneFromLongPressCommand;

    /**
     * {@link Animator} for showing the up/down arrows.
     */
    //private final AnimatorSet mShowInputControlsAnimator;

    /**
     * {@link Animator} for dimming the selector wheel.
     */
    //private final Animator mDimSelectorWheelAnimator;

    /**
     * The Y position of the last down event.
     */
    private float mLastDownEventY;

    /**
     * The Y position of the last motion event.
     */
    private float mLastMotionEventY;

    /**
     * Flag if to begin edit on next up event.
     */
    private boolean mBeginEditOnUpEvent;

    /**
     * Flag if to adjust the selector wheel on next up event.
     */
    private boolean mAdjustScrollerOnUpEvent;

    /**
     * The state of the selector wheel.
     */
    private int mSelectorWheelState;

    /**
     * Determines speed during touch scrolling.
     */
    private VelocityTracker mVelocityTracker;

    /**
     * @see ViewConfiguration#getScaledTouchSlop()
     */
    private int mTouchSlop;

    /**
     * @see ViewConfiguration#getScaledMinimumFlingVelocity()
     */
    private int mMinimumFlingVelocity;

    /**
     * @see ViewConfiguration#getScaledMaximumFlingVelocity()
     */
    private int mMaximumFlingVelocity;

    /**
     * Flag whether the selector should wrap around.
     */
    private boolean mWrapSelectorWheel;

    /**
     * The back ground color used to optimize scroller fading.
     */
    private final int mSolidColor;

    /**
     * Flag indicating if this widget supports flinging.
     */
    private final boolean mFlingable;

    /**
     * Divider for showing item to be selected while scrolling
     */
    private final Drawable mSelectionDivider;

    /**
     * The height of the selection divider.
     */
    private final int mSelectionDividerHeight;

    /**
     * Reusable {@link Rect} instance.
     */
    private final Rect mTempRect = new Rect();

    /**
     * The current scroll state of the number picker.
     */
    private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /**
     * The duration of the animation for showing the input controls.
     */
    private final long mShowInputControlsAnimimationDuration;

    /**
     * Flag whether the scoll wheel and the fading edges have been initialized.
     */
    private boolean mScrollWheelAndFadingEdgesInitialized;

	private Scroller mLongPressScroller;

	private boolean mIsLongPressed = false;

	private ScrollabilityCache mScrollCache;

	private boolean mDisableInputText;

	/**
     * <p>ScrollabilityCache holds various fields used by a View when scrolling
     * is supported. This avoids keeping too many unused fields in most
     * instances of View.</p>
     */
    private static class ScrollabilityCache {

        public int fadingEdgeLength;

        public final Paint paint;
        public final Matrix matrix;
        public Shader shader;

        private int mLastColor;

        public ScrollabilityCache(ViewConfiguration configuration) {
            fadingEdgeLength = configuration.getScaledFadingEdgeLength();

            paint = new Paint();
            matrix = new Matrix();
            // use use a height of 1, and then wack the matrix each time we
            // actually use it.
            shader = new LinearGradient(0, 0, 0, 1, 0xFF000000, 0, Shader.TileMode.CLAMP);

            paint.setShader(shader);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        }

        public void setFadeColor(int color) {
            if (color != 0 && color != mLastColor) {
                mLastColor = color;
                color |= 0xFF000000;

                shader = new LinearGradient(0, 0, 0, 1, color | 0xFF000000,
                        color & 0x00FFFFFF, Shader.TileMode.CLAMP);

                paint.setShader(shader);
                // Restore the default transfer mode (src_over)
                paint.setXfermode(null);
            }
        }
    }
	
    /**
     * Interface to listen for changes of the current value.
     */
    public interface OnValueChangeListener {

        /**
         * Called upon a change of the current value.
         *
         * @param picker The NumberPicker associated with this listener.
         * @param oldVal The previous value.
         * @param newVal The new value.
         */
        void onValueChange(FasterNumberPicker picker, int oldVal, int newVal);
    }

    /**
     * Interface to listen for the picker scroll state.
     */
    public interface OnScrollListener {

        /**
         * The view is not scrolling.
         */
        public static int SCROLL_STATE_IDLE = 0;

        /**
         * The user is scrolling using touch, and their finger is still on the screen.
         */
        public static int SCROLL_STATE_TOUCH_SCROLL = 1;

        /**
         * The user had previously been scrolling using touch and performed a fling.
         */
        public static int SCROLL_STATE_FLING = 2;

        /**
         * Callback invoked while the number picker scroll state has changed.
         *
         * @param view The view whose scroll state is being reported.
         * @param scrollState The current scroll state. One of
         *            {@link #SCROLL_STATE_IDLE},
         *            {@link #SCROLL_STATE_TOUCH_SCROLL} or
         *            {@link #SCROLL_STATE_IDLE}.
         */
        public void onScrollStateChange(FasterNumberPicker view, int scrollState);
    }

    /**
     * Interface used to format current value into a string for presentation.
     */
    public interface Formatter {

        /**
         * Formats a string representation of the current value.
         *
         * @param value The currently selected value.
         * @return A formatted string representation.
         */
        public String format(int value);
    }

    /**
     * Create a new number picker.
     *
     * @param context The application environment.
     */
    public FasterNumberPicker(Context context) {
        this(context, null);
    }

    /**
     * Create a new number picker.
     *
     * @param context The application environment.
     * @param attrs A collection of attributes.
     */
    public FasterNumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, Resources.getSystem().getIdentifier("numberPickerStyle", "attr", "android"));
    }

    /**
     * Create a new number picker
     *
     * @param context the application environment.
     * @param attrs a collection of attributes.
     * @param defStyle The default style to apply to this view.
     */
    public FasterNumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);//, defStyle);

        // process style attributes
        TypedArray attributesArray = context.obtainStyledAttributes(attrs,
                R.styleable.NumberPicker, defStyle, 0);
        mSolidColor = attributesArray.getColor(R.styleable.NumberPicker_solidColor, 0);
        mFlingable = attributesArray.getBoolean(R.styleable.NumberPicker_flingable, true);
        mSelectionDivider = attributesArray.getDrawable(R.styleable.NumberPicker_selectionDivider);
        Log.i(getClass().getName(), "Got divider " + mSelectionDivider
        		+ " from " + attributesArray.getString(R.styleable.NumberPicker_selectionDivider)
        		+ " from " + toString(attrs));
        int defSelectionDividerHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                UNSCALED_DEFAULT_SELECTION_DIVIDER_HEIGHT,
                getResources().getDisplayMetrics());
        mSelectionDividerHeight = attributesArray.getDimensionPixelSize(
        		R.styleable.NumberPicker_selectionDividerHeight, defSelectionDividerHeight);
        mMinHeight = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_minHeight,
                SIZE_UNSPECIFIED);
        mMaxHeight = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_maxHeight,
                SIZE_UNSPECIFIED);
        if (mMinHeight != SIZE_UNSPECIFIED && mMaxHeight != SIZE_UNSPECIFIED
                && mMinHeight > mMaxHeight) {
            throw new IllegalArgumentException("minHeight > maxHeight");
        }
        mMinWidth = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_minWidth,
                SIZE_UNSPECIFIED);
        mMaxWidth = attributesArray.getDimensionPixelSize(R.styleable.NumberPicker_maxWidth,
                SIZE_UNSPECIFIED);
        if (mMinWidth != SIZE_UNSPECIFIED && mMaxWidth != SIZE_UNSPECIFIED
                && mMinWidth > mMaxWidth) {
            throw new IllegalArgumentException("minWidth > maxWidth");
        }
        mComputeMaxWidth = (mMaxWidth == Integer.MAX_VALUE);
        attributesArray.recycle();

        mShowInputControlsAnimimationDuration = getResources().getInteger(
        		Resources.getSystem().getIdentifier("config_longAnimTime", "integer", "android")) / 10;

        // By default Linearlayout that we extend is not drawn. This is
        // its draw() method is not called but dispatchDraw() is called
        // directly (see ViewGroup.drawChild()). However, this class uses
        // the fading edge effect implemented by View and we need our
        // draw() method to be called. Therefore, we declare we will draw.
        setWillNotDraw(false);
        setSelectorWheelState(SELECTOR_WHEEL_STATE_NONE);

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        Log.i(getClass().getName(), "ID: " + R.layout.number_picker);
        inflater.inflate(R.layout.number_picker, this, true);

        OnClickListener onClickListener = new OnClickListener() {
            public void onClick(View v) {
                InputMethodManager inputMethodManager = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
                if (inputMethodManager != null && inputMethodManager.isActive(mInputText)) {
                    inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
                }
                mInputText.clearFocus();
                if (v.getId() == R.id.increment) {
                    changeCurrentByOne(true);
                } else {
                    changeCurrentByOne(false);
                }
            }
        };

        OnLongClickListener onLongClickListener = new OnLongClickListener() {
            public boolean onLongClick(View v) {
                mInputText.clearFocus();
                mIsLongPressed = true;
                if (v.getId() == R.id.increment) {
                    postChangeCurrentByOneFromLongPress(true);
                } else {
                    postChangeCurrentByOneFromLongPress(false);
                }
                return true;
            }
        };

        // increment button
        mIncrementButton = (ImageButton) findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(onClickListener);
        mIncrementButton.setOnLongClickListener(onLongClickListener);

        // decrement button
        mDecrementButton = (ImageButton) findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(onClickListener);
        mDecrementButton.setOnLongClickListener(onLongClickListener);

        // input text
        //Log.i(getClass().getName(), this.getChildAt(1).getClass().getName());
        mDisableInputText = false;
        EditText inputText = (EditText) findViewById(R.id.numberpicker_input);
        if (inputText == null) {
        	inputText = (EditText) getChildAt(1);
        }
        mInputText = inputText;
        mInputText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50);
        mInputText.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mInputText.selectAll();
                    InputMethodManager inputMethodManager = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
                    if (inputMethodManager != null) {
                        inputMethodManager.showSoftInput(mInputText, 0);
                    }
                } else {
                    mInputText.setSelection(0, 0);
                    validateInputTextView(v);
                }
            }
        });
        mInputText.setFilters(new InputFilter[] {
            new InputTextFilter()
        });

        mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);

        // initialize constants
        mTouchSlop = ViewConfiguration.getTapTimeout();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity()
                / SELECTOR_MAX_FLING_VELOCITY_ADJUSTMENT;
        mTextSize = (int) mInputText.getTextSize();

        // create the selector wheel paint
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
        paint.setTextSize(mTextSize);
        paint.setTypeface(mInputText.getTypeface());
        ColorStateList colors = mInputText.getTextColors();
        int color = COMPAT_NEEDED ? Color.WHITE : colors.getColorForState(ENABLED_STATE_SET, Color.WHITE);
        paint.setColor(color);
        mSelectorWheelPaint = paint;

        // create the animator for showing the input controls
        //mDimSelectorWheelAnimator = ObjectAnimator.ofInt(this, PROPERTY_SELECTOR_PAINT_ALPHA,
        //        SELECTOR_WHEEL_BRIGHT_ALPHA, SELECTOR_WHEEL_DIM_ALPHA);
        //final ObjectAnimator showIncrementButton = ObjectAnimator.ofFloat(mIncrementButton,
        //        PROPERTY_BUTTON_ALPHA, BUTTON_ALPHA_TRANSPARENT, BUTTON_ALPHA_OPAQUE);
        //final ObjectAnimator showDecrementButton = ObjectAnimator.ofFloat(mDecrementButton,
        //        PROPERTY_BUTTON_ALPHA, BUTTON_ALPHA_TRANSPARENT, BUTTON_ALPHA_OPAQUE);
        //mShowInputControlsAnimator = new AnimatorSet();
        //mShowInputControlsAnimator.playTogether(mDimSelectorWheelAnimator, showIncrementButton,
        //        showDecrementButton);
        /*mShowInputControlsAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean mCanceled = false;

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mCanceled) {
                    // if canceled => we still want the wheel drawn
                    setSelectorWheelState(SELECTOR_WHEEL_STATE_SMALL);
                }
                mCanceled = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (mShowInputControlsAnimator.isRunning()) {
                    mCanceled = true;
                }
            }
        });*/

        // create the fling and adjust scrollers
        mFlingScroller = new Scroller(getContext(), null);//, true);
        mAdjustScroller = new Scroller(getContext(), new DecelerateInterpolator(2.5f));
        mLongPressScroller = new Scroller(getContext(), new LinearInterpolator());

        updateInputTextView();
        updateIncrementAndDecrementButtonsVisibilityState();

        if (mFlingable) {
           // if (isInEditMode()) {
               setSelectorWheelState(SELECTOR_WHEEL_STATE_SMALL);
           // } else {
                // Start with shown selector wheel and hidden controls. When made
                // visible hide the selector and fade-in the controls to suggest
                // fling interaction.
           //      setSelectorWheelState(SELECTOR_WHEEL_STATE_LARGE);
           //      hideInputControls();
           // }
        }
        
    	mInputTextVisible = true;
        
        mScrollCache = new ScrollabilityCache(ViewConfiguration.get(context));
        mScrollCache.fadingEdgeLength = (getBottom() - getTop() - mTextSize) / 2;
        
        mWrapSelectorWheel = true;
    }

    private String toString(AttributeSet attrs) {
    	final StringBuilder sb = new StringBuilder();
    	sb.append("Number of attrs: " + attrs.getAttributeCount() + ": [");
    	for (int i = 0; i < attrs.getAttributeCount(); i++, sb.append(i < attrs.getAttributeCount() ? ", " : "")) {
    		sb.append("{" + attrs.getAttributeName(i) + ": " + attrs.getAttributeValue(i) + "}");
    	}
    	sb.append("]");
		return sb.toString();
	}

	private int lastSizeHash = 0;
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int msrdWdth = getMeasuredWidth();
        final int msrdHght = getMeasuredHeight();
        
        // Increment button at the top.
        final int inctBtnMsrdWdth = mIncrementButton.getMeasuredWidth();
        final int incrBtnLeft = (msrdWdth - inctBtnMsrdWdth) / 2;
        final int incrBtnTop = 0;
        final int incrBtnRight = incrBtnLeft + inctBtnMsrdWdth;
        final int incrBtnBottom = incrBtnTop + mIncrementButton.getMeasuredHeight();
        mIncrementButton.layout(incrBtnLeft, incrBtnTop, incrBtnRight, incrBtnBottom);

        // Input text centered horizontally.
        final int inptTxtMsrdWdth = mInputText.getMeasuredWidth();
        final int inptTxtMsrdHght = mInputText.getMeasuredHeight();
        final int inptTxtLeft = (msrdWdth - inptTxtMsrdWdth) / 2;
        final int inptTxtTop = (msrdHght - inptTxtMsrdHght) / 2;
        final int inptTxtRight = inptTxtLeft + inptTxtMsrdWdth;
        final int inptTxtBottom = inptTxtTop + inptTxtMsrdHght;
        mInputText.layout(inptTxtLeft, inptTxtTop, inptTxtRight, inptTxtBottom);

        // Decrement button at the top.
        final int decrBtnMsrdWdth = mIncrementButton.getMeasuredWidth();
        final int decrBtnLeft = (msrdWdth - decrBtnMsrdWdth) / 2;
        final int decrBtnTop = msrdHght - mDecrementButton.getMeasuredHeight();
        final int decrBtnRight = decrBtnLeft + decrBtnMsrdWdth;
        final int decrBtnBottom = msrdHght;
        mDecrementButton.layout(decrBtnLeft, decrBtnTop, decrBtnRight, decrBtnBottom);

        int sizeHash = Arrays.hashCode(new int[] {msrdWdth, msrdHght, mInputText.getBaseline() + mInputText.getTop()});
        if (!mScrollWheelAndFadingEdgesInitialized || sizeHash != lastSizeHash) {
        	lastSizeHash = sizeHash;
            mScrollWheelAndFadingEdgesInitialized = true;
            // need to do all this when we know our size
            initializeSelectorWheel();
            initializeFadingEdges();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try greedily to fit the max width and height.
        final int newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
        		Math.max(Math.min(MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED ? MeasureSpec.getSize(widthMeasureSpec) : Integer.MAX_VALUE,
        						(int) (mSelectorWheelPaint.measureText("00") + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 4.0f, getContext().getResources().getDisplayMetrics()))
        					), 1),
        		MeasureSpec.AT_MOST);
        		// widthMeasureSpec; // makeMeasureSpec(widthMeasureSpec, mMaxWidth);
        Log.i(getClass().getName(), "Width measurespec: " + MeasureSpec.toString(newWidthMeasureSpec));
        Log.i(getClass().getName(), "Original width measurespec: " + MeasureSpec.toString(widthMeasureSpec));
        final int newHeightMeasureSpec = makeMeasureSpec(heightMeasureSpec, mMaxHeight);
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
        // Flag if we are measured with width or height less than the respective min.
        final int widthSize = resolveSizeAndStateRespectingMinSize(mMinWidth, getMeasuredWidth(),
                widthMeasureSpec);
        //final int heightSize = resolveSizeAndStateRespectingMinSize(mMinHeight, getMeasuredHeight(),
        //        heightMeasureSpec);
        int heightSize = (mTextSize + 4) * 5;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
        	int maxSize = MeasureSpec.getSize(heightMeasureSpec);
        	heightSize = Math.min(heightSize, maxSize - (maxSize % (mTextSize + 4)));
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!isEnabled() || !mFlingable) {
            return false;
        }
        switch (COMPAT_NEEDED ? event.getAction() : event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionEventY = mLastDownEventY = event.getY();
                removeAllCallbacks();
                //mShowInputControlsAnimator.cancel();
                //mDimSelectorWheelAnimator.cancel();
                cancelDim();
                mBeginEditOnUpEvent = false;
                mAdjustScrollerOnUpEvent = true;
                if (mSelectorWheelState == SELECTOR_WHEEL_STATE_LARGE) {
                    mSelectorWheelPaint.setAlpha(SELECTOR_WHEEL_BRIGHT_ALPHA);
                    boolean scrollersFinished = mFlingScroller.isFinished()
                            && mAdjustScroller.isFinished();
                    if (!scrollersFinished) {
                        mFlingScroller.forceFinished(true);
                        mAdjustScroller.forceFinished(true);
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
                    }
                    mBeginEditOnUpEvent = scrollersFinished;
                    mAdjustScrollerOnUpEvent = true;
                    hideInputControls();
                    return true;
                }
                if (isEventInVisibleViewHitRect(event, mIncrementButton)
                        || isEventInVisibleViewHitRect(event, mDecrementButton)) {
                    return false;
                }
                mAdjustScrollerOnUpEvent = false;
                setSelectorWheelState(SELECTOR_WHEEL_STATE_LARGE);
                hideInputControls();
                return true;
            case MotionEvent.ACTION_MOVE:
                float currentMoveY = event.getY();
                int deltaDownY = (int) Math.abs(currentMoveY - mLastDownEventY);
                if (deltaDownY > mTouchSlop) {
                    mBeginEditOnUpEvent = false;
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    setSelectorWheelState(SELECTOR_WHEEL_STATE_LARGE);
                    hideInputControls();
                    return true;
                }
                break;
        }
        return false;
    }

    private void cancelDim() {
    	setSelectorPaintAlpha(SELECTOR_WHEEL_BRIGHT_ALPHA);
	}

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        int action = COMPAT_NEEDED ? ev.getAction() : ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                float currentMoveY = ev.getY();
                if (mBeginEditOnUpEvent
                        || mScrollState != OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    int deltaDownY = (int) Math.abs(currentMoveY - mLastDownEventY);
                    if (deltaDownY > mTouchSlop) {
                        mBeginEditOnUpEvent = false;
                        onScrollStateChange(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
                    }
                }
                int deltaMoveY = (int) (currentMoveY - mLastMotionEventY);
                scrollBy(0, deltaMoveY);
                invalidate();
                mLastMotionEventY = currentMoveY;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mBeginEditOnUpEvent) {
                    setSelectorWheelState(SELECTOR_WHEEL_STATE_SMALL);
                    showInputControls(mShowInputControlsAnimimationDuration);
                    mInputText.requestFocus();
                    return true;
                }
                VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumFlingVelocity);
                int initialVelocity = (int) velocityTracker.getYVelocity();
                if (Math.abs(initialVelocity) > mMinimumFlingVelocity) {
                    fling(initialVelocity);
                    onScrollStateChange(OnScrollListener.SCROLL_STATE_FLING);
                } else {
                    if (mAdjustScrollerOnUpEvent) {
                        if (mFlingScroller.isFinished() && mAdjustScroller.isFinished()) {
                            postAdjustScrollerCommand(0);
                        }
                    } else {
                        postAdjustScrollerCommand(SHOW_INPUT_CONTROLS_DELAY_MILLIS);
                    }
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                break;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = COMPAT_NEEDED ? event.getAction() : event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mSelectorWheelState == SELECTOR_WHEEL_STATE_LARGE) {
                    removeAllCallbacks();
                    forceCompleteChangeCurrentByOneViaScroll();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                removeAllCallbacks();
                if (mIsLongPressed) {
                	mIsLongPressed = false;
                    forceCompleteChangeCurrentByOneViaScroll();
                    postAdjustScrollerCommand(0);
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            removeAllCallbacks();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent event) {
        int action = COMPAT_NEEDED ? event.getAction() : event.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            removeAllCallbacks();
        }
        return super.dispatchTrackballEvent(event);
    }

    @Override
    public void computeScroll() {
        if (mSelectorWheelState == SELECTOR_WHEEL_STATE_NONE) {
            return;
        }
        Scroller scroller = mFlingScroller;
        if (scroller.isFinished()) {
            scroller = mAdjustScroller;
            if (scroller.isFinished()) {
            	scroller = mLongPressScroller;
                if (scroller.isFinished()) {
                	return;
                }
            }
        }
        scroller.computeScrollOffset();
        int currentScrollerY = scroller.getCurrY();
        if (mPreviousScrollerY == 0) {
            mPreviousScrollerY = scroller.getStartY();
        }
        scrollBy(0, currentScrollerY - mPreviousScrollerY);
        mPreviousScrollerY = currentScrollerY;
        if (scroller.isFinished()) {
            onScrollerFinished(scroller);
        } else {
            invalidate();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIncrementButton.setEnabled(enabled);
        mDecrementButton.setEnabled(enabled);
        mInputText.setEnabled(enabled);
    }

    @Override
    public void scrollBy(int x, int y) {
        if (mSelectorWheelState == SELECTOR_WHEEL_STATE_NONE) {
            return;
        }
        //TODO(dpadgett): fixme
        if (!mWrapSelectorWheel && y > 0
                && mValue <= mMinValue) {
            mCurrentScrollOffset = mInitialScrollOffset;
            return;
        }
        if (!mWrapSelectorWheel && y < 0
                && mValue >= mMaxValue) {
            mCurrentScrollOffset = mInitialScrollOffset;
            return;
        }

        mCurrentScrollOffset += y;

        int totalHeight = mSelectorElementHeight * (mMaxValue - mMinValue + 1);
        mCurrentScrollOffset = mCurrentScrollOffset % totalHeight;
        // clamp to the range (-totalHeight, 0]
        if (mCurrentScrollOffset > 0) {
        	mCurrentScrollOffset -= totalHeight;
        }
        // the current item is the one at getHeight() / 2
        int offsetForCurrent = mCurrentScrollOffset;
        if (offsetForCurrent + totalHeight < getHeight() / 2) {
        	offsetForCurrent += totalHeight;
        }
        int current = ((getHeight() / 2) - offsetForCurrent) / mSelectorElementHeight + mMinValue;
        mValue = current;
    }

    @Override
    public int getSolidColor() {
        return mSolidColor;
    }

    /**
     * Sets the listener to be notified on change of the current value.
     *
     * @param onValueChangedListener The listener.
     */
    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        mOnValueChangeListener = onValueChangedListener;
    }

    /**
     * Set listener to be notified for scroll state changes.
     *
     * @param onScrollListener The listener.
     */
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    /**
     * Set the formatter to be used for formatting the current value.
     * <p>
     * Note: If you have provided alternative values for the values this
     * formatter is never invoked.
     * </p>
     *
     * @param formatter The formatter object. If formatter is <code>null</code>,
     *            {@link String#valueOf(int)} will be used.
     *
     * @see #setDisplayedValues(String[])
     */
    public void setFormatter(Formatter formatter) {
        if (formatter == mFormatter) {
            return;
        }
        mFormatter = formatter;
        initializeSelectorWheelIndices();
        updateInputTextView();
    }

    /**
     * Set the current value for the number picker.
     * <p>
     * If the argument is less than the {@link FasterNumberPicker#getMinValue()} and
     * {@link FasterNumberPicker#getWrapSelectorWheel()} is <code>false</code> the
     * current value is set to the {@link FasterNumberPicker#getMinValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link FasterNumberPicker#getMinValue()} and
     * {@link FasterNumberPicker#getWrapSelectorWheel()} is <code>true</code> the
     * current value is set to the {@link FasterNumberPicker#getMaxValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link FasterNumberPicker#getMaxValue()} and
     * {@link FasterNumberPicker#getWrapSelectorWheel()} is <code>false</code> the
     * current value is set to the {@link FasterNumberPicker#getMaxValue()} value.
     * </p>
     * <p>
     * If the argument is less than the {@link FasterNumberPicker#getMaxValue()} and
     * {@link FasterNumberPicker#getWrapSelectorWheel()} is <code>true</code> the
     * current value is set to the {@link FasterNumberPicker#getMinValue()} value.
     * </p>
     *
     * @param value The current value.
     * @see #setWrapSelectorWheel(boolean)
     * @see #setMinValue(int)
     * @see #setMaxValue(int)
     */
    public void setValue(int value) {
        // this is one of those things that shouldn't matter, but apparently, it does.
    	//if (mValue == value) {
        //    return;
        //}
        //TODO(dpadgett): fix wrapping for delta > 1
        if (value < mMinValue) {
            value = mWrapSelectorWheel ? mMaxValue : mMinValue;
        }
        if (value > mMaxValue) {
            value = mWrapSelectorWheel ? mMinValue : mMaxValue;
        }
        mValue = value;
        updateScrollOffset();
        initializeSelectorWheelIndices();
        updateInputTextView();
        updateIncrementAndDecrementButtonsVisibilityState();
        invalidate();
    }

    public void setDisableInputText(boolean disable) {
    	mDisableInputText = disable;
    	if (mDisableInputText) {
    		mInputText.setVisibility(INVISIBLE);
    		mInputTextVisible = false;
    	}
    }

    private void updateScrollOffset() {
		mCurrentScrollOffset = mInitialScrollOffset - (mValue - mMinValue) * mSelectorElementHeight;
	}

	/**
     * Computes the max width if no such specified as an attribute.
     */
    private void tryComputeMaxWidth() {
        if (!mComputeMaxWidth) {
            return;
        }
        int maxTextWidth = 0;
        if (mDisplayedValues == null) {
            float maxDigitWidth = 0;
            for (int i = 0; i <= 9; i++) {
                final float digitWidth = mSelectorWheelPaint.measureText(String.valueOf(i));
                if (digitWidth > maxDigitWidth) {
                    maxDigitWidth = digitWidth;
                }
            }
            int numberOfDigits = 0;
            int current = mMaxValue;
            while (current > 0) {
                numberOfDigits++;
                current = current / 10;
            }
            maxTextWidth = (int) (numberOfDigits * maxDigitWidth);
        } else {
            final int valueCount = mDisplayedValues.length;
            for (int i = 0; i < valueCount; i++) {
                final float textWidth = mSelectorWheelPaint.measureText(mDisplayedValues[i]);
                if (textWidth > maxTextWidth) {
                    maxTextWidth = (int) textWidth;
                }
            }
        }
        maxTextWidth += mInputText.getPaddingLeft() + mInputText.getPaddingRight();
        if (mMaxWidth != maxTextWidth) {
            if (maxTextWidth > mMinWidth) {
                mMaxWidth = maxTextWidth;
            } else {
                mMaxWidth = mMinWidth;
            }
            invalidate();
        }
    }

    /**
     * Gets whether the selector wheel wraps when reaching the min/max value.
     *
     * @return True if the selector wheel wraps.
     *
     * @see #getMinValue()
     * @see #getMaxValue()
     */
    public boolean getWrapSelectorWheel() {
        return mWrapSelectorWheel;
    }

    /**
     * Sets whether the selector wheel shown during flinging/scrolling should
     * wrap around the {@link FasterNumberPicker#getMinValue()} and
     * {@link FasterNumberPicker#getMaxValue()} values.
     * <p>
     * By default if the range (max - min) is more than five (the number of
     * items shown on the selector wheel) the selector wheel wrapping is
     * enabled.
     * </p>
     *
     * @param wrapSelectorWheel Whether to wrap.
     */
    //TODO(dpadgett): this is a bit broken, seems to get stuck at the extremities
    public void setWrapSelectorWheel(boolean wrapSelectorWheel) {
    	int numTexts = (getBottom() - getTop()) / mTextSize;
        if (wrapSelectorWheel && (mMaxValue - mMinValue) < numTexts) {
            throw new IllegalStateException("Range less than selector items count.");
        }
        if (wrapSelectorWheel != mWrapSelectorWheel) {
            mWrapSelectorWheel = wrapSelectorWheel;
            updateIncrementAndDecrementButtonsVisibilityState();
        }
    }

    /**
     * Sets the speed at which the numbers be incremented and decremented when
     * the up and down buttons are long pressed respectively.
     * <p>
     * The default value is 300 ms.
     * </p>
     *
     * @param intervalMillis The speed (in milliseconds) at which the numbers
     *            will be incremented and decremented.
     */
    public void setOnLongPressUpdateInterval(long intervalMillis) {
        mLongPressUpdateInterval = intervalMillis;
    }

    /**
     * Returns the value of the picker.
     *
     * @return The value.
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Returns the min value of the picker.
     *
     * @return The min value
     */
    public int getMinValue() {
        return mMinValue;
    }

    /**
     * Sets the min value of the picker.
     *
     * @param minValue The min value.
     */
    public void setMinValue(int minValue) {
        if (mMinValue == minValue) {
            return;
        }
        if (minValue < 0) {
            throw new IllegalArgumentException("minValue must be >= 0");
        }
        mMinValue = minValue;
        if (mMinValue > mValue) {
            mValue = mMinValue;
        }
        setWrapSelectorWheel(mWrapSelectorWheel);
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
    }

    /**
     * Returns the max value of the picker.
     *
     * @return The max value.
     */
    public int getMaxValue() {
        return mMaxValue;
    }

    /**
     * Sets the max value of the picker.
     *
     * @param maxValue The max value.
     */
    public void setMaxValue(int maxValue) {
        if (mMaxValue == maxValue) {
            return;
        }
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue must be >= 0");
        }
        mMaxValue = maxValue;
        if (mMaxValue < mValue) {
            mValue = mMaxValue;
        }
        setWrapSelectorWheel(mWrapSelectorWheel);
        initializeSelectorWheelIndices();
        updateInputTextView();
        tryComputeMaxWidth();
    }

    /**
     * Gets the values to be displayed instead of string values.
     *
     * @return The displayed values.
     */
    public String[] getDisplayedValues() {
        return mDisplayedValues;
    }

    /**
     * Sets the values to be displayed.
     *
     * @param displayedValues The displayed values.
     */
    public void setDisplayedValues(String[] displayedValues) {
        if (mDisplayedValues == displayedValues) {
            return;
        }
        mDisplayedValues = displayedValues;
        if (mDisplayedValues != null) {
            // Allow text entry rather than strictly numeric entry.
            mInputText.setRawInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        } else {
            mInputText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
        }
        updateInputTextView();
        initializeSelectorWheelIndices();
        tryComputeMaxWidth();
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        return TOP_AND_BOTTOM_FADING_EDGE_STRENGTH;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // make sure we show the controls only the very
        // first time the user sees this widget
        if (mFlingable && !isInEditMode()) {
            // animate a bit slower the very first time
            showInputControls(mShowInputControlsAnimimationDuration * 2);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeAllCallbacks();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // There is a good reason for doing this. See comments in draw().
    }

    @Override
    public void draw(Canvas canvas) {
        // Dispatch draw to our children only if we are not currently running
        // the animation for simultaneously dimming the scroll wheel and
        // showing in the buttons. This class takes advantage of the View
        // implementation of fading edges effect to draw the selector wheel.
        // However, in View.draw(), the fading is applied after all the children
        // have been drawn and we do not want this fading to be applied to the
        // buttons. Therefore, we draw our children after we have completed
        // drawing ourselves.
        super.draw(canvas);

        // Draw our children if we are not showing the selector wheel or fading
        // it out
        if (/*mShowInputControlsAnimator.isRunning()
                || */ mSelectorWheelState != SELECTOR_WHEEL_STATE_LARGE) {
            long drawTime = getDrawingTime();
            for (int i = 0, count = getChildCount(); i < count; i++) {
                View child = getChildAt(i);
                if (!child.isShown()) {
                    continue;
                }
                drawChild(canvas, getChildAt(i), drawTime);
            }
        }
    }

    private class TileLoader implements LRUCache.Loader<Integer, Bitmap> {
		@Override
		public Bitmap load(Integer idx, Bitmap unusedValue) {
			Log.i(getClass().getName(), "Loading index " + idx);
	        int totalHeight = (mMaxValue - mMinValue + 1) * mSelectorElementHeight;
	    	//int bitmapHeight = Math.min(2048 - (2048 % mSelectorElementHeight), totalHeight);
	        int bitmapHeight = Math.min(512 - (512 % mSelectorElementHeight), totalHeight);
    		int height = Math.min(totalHeight - (idx * bitmapHeight), bitmapHeight);

	        Bitmap bitmap;
			if (unusedValue == null ||
					unusedValue.getHeight() != height) {
				bitmap = Bitmap.createBitmap(getWidth(),
            			height, Config.ARGB_8888);
			} else {
				bitmap = unusedValue;
        		bitmap.eraseColor(Color.TRANSPARENT);
			}
        	Paint paint = new Paint(mSelectorWheelPaint);
        	paint.setAlpha(255);
        	bumpOffset = mSelectorTextGapHeight;
            float x = (getRight() - getLeft()) / 2;
    		float y = mSelectorElementHeight - bumpOffset;
        	int itemsPerBitmap = bitmapHeight / mSelectorElementHeight;
        	Canvas newCanvas = new Canvas(bitmap);
	        for (int i = mMinValue + itemsPerBitmap * idx; i <= Math.min(mMaxValue, mMinValue + itemsPerBitmap * (idx + 1) - 1); i++) {
	        	ensureCachedScrollSelectorValue(i);
	            String scrollSelectorValue = mSelectorIndexToStringCache.get(i);
                newCanvas.drawText(scrollSelectorValue, x, y, paint);
	            y += mSelectorElementHeight;
	        }
			return bitmap;
		}
    }
    
    private int lastHashCode = 0;
    private int lastMetaHashCode = 0;
    private int lastFakeInputHashCode = 0;
	private Bitmap metaSaved;
	private Bitmap fakeInputSaved;
	private int bumpOffset = 0;
	private LRUCache<Integer, Bitmap> savedCache = new LRUCache<Integer, Bitmap>(new TileLoader(), 3);
    @Override
    protected void onDraw(Canvas canvas) {
        if (mSelectorWheelState == SELECTOR_WHEEL_STATE_NONE) {
            return;
        }

        float x = (getRight() - getLeft()) / 2;

        final int restoreCount = canvas.save();

        //Rect clipBounds = canvas.getClipBounds();
        if (mSelectorWheelState == SELECTOR_WHEEL_STATE_SMALL) {
            //clipBounds.inset(0, mSelectorElementHeight / 2);
            //canvas.clipRect(clipBounds);
        }

        // draw the selector wheel

        int totalHeight = (mMaxValue - mMinValue + 1) * mSelectorElementHeight;
    	//int bitmapHeight = Math.min(2048 - (2048 % mSelectorElementHeight), totalHeight);
        int bitmapHeight = Math.min(512 - (512 % mSelectorElementHeight), totalHeight);

    	int hashCode = Arrays.hashCode(new int[] {
        		mMinValue,
        		mMaxValue,
        		mSelectorElementHeight});
        if (hashCode != lastHashCode) {
        	lastHashCode = hashCode;
			savedCache.clear();
        }


        // second layer of caching
    	int metaHashCode = Arrays.hashCode(new int[] {
        		mCurrentScrollOffset,
        		mInputText.getVisibility(),
        		mInputTextVisible ? 0 : 1,
        		mWrapSelectorWheel ? 0 : 1,
        		hashCode});
    	if (metaHashCode != lastMetaHashCode) {
    		lastMetaHashCode = metaHashCode;
    		if (metaSaved == null) {
    			metaSaved = Bitmap.createBitmap(getWidth(),
            			getHeight(), Config.ARGB_8888);
    		} else {
    			metaSaved.eraseColor(Color.TRANSPARENT);
    		}
        	// this is applied when the bitmap is drawn, too
        	Paint paint = new Paint(mSelectorWheelPaint);
        	paint.setAlpha(255);
    		Canvas metaCanvas = new Canvas(metaSaved);
    		
    		final int currentScrollOffset = mCurrentScrollOffset + bumpOffset;
    		
	        // offset for the 0th bitmap
	        int offset = currentScrollOffset;
	        // this is a bit inefficient
        	int numTiles = (int) Math.ceil(((double) totalHeight) / bitmapHeight);
	        if (mWrapSelectorWheel) {
		        if (offset > 0) {
		        	// in this case we would be missing parts on the top, so ensure they are drawn.
		        	offset -= (mMaxValue - mMinValue + 1) * mSelectorElementHeight;
		        }
		        for(int idx = 0;
		        		offset < getHeight();
		        		offset += getBitmapHeight(idx), idx = (idx + 1) % numTiles) {
		        	int top = offset;
		        	int bottom = offset + getBitmapHeight(idx);
		        	if (!(bottom < 0 || top > getHeight())) {
		        		metaCanvas.drawBitmap(savedCache.get(idx), 0, offset, paint);
		        	}
		        }
	        } else {
		        for(int idx = 0;
		        		offset < getHeight() && idx < numTiles;
		        		offset += savedCache.get(idx).getHeight(), idx++) {
		        	int top = offset;
		        	int bottom = offset + savedCache.get(idx).getHeight();
		        	if (!(bottom < 0 || top > getHeight())) {
		        		metaCanvas.drawBitmap(savedCache.get(idx), 0, offset, paint);
		        	}
		        }
	        }
	        if (mInputTextVisible) {
	        	Paint clearPaint = new Paint(mSelectorWheelPaint);
	        	clearPaint.setColor(0x00000000);
	        	clearPaint.setAlpha(0);
	        	clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
	        	metaCanvas.drawRect(new Rect(0,
	        			mSelectorElementHeight * mSelectorMiddleItemIndex,
	        			getWidth(),
	        			mSelectorElementHeight * (mSelectorMiddleItemIndex + 1)), clearPaint);
	        }

        	// re-implementation of the fading edge effect, since it's horribly slow
            final int left = 0;
            final int top = 0;
            final int right = getWidth();
            final int bottom = getHeight();

            boolean drawTop = false;
            boolean drawBottom = false;

            float topFadeStrength = 0.0f;
            float bottomFadeStrength = 0.0f;

            mScrollCache.fadingEdgeLength = (getHeight() - mTextSize) / 2;
            final ScrollabilityCache scrollabilityCache = mScrollCache;
            final float fadeHeight = scrollabilityCache.fadingEdgeLength;
            int length = (int) fadeHeight;
            
            // clip the fade length if top and bottom fades overlap
            // overlapping fades produce odd-looking artifacts
            if ((top + length > bottom - length)) {
                length = (bottom - top) / 2;
            }

            topFadeStrength = 1.0f;
            drawTop = topFadeStrength * fadeHeight > 1.0f;
            bottomFadeStrength = 1.0f;
            drawBottom = bottomFadeStrength * fadeHeight > 1.0f;

            final Paint p = scrollabilityCache.paint;
            final Matrix matrix = scrollabilityCache.matrix;
            final Shader fade = scrollabilityCache.shader;
            
            if (drawTop) {
                matrix.setScale(1, fadeHeight * topFadeStrength);
                matrix.postTranslate(left, top);
                fade.setLocalMatrix(matrix);
                metaCanvas.drawRect(left, top, right, top + length, p);
            }

            if (drawBottom) {
                matrix.setScale(1, fadeHeight * bottomFadeStrength);
                matrix.postRotate(180);
                matrix.postTranslate(left, bottom);
                fade.setLocalMatrix(matrix);
                metaCanvas.drawRect(left, bottom - length, right, bottom, p);
            }
    	}
    	
    	canvas.drawBitmap(metaSaved, 0, 0, mSelectorWheelPaint);

        if (mInputTextVisible && mDisableInputText) {
        	// manually draw the full alpha middle value
        	Paint fullAlpha = new Paint(mSelectorWheelPaint);
        	fullAlpha.setAlpha(255);
        	int fakeInputHashCode = Arrays.hashCode(new int[] {
        		mValue,
        		mSelectorElementHeight
        	});
        	if (fakeInputHashCode != lastFakeInputHashCode) {
        		lastFakeInputHashCode = fakeInputHashCode;
        		int height = mInputText.getHeight();
        		if (fakeInputSaved == null ||
        				fakeInputSaved.getHeight() != height) {
        			fakeInputSaved = Bitmap.createBitmap(getWidth(),
                			height, Config.ARGB_8888);
        		} else {
        			fakeInputSaved.eraseColor(Color.TRANSPARENT);
        		}
        		Canvas newCanvas = new Canvas(fakeInputSaved);
            	// draw current text with full alpha
            	ensureCachedScrollSelectorValue(mValue);
                String scrollSelectorValue = mSelectorIndexToStringCache.get(mValue);
                newCanvas.drawText(scrollSelectorValue, x, mInputText.getBaseline(), fullAlpha);
        	}
        	canvas.drawBitmap(fakeInputSaved, 0, mInputText.getTop(), fullAlpha);
        }
    	
        // draw the selection dividers (only if scrolling and drawable specified)
        if (mSelectionDivider != null) {
            // draw the top divider
            int topOfTopDivider =
                (getHeight() - mSelectorElementHeight - mSelectionDividerHeight) / 2;
            int bottomOfTopDivider = topOfTopDivider + mSelectionDividerHeight;
            mSelectionDivider.setBounds(0, topOfTopDivider, getRight(), bottomOfTopDivider);
            mSelectionDivider.draw(canvas);

            // draw the bottom divider
            int topOfBottomDivider =  topOfTopDivider + mSelectorElementHeight;
            int bottomOfBottomDivider = bottomOfTopDivider + mSelectorElementHeight;
            mSelectionDivider.setBounds(0, topOfBottomDivider, getRight(), bottomOfBottomDivider);
            mSelectionDivider.draw(canvas);
        }

        canvas.restoreToCount(restoreCount);
    }

    private int getBitmapHeight(int idx) {
    	int totalHeight = (mMaxValue - mMinValue + 1) * mSelectorElementHeight;
    	//int bitmapHeight = Math.min(2048 - (2048 % mSelectorElementHeight), totalHeight);
        int bitmapHeight = Math.min(512 - (512 % mSelectorElementHeight), totalHeight);
        int height = Math.min(totalHeight - (idx * bitmapHeight), bitmapHeight);
        return height;
	}

	@Override
    public void sendAccessibilityEvent(int eventType) {
        // Do not send accessibility events - we want the user to
        // perceive this widget as several controls rather as a whole.
    }

    /**
     * Makes a measure spec that tries greedily to use the max value.
     *
     * @param measureSpec The measure spec.
     * @param maxSize The max value for the size.
     * @return A measure spec greedily imposing the max size.
     */
    private int makeMeasureSpec(int measureSpec, int maxSize) {
        if (maxSize == SIZE_UNSPECIFIED) {
            return measureSpec;
        }
        final int size = MeasureSpec.getSize(measureSpec);
        final int mode = MeasureSpec.getMode(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                return measureSpec;
            case MeasureSpec.AT_MOST:
                return MeasureSpec.makeMeasureSpec(Math.min(size, maxSize), MeasureSpec.EXACTLY);
            case MeasureSpec.UNSPECIFIED:
                return MeasureSpec.makeMeasureSpec(maxSize, MeasureSpec.EXACTLY);
            default:
                throw new IllegalArgumentException("Unknown measure mode: " + mode);
        }
    }

    /**
     * Utility to reconcile a desired size and state, with constraints imposed by
     * a MeasureSpec. Tries to respect the min size, unless a different size is
     * imposed by the constraints.
     *
     * @param minSize The minimal desired size.
     * @param measuredSize The currently measured size.
     * @param measureSpec The current measure spec.
     * @return The resolved size and state.
     */
    private int resolveSizeAndStateRespectingMinSize(int minSize, int measuredSize,
            int measureSpec) {
        if (minSize != SIZE_UNSPECIFIED) {
            final int desiredWidth = Math.max(minSize, measuredSize);
            return resolveSizeAndState(desiredWidth, measureSpec, 0);
        } else {
            return measuredSize;
        }
    }

    /**
     * Resets the selector indices and clear the cached
     * string representation of these indices.
     */
    private void initializeSelectorWheelIndices() {
        mSelectorIndexToStringCache.clear();
        for (int i = mMinValue; i < mMaxValue; i++) {
            ensureCachedScrollSelectorValue(i);
        }
    }

    /**
     * Sets the current value of this NumberPicker, and sets mPrevious to the
     * previous value. If current is greater than mEnd less than mStart, the
     * value of mCurrent is wrapped around. Subclasses can override this to
     * change the wrapping behavior
     *
     * @param current the new value of the NumberPicker
     */
    private void changeCurrent(int current) {
        if (mValue == current) {
            return;
        }
        // Wrap around the values if we go past the start or end
        if (mWrapSelectorWheel) {
            current = getWrappedSelectorIndex(current);
        }
        int previous = mValue;
        setValue(current);
        notifyChange(previous, current);
    }

    /**
     * Changes the current value by one which is increment or
     * decrement based on the passes argument.
     *
     * @param increment True to increment, false to decrement.
     */
    private void changeCurrentByOne(boolean increment) {
        if (mFlingable) {
        	cancelDim();
            //mDimSelectorWheelAnimator.cancel();
            mInputText.setVisibility(View.INVISIBLE);
            mInputTextVisible = false;
            mSelectorWheelPaint.setAlpha(SELECTOR_WHEEL_BRIGHT_ALPHA);
            mPreviousScrollerY = 0;
            forceCompleteChangeCurrentByOneViaScroll();
            if (increment) {
                mFlingScroller.startScroll(0, 0, 0, -mSelectorElementHeight,
                        CHANGE_CURRENT_BY_ONE_SCROLL_DURATION);
            } else {
                mFlingScroller.startScroll(0, 0, 0, mSelectorElementHeight,
                        CHANGE_CURRENT_BY_ONE_SCROLL_DURATION);
            }
            invalidate();
        } else {
            if (increment) {
                changeCurrent(mValue + 1);
            } else {
                changeCurrent(mValue - 1);
            }
        }
    }

    /**
     * Ensures that if we are in the process of changing the current value
     * by one via scrolling the scroller gets to its final state and the
     * value is updated.
     */
    private void forceCompleteChangeCurrentByOneViaScroll() {
        Scroller scroller = mFlingScroller;
        if (!scroller.isFinished()) {
            final int yBeforeAbort = scroller.getCurrY();
            scroller.abortAnimation();
            final int yDelta = scroller.getCurrY() - yBeforeAbort;
            scrollBy(0, yDelta);
        }

        scroller = mLongPressScroller;
        if (!scroller.isFinished()) {
        	// for the long press scroller, we set the final y to be twice as far
        	// as it truly should, to prevent jitter when reposting the runnable.
        	// so here, we subtract a full item's length from the final y before
        	// forcing the scroll.
            final int yBeforeAbort = scroller.getCurrY();
            int finalY = scroller.getFinalY();
            if (finalY > yBeforeAbort) {
            	finalY -= mSelectorElementHeight;
            	finalY = Math.max(finalY, yBeforeAbort);
            } else {
            	finalY += mSelectorElementHeight;
            	finalY = Math.min(finalY, yBeforeAbort);
            }
            // scroller.setFinalY(finalY);
            scroller.setFinalY(yBeforeAbort);
            scroller.abortAnimation();
            // final int yDelta = scroller.getCurrY() - yBeforeAbort;
            // scrollBy(0, yDelta);
        }
    }

    /**
     * Sets the <code>alpha</code> of the {@link Paint} for drawing the selector
     * wheel.
     */
    @SuppressWarnings("unused")
    // Called via reflection
    private void setSelectorPaintAlpha(int alpha) {
        mSelectorWheelPaint.setAlpha(alpha);
        invalidate();
    }

    /**
     * @return If the <code>event</code> is in the visible <code>view</code>.
     */
    private boolean isEventInVisibleViewHitRect(MotionEvent event, View view) {
        if (view.getVisibility() == VISIBLE) {
            view.getHitRect(mTempRect);
            return mTempRect.contains((int) event.getX(), (int) event.getY());
        }
        return false;
    }

    /**
     * Sets the <code>selectorWheelState</code>.
     */
    private void setSelectorWheelState(int selectorWheelState) {
        mSelectorWheelState = selectorWheelState;
        if (selectorWheelState == SELECTOR_WHEEL_STATE_LARGE) {
            mSelectorWheelPaint.setAlpha(SELECTOR_WHEEL_BRIGHT_ALPHA);
        }

        if (mFlingable && selectorWheelState == SELECTOR_WHEEL_STATE_LARGE
                && ((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled()) {
        	((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).interrupt();
            String text = getContext().getString(R.string.number_picker_increment_scroll_action);
            mInputText.setContentDescription(text);
            mInputText.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
            mInputText.setContentDescription(null);
        }
    }

    private void initializeSelectorWheel() {
        int height = getBottom() - getTop();
        // mSelectorTextGapHeight must be > 0.  i.e. we should not have overlapping texts.
        int numTexts = height / mTextSize;
        // must be odd
        if (numTexts % 2 == 0) {
        	numTexts--;
        }
        mSelectorMiddleItemIndex = numTexts / 2;
        
        initializeSelectorWheelIndices();

        int totalTextHeight = (numTexts) * mTextSize;
        float totalTextGapHeight = (getBottom() - getTop()) - totalTextHeight;
        float textGapCount = numTexts - 1;
        mSelectorTextGapHeight = (int) (totalTextGapHeight / textGapCount + 0.5f);
        mSelectorElementHeight = mTextSize + mSelectorTextGapHeight;
        // Ensure that the middle item is positioned the same as the text in mInputText
        int editTextTextPosition = mInputText.getBaseline() + mInputText.getTop();
        mInitialScrollOffset = editTextTextPosition - mSelectorElementHeight;
        // mCurrentScrollOffset = mInitialScrollOffset;
        updateScrollOffset();
        updateInputTextView();
        
        // force scroll wheel refresh
        lastMetaHashCode = 0;
    }

    private void initializeFadingEdges() {
        //setVerticalFadingEdgeEnabled(true);
        //setFadingEdgeLength((getBottom() - getTop() - mTextSize) / 2);
    }

    /**
     * Callback invoked upon completion of a given <code>scroller</code>.
     */
    private void onScrollerFinished(Scroller scroller) {
        if (scroller == mFlingScroller) {
            if (mSelectorWheelState == SELECTOR_WHEEL_STATE_LARGE) {
                postAdjustScrollerCommand(0);
                onScrollStateChange(OnScrollListener.SCROLL_STATE_IDLE);
            } else {
                updateInputTextView();
                fadeSelectorWheel(mShowInputControlsAnimimationDuration);
            }
        } else if (scroller == mLongPressScroller) {
        	// adjust scroller command is posted on long press up.
        } else {
            updateInputTextView();
            showInputControls(mShowInputControlsAnimimationDuration);
        }
    }

    /**
     * Handles transition to a given <code>scrollState</code>
     */
    private void onScrollStateChange(int scrollState) {
        if (mScrollState == scrollState) {
            return;
        }
        mScrollState = scrollState;
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChange(this, scrollState);
        }
    }

    /**
     * Flings the selector with the given <code>velocityY</code>.
     */
    private void fling(int velocityY) {
        mPreviousScrollerY = 0;

        if (velocityY > 0) {
            mFlingScroller.fling(0, 0, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        } else {
            mFlingScroller.fling(0, Integer.MAX_VALUE, 0, velocityY, 0, 0, 0, Integer.MAX_VALUE);
        }

        invalidate();
    }

    /**
     * Hides the input controls which is the up/down arrows and the text field.
     */
    private void hideInputControls() {
        cancelDim();
        //mShowInputControlsAnimator.cancel();
        mIncrementButton.setVisibility(INVISIBLE);
        mDecrementButton.setVisibility(INVISIBLE);
        mInputText.setVisibility(INVISIBLE);
        mInputTextVisible = false;
    }

    /**
     * Show the input controls by making them visible and animating the alpha
     * property up/down arrows.
     *
     * @param animationDuration The duration of the animation.
     */
    private void showInputControls(long animationDuration) {
        updateIncrementAndDecrementButtonsVisibilityState();
        if (!mDisableInputText) {
        	mInputText.setVisibility(VISIBLE);
        }
        mInputTextVisible = true;
    	setSelectorPaintAlpha(SELECTOR_WHEEL_DIM_ALPHA);
        setSelectorWheelState(SELECTOR_WHEEL_STATE_SMALL);
        //mShowInputControlsAnimator.setDuration(animationDuration);
        //mShowInputControlsAnimator.start();
    }

    /**
     * Fade the selector wheel via an animation.
     *
     * @param animationDuration The duration of the animation.
     */
    private void fadeSelectorWheel(long animationDuration) {
    	//if (!mDisableInputText) {
    	//  mInputText.setVisibility(VISIBLE);
    	//}
    	//mInputTextVisible = true;
    	//setSelectorPaintAlpha(SELECTOR_WHEEL_DIM_ALPHA);
    	showInputControls(animationDuration);
    //    mDimSelectorWheelAnimator.setDuration(animationDuration);
    //    mDimSelectorWheelAnimator.start();
    }

    /**
     * Updates the visibility state of the increment and decrement buttons.
     */
    private void updateIncrementAndDecrementButtonsVisibilityState() {
        if (mWrapSelectorWheel || mValue < mMaxValue) {
            mIncrementButton.setVisibility(VISIBLE);
        } else {
            mIncrementButton.setVisibility(INVISIBLE);
        }
        if (mWrapSelectorWheel || mValue > mMinValue) {
            mDecrementButton.setVisibility(VISIBLE);
        } else {
            mDecrementButton.setVisibility(INVISIBLE);
        }
    }

    /**
     * @return The wrapped index <code>selectorIndex</code> value.
     */
    private int getWrappedSelectorIndex(int selectorIndex) {
        if (selectorIndex > mMaxValue) {
            return mMinValue + (selectorIndex - mMaxValue) % (mMaxValue - mMinValue) - 1;
        } else if (selectorIndex < mMinValue) {
            return mMaxValue - (mMinValue - selectorIndex) % (mMaxValue - mMinValue) + 1;
        }
        return selectorIndex;
    }

    /**
     * Ensures we have a cached string representation of the given <code>
     * selectorIndex</code>
     * to avoid multiple instantiations of the same string.
     */
    private void ensureCachedScrollSelectorValue(int selectorIndex) {
        SparseArray<String> cache = mSelectorIndexToStringCache;
        String scrollSelectorValue = cache.get(selectorIndex);
        if (scrollSelectorValue != null) {
            return;
        }
        if (selectorIndex < mMinValue || selectorIndex > mMaxValue) {
            scrollSelectorValue = "";
        } else {
            if (mDisplayedValues != null) {
                int displayedValueIndex = selectorIndex - mMinValue;
                scrollSelectorValue = mDisplayedValues[displayedValueIndex];
            } else {
                scrollSelectorValue = formatNumber(selectorIndex);
            }
        }
        cache.put(selectorIndex, scrollSelectorValue);
    }

    private String formatNumber(int value) {
        return (mFormatter != null) ? mFormatter.format(value) : String.valueOf(value);
    }

    private void validateInputTextView(View v) {
        String str = String.valueOf(((TextView) v).getText());
        if (TextUtils.isEmpty(str)) {
            // Restore to the old value as we don't allow empty values
            updateInputTextView();
        } else {
            // Check the new value and ensure it's in range
            int current = getSelectedPos(str.toString());
            changeCurrent(current);
        }
    }

    /**
     * Updates the view of this NumberPicker. If displayValues were specified in
     * the string corresponding to the index specified by the current value will
     * be returned. Otherwise, the formatter specified in {@link #setFormatter}
     * will be used to format the number.
     */
    private void updateInputTextView() {
        /*
         * If we don't have displayed values then use the current number else
         * find the correct value in the displayed values for the current
         * number.
         */
        if (mDisplayedValues == null) {
            mInputText.setText(formatNumber(mValue));
        } else {
            mInputText.setText(mDisplayedValues[mValue - mMinValue]);
        }
        mInputText.setSelection(mInputText.getText().length());

        if (mFlingable && ((AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled()) {
            String text = getContext().getString(R.string.number_picker_increment_scroll_mode,
                    mInputText.getText());
            mInputText.setContentDescription(text);
        }
    }

    /**
     * Notifies the listener, if registered, of a change of the value of this
     * NumberPicker.
     */
    private void notifyChange(int previous, int current) {
        if (mOnValueChangeListener != null) {
            mOnValueChangeListener.onValueChange(this, previous, mValue);
        }
    }

    /**
     * Posts a command for changing the current value by one.
     *
     * @param increment Whether to increment or decrement the value.
     */
    private void postChangeCurrentByOneFromLongPress(boolean increment) {
        mInputText.clearFocus();
        removeAllCallbacks();
        if (mChangeCurrentByOneFromLongPressCommand == null) {
            mChangeCurrentByOneFromLongPressCommand = new ChangeCurrentByOneFromLongPressCommand();
        }
        mChangeCurrentByOneFromLongPressCommand.setIncrement(increment);
        post(mChangeCurrentByOneFromLongPressCommand);
    }

    /**
     * Removes all pending callback from the message queue.
     */
    private void removeAllCallbacks() {
        if (mChangeCurrentByOneFromLongPressCommand != null) {
            removeCallbacks(mChangeCurrentByOneFromLongPressCommand);
        }
        if (mAdjustScrollerCommand != null) {
            removeCallbacks(mAdjustScrollerCommand);
        }
        if (mSetSelectionCommand != null) {
            removeCallbacks(mSetSelectionCommand);
        }
    }

    /**
     * @return The selected index given its displayed <code>value</code>.
     */
    private int getSelectedPos(String value) {
        if (mDisplayedValues == null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Ignore as if it's not a number we don't care
            }
        } else {
            for (int i = 0; i < mDisplayedValues.length; i++) {
                // Don't force the user to type in jan when ja will do
                value = value.toLowerCase();
                if (mDisplayedValues[i].toLowerCase().startsWith(value)) {
                    return mMinValue + i;
                }
            }

            /*
             * The user might have typed in a number into the month field i.e.
             * 10 instead of OCT so support that too.
             */
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {

                // Ignore as if it's not a number we don't care
            }
        }
        return mMinValue;
    }

    /**
     * Posts an {@link SetSelectionCommand} from the given <code>selectionStart
     * </code> to
     * <code>selectionEnd</code>.
     */
    private void postSetSelectionCommand(int selectionStart, int selectionEnd) {
        if (mSetSelectionCommand == null) {
            mSetSelectionCommand = new SetSelectionCommand();
        } else {
            removeCallbacks(mSetSelectionCommand);
        }
        mSetSelectionCommand.mSelectionStart = selectionStart;
        mSetSelectionCommand.mSelectionEnd = selectionEnd;
        post(mSetSelectionCommand);
    }

    /**
     * Posts an {@link AdjustScrollerCommand} within the given <code>
     * delayMillis</code>
     * .
     */
    private void postAdjustScrollerCommand(int delayMillis) {
        if (mAdjustScrollerCommand == null) {
            mAdjustScrollerCommand = new AdjustScrollerCommand();
        } else {
            removeCallbacks(mAdjustScrollerCommand);
        }
        postDelayed(mAdjustScrollerCommand, delayMillis);
    }

    /**
     * Filter for accepting only valid indices or prefixes of the string
     * representation of valid indices.
     */
    class InputTextFilter extends NumberKeyListener {

        // XXX This doesn't allow for range limits when controlled by a
        // soft input method!
        public int getInputType() {
            return InputType.TYPE_CLASS_TEXT;
        }

        @Override
        protected char[] getAcceptedChars() {
            return DIGIT_CHARACTERS;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                int dstart, int dend) {
            if (mDisplayedValues == null) {
                CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
                if (filtered == null) {
                    filtered = source.subSequence(start, end);
                }

                String result = String.valueOf(dest.subSequence(0, dstart)) + filtered
                        + dest.subSequence(dend, dest.length());

                if ("".equals(result)) {
                    return result;
                }
                int val = getSelectedPos(result);

                /*
                 * Ensure the user can't type in a value greater than the max
                 * allowed. We have to allow less than min as the user might
                 * want to delete some numbers and then type a new number.
                 */
                if (val > mMaxValue) {
                    return "";
                } else {
                    return filtered;
                }
            } else {
                CharSequence filtered = String.valueOf(source.subSequence(start, end));
                if (TextUtils.isEmpty(filtered)) {
                    return "";
                }
                String result = String.valueOf(dest.subSequence(0, dstart)) + filtered
                        + dest.subSequence(dend, dest.length());
                String str = String.valueOf(result).toLowerCase();
                for (String val : mDisplayedValues) {
                    String valLowerCase = val.toLowerCase();
                    if (valLowerCase.startsWith(str)) {
                        postSetSelectionCommand(result.length(), val.length());
                        return val.subSequence(dstart, val.length());
                    }
                }
                return "";
            }
        }
    }

    /**
     * Command for setting the input text selection.
     */
    class SetSelectionCommand implements Runnable {
        private int mSelectionStart;

        private int mSelectionEnd;

        public void run() {
            mInputText.setSelection(mSelectionStart, mSelectionEnd);
        }
    }

    /**
     * Command for adjusting the scroller to show in its center the closest of
     * the displayed items.
     */
    class AdjustScrollerCommand implements Runnable {
        public void run() {
            mPreviousScrollerY = 0;
            int totalHeight = mSelectorElementHeight * (mMaxValue - mMinValue + 1);
            // the current item should already be set correctly by scrollBy.
            int correctOffset = mInitialScrollOffset - (mValue - mMinValue) * mSelectorElementHeight;
            correctOffset = correctOffset % totalHeight;
            if (correctOffset > 0) {
            	correctOffset -= totalHeight;
            }
            if (correctOffset == mCurrentScrollOffset) {
                updateInputTextView();
                showInputControls(mShowInputControlsAnimimationDuration);
                return;
            }
            // adjust to the closest value
            int deltaY = correctOffset - mCurrentScrollOffset;
            // special case for wrapping around
            if (Math.abs(deltaY) > totalHeight / 2) {
            	if (deltaY > 0) {
            		deltaY -= totalHeight;
            	} else {
            		deltaY += totalHeight;
            	}
            }
            if (Math.abs(deltaY) > mSelectorElementHeight / 2) {
                deltaY += (deltaY > 0) ? -mSelectorElementHeight : mSelectorElementHeight;
            }
            mAdjustScroller.startScroll(0, 0, 0, deltaY, SELECTOR_ADJUSTMENT_DURATION_MILLIS);
            invalidate();
        }
    }

    /**
     * Command for changing the current value from a long press by one.
     */
    class ChangeCurrentByOneFromLongPressCommand implements Runnable {
        private boolean mIncrement;

        private void setIncrement(boolean increment) {
            mIncrement = increment;
        }

        public void run() {
            //changeCurrentByOne(mIncrement);
            if (mFlingable) {
            	cancelDim();
                mInputText.setVisibility(View.INVISIBLE);
                mInputTextVisible = false;
                mSelectorWheelPaint.setAlpha(SELECTOR_WHEEL_BRIGHT_ALPHA);
                mPreviousScrollerY = 0;
                if (mIncrement) {
                	mLongPressScroller.setFinalY(mLongPressScroller.getCurrY());
                	mLongPressScroller.forceFinished(true);
                    mLongPressScroller.startScroll(0, 0, 0, -mSelectorElementHeight * 2,
                    		(int) mLongPressUpdateInterval * 2);
                } else {
                	mLongPressScroller.setFinalY(mLongPressScroller.getCurrY());
                	mLongPressScroller.forceFinished(true);
                	mLongPressScroller.startScroll(0, 0, 0, mSelectorElementHeight * 2,
                			(int) mLongPressUpdateInterval * 2);
                }
                invalidate();
            } else {
                if (mIncrement) {
                    changeCurrent(mValue + 1);
                } else {
                    changeCurrent(mValue - 1);
                }
            }
            postDelayed(this, mLongPressUpdateInterval);
        }
    }
}
