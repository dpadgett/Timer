package org.dpadgett.timer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.DigitalClock;
import android.widget.LinearLayout;

public class WorldClockFragment extends Fragment {
	
	private DanResourceFinder finder;
	private Context context;
	private Handler uiHandler;
	
	private interface EasyLayoutInflater {
		View inflate(int id);
	}
	
	private EasyLayoutInflater inflater;

	public WorldClockFragment() {
	}

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
    	uiHandler = new Handler();
        View rootView = inflater.inflate(R.layout.world_clock, container, false);
        finder = DanWidgets.finderFrom(rootView);
        context = rootView.getContext();
        this.inflater = new EasyLayoutInflater() {
			@Override
			public View inflate(int id) {
				return inflater.inflate(id, container, false);
			}
        };
        LinearLayout addClockView = (LinearLayout) finder.findViewById(R.id.addClockView);
        addClockView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				view.setBackgroundResource(android.R.drawable.list_selector_background);
				uiHandler.post(new Runnable() {
					@Override
					public void run() {
						addNewClock();
					}
				});
			}
        });
        //Resources.getSystem().getIdentifier("list_selector_background", "drawable", "android");
        return rootView;
    }

	/**
	 * Adds a new clock to the view
	 */
	private void addNewClock() {
		LinearLayout clocksLayout = (LinearLayout) finder.findViewById(R.id.clocksLayout);
		LinearLayout newClock = (LinearLayout) inflater.inflate(R.layout.single_world_clock);
		DigitalClock clock = (DigitalClock) newClock.findViewById(R.id.digitalClock);
		String[] allIDs = TimeZone.getAvailableIDs();
		final String myID = allIDs[new Random().nextInt(allIDs.length)];
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
		
		clocksLayout.addView(newClock);
	}
}
