package org.dpadgett.timer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Main activity class.  Its job is mainly to create the different fragments and put them
 * into the tab host in the right order.  It also handles the popup dialog to dismiss the
 * countdown completed notification, since we want this to show up regardless of which
 * fragment the user is currently using.
 *
 * @author dpadgett
 */
public class TimerActivity extends SherlockFragmentActivity {

	static final String ACTION_SHOW_DIALOG = "org.dpadgett.timer.CountdownFragment.SHOW_DIALOG";
	static final String ACTION_DISMISS_DIALOG = "org.dpadgett.timer.CountdownFragment.DISMISS_DIALOG";
	
	private static enum Tab {
		WORLD_CLOCK("World Clock", WorldClockFragment.class),
		STOPWATCH("Stopwatch", StopwatchFragment.class),
		COUNTDOWN("Countdown", CountdownFragment.class);

		private final String title;
		private final Class<? extends Fragment> clazz;

		private Tab(String title, Class<? extends Fragment> clazz) {
			this.title = title;
			this.clazz = clazz;
		}
		
		private String getTitle() {
			return title;
		}
		
		private Class<? extends Fragment> getFragmentClass() {
			return clazz;
		}
	}
	
	private AlertDialog alarmDialog;
	private TabsAdapter mTabsAdapter;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final ViewPager mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.viewPager);
        mViewPager.setOffscreenPageLimit(Tab.values().length);
        setContentView(mViewPager);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayShowHomeEnabled(false);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        for (Tab tab : Tab.values()) {
	        mTabsAdapter.addTab(bar.newTab().setText(tab.getTitle()),
	                tab.getFragmentClass(), null);
        }

        SharedPreferences prefs = getSharedPreferences("TimerActivity", Context.MODE_PRIVATE);
        if (prefs.contains("tab")) {
            bar.setSelectedNavigationItem(prefs.getInt("tab", 0));
        } else if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

		alarmDialog = new AlertDialog.Builder(this)
				.setTitle("Countdown timer finished")
				.setPositiveButton("Dismiss",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent intent = new Intent(TimerActivity.this, AlarmService.class)
									.putExtra("startAlarm", false).putExtra("fromFragment", true)
									.setAction("stopAlarm");
								TimerActivity.this.startService(intent);
							}
						})
				.setCancelable(false)
				.create();

		if (prefs.getBoolean("countdownDialogShowing", false)) {
			alarmDialog.show();
		}

		getApplicationContext().registerReceiver(showDialogReceiver, new IntentFilter(ACTION_SHOW_DIALOG));
		getApplicationContext().registerReceiver(dismissDialogReceiver, new IntentFilter(ACTION_DISMISS_DIALOG));
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
    	getApplicationContext().unregisterReceiver(showDialogReceiver);
    	getApplicationContext().unregisterReceiver(dismissDialogReceiver);
    	if (alarmDialog.isShowing()) {
    		alarmDialog.dismiss();
    	}
	}

    private BroadcastReceiver showDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (alarmDialog != null) {
				alarmDialog.show();
			}

			CountdownFragment countdown =
					(CountdownFragment) mTabsAdapter.getCachedItem(Tab.COUNTDOWN.ordinal());
			countdown.inputModeOn();
		}
    };

    private BroadcastReceiver dismissDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// alarmDialog could be null due to a variety of race conditions
			if (alarmDialog != null) {
				alarmDialog.dismiss();
			}
		}
    };
}