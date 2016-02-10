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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import net.fred.taskgame.R;
import net.fred.taskgame.service.SyncService;
import net.fred.taskgame.utils.PrefUtils;


@SuppressLint("Registered")
public class BaseActivity extends BaseGameActivity {

    public long mWidgetCatId = -1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Restores savedInstanceState
        if (savedInstanceState != null) {
            mWidgetCatId = savedInstanceState.getLong("mWidgetCatId");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("mWidgetCatId", mWidgetCatId);
    }

    public void updateNavigation(String nav) {
        PrefUtils.putString(PrefUtils.PREF_NAVIGATION, nav);
        mWidgetCatId = -1;
    }

    public void setActionBarTitle(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }


    public long getWidgetCatId() {
        return mWidgetCatId;
    }


    @Override
    public void onSignInFailed() {
        Toast.makeText(this, "sign in failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSignInSucceeded() {
        PrefUtils.putBoolean(PrefUtils.PREF_ALREADY_LOGGED_TO_GAMES, true);
        SyncService.triggerSync(this, true);
    }
}
