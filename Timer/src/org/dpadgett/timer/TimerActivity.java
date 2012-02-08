package org.dpadgett.timer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class TimerActivity extends Activity {

	static final String ACTION_SHOW_DIALOG = "org.dpadgett.timer.CountdownFragment.SHOW_DIALOG";
	static final String ACTION_DISMISS_DIALOG = "org.dpadgett.timer.CountdownFragment.DISMISS_DIALOG";
	
	private AlertDialog alarmDialog;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        
        ViewPager mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.viewPager);
        setContentView(mViewPager);

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayShowHomeEnabled(false);

        TabsAdapter mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText("Stopwatch"),
                StopwatchFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText("World Clock"),
                WorldClockFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText("Countdown"),
                CountdownFragment.class, null);

        if (savedInstanceState != null) {
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

		getApplicationContext().registerReceiver(showDialogReceiver, new IntentFilter(ACTION_SHOW_DIALOG));
		getApplicationContext().registerReceiver(dismissDialogReceiver, new IntentFilter(ACTION_DISMISS_DIALOG));
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
    	getApplicationContext().unregisterReceiver(showDialogReceiver);
    	getApplicationContext().unregisterReceiver(dismissDialogReceiver);
	}

    private BroadcastReceiver showDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (alarmDialog != null) {
				alarmDialog.show();
			}
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