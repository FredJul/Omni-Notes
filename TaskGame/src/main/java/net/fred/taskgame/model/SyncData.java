package net.fred.taskgame.model;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.fred.taskgame.utils.PrefUtils;

public class SyncData {
    @Expose
    public long lastSyncDate;
    @Expose
    public Category[] categories;
    @Expose
    public Task[] tasks;

    public static SyncData getLastData() {
        SyncData syncData = new SyncData();
        syncData.lastSyncDate = System.currentTimeMillis();
        PrefUtils.putLong(PrefUtils.PREF_LAST_SYNC_DATE, syncData.lastSyncDate);
        syncData.categories = new Select().from(Category.class).queryList().toArray(new Category[]{});
        syncData.tasks = new Select().from(Task.class).queryList().toArray(new Task[]{});

        return syncData;
    }
}