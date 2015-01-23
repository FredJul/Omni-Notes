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

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import it.feio.android.omninotes.utils.BitmapCache;
import it.feio.android.omninotes.utils.Constants;

public class OmniNotes extends Application {

	private static Context mContext;

	static SharedPreferences prefs;
	private static BitmapCache mBitmapCache;


	@Override
	public void onCreate() {
		mContext = getApplicationContext();
		prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_MULTI_PROCESS);


		// Instantiate bitmap cache
		mBitmapCache = new BitmapCache(getApplicationContext(), 0, 0, getExternalCacheDir());

		super.onCreate();
	}

	public static Context getAppContext() {
		return OmniNotes.mContext;
	}

	/*
	 * Returns the Google Analytics instance.
	 */
	public static BitmapCache getBitmapCache() {
		return mBitmapCache;
	}


	/**
	 * Performs a full app restart
	 */
	public static void restartApp(final Context mContext) {

		if (MainActivity.getInstance() != null) {
			MainActivity.getInstance().finish();
			Intent intent = new Intent(mContext, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			int mPendingIntentId = 123456;
			PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, mPendingIntentId, intent,
					PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
			System.exit(0);
		}
	}

}
