package org.dpadgett.timer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DigitalClock;
import android.widget.LinearLayout;
import android.widget.ListView;

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
						addNewClock();
					}
				});
			}
        });
		ListView clocksList = (ListView) finder.findViewById(R.id.clocksList);
		clocksList.setAdapter(clocksListAdapter);
        return rootView;
    }

	/**
	 * Adds a new clock to the view
	 */
	private void addNewClock() {
		String[] allIDs = TimeZone.getAvailableIDs();
		String myID = allIDs[new Random().nextInt(allIDs.length)];
		clockList.add(myID);
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
	        final String myID = clockList.get(position);
	 
	        LinearLayout newClock =
	        		(LinearLayout) LayoutInflater.from(context)
	        			.inflate(R.layout.single_world_clock, parent, false);
	 
	        DigitalClock clock = (DigitalClock) newClock.findViewById(R.id.digitalClock);
			clock.addTextChangedListener(new TextWatcher() {
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { }

				private boolean isInternalEdit = true;
				@Override
				public void afterTextChanged(Editable s) {
					isInternalEdit = !isInternalEdit;
					if (!isInternalEdit) {
						SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
						Date newDate = new Date(); // as a fallback
						try {
							newDate = sdf.parse(s.toString());
						} catch (ParseException e) {
						}
						sdf.setTimeZone(TimeZone.getTimeZone(myID));
						String toText = sdf.format(newDate);
						s.replace(0, s.length(), toText.toLowerCase());
					}
				}
			});
			
			AnalogClockWithTimezone analogClock =
					(AnalogClockWithTimezone) newClock.findViewById(R.id.analogClock);
			analogClock.setTimezone(myID);
	 
	        return newClock;
	    }
	 
	}
}
