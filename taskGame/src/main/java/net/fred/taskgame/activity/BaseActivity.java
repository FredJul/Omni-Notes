/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.fred.taskgame.activity;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewConfiguration;
import android.widget.Toast;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.GeocodeHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.widget.ListWidgetProvider;

import java.lang.reflect.Field;


public class BaseActivity extends ActionBarActivity implements LocationListener {

	public final int TRANSITION_VERTICAL = 0;
	public final int TRANSITION_HORIZONTAL = 1;

	// Location variables
	protected LocationManager locationManager;
	protected Location currentLocation;
	public double currentLatitude;
	public double currentLongitude;

	public String navigation;
	public String navigationTmp; // used for widget navigation


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_list, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Starts location manager
		locationManager = GeocodeHelper.getLocationManager(this, this);
		// Force menu overflow icon
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
		}
		super.onCreate(savedInstanceState);
	}


	@Override
	protected void onResume() {
		super.onResume();
		// Navigation selected
		String navTasks = getResources().getStringArray(R.array.navigation_list_codes)[0];
		navigation = PrefUtils.getString(PrefUtils.PREF_NAVIGATION, navTasks);
	}


	@Override
	public void onStop() {
		super.onStop();
		if (locationManager != null)
			locationManager.removeUpdates(this);
	}


	@Override
	public void onLocationChanged(Location location) {
		currentLocation = location;
		currentLatitude = currentLocation.getLatitude();
		currentLongitude = currentLocation.getLongitude();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {

	}

	public void showToast(CharSequence text, int duration) {
		if (PrefUtils.getBoolean("settings_enable_info", true)) {
			Toast.makeText(getApplicationContext(), text, duration).show();
		}
	}

	public void updateNavigation(String nav) {
		PrefUtils.putString(PrefUtils.PREF_NAVIGATION, nav);
		navigation = nav;
		navigationTmp = null;
	}

	/**
	 * Notifies App Widgets about data changes so they can update themselves
	 */
	public static void notifyAppWidgets(Context mActivity) {
		// Home widgets
		AppWidgetManager mgr = AppWidgetManager.getInstance(mActivity);
		int[] ids = mgr.getAppWidgetIds(new ComponentName(mActivity, ListWidgetProvider.class));
		mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);
	}

	public void animateTransition(FragmentTransaction transaction, int direction) {
		if (direction == TRANSITION_HORIZONTAL) {
			transaction.setCustomAnimations(R.animator.fade_in_support, R.animator.fade_out_support, R.animator.fade_in_support, R.animator.fade_out_support);
		}
		if (direction == TRANSITION_VERTICAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			transaction.setCustomAnimations(
					R.animator.anim_in, R.animator.anim_out, R.animator.anim_in_pop, R.animator.anim_out_pop);
		}
	}


	public void setActionBarTitle(String title) {
		// Creating a spannable to support custom fonts on ActionBar
		int actionBarTitle = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
		android.widget.TextView actionBarTitleView = (android.widget.TextView) getWindow().findViewById(actionBarTitle);
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
		if (actionBarTitleView != null) {
			actionBarTitleView.setTypeface(font);
		}

		if (getSupportActionBar() != null) {
			getSupportActionBar().setTitle(title);
		}
	}


	public String getNavigationTmp() {
		return navigationTmp;
	}


}
