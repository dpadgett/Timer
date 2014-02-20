/*  
 * Copyright 2012 Dan Padgett
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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
