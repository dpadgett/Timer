package org.dpadgett.timer;

import android.os.Handler;
import android.widget.ScrollView;

public class DanScrollView {
	private final Handler handler;
	private final ScrollView scrollView;

	public DanScrollView(Handler handler, DanResourceFinder finder, int scrollViewId) {
		this.handler = handler;
		this.scrollView = (ScrollView) finder.findViewById(scrollViewId);
	}

	/**
	 * @see ScrollView#fullScroll(int)
	 */
	public void fullScroll(final int direction) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				scrollView.fullScroll(direction);
			}
		});
	}
}
