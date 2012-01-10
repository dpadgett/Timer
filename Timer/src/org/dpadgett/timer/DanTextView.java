package org.dpadgett.timer;

import android.os.Handler;
import android.widget.TextView;

public class DanTextView {
	private final Handler handler;
	private final TextView textView;

	public DanTextView(Handler handler, DanResourceFinder finder, int textViewId) {
		this.handler = handler;
		this.textView = (TextView) finder.findViewById(textViewId);
	}

	public void setText(final String textToSet) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				textView.setText(textToSet);
			}
		});
	}
}
