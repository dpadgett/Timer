package org.dpadgett.timer;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

public class DanLinearLayout {
	private final Handler handler;
	private final LinearLayout linearLayout;

	public DanLinearLayout(Handler handler, DanResourceFinder finder, int linearLayoutId) {
		this.handler = handler;
		this.linearLayout = (LinearLayout) finder.findViewById(linearLayoutId);
	}

	public void addView(final View viewToAdd) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				linearLayout.addView(viewToAdd);
			}
		});
	}
	
	public void removeAllViews() {
		handler.post(new Runnable() {
			@Override
			public void run() {
				linearLayout.removeAllViews();
			}
		});
	}
	
	public Context getContext() {
		return linearLayout.getContext();
	}
}
