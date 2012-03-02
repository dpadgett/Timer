package org.dpadgett.timer;

import android.app.Activity;
import android.view.View;

/**
 * Factory methods to create ResourceFinders from a variety of sources.
 *
 * @author dpadgett
 */
public final class ResourceFinders {
	private ResourceFinders() { }

	public static ResourceFinder from(final Activity activity) {
		return new ResourceFinder() {
			@Override
			public View findViewById(int id) {
				return activity.findViewById(id);
			}
		};
	}
	
	public static ResourceFinder from(final View view) {
		return new ResourceFinder() {
			@Override
			public View findViewById(int id) {
				return view.findViewById(id);
			}
		};
	}
}
