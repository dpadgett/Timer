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

package org.dpadgett.timer;

import java.util.ArrayList;
import java.util.List;

import org.dpadgett.compat.LinearLayout;
import org.dpadgett.compat.LinearLayout.OnLayoutChangeListener;
import org.dpadgett.compat.Space;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Class to encapsulate the functionality and logic for the lap times
 * list.
 * 
 * @author dpadgett
 */
public class LapTimes {

    private final ScrollView scrollView;
    private final LinearLayout lapTimesView;
    private final List<Long> lapTimes;
    private final OnLayoutChangeListener bottomScroller;
    private final Context context;
    private int lastLapTimesViewHeight;

    public LapTimes(final ScrollView scrollView) {
        this.scrollView = scrollView;
        this.lapTimesView = (LinearLayout) scrollView.findViewById(R.id.lapTimesView);
        this.context = scrollView.getContext();

        lastLapTimesViewHeight = lapTimesView.getMeasuredHeight();
        bottomScroller = new OnLayoutChangeListener() {

            @Override
            public void onLayoutChange(final View v,
                    final int left,
                    final int top,
                    final int right,
                    final int bottom,
                    final int oldLeft,
                    final int oldTop,
                    final int oldRight,
                    final int oldBottom) {
                if (v.getMeasuredHeight() != lastLapTimesViewHeight) {
                    lastLapTimesViewHeight = v.getMeasuredHeight();
                    LapTimes.this.scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        };
        lapTimesView.addOnLayoutChangeListener(bottomScroller);
        lapTimesView.setDividerDrawable(new ListView(lapTimesView.getContext()).getDivider());
        lapTimes = new ArrayList<Long>();
    }

    /**
     * Add this lap time to the list of lap times.
     * 
     * @param lapTime
     *            the time to add
     */
    public void add(final long lapTime) {
        // unhack
        if (lapTimesView.getChildCount() > 0) {
            lapTimesView.removeViewAt(lapTimesView.getChildCount() - 1);
        }

        final android.widget.LinearLayout lapLayout = (android.widget.LinearLayout) LayoutInflater.from(
                lapTimesView.getContext()).inflate(R.layout.single_lap_time, lapTimesView, false);

        final TextView lapLabel = (TextView) lapLayout.findViewById(R.id.lapLabel);
        lapLabel.setText(context.getString(R.string.lap_times_lap_label) + " " + (lapTimes.size() + 1));

        final TextView lapTimeView = (TextView) lapLayout.findViewById(R.id.lapTime);
        lapTimeView.setText(getTimerText(lapTime));

        lapTimesView.addView(lapLayout);

        // hack to get it to draw the lower divider
        final Space space = new Space(lapTimesView.getContext());
        space.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 1));

        lapTimesView.addView(space);
        lapTimes.add(lapTime);

        final SharedPreferences.Editor prefs = context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
        prefs.putLong("lapTime" + (lapTimes.size() - 1), lapTime);
        prefs.putInt("lapTimesCount", lapTimes.size());
        prefs.commit();
    }

    /**
     * Save the state of this lap times list.
     */
    public void saveState() {
        final SharedPreferences.Editor prefs = context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
        prefs.putFloat("lapTimesScrollPosition", (scrollView.getScrollY() + scrollView.getMeasuredHeight())
                / (float) lapTimesView.getMeasuredHeight());
        prefs.commit();
    }

    /**
     * Restore this lap times list to a previously saved state.
     * 
     * @param savedInstanceState
     */
    public void restoreState(final SharedPreferences prefs) {
        if (bottomScroller != null) {
            lapTimesView.removeOnLayoutChangeListener(bottomScroller);
        }

        lapTimes.clear();
        lapTimesView.removeAllViews();

        final int lapTimesCount = prefs.getInt("lapTimesCount", 0);

        for (int idx = 0; idx < lapTimesCount; idx++) {
            if (prefs.contains("lapTime" + idx)) {
                final long lapTime = prefs.getLong("lapTime" + idx, 0);
                // add it to the list of lap times
                add(lapTime);
            }
        }

        final float scrollPosition = prefs.getFloat("lapTimesScrollPosition", 0);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0,
                        (int) (scrollPosition * lapTimesView.getMeasuredHeight()) - scrollView.getMeasuredHeight());
                lapTimesView.post(new Runnable() {
                    @Override
                    public void run() {
                        lastLapTimesViewHeight = lapTimesView.getMeasuredHeight();
                        if (bottomScroller != null) {
                            lapTimesView.addOnLayoutChangeListener(bottomScroller);
                        }
                    }
                });
            }
        });
    }

    /**
     * Removes all lap times in the list.
     */
    public void clear() {
        lapTimesView.removeAllViews();

        final SharedPreferences.Editor prefs = context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
        for (int idx = 0; idx < lapTimes.size(); idx++) {
            prefs.remove("lapTime" + idx);
        }
        prefs.putInt("lapTimesCount", 0);
        prefs.commit();

        lapTimes.clear();
    }

    private static String getTimerText(long elapsedTime) {
        final long millis = elapsedTime % 1000;
        elapsedTime /= 1000;
        final long secs = elapsedTime % 60;
        elapsedTime /= 60;
        final long mins = elapsedTime % 60;
        elapsedTime /= 60;
        final long hours = elapsedTime % 60;
        return String.format("%02d:%02d:%02d.%03d", hours, mins, secs, millis);
    }
}
