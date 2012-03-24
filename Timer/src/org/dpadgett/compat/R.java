package org.dpadgett.compat;

import android.content.res.Resources;
import android.os.Build;

public final class R {
	private R() { }
	
	private static final boolean COMPAT_NEEDED = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

	private static final int resolveId(String name, String defType, String defPackage, int def) {
		int nativeId = Resources.getSystem().getIdentifier(name, defType, defPackage);
		return COMPAT_NEEDED ? def : nativeId;
	}
	
	private static final int[] resolveArray(String name, String defType, int[] def) {
		int[] nativeIds = def;
		if (COMPAT_NEEDED) {
			return nativeIds;
		}
		try {
			Class<?> clazz = Class.forName("android.R$" + defType);
	        nativeIds = (int[]) clazz.getField(name).get(clazz);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return nativeIds;
	}
	
	public static final class styleable {
    	public static final int NumberPicker_solidColor = 
    			resolveId("NumberPicker_solidColor", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_solidColor);
    	public static final int NumberPicker_flingable = 
    			resolveId("NumberPicker_flingable", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_flingable);
    	public static final int NumberPicker_selectionDivider = 
    			resolveId("NumberPicker_selectionDivider", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_selectionDivider);
    	public static final int NumberPicker_selectionDividerHeight = 
    			resolveId("NumberPicker_selectionDividerHeight", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_selectionDividerHeight);
    	public static final int NumberPicker_minHeight = 
    			resolveId("NumberPicker_minHeight", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_minHeight);
    	public static final int NumberPicker_maxHeight = 
    			resolveId("NumberPicker_maxHeight", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_maxHeight);
    	public static final int NumberPicker_minWidth = 
    			resolveId("NumberPicker_minWidth", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_minWidth);
    	public static final int NumberPicker_maxWidth = 
    			resolveId("NumberPicker_maxWidth", "styleable", "android", org.dpadgett.timer.R.styleable.NumberPicker_maxWidth);
    	public static final int[] NumberPicker = 
    			resolveArray("NumberPicker", "styleable", org.dpadgett.timer.R.styleable.NumberPicker);
	}
	
	public static final class layout {
		public static final int number_picker =
				resolveId("number_picker", "layout", "android", org.dpadgett.timer.R.layout.number_picker);
	}
	
	public static final class id {
		public static final int increment =
				resolveId("increment", "id", "android", org.dpadgett.timer.R.id.increment);
		public static final int decrement =
				resolveId("decrement", "id", "android", org.dpadgett.timer.R.id.decrement);
		public static final int numberpicker_input =
				resolveId("numberpicker_input", "id", "android", org.dpadgett.timer.R.id.numberpicker_input);
	}
	
	public static final class string {
		public static final int number_picker_increment_scroll_action =
				resolveId("number_picker_increment_scroll_action", "string", "android", org.dpadgett.timer.R.string.number_picker_increment_scroll_action);
		public static final int number_picker_increment_scroll_mode =
				resolveId("number_picker_increment_scroll_mode", "string", "android", org.dpadgett.timer.R.string.number_picker_increment_scroll_mode);
	}
	
	public static final class drawable {
		public static final int numberpicker_selection_divider =
				resolveId("numberpicker_selection_divider", "drawable", "android", org.dpadgett.timer.R.drawable.numberpicker_selection_divider);
	}
}
