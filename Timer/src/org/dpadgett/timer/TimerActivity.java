/*  
 * Copyright 2012 Dan Padgett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.dpadgett.timer;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

/**
 * Main activity class. Its job is mainly to create the different
 * fragments and put them into the tab host in the right order. It
 * also handles the popup dialog to dismiss the countdown completed
 * notification, since we want this to show up regardless of which
 * fragment the user is currently using.
 * 
 * @author dpadgett
 */
public class TimerActivity extends SherlockFragmentActivity {

    static final String ACTION_SHOW_DIALOG = "org.dpadgett.timer.CountdownFragment.SHOW_DIALOG";
    static final String ACTION_DISMISS_DIALOG = "org.dpadgett.timer.CountdownFragment.DISMISS_DIALOG";
    static final String START_REASON = "START_REASON";

    public enum StartReason {
        START_REASON_AUTOSTART_STOPWATCH,
        START_REASON_NONE
    };

    private static enum Tab {
        WORLD_CLOCK(R.string.tab_title_worldclock, WorldClockFragment.class),
        STOPWATCH(R.string.tab_title_stopwatch, StopwatchFragment.class),
        COUNTDOWN(R.string.tab_title_countdown, CountdownFragment.class);

        private final int title;
        private final Class<? extends Fragment> clazz;

        private Tab(final int title, final Class<? extends Fragment> clazz) {
            this.title = title;
            this.clazz = clazz;
        }

        private int getTitle() {
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

        final Bundle extras = getIntent().getExtras();

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        final Context context = getApplicationContext();
        for (final Tab tab : Tab.values()) {
            final String tabTitle = context.getString(tab.getTitle());
            mTabsAdapter.addTab(bar.newTab().setText(tabTitle), tab.getFragmentClass(), extras);
        }

        StartReason startReason = StartReason.START_REASON_NONE;
        if (extras != null) {
            startReason = (StartReason) extras.getSerializable(START_REASON);
        }

        final SharedPreferences prefs = getSharedPreferences("TimerActivity", Context.MODE_PRIVATE);
        if (startReason == StartReason.START_REASON_AUTOSTART_STOPWATCH) {
            bar.setSelectedNavigationItem(Tab.STOPWATCH.ordinal());
        } else if (prefs.contains("tab")) {
            bar.setSelectedNavigationItem(prefs.getInt("tab", 0));
        } else if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

        alarmDialog = new AlertDialog.Builder(this)
                .setTitle(context.getString(R.string.countdown_timer_finished))
                .setPositiveButton(context.getString(R.string.alarm_dialog_dismiss),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dialog.dismiss();
                                final Intent intent = new Intent(TimerActivity.this, AlarmService.class)
                                        .putExtra("startAlarm", false).putExtra("fromFragment", true)
                                        .setAction("stopAlarm");
                                TimerActivity.this.startService(intent);
                            }
                        }).setCancelable(false).create();

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

    private final BroadcastReceiver showDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (alarmDialog != null) {
                alarmDialog.show();
            }

            final CountdownFragment countdown = (CountdownFragment) mTabsAdapter.getCachedItem(Tab.COUNTDOWN.ordinal());
            countdown.inputModeOn();
        }
    };

    private final BroadcastReceiver dismissDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            // alarmDialog could be null due to a variety of race conditions
            if (alarmDialog != null) {
                alarmDialog.dismiss();
            }
        }
    };
}
