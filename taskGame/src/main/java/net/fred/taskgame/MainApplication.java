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

package net.fred.taskgame;

import android.app.Application;
import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowManager;

import net.fred.taskgame.utils.BitmapCache;

public class MainApplication extends Application {

	private static Context mContext;
	private static BitmapCache mBitmapCache;

	@Override
	public void onCreate() {
		super.onCreate();

		mContext = getApplicationContext();

		// Instantiate bitmap cache
		mBitmapCache = new BitmapCache(getApplicationContext(), 0, 0, getExternalCacheDir());

		FlowManager.init(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		FlowManager.destroy();
	}

	public static Context getContext() {
		return MainApplication.mContext;
	}

	/*
	 * Returns the Google Analytics instance.
	 */
	public static BitmapCache getBitmapCache() {
		return mBitmapCache;
	}
}
