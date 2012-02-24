package org.dpadgett.timer;

import android.app.Activity;
import android.os.Handler;
import android.view.View;

/**
 * A library of wrapper widgets which may be mutated from any thread by deferring
 * the mutation to the ui thread's message handler.
 *
 * @author dpadgett
 * @deprecated should no longer be necessary - can usually be achieved by
 *             better methods such as extending the widget type.
 */
@Deprecated
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

	public static DanWidgets create(Activity activity) {
		return new DanWidgets(finderFrom(activity));
	}
	
	public static DanResourceFinder finderFrom(final Activity activity) {
		return new DanResourceFinder() {
			@Override
			public View findViewById(int id) {
				return activity.findViewById(id);
			}
		};
	}
	
	public static DanResourceFinder finderFrom(final View view) {
		return new DanResourceFinder() {
			@Override
			public View findViewById(int id) {
				return view.findViewById(id);
			}
		};
	}

	public static DanWidgets create(final View view) {
		return new DanWidgets(finderFrom(view));
	}
}
