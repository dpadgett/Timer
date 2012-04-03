package org.dpadgett.timer;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Utility class to provide alarm sound selection capabilities.  Caches
 * and auto-refreshes alarm list asynchronously.
 *
 * @author dpadgett
 */
public class AlarmSelector {
	
	private static final long DELAY_MILLIS = 5 * 1000;

	private final Spinner selector;
	private final Context context;
	private List<String> names;
	private List<String> uris;
	private List<String> paths;
	private ArrayAdapter<String> alarmTonesAdapter;
	private final Handler handler;
	private final Runnable reload = new Runnable() {
		@Override
		public void run() {
			new Thread() {
				@Override
				public void run() {
					List<String> oldNames = names;
					fetchAlarms();
					if (names != oldNames) {
						handler.post(new Runnable() {
							@Override
							public void run() {
								updateAlarms();
							}
						});
					}
				}
			}.start();
			handler.postDelayed(this, DELAY_MILLIS);
		}
	};

	public AlarmSelector(Spinner selector) {
		this.selector = selector;
		this.context = selector.getContext();
		names = new ArrayList<String>();
		uris = new ArrayList<String>();
		paths = new ArrayList<String>();
		init();
		handler = new Handler();
		handler.postDelayed(reload, DELAY_MILLIS);
	}

	private void init() {
		reloadCache();

		alarmTonesAdapter =
				new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
						names);
		alarmTonesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selector.setAdapter(alarmTonesAdapter);
        selector.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				SharedPreferences.Editor prefs = 
						context.getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();
				prefs.putString("alarmUri", uris.get(position).toString());
				prefs.commit();
				Log.i(getClass().getName(), "Saved uri " + uris.get(position));
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
	
	private void updateAlarms() {
		restoreState();
		alarmTonesAdapter.notifyDataSetChanged();
	}

	private void reloadCache() {
		SharedPreferences prefs =
				context.getSharedPreferences("Countdown_alarmSelector", Context.MODE_PRIVATE);
		if (!prefs.contains("paths_0")) {
			Log.i(getClass().getName(), "Cache miss...");
			fetchAlarms();
			return;
		}
		
		List<String> newPaths = new ArrayList<String>();
		List<String> newNames = new ArrayList<String>();
		List<String> newUris = new ArrayList<String>();
		
		{
			String keyPrefix = "paths_";
			for (int i = 0; prefs.contains(keyPrefix + i); i++) {
				newPaths.add(prefs.getString(keyPrefix + i, null));
			}
		}

		{
			String keyPrefix = "names_";
			for (int i = 0; prefs.contains(keyPrefix + i); i++) {
				newNames.add(prefs.getString(keyPrefix + i, null));
			}
		}

		{
			String keyPrefix = "uris_";
			for (int i = 0; prefs.contains(keyPrefix + i); i++) {
				newUris.add(prefs.getString(keyPrefix + i, null));
			}
		}
		
		if (!names.equals(newNames) || !uris.equals(newUris) || !paths.equals(newPaths)) {
			names = newNames;
			uris = newUris;
			paths = newPaths;
		}

		Log.i(getClass().getName(), "Cache hit!");
	}

	private void fetchAlarms() {
		RingtoneManager manager = new RingtoneManager(context);
		manager.setType(RingtoneManager.TYPE_ALARM);
		Cursor c = manager.getCursor();
		if (c == null) {
			return;
		}
		
		List<String> newPaths = new ArrayList<String>();
		List<String> newNames = new ArrayList<String>();
		List<String> newUris = new ArrayList<String>();
		
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			newNames.add(c.getString(RingtoneManager.TITLE_COLUMN_INDEX));
			newUris.add(manager.getRingtoneUri(c.getPosition()).toString());
			newPaths.add(getRealPathFromURI(manager.getRingtoneUri(c.getPosition())));
			System.out.println("path: " + newPaths.get(newPaths.size() - 1));
		}
		c.close();
		
		if (!names.equals(newNames) || !uris.equals(newUris) || !paths.equals(newPaths)) {
			names = newNames;
			uris = newUris;
			paths = newPaths;
		}

		saveCache();
	}

	private void saveCache() {
		SharedPreferences.Editor prefs =
				context.getSharedPreferences("Countdown_alarmSelector", Context.MODE_PRIVATE).edit();

		prefs.clear();

		{
			String keyPrefix = "paths_";
			for (int i = 0; i < paths.size(); i++) {
				prefs.putString(keyPrefix + i, paths.get(i));
			}
		}

		{
			String keyPrefix = "names_";
			for (int i = 0; i < names.size(); i++) {
				prefs.putString(keyPrefix + i, names.get(i));
			}
		}

		{
			String keyPrefix = "uris_";
			for (int i = 0; i < uris.size(); i++) {
				prefs.putString(keyPrefix + i, uris.get(i));
			}
		}
		
		prefs.commit();

		Log.i(getClass().getName(), "Cache updated");
	}

	private String getRealPathFromURI(Uri contentUri) {
        String toReturn = "unknown";
        try {
	        Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
	        int column_index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
	        if (column_index != -1) {
		        cursor.moveToFirst();
		        toReturn = cursor.getString(column_index);
	        } else {
	        	cursor.moveToFirst();
	        	toReturn = getRealPathFromURI(
	        			Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow("value"))));
	        }
	        cursor.close();
        } catch (SQLiteException e) {
        	e.printStackTrace();
        }
        return toReturn;
    }

	public void restoreState() {
        SharedPreferences prefs =
				context.getSharedPreferences("Countdown", Context.MODE_PRIVATE);

        Uri alarmUri = AlarmService.getRingtoneUri(prefs);
        Log.i(getClass().getName(), "alarmUri path is " + getRealPathFromURI(alarmUri));
		int idx = uris.indexOf(alarmUri.toString());
		if (idx == -1) {
			idx = paths.indexOf(getRealPathFromURI(alarmUri));
		}
		if (idx != -1) {
			selector.setSelection(idx);
		} else {
			Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
			if (ringtone == null) {
				// in this case our default url is bogus, so we should fix it
				int sel = selector.getSelectedItemPosition();
				SharedPreferences.Editor prefsEdit = 
						context.getSharedPreferences("Countdown", Context.MODE_PRIVATE).edit();
				prefsEdit.putString("alarmUri", uris.get(sel));
				prefsEdit.commit();
				Log.i(getClass().getName(), "Saved default uri " + uris.get(sel).toString());
			} else {
				Log.i(getClass().getName(), "ringtone path: " + ringtone + " vs " + Settings.System.DEFAULT_ALARM_ALERT_URI.getPath());
				alarmTonesAdapter.add(
						ringtone.getTitle(context));
				uris.add(alarmUri.toString());
				paths.add(getRealPathFromURI(alarmUri));
				selector.setSelection(uris.size() - 1);
			}
		}
	}

	public void destroy() {
		handler.removeCallbacks(reload);
	}
}
