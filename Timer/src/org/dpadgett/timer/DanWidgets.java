package org.dpadgett.timer;

import android.app.Activity;
import android.os.Handler;
import android.view.View;

public class DanWidgets {
	private final DanResourceFinder finder;
	private final Handler handler;
	
	public DanWidgets(DanResourceFinder finder) {
		this.finder = finder;
		this.handler = new Handler();
	}

	public DanButton getButton(int buttonId) {
		return new DanButton(handler, finder, buttonId);
	}

	public DanTextView getTextView(int textViewId) {
		return new DanTextView(handler, finder, textViewId);
	}

	public DanLinearLayout getLinearLayout(int linearLayoutId) {
		return new DanLinearLayout(handler, finder, linearLayoutId);
	}

	public DanScrollView getScrollView(int scrollViewId) {
		return new DanScrollView(handler, finder, scrollViewId);
	}

	public static DanWidgets create(final Activity activity) {
		return new DanWidgets(new DanResourceFinder() {
			@Override
			public View findViewById(int id) {
				return activity.findViewById(id);
			}
		});
	}

	public static DanWidgets create(final View view) {
		return new DanWidgets(new DanResourceFinder() {
			@Override
			public View findViewById(int id) {
				return view.findViewById(id);
			}
		});
	}
}
