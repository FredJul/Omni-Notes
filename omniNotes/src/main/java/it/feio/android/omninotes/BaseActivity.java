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
package it.feio.android.omninotes;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewConfiguration;
import android.widget.Toast;

import java.lang.reflect.Field;

import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.GeocodeHelper;
import it.feio.android.omninotes.widget.ListWidgetProvider;


public class BaseActivity extends ActionBarActivity implements LocationListener {

	protected final int TRANSITION_VERTICAL = 0;
	protected final int TRANSITION_HORIZONTAL = 1;

	protected SharedPreferences prefs;

	// Location variables
	protected LocationManager locationManager;
	protected Location currentLocation;
	protected double currentLatitude;
	protected double currentLongitude;

	protected String navigation;
	protected String navigationTmp; // used for widget navigation


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_list, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Preloads shared preferences for all derived classes
		prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_MULTI_PROCESS);
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
		String navNotes = getResources().getStringArray(R.array.navigation_list_codes)[0];
		navigation = prefs.getString(Constants.PREF_NAVIGATION, navNotes);
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

	protected void showToast(CharSequence text, int duration) {
		if (prefs.getBoolean("settings_enable_info", true)) {
			Toast.makeText(getApplicationContext(), text, duration).show();
		}
	}

	protected void updateNavigation(String nav) {
		prefs.edit().putString(Constants.PREF_NAVIGATION, nav).commit();
		navigation = nav;
		navigationTmp = null;
	}

	/**
	 * Notifies App Widgets about data changes so they can update theirselves
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static void notifyAppWidgets(Context mActivity) {
		// Home widgets
		AppWidgetManager mgr = AppWidgetManager.getInstance(mActivity);
		int[] ids = mgr.getAppWidgetIds(new ComponentName(mActivity, ListWidgetProvider.class));
		mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list);

		// Dashclock
		LocalBroadcastManager.getInstance(mActivity).sendBroadcast(new Intent(Constants.INTENT_UPDATE_DASHCLOCK));
	}


	@SuppressLint("InlinedApi")
	protected void animateTransition(FragmentTransaction transaction, int direction) {
		if (direction == TRANSITION_HORIZONTAL) {
			transaction.setCustomAnimations(R.animator.fade_in_support, R.animator.fade_out_support, R.animator.fade_in_support, R.animator.fade_out_support);
		}
		if (direction == TRANSITION_VERTICAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			transaction.setCustomAnimations(
					R.animator.anim_in, R.animator.anim_out, R.animator.anim_in_pop, R.animator.anim_out_pop);
		}
	}


	protected void setActionBarTitle(String title) {
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
