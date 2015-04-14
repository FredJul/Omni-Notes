package net.fred.taskgame.model;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.fred.taskgame.utils.PrefUtils;

import java.util.List;

public class SyncData {
    @Expose
    public long lastSyncDate;
    @Expose
    public List<Category> categories;
    @Expose
    public List<Task> tasks;

    public static SyncData getLastData() {
        SyncData syncData = new SyncData();
        syncData.lastSyncDate = System.currentTimeMillis();
        PrefUtils.putLong(PrefUtils.PREF_LAST_SYNC_DATE, syncData.lastSyncDate);
        syncData.categories = new Select().from(Category.class).queryList();
        syncData.tasks = new Select().from(Task.class).queryList();

        return syncData;
    }
}