package org.dpadgett.compat;

import android.content.res.Resources;

public final class R {
	private R() { }
	
	private static final int resolveId(String name, String defType, String defPackage, int def) {
		int nativeId = Resources.getSystem().getIdentifier(name, defType, defPackage);
		return nativeId == 0 ? def : nativeId;
	}
	
	private static final int[] resolveArray(String name, String defType, int[] def) {
		int[] nativeIds = def;
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
}
