package org.dpadgett.timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.ListView;

/**
 * This is a helper class that implements the management of tabs and all
 * details of connecting a ViewPager with associated TabHost.  It relies on a
 * trick.  Normally a tab host has a simple API for supplying a View or
 * Intent that each tab will show.  This is not sufficient for switching
 * between pages.  So instead we make the content part of the tab host
 * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
 * view to show as the tab content.  It listens to changes in tabs, and takes
 * care of switch to the correct paged in the ViewPager whenever the selected
 * tab changes.
 */
public class TabsAdapter extends FragmentPagerAdapter
        implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
    private final Context mContext;
    private final ActionBar mActionBar;
    private final ViewPager mViewPager;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    private final Map<Integer, Fragment> mSingletonMap = new HashMap<Integer, Fragment>();

    static final class TabInfo {
        private final Class<?> clss;
        private final Bundle args;

        TabInfo(Class<?> _class, Bundle _args) {
            clss = _class;
            args = _args;
        }
	}

    public TabsAdapter(Activity activity, ViewPager pager) {
        super(activity.getFragmentManager());
        mContext = activity;
        mActionBar = activity.getActionBar();
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setPageMargin(1);
        mViewPager.setPageMarginDrawable(new ListView(activity).getDivider());
        mViewPager.setOnPageChangeListener(this);
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
        TabInfo info = new TabInfo(clss, args);
        tab.setTag(info);
        tab.setTabListener(this);
        mTabs.add(info);
        mActionBar.addTab(tab);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
    	if (!mSingletonMap.containsKey(position)) {
	        TabInfo info = mTabs.get(position);
	        Fragment fragment = Fragment.instantiate(mContext, info.clss.getName(), info.args);
	        mSingletonMap.put(position, fragment);
	        return fragment;
    	} else {
    		return mSingletonMap.get(position);
    	}
    }

    public Fragment getCachedItem(int position) {
        // Do we already have this fragment?
    	if (!mSingletonMap.containsKey(position)) {
    		Log.i(getClass().getName(), "Fragment cache miss");
	        Fragment fragment = (Fragment) instantiateItem(mViewPager, position);
	        mSingletonMap.put(position, fragment);
	        return fragment;
    	} else {
    		Log.i(getClass().getName(), "Fragment cache hit");
    		return mSingletonMap.get(position);
    	}
    }

	@Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        mActionBar.setSelectedNavigationItem(position);
        SharedPreferences.Editor prefs = mContext.getSharedPreferences("TimerActivity", Context.MODE_PRIVATE).edit();
        prefs.putInt("tab", position);
        prefs.apply();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Object tag = tab.getTag();
        for (int i=0; i<mTabs.size(); i++) {
            if (mTabs.get(i) == tag) {
                mViewPager.setCurrentItem(i);
            }
        }
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }
}