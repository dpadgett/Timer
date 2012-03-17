package org.dpadgett.compat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.os.Build;

public class ArrayAdapter<T> extends android.widget.ArrayAdapter<T> {

	private static final boolean COMPAT_NEEDED =
			Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

	private static class CompatArrayAdapter<T> extends ArrayAdapter<T> {
		private CompatArrayAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		private CompatArrayAdapter(Context context, int resource, int textViewResourceId) {
			super(context, resource, textViewResourceId);
		}

		private CompatArrayAdapter(Context context, int textViewResourceId, T[] objects) {
			super(context, textViewResourceId, objects);
		}

		private CompatArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
			super(context, textViewResourceId, objects);
		}

		private CompatArrayAdapter(Context context, int resource, int textViewResourceId,
				T[] objects) {
			super(context, resource, textViewResourceId, objects);
		}

		private CompatArrayAdapter(Context context, int resource, int textViewResourceId,
				List<T> objects) {
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public void addAll(Collection<? extends T> collection) {
			for (T item : collection) {
				add(item);
			}
		}

		@Override
		public void addAll(T... items) {
			addAll(Arrays.asList(items));
		}
	}

	public static <T> ArrayAdapter<T> newArrayAdapter(Context context, int textViewResourceId) {
		return COMPAT_NEEDED ? new CompatArrayAdapter<T>(context, textViewResourceId) :
			new ArrayAdapter<T>(context, textViewResourceId);
	}

	public static <T> ArrayAdapter<T> newArrayAdapter(Context context, int resource, int textViewResourceId) {
		return COMPAT_NEEDED ? new CompatArrayAdapter<T>(context, resource, textViewResourceId) :
			new ArrayAdapter<T>(context, resource, textViewResourceId);
	}

	public static <T> ArrayAdapter<T> newArrayAdapter(Context context, int textViewResourceId, T[] objects) {
		return COMPAT_NEEDED ? new CompatArrayAdapter<T>(context, textViewResourceId, objects) :
			new ArrayAdapter<T>(context, textViewResourceId, objects);
	}

	public static <T> ArrayAdapter<T> newArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
		return COMPAT_NEEDED ? new CompatArrayAdapter<T>(context, textViewResourceId, objects) :
			new ArrayAdapter<T>(context, textViewResourceId, objects);
	}

	public static <T> ArrayAdapter<T> newArrayAdapter(Context context, int resource, int textViewResourceId,
			T[] objects) {
		return COMPAT_NEEDED ? new CompatArrayAdapter<T>(context, resource, textViewResourceId, objects) :
			new ArrayAdapter<T>(context, resource, textViewResourceId, objects);
	}

	public static <T> ArrayAdapter<T> newArrayAdapter(Context context, int resource, int textViewResourceId,
			List<T> objects) {
		return COMPAT_NEEDED ? new CompatArrayAdapter<T>(context, resource, textViewResourceId, objects) :
			new ArrayAdapter<T>(context, resource, textViewResourceId, objects);
	}

	public ArrayAdapter(Context context, int resource, int textViewResourceId,
			List<T> objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ArrayAdapter(Context context, int resource, int textViewResourceId,
			T[] objects) {
		super(context, resource, textViewResourceId, objects);
	}

	public ArrayAdapter(Context context, int resource, int textViewResourceId) {
		super(context, resource, textViewResourceId);
	}

	public ArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
		super(context, textViewResourceId, objects);
	}

	public ArrayAdapter(Context context, int textViewResourceId, T[] objects) {
		super(context, textViewResourceId, objects);
	}

	public ArrayAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
	}

	@Override
	public void addAll(Collection<? extends T> collection) {
		super.addAll(collection);
	}

	@Override
	public void addAll(T... items) {
		super.addAll(items);
	}

}
