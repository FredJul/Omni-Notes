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

import android.content.Context;
import android.content.Intent;

import com.google.firebase.database.DatabaseReference;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.DbUtils;
import net.fred.taskgame.utils.EqualityChecker;

import org.parceler.Parcel;

import java.util.Calendar;

@Parcel
@Table(database = AppDatabase.class)
public class Task extends IdBasedModel {

    public final static long LOW_POINT_REWARD = 20;
    public final static long NORMAL_POINT_REWARD = 50;
    public final static long HIGH_POINT_REWARD = 100;
    public final static long VERY_HIGH_POINT_REWARD = 200;

    @Column
    public String title = "";
    @Column
    public String content = "";
    @Column
    public long creationDate;
    @Column
    public long lastModificationDate;
    @Column
    public int displayPriority;
    @Column
    public boolean isFinished;
    @Column
    public long alarmDate;
    @Column
    public boolean isChecklist;
    @Column
    public long categoryId = INVALID_ID;
    @Column
    public long pointReward = NORMAL_POINT_REWARD;
    @Column
    public String questId = "";

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
        questId = task.questId;
        mCategory = task.mCategory;
    }

    public Category getCategory() {
        if (categoryId == INVALID_ID) {
            mCategory = null;
            return null;
        }

        if (mCategory == null) {
            mCategory = new Select().from(Category.class).where(Category_Table.id.is(categoryId)).querySingle();
        }

        return mCategory;
    }

    public void setCategory(Category category) {
        categoryId = category != null ? category.id : INVALID_ID;
        mCategory = category;
    }

    public boolean hasAlarmInFuture() {
        if (alarmDate > Calendar.getInstance().getTimeInMillis()) {
            return true;
        }
        return false;
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
                alarmDate, isChecklist, categoryId, pointReward, questId};
        Object[] b = {task.id, task.title, task.content, task.creationDate, task.lastModificationDate, task.displayPriority, task.isFinished,
                task.alarmDate, task.isChecklist, task.categoryId, task.pointReward, task.questId};
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

    @Override
    public void save() {
        if (creationDate == 0) {
            creationDate = System.currentTimeMillis();
        }

        super.save();

        DatabaseReference firebase = DbUtils.getFirebaseTasksNode();
        if (firebase != null) {
            firebase.child(String.valueOf(id)).setValue(this);
        }
    }

    public void saveWithoutFirebase() {
        if (creationDate == 0) {
            creationDate = System.currentTimeMillis();
        }

        super.save();
    }

    @Override
    public void delete() {
        DatabaseReference firebase = DbUtils.getFirebaseTasksNode();
        if (firebase != null) {
            firebase.child(String.valueOf(id)).removeValue();
        }

        super.delete();
    }

    public void deleteWithoutFirebase() {
        super.delete();
    }
}
