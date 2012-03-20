package org.dpadgett.compat;

import android.content.res.Resources;

public final class R {
	private R() { }
	
	private static final int resolveId(String name, String defType, String defPackage, int def) {
		int nativeId = Resources.getSystem().getIdentifier(name, defType, defPackage);
		return nativeId == 0 ? def : nativeId;
	}
	
	public static final class styleable {
    	public static final int NumberPicker_solidColor = 
    			resolveId("NumberPicker_solidColor", "styleable", "android", 0);
    	public static final int NumberPicker_flingable = 
    			resolveId("NumberPicker_flingable", "styleable", "android", 0);
    	public static final int NumberPicker_selectionDivider = 
    			resolveId("NumberPicker_selectionDivider", "styleable", "android", 0);
    	public static final int NumberPicker_selectionDividerHeight = 
    			resolveId("NumberPicker_selectionDividerHeight", "styleable", "android", 0);
    	public static final int NumberPicker_minHeight = 
    			resolveId("NumberPicker_minHeight", "styleable", "android", 0);
    	public static final int NumberPicker_maxHeight = 
    			resolveId("NumberPicker_maxHeight", "styleable", "android", 0);
    	public static final int NumberPicker_minWidth = 
    			resolveId("NumberPicker_minWidth", "styleable", "android", 0);
    	public static final int NumberPicker_maxWidth = 
    			resolveId("NumberPicker_maxWidth", "styleable", "android", 0);
	}
}
