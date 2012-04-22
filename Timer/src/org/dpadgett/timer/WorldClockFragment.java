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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * Fragment which shows a static list of live clocks from different timezones.
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
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
    	uiHandler = new Handler();
        View rootView = inflater.inflate(R.layout.world_clock, container, false);
        finder = ResourceFinders.from(rootView);
        context = rootView.getContext();
        Button addClockButton = (Button) finder.findViewById(R.id.addClockButton);
        addClockButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				uiHandler.post(new Runnable() {
					@Override
					public void run() {
						newClockDialog(-1);
					}
				});
			}
        });
        
        clockList.clear();
        SharedPreferences prefs =
				context.getSharedPreferences("WorldClocks", Context.MODE_PRIVATE);
		int numClocks = prefs.getInt("numClocks", -1);
		if (numClocks >= 0) {
			for (int idx = 0; idx < numClocks; idx++) {
				String clock = prefs.getString("clock" + idx, null);
				if (clock != null) {
					clockList.add(clock);
				}
			}
		} else {
			addNewClock(TimeZone.getDefault().getID(), -1);
		}
        
		ListView clocksList = (ListView) finder.findViewById(R.id.clocksList);
		clocksList.setAdapter(clocksListAdapter);
		clocksList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				newClockDialog(position);
			}
		});
		
		// hack to get the bottom divider to be the same as the listview dividers
		Drawable divider = new ListView(context).getDivider();
		((org.dpadgett.compat.LinearLayout) rootView).setDividerDrawable(divider);
		
		// forcefully pre-render content so it is cached
		rootView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
		rootView.draw(new Canvas(Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888)));

		return rootView;
    }

	private void newClockDialog(final int position) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle("Select a timezone");
    	final Map<String, String> timezoneNameToId = new HashMap<String, String>();
    	Set<Integer> timezones = new TreeSet<Integer>();
    	final Map<Integer, List<String>> offsetToName = new HashMap<Integer, List<String>>();
    	final long currentTime = System.currentTimeMillis();
    	for (String timezone : TimeZone.getAvailableIDs()) {
    		String timezoneName = TimeZone.getTimeZone(timezone).getDisplayName();
    		if (timezoneNameToId.containsKey(timezoneName)) {
    			continue;
    		}
    		int millisOffset = TimeZone.getTimeZone(timezone).getOffset(currentTime);
			timezones.add(millisOffset);
			if (!offsetToName.containsKey(millisOffset)) {
				offsetToName.put(millisOffset, new ArrayList<String>());
			}
			offsetToName.get(millisOffset).add(timezoneName);
			timezoneNameToId.put(timezoneName, timezone);
    	}
    	for (List<String> names : offsetToName.values()) {
    		Collections.sort(names);
    	}
    	if (position > -1) {
	    	builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					clockList.remove(position);
					clocksListAdapter.notifyDataSetChanged();
					
					SharedPreferences.Editor prefs =
							context.getSharedPreferences("WorldClocks", Context.MODE_PRIVATE).edit();
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
    	LinearLayout tzView = (LinearLayout) LayoutInflater.from(context)
			.inflate(R.layout.timezone_picker_dialog, (ViewGroup) finder.findViewById(R.id.layout_root));

    	final List<String> initialItems = new ArrayList<String>();
    	initialItems.add("GMT");
    	initialItems.add("UTC");
    	final ArrayAdapter<String> adapter =
    			ArrayAdapter.newArrayAdapter(context, R.layout.timezone_dialog_list_item, initialItems);
    	ListView timezoneList = (ListView) tzView.findViewById(R.id.timezoneList);
    	timezoneList.setAdapter(adapter);
    	
    	final TextView sliderView = (TextView) tzView.findViewById(R.id.timezoneLabel);

    	final SeekBar timezoneSeeker = (SeekBar) tzView.findViewById(R.id.timezoneSeeker);
    	final List<Integer> timezonesList = new ArrayList<Integer>(timezones);
    	timezoneSeeker.setMax(timezonesList.size() - 1);
    	if (position > -1) {
    		int offset = TimeZone.getTimeZone(clockList.get(position)).getOffset(currentTime);
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
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				adapter.clear();
				adapter.addAll(offsetToName.get(timezonesList.get(progress)));
				int millisOffset = timezonesList.get(progress);
				String offset = String.format("%02d:%02d", Math.abs(millisOffset / 1000 / 60 / 60), Math.abs(millisOffset / 1000 / 60) % 60);
				if (millisOffset / 1000 / 60 / 60 < 0) {
					offset = "-" + offset;
				} else {
					offset = "+" + offset;
				}
				sliderView.setText("UTC Offset: " + offset);
			}

			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
    	});
    	builder.setView(tzView);
    	final AlertDialog alert = builder.create();

    	timezoneList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int selectedPosition, long id) {
				String timezoneName = adapter.getItem(selectedPosition);
				String timezone = timezoneNameToId.get(timezoneName);
		    	addNewClock(timezone, position);
		    	alert.dismiss();
			}
    	});

        alert.show();
    }

    /**
	 * Adds a new clock to the view
	 */
	private void addNewClock(String timeZone, int position) {
		if (position == -1) {
			clockList.add(timeZone);
			position = clockList.size() - 1;
		} else {
			clockList.set(position, timeZone);
		}
		clocksListAdapter.notifyDataSetChanged();

		// save to prefs
		SharedPreferences.Editor prefs =
				context.getSharedPreferences("WorldClocks", Context.MODE_PRIVATE).edit();
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
	 
	    public String getItem(int position) {
	        return clockList.get(position);
	    }
	 
	    public long getItemId(int position) {
	        return clockList.get(position).hashCode();
	    }
	 
	    public View getView(int position, View convertView, ViewGroup parent) {
	        final String timezone = clockList.get(position);
	 
	        LinearLayout newClock =
	        		(LinearLayout) LayoutInflater.from(context)
	        			.inflate(R.layout.single_world_clock, parent, false);
	 
			AnalogClockWithTimezone analogClock =
					(AnalogClockWithTimezone) newClock.findViewById(R.id.analogClock);
			analogClock.setTimezone(timezone);

			final TextView clock = (TextView) newClock.findViewById(R.id.digitalClock);
			analogClock.addOnTickListener(new AnalogClockWithTimezone.OnTickListener() {
				@Override
				public void onTick() {
					updateClockTextView(clock, timezone);
				}
			});
	 
			TextView timezoneText = (TextView) newClock.findViewById(R.id.timezone);
			timezoneText.setText(TimeZone.getTimeZone(timezone).getDisplayName());
			updateClockTextView(clock, timezone);
	        return newClock;
	    }
	 
	}
	
	private void updateClockTextView(TextView clockToUpdate, String timezone) {
		SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
		Date newDate = new Date(); // as a fallback
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
		String toText = sdf.format(newDate).toLowerCase();
		clockToUpdate.setText(toText);
	}
}
