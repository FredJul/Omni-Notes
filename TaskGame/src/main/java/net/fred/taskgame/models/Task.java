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
package net.fred.taskgame.models;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.database.DatabaseReference;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.models.providers.ContentDatabaseProvider;
import net.fred.taskgame.models.providers.LocalDatabaseProvider;
import net.fred.taskgame.receivers.AlarmReceiver;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbUtils;
import net.fred.taskgame.utils.EqualityChecker;
import net.frju.androidquery.annotation.DbField;
import net.frju.androidquery.annotation.DbModel;
import net.frju.androidquery.database.ModelListener;
import net.frju.androidquery.gen.Q;
import net.frju.androidquery.operation.condition.Where;

import org.parceler.Parcel;

import java.util.Calendar;
import java.util.UUID;

@Parcel
@DbModel(localDatabaseProvider = LocalDatabaseProvider.class, contentDatabaseProvider = ContentDatabaseProvider.class)
public class Task implements ModelListener {

    public final static long LOW_POINT_REWARD = 20;
    public final static long NORMAL_POINT_REWARD = 50;
    public final static long HIGH_POINT_REWARD = 100;
    public final static long VERY_HIGH_POINT_REWARD = 200;

    @DbField(primaryKey = true)
    public String id;
    @DbField
    public String title = "";
    @DbField
    public String content = "";
    @DbField
    public long creationDate;
    @DbField
    public long lastModificationDate;
    @DbField
    public int displayPriority;
    @DbField
    public boolean isFinished;
    @DbField
    public long alarmDate;
    @DbField
    public boolean isChecklist;
    @DbField
    public String categoryId;
    @DbField
    public long pointReward = NORMAL_POINT_REWARD;

    private transient Category mCategory;

    public Task() {
    }

    public Task(Task task) {
        id = task.id;
        title = task.title;
        content = task.content;
        creationDate = task.creationDate;
        lastModificationDate = task.lastModificationDate;
        displayPriority = task.displayPriority;
        isFinished = task.isFinished;
        alarmDate = task.alarmDate;
        isChecklist = task.isChecklist;
        categoryId = task.categoryId;
        pointReward = task.pointReward;
        mCategory = task.mCategory;
    }

    public Category getCategory() {
        if (categoryId == null) {
            mCategory = null;
            return null;
        }

        if (mCategory == null) {
            mCategory = Q.Category.select().where(Where.where(Q.Category.ID, Where.Op.IS, categoryId)).querySingle();
        }

        return mCategory;
    }

    public void setCategory(Category category) {
        categoryId = category != null ? category.id : null;
        mCategory = category;
    }

    public boolean hasAlarmInFuture() {
        return alarmDate > Calendar.getInstance().getTimeInMillis();
    }

    public boolean equals(Object o) {
        boolean res = false;
        Task task;
        try {
            task = (Task) o;
        } catch (Exception e) {
            return res;
        }

        Object[] a = {id, title, content, creationDate, lastModificationDate, displayPriority, isFinished,
                alarmDate, isChecklist, categoryId, pointReward};
        Object[] b = {task.id, task.title, task.content, task.creationDate, task.lastModificationDate, task.displayPriority, task.isFinished,
                task.alarmDate, task.isChecklist, task.categoryId, task.pointReward};
        if (EqualityChecker.check(a, b)) {
            res = true;
        }

        return res;
    }

    public String toString() {
        return title;
    }

    public void share(Context context) {
        String titleText = title;
        String contentText = titleText
                + System.getProperty("line.separator")
                + content;


        Intent shareIntent = new Intent();

        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
        shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

        context.startActivity(Intent.createChooser(shareIntent, context.getResources().getString(R.string.share_message_chooser)));
    }

    public void saveInFirebase() {
        DatabaseReference firebase = DbUtils.getFirebaseTasksNode();
        if (firebase != null) {
            firebase.child(id).setValue(this);
        }
    }

    public void deleteInFirebase() {
        DatabaseReference firebase = DbUtils.getFirebaseTasksNode();
        if (firebase != null) {
            firebase.child(id).removeValue();
        }
    }

    public String[] computeListItemTitleAndContent() {

        // Defining title and content texts
        String titleText = title.trim();
        String contentText = content.trim();

        if (titleText.length() <= 0 && contentText.length() >= 0) {
            int wrapIndex = contentText.indexOf('\n');
            if (wrapIndex != -1) {
                titleText = contentText.substring(0, wrapIndex);
                contentText = contentText.substring(titleText.length()).trim();
            } else {
                titleText = contentText;
                contentText = "";
            }
        }

        // Replacing checkmarks symbols with html entities
        if (isChecklist) {
            titleText = titleText.replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM, "✓ ")
                    .replace(it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM, "□ ");
            contentText = contentText
                    .replace(it.feio.android.checklistview.interfaces.Constants.CHECKED_SYM, "✓ ")
                    .replace(it.feio.android.checklistview.interfaces.Constants.UNCHECKED_SYM, "□ ");
        }

        return new String[]{titleText, contentText};
    }

    public void setupReminderAlarm(Context context) {
        if (hasAlarmInFuture()) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(Constants.INTENT_TASK_ID, id); // Do not use parcelable with API 24+ for PendingIntentve
            PendingIntent sender = PendingIntent.getBroadcast(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmDate, sender);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, alarmDate, sender);
            }
        }
    }

    public void cancelReminderAlarm(Context context) {
        if (alarmDate != 0) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent p = PendingIntent.getBroadcast(context, (int) creationDate, intent, 0);
            am.cancel(p);
            p.cancel();
        }
    }

    @Override
    public void onPreInsert() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        if (creationDate == 0) {
            creationDate = System.currentTimeMillis();
        }
    }

    @Override
    public void onPreUpdate() {

    }

    @Override
    public void onPreDelete() {
        cancelReminderAlarm(MainApplication.getContext());
    }
}
