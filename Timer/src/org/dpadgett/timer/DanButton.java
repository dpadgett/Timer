package org.dpadgett.timer;

import android.os.Handler;
import android.widget.Button;

public class DanButton {
	private final Handler handler;
	private final Button button;

	public DanButton(Handler handler, DanResourceFinder finder, int buttonId) {
		this.handler = handler;
		this.button = (Button) finder.findViewById(buttonId);
	}

	public void setText(final String textToSet) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				button.setText(textToSet);
			}
		});
	}
}
