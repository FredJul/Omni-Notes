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
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewConfiguration;
import android.widget.Toast;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.quest.Quest;
import com.google.android.gms.games.quest.QuestBuffer;
import com.google.android.gms.games.quest.Quests;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Attachment_Table;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.SyncData;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task_Table;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.Dog;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.widget.ListWidgetProvider;

import java.lang.reflect.Field;
import java.nio.charset.Charset;


@SuppressLint("Registered")
public class BaseActivity extends BaseGameActivity {

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

    public void showToast(CharSequence text, int duration) {
        Toast.makeText(getApplicationContext(), text, duration).show();
    }

    public void updateNavigation(String nav) {
        PrefUtils.putString(PrefUtils.PREF_NAVIGATION, nav);
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
        Toast.makeText(this, "sign in failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSignInSucceeded() {
        sync();
    }

    private void sync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Dog.i("sync started");
                    // Open the saved game using its name.
                    Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(), "save", true).await();

                    Snapshot snapshot = result.getSnapshot();
                    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

                    // Check the result of the open operation
                    if (result.getStatus().isSuccess()) {
                        // Read the byte content of the saved game.
                        byte[] savedBytes = snapshot.getSnapshotContents().readFully();

                        String json = new String(savedBytes);
                        Dog.i("get back " + json);
                        SyncData syncedData = gson.fromJson(json, SyncData.class);

                        if (syncedData.lastSyncDate > PrefUtils.getLong(PrefUtils.PREF_LAST_SYNC_DATE, -1)) {
                            PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, syncedData.currentPoints);
                            Delete.tables(Category.class, Task.class);
                            for (Category cat : syncedData.categories) {
                                Dog.i("write cat " + cat);
                                cat.save();
                            }
                            for (Task task : syncedData.tasks) {
                                task.save();
                            }
                        }

                        SyncData syncData = SyncData.getLastData();
                        json = gson.toJson(syncData);
                        Dog.i("write " + json);

                        // Sync leaderboard
                        Games.Leaderboards.submitScore(getApiClient(), Constants.LEADERBOARD_ID, syncData.currentPoints);

                        // Set the data payload for the snapshot
                        snapshot.getSnapshotContents().writeBytes(json.getBytes());

                        // Create the change operation
                        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder().build();

                        // Commit the operation
                        Games.Snapshots.commitAndClose(getApiClient(), snapshot, metadataChange);
                        PrefUtils.putLong(PrefUtils.PREF_LAST_SYNC_DATE, syncData.lastSyncDate);

                    } else if (result.getStatus().getStatusCode() == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
                        Dog.i("Save conflict, use last version");

                        SyncData syncData = SyncData.getLastData();
                        String json = gson.toJson(syncData);
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
                        PrefUtils.putLong(PrefUtils.PREF_LAST_SYNC_DATE, syncData.lastSyncDate);
                    } else {
                        Dog.i("error status: " + result.getStatus().getStatusCode());
                        return; // We just got a timeout, it's maybe because we left, it's better to not continue
                    }

                    Dog.i("retrieve quests");
                    Quests.LoadQuestsResult questsResult = Games.Quests.load(getApiClient(), new int[]{Games.Quests.SELECT_ACCEPTED}, Games.Quests.SORT_ORDER_ENDING_SOON_FIRST, false).await();
                    if (questsResult.getStatus().isSuccess()) {
                        Dog.i("retrieve quests success");
                        QuestBuffer quests = questsResult.getQuests();
                        for (Quest quest : quests) {
                            Dog.i("quest: " + quest);
                            String questId = quest.getQuestId();
                            Task task = new Select().from(Task.class).where(Task_Table.questId.eq(questId)).querySingle();
                            if (task == null) {
                                task = new Task();
                            }

                            task.questId = questId;
                            task.title = quest.getName();
                            task.content = quest.getDescription();
                            String reward = new String(quest.getCurrentMilestone().getCompletionRewardData(), Charset.forName("UTF-8"));
                            Dog.i("quest reward: " + reward);
                            task.pointReward = Integer.valueOf(reward);

                            task.save();

                            // Delete task's attachments
                            Delete.table(Attachment.class, Attachment_Table.taskId.eq(task.id));
                            Attachment attachment = new Attachment();
                            attachment.taskId = task.id;
                            attachment.mimeType = Constants.MIME_TYPE_IMAGE;
                            attachment.uri = Uri.parse(quest.getIconImageUrl());
                            attachment.save();
                        }
                    }
                } catch (Throwable t) {
                    Dog.e("ERROR", t);
                }
            }
        }).start();
    }
}
