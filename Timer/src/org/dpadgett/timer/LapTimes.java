package org.dpadgett.timer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

/**
 * Class to encapsulate the functionality and logic for the lap times list.
 *
 * @author dan
 */
public class LapTimes {

	private final ScrollView scrollView;
	private final LinearLayout lapTimesView;
	private final List<Long> lapTimes;
	private OnLayoutChangeListener bottomScroller;
	private final Context context;

	public LapTimes(ScrollView scrollView) {
		this.scrollView = scrollView;
		this.lapTimesView = (LinearLayout) scrollView.findViewById(R.id.lapTimesView);
		this.context = scrollView.getContext();
		bottomScroller = new OnLayoutChangeListener() {
			@Override
			public void onLayoutChange(View v, int left, int top, int right,
					int bottom, int oldLeft, int oldTop, int oldRight,
					int oldBottom) {
				LapTimes.this.scrollView.fullScroll(View.FOCUS_DOWN);
			}
		};
		lapTimesView.addOnLayoutChangeListener(bottomScroller);
		lapTimesView.setDividerDrawable(
        		new ListView(lapTimesView.getContext()).getDivider());
		lapTimes = new ArrayList<Long>();
	}

	/**
	 * Add this lap time to the list of lap times.
	 *
	 * @param lapTime the time to add
	 */
	public void add(long lapTime) {
		// unhack
		if (lapTimesView.getChildCount() > 0) {
			lapTimesView.removeViewAt(lapTimesView.getChildCount() - 1);
		}
		
		LinearLayout lapLayout = (LinearLayout) LayoutInflater.from(lapTimesView.getContext())
				.inflate(R.layout.single_lap_time, (ViewGroup) lapTimesView, false);

		TextView lapLabel = (TextView) lapLayout.findViewById(R.id.lapLabel);
		lapLabel.setText("lap " + (lapTimes.size() + 1));
		
		TextView lapTimeView = (TextView) lapLayout.findViewById(R.id.lapTime);
		lapTimeView.setText(StopwatchFragment.getTimerText(lapTime));
		
		lapTimesView.addView(lapLayout);
		
		// hack to get it to draw the lower divider
		Space space = new Space(lapTimesView.getContext());
		space.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, 1));
		
		lapTimesView.addView(space);
		lapTimes.add(lapTime);
		
		SharedPreferences.Editor prefs =
				context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
		prefs.putLong("lapTime" + (lapTimes.size() - 1), lapTime);
		prefs.putInt("lapTimesCount", lapTimes.size());
		prefs.apply();
	}
	
	/**
	 * Save the state of this lap times list.
	 *
	 * @param saveState
	 */
	public void onSaveInstanceState(Bundle saveState) {
        long[] lapTimesArray = new long[lapTimes.size()];
        for (int idx = 0; idx < lapTimes.size(); idx++) {
        	lapTimesArray[idx] = lapTimes.get(idx);
        }
        saveState.putLongArray("lapTimes", lapTimesArray);
        saveState.putInt("lapTimesScrollPosition", scrollView.getScrollY() + scrollView.getMeasuredHeight());

		SharedPreferences.Editor prefs =
				context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
		prefs.putInt("lapTimesScrollPosition", scrollView.getScrollY() + scrollView.getMeasuredHeight());
		prefs.apply();
	}

	/**
	 * Restore this lap times list to a previously saved state.
	 *
	 * @param savedInstanceState
	 */
	public void restoreState(Bundle savedInstanceState) {
		lapTimesView.removeOnLayoutChangeListener(bottomScroller);
		
		long[] lapTimesArray = savedInstanceState.getLongArray("lapTimes");
    	
    	if (lapTimesArray != null) {
    		for (long lapTime : lapTimesArray) {
    			// add it to the list of lap times
				add(lapTime);
    		}
    	}
    	
    	final int scrollPosition = savedInstanceState.getInt("lapTimesScrollPosition", 0);
    	scrollView.post(new Runnable() {
			@Override
			public void run() {
				scrollView.scrollTo(0, scrollPosition - scrollView.getMeasuredHeight());
		    	System.out.println("scrolled to " + (scrollPosition - scrollView.getMeasuredHeight()));
				lapTimesView.post(new Runnable() {
					@Override
					public void run() {
						lapTimesView.addOnLayoutChangeListener(bottomScroller);
					}
				});
			}
    	});
	}

	/**
	 * Restore this lap times list to a previously saved state.
	 *
	 * @param savedInstanceState
	 */
	public void restoreState(SharedPreferences prefs) {
		lapTimesView.removeOnLayoutChangeListener(bottomScroller);
		
		int lapTimesCount = prefs.getInt("lapTimesCount", 0);
    	
  		for (int idx = 0; idx < lapTimesCount; idx++) {
  			if (prefs.contains("lapTime" + idx)) {
	  			long lapTime = prefs.getLong("lapTime" + idx, 0);
	   			// add it to the list of lap times
				add(lapTime);
  			}
   		}

    	final int scrollPosition = prefs.getInt("lapTimesScrollPosition", 0);
    	scrollView.post(new Runnable() {
			@Override
			public void run() {
				scrollView.scrollTo(0, scrollPosition - scrollView.getMeasuredHeight());
		    	System.out.println("scrolled to " + (scrollPosition - scrollView.getMeasuredHeight()));
				lapTimesView.post(new Runnable() {
					@Override
					public void run() {
						lapTimesView.addOnLayoutChangeListener(bottomScroller);
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

		SharedPreferences.Editor prefs =
				context.getSharedPreferences("Stopwatch", Context.MODE_PRIVATE).edit();
		for (int idx = 0; idx < lapTimes.size(); idx++) {
			prefs.remove("lapTime" + idx);
		}
		prefs.putInt("lapTimesCount", 0);
		prefs.apply();

		lapTimes.clear();
	}
}
