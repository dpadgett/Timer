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

import java.text.SimpleDateFormat;
import java.util.*;

import org.dpadgett.compat.ArrayAdapter;
import org.dpadgett.widget.AnalogClockWithTimezone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Fragment which shows a static list of live clocks from different
 * timezones.
 * 
 * @author dpadgett
 */
public class WorldClockFragment extends Fragment {

    private ResourceFinder finder;
    private Context context;
    private Handler uiHandler;
    private final ClockListAdapter clocksListAdapter;
    private final List<String> clockList;

    public WorldClockFragment() {
        clockList = new ArrayList<String>();
        clocksListAdapter = new ClockListAdapter();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        uiHandler = new Handler();
        final View rootView = inflater.inflate(R.layout.world_clock, container, false);
        finder = ResourceFinders.from(rootView);
        context = rootView.getContext();
        final Button addClockButton = (Button) finder.findViewById(R.id.addClockButton);
        addClockButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        newClockDialog(-1);
                    }
                });
            }
        });

        clockList.clear();
        final SharedPreferences prefs = context.getSharedPreferences("WorldClocks", Context.MODE_PRIVATE);
        final int numClocks = prefs.getInt("numClocks", -1);
        if (numClocks >= 0) {
            for (int idx = 0; idx < numClocks; idx++) {
                final String clock = prefs.getString("clock" + idx, null);
                if (clock != null) {
                    clockList.add(clock);
                }
            }
        } else {
            addNewClock(TimeZone.getDefault().getID(), -1);
        }

        final ListView clocksList = (ListView) finder.findViewById(R.id.clocksList);
        clocksList.setAdapter(clocksListAdapter);
        clocksList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                newClockDialog(position);
            }
        });

        // hack to get the bottom divider to be the same as the listview dividers
        final Drawable divider = new ListView(context).getDivider();
        ((org.dpadgett.compat.LinearLayout) rootView).setDividerDrawable(divider);

        // forcefully pre-render content so it is cached
        rootView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
                rootView.draw(new Canvas(Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(),
                        Bitmap.Config.ARGB_8888)));
            }
        }, 1000);

        return rootView;
    }

    private void newClockDialog(final int position) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.world_clock_select_timezone));
        final Map<String, String> timezoneNameToId = new HashMap<String, String>();
        final Set<Integer> timezones = new TreeSet<Integer>();
        final Map<Integer, List<String>> offsetToName = new HashMap<Integer, List<String>>();
        final long currentTime = System.currentTimeMillis();

        for (final String timezone : TimeZone.getAvailableIDs()) {
            final TimeZone tz = TimeZone.getTimeZone(timezone);
            final boolean isDaylight = tz.useDaylightTime();
            final String timezoneName = tz.getDisplayName(isDaylight, TimeZone.LONG, Locale.getDefault());
            if (timezoneNameToId.containsKey(timezoneName)) {
                continue;
            }
            final int millisOffset = tz.getOffset(currentTime);
            timezones.add(millisOffset);
            if (!offsetToName.containsKey(millisOffset)) {
                offsetToName.put(millisOffset, new ArrayList<String>());
            }
            offsetToName.get(millisOffset).add(timezoneName);
            timezoneNameToId.put(timezoneName, timezone);
        }
        for (final List<String> names : offsetToName.values()) {
            Collections.sort(names);
        }
        if (position > -1) {
            builder.setPositiveButton(context.getString(R.string.world_clock_button_remove),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            clockList.remove(position);
                            clocksListAdapter.notifyDataSetChanged();

                            final SharedPreferences.Editor prefs = context.getSharedPreferences("WorldClocks",
                                    Context.MODE_PRIVATE).edit();
                            prefs.putInt("numClocks", clockList.size());
                            int idx;
                            for (idx = position; idx < clockList.size(); idx++) {
                                prefs.putString("clock" + idx, clockList.get(idx));
                            }
                            prefs.remove("clock" + idx);
                            prefs.commit();
                        }
                    });
        }
        final LinearLayout tzView = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.timezone_picker_dialog, (ViewGroup) finder.findViewById(R.id.layout_root));

        final List<String> initialItems = new ArrayList<String>();
        initialItems.add(context.getString(R.string.world_clock_timezone_gmt));
        initialItems.add(context.getString(R.string.world_clock_timezone_utc));
        final ArrayAdapter<String> adapter = ArrayAdapter.newArrayAdapter(context, R.layout.timezone_dialog_list_item,
                initialItems);
        final ListView timezoneList = (ListView) tzView.findViewById(R.id.timezoneList);
        timezoneList.setAdapter(adapter);

        final TextView sliderView = (TextView) tzView.findViewById(R.id.timezoneLabel);

        final SeekBar timezoneSeeker = (SeekBar) tzView.findViewById(R.id.timezoneSeeker);
        final List<Integer> timezonesList = new ArrayList<Integer>(timezones);
        timezoneSeeker.setMax(timezonesList.size() - 1);
        if (position > -1) {
            final int offset = TimeZone.getTimeZone(clockList.get(position)).getOffset(currentTime);
            timezoneSeeker.setProgress(timezonesList.indexOf(offset));
        } else {
            timezoneSeeker.setProgress(timezonesList.indexOf(0));
        }
        timezoneSeeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            // initialize the timezoneSeeker
            {
                onProgressChanged(timezoneSeeker, timezoneSeeker.getProgress(), false);
            }

            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                adapter.clear();
                adapter.addAll(offsetToName.get(timezonesList.get(progress)));
                final int millisOffset = timezonesList.get(progress);
                String offset = String.format("%02d:%02d", Math.abs(millisOffset / 1000 / 60 / 60),
                        Math.abs(millisOffset / 1000 / 60) % 60);
                if (millisOffset / 1000 / 60 / 60 < 0) {
                    offset = "-" + offset;
                } else {
                    offset = "+" + offset;
                }
                sliderView.setText(context.getString(R.string.world_clock_timezone_label) + offset);
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
            }
        });
        builder.setView(tzView);
        final AlertDialog alert = builder.create();

        timezoneList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent,
                    final View view,
                    final int selectedPosition,
                    final long id) {
                final String timezoneName = adapter.getItem(selectedPosition);
                final String timezone = timezoneNameToId.get(timezoneName);
                addNewClock(timezone, position);
                alert.dismiss();
            }
        });

        alert.show();
    }

    /**
     * Adds a new clock to the view
     */
    private void addNewClock(final String timeZone, int position) {
        if (position == -1) {
            clockList.add(timeZone);
            position = clockList.size() - 1;
        } else {
            clockList.set(position, timeZone);
        }
        clocksListAdapter.notifyDataSetChanged();

        // save to prefs
        final SharedPreferences.Editor prefs = context.getSharedPreferences("WorldClocks", Context.MODE_PRIVATE).edit();
        prefs.putInt("numClocks", clockList.size());
        prefs.putString("clock" + position, timeZone);
        prefs.commit();
    }

    private class ClockListAdapter extends BaseAdapter {

        public ClockListAdapter() {
        }

        public int getCount() {
            return clockList.size();
        }

        public String getItem(final int position) {
            return clockList.get(position);
        }

        public long getItemId(final int position) {
            return clockList.get(position).hashCode();
        }

        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final String timezone = clockList.get(position);

            final LinearLayout newClock = (LinearLayout) LayoutInflater.from(context).inflate(
                    R.layout.single_world_clock, parent, false);

            final AnalogClockWithTimezone analogClock = (AnalogClockWithTimezone) newClock
                    .findViewById(R.id.analogClock);
            analogClock.setTimezone(timezone);

            final TextView clock = (TextView) newClock.findViewById(R.id.digitalClock);
            analogClock.addOnTickListener(new AnalogClockWithTimezone.OnTickListener() {
                @Override
                public void onTick() {
                    updateClockTextView(clock, timezone);
                }
            });

            final TextView timezoneText = (TextView) newClock.findViewById(R.id.timezone);
            final TimeZone tz = TimeZone.getTimeZone(timezone);
            final boolean isDaylight = tz.useDaylightTime();
            final String timezoneName = tz.getDisplayName(isDaylight, TimeZone.LONG, Locale.getDefault());
            timezoneText.setText(timezoneName);
            updateClockTextView(clock, timezone);
            return newClock;
        }

    }

    private void updateClockTextView(final TextView clockToUpdate, final String timezone) {
        final SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
        final Date newDate = new Date(); // as a fallback
        sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        final String toText = sdf.format(newDate).toLowerCase();
        clockToUpdate.setText(toText);
    }
}
