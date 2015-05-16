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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raizlabs.android.dbflow.sql.language.Delete;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.SyncData;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.utils.Dog;
import net.fred.taskgame.utils.GeocodeHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.widget.ListWidgetProvider;

import java.lang.reflect.Field;


public class BaseActivity extends BaseGameActivity implements LocationListener {

    public final int TRANSITION_VERTICAL = 0;
    public final int TRANSITION_HORIZONTAL = 1;

    // Location variables
    protected LocationManager locationManager;
    protected Location currentLocation;
    public double currentLatitude;
    public double currentLongitude;

    public String navigation;
    public String navigationTmp; // used for widget navigation


    protected BaseActivity() {
        super(BaseGameActivity.CLIENT_ALL); // we need snapshot support
    }

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
        Toast.makeText(getApplicationContext(), text, duration).show();
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }


    public String getNavigationTmp() {
        return navigationTmp;
    }


    @Override
    public void onSignInFailed() {
    }

    @Override
    public void onSignInSucceeded() {
        sync();
    }

    private void sync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Dog.i("sync started");
                // Open the saved game using its name.
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(), "save", true).await();

                Snapshot snapshot = result.getSnapshot();
                Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

                // Check the result of the open operation
                if (result.getStatus().isSuccess()) {
                    // Read the byte content of the saved game.
                    try {
                        byte[] savedBytes = snapshot.getSnapshotContents().readFully();

                        String json = new String(savedBytes);
                        Dog.i("get back " + json);
                        SyncData syncedData = gson.fromJson(json, SyncData.class);

                        if (syncedData.lastSyncDate > PrefUtils.getLong(PrefUtils.PREF_LAST_SYNC_DATE, -1)) {
                            Delete.tables(Category.class, Task.class);
                            for (Category cat : syncedData.categories) {
                                cat.save();
                            }
                            for (Task task : syncedData.tasks) {
                                task.save();
                            }
                        }
                    } catch (Exception e) {
                        Dog.e("ERROR", e);
                    }

                    String json = gson.toJson(SyncData.getLastData());
                    Dog.i("write " + json);

                    // Set the data payload for the snapshot
                    snapshot.getSnapshotContents().writeBytes(json.getBytes());

                    // Create the change operation
                    SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder().build();

                    // Commit the operation
                    Games.Snapshots.commitAndClose(getApiClient(), snapshot, metadataChange);

                } else if (result.getStatus().getStatusCode() == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
                    Dog.i("Save conflict, use last version");

                    String json = gson.toJson(SyncData.getLastData());
                    Dog.i("write " + json);

                    Snapshot conflictSnapshot = result.getConflictingSnapshot();
                    // Resolve between conflicts by selecting the newest of the conflicting snapshots.
                    Snapshot resolvedSnapshot = snapshot;

                    if (snapshot.getMetadata().getLastModifiedTimestamp() <
                            conflictSnapshot.getMetadata().getLastModifiedTimestamp()) {
                        resolvedSnapshot = conflictSnapshot;
                    }
                    // Set the data payload for the snapshot
                    snapshot.getSnapshotContents().writeBytes(json.getBytes());
                    Games.Snapshots.resolveConflict(getApiClient(), result.getConflictId(), resolvedSnapshot);
                }
            }
        }).start();
    }
}
