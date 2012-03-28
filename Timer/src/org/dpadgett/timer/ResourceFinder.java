package org.dpadgett.timer;

import android.view.View;

/**
 * Interface to encapsulate the ability to find a view by id.
 *
 * @author dpadgett
 */
public interface ResourceFinder {
	View findViewById(int id);
}
