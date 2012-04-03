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

	private final Spinner selector;
	private final Context context;
	private List<String> names;
	private List<String> uris;
	private List<String> paths;
	private ArrayAdapter<String> alarmTonesAdapter;

	public AlarmSelector(Spinner selector) {
		this.selector = selector;
		this.context = selector.getContext();
		names = new ArrayList<String>();
		uris = new ArrayList<String>();
		paths = new ArrayList<String>();
		init();
	}

	private void init() {
		fetchAlarms();

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

	private void fetchAlarms() {
		RingtoneManager manager = new RingtoneManager(context);
		manager.setType(RingtoneManager.TYPE_ALARM);
		Cursor c = manager.getCursor();
		if (c == null) {
			return;
		}
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			names.add(c.getString(RingtoneManager.TITLE_COLUMN_INDEX));
			uris.add(manager.getRingtoneUri(c.getPosition()).toString());
			paths.add(getRealPathFromURI(manager.getRingtoneUri(c.getPosition())));
			System.out.println("path: " + paths.get(paths.size() - 1));
		}
		c.close();
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
}
