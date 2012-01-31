package org.dpadgett.timer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class WorldClockFragment extends Fragment {
	
	private DanResourceFinder finder;
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
        finder = DanWidgets.finderFrom(rootView);
        context = rootView.getContext();
        LinearLayout addClockView = (LinearLayout) finder.findViewById(R.id.addClockView);
        addClockView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				view.setBackgroundResource(
						Resources.getSystem().getIdentifier("list_selector_holo_dark", "drawable", "android"));
						//android.R.drawable.list_selector_background);
				uiHandler.post(new Runnable() {
					@Override
					public void run() {
						newClockDialog(-1);
					}
				});
			}
        });
		ListView clocksList = (ListView) finder.findViewById(R.id.clocksList);
		clocksList.setAdapter(clocksListAdapter);
		clocksList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				newClockDialog(position);
			}
		});
        return rootView;
    }

    private void newClockDialog(final int position) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle("Select a timezone");
    	List<String> timezones = new ArrayList<String>();
    	final Map<String, Integer> strToOffset = new HashMap<String, Integer>();
    	final long currentTime = System.currentTimeMillis();
    	for (String timezone : TimeZone.getAvailableIDs()) {
    		int millisOffset = TimeZone.getTimeZone(timezone).getOffset(currentTime);
			String offset = String.format("%02d:%02d", Math.abs(millisOffset / 1000 / 60 / 60), (millisOffset / 1000 / 60) % 60);
			if (millisOffset / 1000 / 60 / 60 < 0) {
				offset = "-" + offset;
			} else {
				offset = "+" + offset;
			}
			timezones.add("(UTC" + offset + ") - " + timezone);
			strToOffset.put("(UTC" + offset + ") - " + timezone, millisOffset);
    	}
    	Collections.sort(timezones, new Comparator<String>() {
			@Override
			public int compare(String lhs, String rhs) {
				return strToOffset.get(lhs) - strToOffset.get(rhs);
			}
    	});
    	final String[] items = timezones.toArray(new String[timezones.size()]);
    	builder.setItems(items, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int item) {
    	    	String timezone = items[item];
    	    	timezone = timezone.substring(timezone.lastIndexOf(' ') + 1);
    	    	addNewClock(timezone, position);
    	    }
    	});
    	if (position > -1) {
	    	builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					clockList.remove(position);
					clocksListAdapter.notifyDataSetChanged();
				}
	    	});
    	}
    	AlertDialog alert = builder.create();
    	alert.show();
    }

    /**
	 * Adds a new clock to the view
	 */
	private void addNewClock(String timeZone, int position) {
		if (position == -1) {
			clockList.add(timeZone);
		} else {
			clockList.set(position, timeZone);
		}
		clocksListAdapter.notifyDataSetChanged();
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
			timezoneText.setText(timezone);
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
