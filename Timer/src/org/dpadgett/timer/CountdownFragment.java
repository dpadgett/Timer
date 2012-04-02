package org.dpadgett.timer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dpadgett.widget.CountdownTextView;
import org.dpadgett.widget.FasterNumberPicker;
import org.dpadgett.widget.FasterNumberPicker.OnValueChangeListener;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

/**
 * Fragment which handles the UI and logic for the countdown timer.
 *
 * @author dpadgett
 */
public class CountdownFragment extends Fragment {

	private boolean inputMode;
	private LinearLayout inputLayout;
	private LinearLayout timerLayout;
	private View rootView;
	private final Handler handler;
	private FasterNumberPicker countdownHours;
	private FasterNumberPicker countdownMinutes;
	private FasterNumberPicker countdownSeconds;
	private CountdownState timingState;

	public PendingIntent alarmPendingIntent;
	private List<String> uris;
	private ArrayAdapter<String> alarmTonesAdapter;
	private List<String> paths;

	public CountdownFragment() {
		this.inputMode = true;
		this.handler = new Handler();
	}

	public Context getContext() {
		return rootView.getContext();
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.countdown_simplified, container, false);
        this.inputLayout = (LinearLayout) rootView.findViewById(R.id.inputsInnerLayout);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);

        FasterNumberPicker.Formatter twoDigitFormatter = new FasterNumberPicker.Formatter() {
			@Override
			public String format(int value) {
				return String.format("%02d", value);
			}
        };
        
        FasterNumberPicker.OnValueChangeListener saveTimestampListener = new OnValueChangeListener() {
			@Override
			public void onValueChange(FasterNumberPicker picker, int oldVal, int newVal) {
				SharedPreferences.Editor prefs = 
						getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();    		
				prefs.putLong("countdownInputs", getInputTimestamp());
				prefs.commit();
			}
		};

        countdownHours = (FasterNumberPicker) rootView.findViewById(R.id.countdownHours);
        countdownHours.setMinValue(0);
        countdownHours.setMaxValue(99);
		countdownHours.setFormatter(twoDigitFormatter);
		// I will burn in hell for this
		View view = countdownHours.findViewById(org.dpadgett.compat.R.id.numberpicker_input);
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownHours.setDisableInputText(true);
		countdownHours.setOnValueChangedListener(saveTimestampListener);

		countdownMinutes = (FasterNumberPicker) rootView.findViewById(R.id.countdownMinutes);
        countdownMinutes.setMinValue(0);
        countdownMinutes.setMaxValue(59);
		countdownMinutes.setFormatter(twoDigitFormatter);
		view = countdownMinutes.findViewById(org.dpadgett.compat.R.id.numberpicker_input);
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownMinutes.setDisableInputText(true);
		countdownMinutes.setOnValueChangedListener(saveTimestampListener);

		countdownSeconds = (FasterNumberPicker) rootView.findViewById(R.id.countdownSeconds);
        countdownSeconds.setMinValue(0);
        countdownSeconds.setMaxValue(59);
		countdownSeconds.setFormatter(twoDigitFormatter);
		view = countdownSeconds.findViewById(org.dpadgett.compat.R.id.numberpicker_input);
		if (view != null) {
			EditText inputText = (EditText) view;
			inputText.setFocusable(false);
		}
		countdownSeconds.setDisableInputText(true);
		countdownSeconds.setOnValueChangedListener(saveTimestampListener);

		RingtoneManager manager = new RingtoneManager(getContext());
		manager.setType(RingtoneManager.TYPE_ALARM);
		List<String> names = new ArrayList<String>();
		uris = new ArrayList<String>();
		paths = new ArrayList<String>();
		Cursor c = manager.getCursor();
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			names.add(c.getString(RingtoneManager.TITLE_COLUMN_INDEX));
			// System.out.println("cursor uri: " + c.getString(RingtoneManager.URI_COLUMN_INDEX));
			// System.out.println("manager uri: " + manager.getRingtoneUri(c.getPosition()));
			uris.add(manager.getRingtoneUri(c.getPosition()).toString());
			paths.add(getRealPathFromURI(manager.getRingtoneUri(c.getPosition())));
			System.out.println("path: " + paths.get(paths.size() - 1));
		}
		c.close();
		Spinner alarmTones = (Spinner) rootView.findViewById(R.id.alarm_tones);
		alarmTonesAdapter =
				new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item,
						names);
		alarmTonesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alarmTones.setAdapter(alarmTonesAdapter);
        alarmTones.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor prefs = 
						getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();
				prefs.putString("alarmUri", uris.get(position).toString());
				prefs.commit();
				Log.i(getClass().getName(), "Saved uri " + uris.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

        this.timerLayout =
        		(LinearLayout) inflater.inflate(R.layout.countdown_timer, container, false);

		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (inputMode) {
					inputModeOff();
				} else {
					inputModeOn();
				}
			}
        });

		restoreState();

		// forcefully pre-render content so it is cached
		rootView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		rootView.layout(0, 0, rootView.getMeasuredWidth(), rootView.getMeasuredHeight());
		rootView.draw(new Canvas(Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888)));

		return rootView;
    }
    
	private void restoreState() {
        SharedPreferences prefs =
				getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE);

    	timingState = new CountdownState(
				(CountdownTextView) timerLayout.findViewById(R.id.countdownTimer), prefs);
		
        if (prefs.contains("countdownInputs")) {
	    	long countdownInputs = prefs.getLong("countdownInputs", 0L);
	    	countdownInputs /= 1000;
	    	countdownSeconds.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownMinutes.setValue((int) (countdownInputs % 60));
	    	countdownInputs /= 60;
	    	countdownHours.setValue((int) (countdownInputs % 100));
	    	inputMode = !timingState.isRunning();
	    	if (!inputMode) {
	    		// countdown view
				LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
				Button startButton = (Button) rootView.findViewById(R.id.startButton);
				inputs.removeAllViews();
				inputs.addView(timerLayout);
				startButton.setText("Cancel");
				// timing thread will auto start itself
	    	}
        }
        
        Uri alarmUri = AlarmService.getRingtoneUri(prefs);
        Log.i(getClass().getName(), "alarmUri path is " + getRealPathFromURI(alarmUri));
		Spinner alarmTones = (Spinner) rootView.findViewById(R.id.alarm_tones);
		int idx = uris.indexOf(alarmUri.toString());
		if (idx == -1) {
			idx = paths.indexOf(getRealPathFromURI(alarmUri));
		}
		if (idx != -1) {
			alarmTones.setSelection(idx);
		} else {
			Ringtone ringtone = RingtoneManager.getRingtone(getContext(), alarmUri);
			if (ringtone == null) {
				// in this case our default url is bogus, so we should fix it
				int sel = alarmTones.getSelectedItemPosition();
				SharedPreferences.Editor prefsEdit = 
						getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();
				prefsEdit.putString("alarmUri", uris.get(sel));
				prefsEdit.commit();
				Log.i(getClass().getName(), "Saved default uri " + uris.get(sel).toString());
			} else {
				Log.i(getClass().getName(), "ringtone path: " + ringtone + " vs " + Settings.System.DEFAULT_ALARM_ALERT_URI.getPath());
				alarmTonesAdapter.add(
						ringtone.getTitle(getContext()));
				uris.add(alarmUri.toString());
				paths.add(getRealPathFromURI(alarmUri));
				alarmTones.setSelection(uris.size() - 1);
			}
		}
    }
    
	private String getRealPathFromURI(Uri contentUri) {
        String toReturn = "unknown";
        try {
	        Cursor cursor = getContext().getContentResolver().query(contentUri, null, null, null, null);
	        int column_index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
	        if (column_index != -1) {
		        cursor.moveToFirst();
		        toReturn = cursor.getString(column_index);
	        } else {
	        	cursor.moveToFirst();
	        	toReturn = getRealPathFromURI(Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow("value"))));
	        }
	        cursor.close();
        } catch (SQLiteException e) {
        	e.printStackTrace();
        }
        return toReturn;
    }

	@Override
    public void onPause() {
    	super.onPause();
    	saveState();
    	handler.removeCallbacks(inputModeOff);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if (rootView != null) {
    		restoreState();
    	}
    }
    
    @Override
    public void onSaveInstanceState(Bundle saveState) {
    	super.onSaveInstanceState(saveState);
    	if (rootView != null) {
    		saveState();
    	}
    }

    private void saveState() {
		SharedPreferences.Editor prefs = 
			getContext().getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();    		
		
    	timingState.onSaveState(prefs);
		prefs.putLong("countdownInputs", getInputTimestamp());
    	prefs.commit();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	timingState.stopTimer();
    	handler.removeCallbacks(inputModeOff);
    }

    public void inputModeOff() {
    	handler.post(inputModeOff);
    }

    public void inputModeOn() {
    	handler.post(inputModeOn);
    }

    private final Runnable inputModeOff = new Runnable() {

		@Override
		public void run() {
			if (rootView == null) {
				return;
			}
			inputMode = false;
			LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
			Button startButton = (Button) rootView.findViewById(R.id.startButton);

			timingState.startTimer(getInputTimestamp());
			
			inputs.removeAllViews();
			inputs.addView(timerLayout);
			startButton.setText("Cancel");
			
			AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
			// should be unique
			Intent intent = new Intent(getContext(), AlarmService.class)
				.putExtra("startAlarm", true)
				.setAction("startAlarmAt" + (timingState.endTime));
			alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
					PendingIntent.FLAG_ONE_SHOT);
			alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + getInputTimestamp(), alarmPendingIntent);

			saveState();
		}
    	
    };
    
    private final Runnable inputModeOn = new Runnable() {

		@Override
		public void run() {
			if (rootView == null) {
				return;
			}
			inputMode = true;
			LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
			Button startButton = (Button) rootView.findViewById(R.id.startButton);

			//TODO: fixme
	    	handler.removeCallbacks(inputModeOff);
	    	handler.removeCallbacks(inputModeOn);
			inputs.removeAllViews();
			inputs.addView(inputLayout);
			startButton.setText("Start");
			timingState.stopTimer();
			if (alarmPendingIntent == null) {
				// should be unique
				Intent intent = new Intent(getContext(), AlarmService.class)
					.putExtra("startAlarm", true)
					.setAction("startAlarmAt" + (timingState.endTime));
				alarmPendingIntent = PendingIntent.getService(getContext(), 0, intent,
						PendingIntent.FLAG_ONE_SHOT);
			}
			AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
			alarmMgr.cancel(alarmPendingIntent);
			alarmPendingIntent = null;

			saveState();
		}
    	
    };
    
    private long getInputTimestamp() {
    	return 1000L * (countdownHours.getValue() * 60 * 60 +
    			countdownMinutes.getValue() * 60 +
    			countdownSeconds.getValue());
    }
}
