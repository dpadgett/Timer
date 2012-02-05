package org.dpadgett.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class StopwatchFragment extends Fragment {

	private long additionalElapsed = 0L;
	private long additionalLapTimeElapsed = 0L;
	private long timeStarted = 0L;
	private Thread updateTimerThread;
	private DanWidgets danWidgets;
	private final List<Long> lapTimes;
	private Context context;
	
	private Semaphore s;

	private boolean isTimerRunning;
	private LapTimeListAdapter lapTimesAdapter;
	private ListView lapTimesView;

	public StopwatchFragment() {
		updateTimerThread = new Thread(new UpdateTimerThread());
		isTimerRunning = false;
		s = new Semaphore(0);
		lapTimes = new ArrayList<Long>();
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.stopwatch, container, false);
        context = rootView.getContext();
        danWidgets = DanWidgets.create(rootView);
        
        ((LinearLayout) rootView).setDividerDrawable(
        		new ListView(rootView.getContext()).getDivider());
        
        Button startButton = (Button) rootView.findViewById(R.id.button1);
        Button resetButton = (Button) rootView.findViewById(R.id.button2);
        lapTimesView = (ListView) rootView.findViewById(R.id.lapTimesView);
        lapTimesAdapter = new LapTimeListAdapter();
		lapTimesView.setAdapter(lapTimesAdapter);
        startButton.setOnClickListener(new OnClickListener() {
			DanButton startButton = danWidgets.getButton(R.id.button1);
			DanButton resetButton = danWidgets.getButton(R.id.button2);
			DanTextView timerText = danWidgets.getTextView(R.id.textView1);
			DanTextView lapTimerText = danWidgets.getTextView(R.id.liveLapTime);

			@Override
			public void onClick(View arg0) {
				isTimerRunning = !isTimerRunning;
				if (isTimerRunning) { // start
					timeStarted = System.currentTimeMillis();
					updateTimerThread.start();
					startButton.setText("Stop");
					resetButton.setText("Lap");
				} else { // stop
					s.release();
					try {
						updateTimerThread.join();
					} catch (InterruptedException e) {
					}
					updateTimerThread = new Thread(new UpdateTimerThread());
					startButton.setText("Start");
					resetButton.setText("Reset");
					additionalElapsed += System.currentTimeMillis() - timeStarted;
					String updateText = getTimerText(additionalElapsed + additionalLapTimeElapsed);
					timerText.setText(updateText);
					updateText = getTimerText(additionalElapsed);
					lapTimerText.setText("lap: " + updateText);
				}
			}
        });
        resetButton.setOnClickListener(new OnClickListener() {
        	
        	DanTextView timerText = danWidgets.getTextView(R.id.textView1);
        	DanTextView lapTimerText = danWidgets.getTextView(R.id.liveLapTime);
        	
			@Override
			public void onClick(View arg0) {
				if (isTimerRunning) { // lap
					long origTimeStarted = timeStarted;
					timeStarted = System.currentTimeMillis();
					long lapTime = timeStarted - origTimeStarted + additionalElapsed; // this is the lap time
					additionalLapTimeElapsed += lapTime;
					additionalElapsed = 0L;

					// add it to the list of lap times
					lapTimes.add(lapTime);
					lapTimesAdapter.notifyDataSetChanged();
				} else { // reset
					timeStarted = 0L;
					additionalElapsed = 0L;
					additionalLapTimeElapsed = 0L;
					timerText.setText("00:00:00.000");
					lapTimerText.setText("lap: 00:00:00.000");
					lapTimes.clear();
					lapTimesAdapter.notifyDataSetChanged();
				}
			}
        });

        if (savedInstanceState != null) {
        	isTimerRunning = savedInstanceState.getBoolean("isTimerRunning", false);
        	timeStarted = savedInstanceState.getLong("timeStarted", 0L);
        	additionalElapsed = savedInstanceState.getLong("additionalElapsed", 0L);
        	additionalLapTimeElapsed = savedInstanceState.getLong("additionalLapTimeElapsed", 0L);
        	long[] lapTimesArray = savedInstanceState.getLongArray("lapTimes");
        	
        	if (lapTimesArray != null) {
        		for (long lapTime : lapTimesArray) {
        			// add it to the list of lap times
					lapTimes.add(lapTime);
					lapTimesAdapter.notifyDataSetChanged();
        		}
        	}

        	TextView timerText = (TextView) rootView.findViewById(R.id.textView1);
        	TextView lapTimerText = (TextView) rootView.findViewById(R.id.liveLapTime);
        	long elapsedTime = additionalElapsed + additionalLapTimeElapsed;

        	if (isTimerRunning) {
        		updateTimerThread.start();
				startButton.setText("Stop");
				resetButton.setText("Lap");
				elapsedTime = System.currentTimeMillis() - timeStarted + additionalElapsed + additionalLapTimeElapsed;
        	}

        	String updateText = getTimerText(elapsedTime);
			timerText.setText(updateText);
			updateText = getTimerText(elapsedTime - additionalLapTimeElapsed);
			lapTimerText.setText("lap: " + updateText);
        }

        return rootView;
    }

    @Override
	public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putBoolean("isTimerRunning", isTimerRunning);
        saveState.putLong("timeStarted", timeStarted);
        saveState.putLong("additionalElapsed", additionalElapsed);
        saveState.putLong("additionalLapTimeElapsed", additionalLapTimeElapsed);
        
        long[] lapTimesArray = new long[lapTimes.size()];
        for (int idx = 0; idx < lapTimes.size(); idx++) {
        	lapTimesArray[idx] = lapTimes.get(idx);
        }
        saveState.putLongArray("lapTimes", lapTimesArray);
    }

    private class UpdateTimerThread implements Runnable {
		@Override
		public void run() {
			DanTextView timerText = danWidgets.getTextView(R.id.textView1);
			DanTextView lapTimerText = danWidgets.getTextView(R.id.liveLapTime);
			while (!s.tryAcquire()) {
				long elapsedTime = System.currentTimeMillis() - timeStarted + additionalElapsed + additionalLapTimeElapsed;
				String updateText = getTimerText(elapsedTime);
				timerText.setText(updateText);
				
				long elapsedLapTime = System.currentTimeMillis() - timeStarted + additionalElapsed;
				updateText = getTimerText(elapsedLapTime);
				lapTimerText.setText("lap: " + updateText);
				//Thread.yield();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
				}
			}
		}
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
	
	private class LapTimeListAdapter extends BaseAdapter {
		 
	    public LapTimeListAdapter() {
	    }
	 
	    public int getCount() {
	        return lapTimes.size();
	    }
	 
	    public Long getItem(int position) {
	        return lapTimes.get(position);
	    }
	 
	    public long getItemId(int position) {
	        return lapTimes.get(position).hashCode();
	    }
	 
	    public View getView(int position, View convertView, ViewGroup parent) {
	    	long lapTime = getItem(position);
			LinearLayout lapLayout;
			if (convertView != null && convertView instanceof LinearLayout) {
				lapLayout = (LinearLayout) convertView;
	    	} else {
	    		lapLayout =
					(LinearLayout) LayoutInflater.from(context).inflate(R.layout.single_lap_time, parent, false);
	    	}
			
			
			TextView lapLabel = (TextView) lapLayout.findViewById(R.id.lapLabel);
			lapLabel.setText("lap " + (position + 1));
			
			TextView lapTimeView = (TextView) lapLayout.findViewById(R.id.lapTime);
			lapTimeView.setText(getTimerText(lapTime));
			return lapLayout;
	    }
	 
	    @Override
	    public void notifyDataSetChanged() {
	    	super.notifyDataSetChanged();
	    	lapTimesView.post(new Runnable() {
				
				@Override
				public void run() {
					//lapTimesView.smoothScrollBy(-1, 0);
					lapTimesView.smoothScrollToPosition(lapTimes.size() - 1);
				}
				
			});
	    }
	}
}
