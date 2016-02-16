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
package net.fred.taskgame.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.EqualityChecker;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Parcel
@Table(database = AppDatabase.class)
public class Task extends IdBasedModel {

    public final static long LOW_POINT_REWARD = 20;
    public final static long NORMAL_POINT_REWARD = 50;
    public final static long HIGH_POINT_REWARD = 100;
    public final static long VERY_HIGH_POINT_REWARD = 200;

    @Column
    @Expose
    public String title = "";
    @Column
    @Expose
    public String content = "";
    @Column
    @Expose
    public long creationDate;
    @Column
    @Expose
    public long lastModificationDate;
    @Column
    @Expose
    public int displayPriority;
    @Column
    @Expose
    public boolean isFinished;
    @Column
    @Expose
    public long alarmDate;
    @Column
    @Expose
    public boolean isChecklist;
    @Column
    @Expose
    long categoryId = INVALID_ID;
    @Column
    @Expose
    public long pointReward = NORMAL_POINT_REWARD;
    @Column
    @Expose
    public String questId = "";

    private transient Category mCategory;
    private transient List<Attachment> mAttachmentsList;

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

        if (task.mAttachmentsList != null) {
            mAttachmentsList = new ArrayList<>(task.mAttachmentsList);
        }
    }

    public List<Attachment> getAttachmentsList() {
        if (mAttachmentsList == null) {
            if (id != INVALID_ID) {
                mAttachmentsList = new Select().from(Attachment.class).where(Attachment_Table.taskId.is(id)).queryList();
            } else {
                mAttachmentsList = new ArrayList<>();
            }
        }

        return mAttachmentsList;
    }

    @SuppressWarnings("unused")
    public void setAttachmentsList(List<Attachment> attachmentsList) {
        mAttachmentsList = attachmentsList;
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

    public boolean equals(Object o) {
        boolean res = false;
        Task task;
        try {
            task = (Task) o;
        } catch (Exception e) {
            return res;
        }

        Object[] a = {id, title, content, creationDate, lastModificationDate, displayPriority, isFinished,
                alarmDate, isChecklist, categoryId, pointReward, questId, getAttachmentsList()};
        Object[] b = {task.id, task.title, task.content, task.creationDate, task.lastModificationDate, task.displayPriority, task.isFinished,
                task.alarmDate, task.isChecklist, task.categoryId, task.pointReward, task.questId, task.getAttachmentsList()};
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
        // Prepare sharing intent with only text
        if (getAttachmentsList().size() == 0) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
            shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

            // Intent with single image attachment
        } else if (getAttachmentsList().size() == 1) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType(getAttachmentsList().get(0).mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, getAttachmentsList().get(0).uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
            shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

            // Intent with multiple images
        } else if (getAttachmentsList().size() > 1) {
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            ArrayList<Uri> uris = new ArrayList<>();
            // A check to decide the mime type of attachments to share is done here
            HashMap<String, Boolean> mimeTypes = new HashMap<>();
            for (Attachment attachment : getAttachmentsList()) {
                uris.add(attachment.uri);
                mimeTypes.put(attachment.mimeType, true);
            }
            // If many mime types are present a general type is assigned to intent
            if (mimeTypes.size() > 1) {
                shareIntent.setType("*/*");
            } else {
                shareIntent.setType((String) mimeTypes.keySet().toArray()[0]);
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
            shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);
        }

        context.startActivity(Intent.createChooser(shareIntent, context.getResources().getString(R.string.share_message_chooser)));
    }

    @Override
    public void save() {
        if (creationDate == 0) {
            creationDate = System.currentTimeMillis();
        }

        super.save();
    }
}
