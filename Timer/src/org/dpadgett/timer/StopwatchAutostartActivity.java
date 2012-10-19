package org.dpadgett.timer;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;

/**
 * Autostart. This is an activity that is "exported" and can be started
 * by other applications. One use case is an NFC tag that 
 * automatically starts a new stopwatch counter.  
 */
public class StopwatchAutostartActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = new Intent(this, TimerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(TimerActivity.START_REASON, TimerActivity.StartReason.START_REASON_AUTOSTART_STOPWATCH);
        startActivity(intent);
    }
}
