package org.dpadgett.timer;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class CountdownFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.countdown, container, false);
        Button startButton = (Button) rootView.findViewById(R.id.startButton);
        final Handler handler = new Handler();
        startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				handler.post(new Runnable() {
					@Override
					public void run() {
						LinearLayout inputs = (LinearLayout) rootView.findViewById(R.id.inputsLayout);
						
						LinearLayout runningLayout = new LinearLayout(inputs.getContext());
						runningLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0f));
						runningLayout.setGravity(Gravity.CENTER_HORIZONTAL);
						runningLayout.setId(R.id.inputsInnerLayout);
						
						TextView timerText = new TextView(inputs.getContext());
						timerText.setText("00:00:00");
						timerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 50f);
						
						inputs.removeAllViews();
					}
				});
			}
        });
        return rootView;
    }

}
