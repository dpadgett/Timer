/*  
 * Copyright 2012 Dan Padgett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
				// Log.i(getClass().getName(), "Time text too wide, shrinking...");
				setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize() * maxWidth / totalWidth);
				// Log.i(getClass().getName(), "Changed textWidth from " + totalWidth + " with max of " + maxWidth);
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
