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
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import net.fred.taskgame.utils.EqualityChecker;
import net.fred.taskgame.utils.date.DateHelper;

import java.util.ArrayList;
import java.util.List;

@Table(databaseName = AppDatabase.NAME)
public class Task extends BaseModel implements Parcelable {

    @Column
    @PrimaryKey(autoincrement = true)
    @Expose
    public int id;
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
    public boolean isTrashed;
    @Column
    @Expose
    public long alarmDate;
    @Column
    @Expose
    public double latitude;
    @Column
    @Expose
    public double longitude;
    @Column
    @Expose
    public String address = "";
    @Column
    @Expose
    public boolean isChecklist;
    @Column
    @Expose
    int categoryId;

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
        isTrashed = task.isTrashed;
        alarmDate = task.alarmDate;
        latitude = task.latitude;
        longitude = task.longitude;
        address = task.address;
        isChecklist = task.isChecklist;
        categoryId = task.categoryId;
        mCategory = task.mCategory;

        if (task.mAttachmentsList != null) {
            mAttachmentsList = new ArrayList<>(task.mAttachmentsList);
        }
    }

    private Task(Parcel in) {
        id = in.readInt();
        title = in.readString();
        content = in.readString();
        creationDate = in.readLong();
        lastModificationDate = in.readLong();
        isTrashed = in.readInt() == 1;
        alarmDate = in.readLong();
        latitude = in.readDouble();
        longitude = in.readDouble();
        address = in.readString();
        isChecklist = in.readInt() == 1;
        categoryId = in.readInt();
        mCategory = in.readParcelable(Category.class.getClassLoader());
        in.readList(mAttachmentsList, Attachment.class.getClassLoader());
    }

    public List<Attachment> getAttachmentsList() {
        if (mAttachmentsList == null) {
            mAttachmentsList = new Select().from(Attachment.class).where(Condition.column(Attachment$Table.TASKID).is(id)).queryList();
        }

        return mAttachmentsList;
    }

    @SuppressWarnings("unused")
    public void setAttachmentsList(List<Attachment> attachmentsList) {
        mAttachmentsList = attachmentsList;
    }

    public Category getCategory() {
        if (categoryId == 0) {
            mCategory = null;
            return null;
        }

        if (mCategory == null) {
            mCategory = new Select().from(Category.class).where(Condition.column(Category$Table.ID).is(categoryId)).querySingle();
        }

        return mCategory;
    }

    public void setCategory(Category category) {
        categoryId = category != null ? category.id : 0;
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

        Object[] a = {id, title, content, creationDate, lastModificationDate, isTrashed,
                alarmDate, latitude, longitude, address, isChecklist, categoryId, getAttachmentsList()};
        Object[] b = {task.id, task.title, task.content, task.creationDate, task.lastModificationDate, task.isTrashed,
                task.alarmDate, task.latitude, task.longitude, task.address, task.isChecklist, task.categoryId, task.getAttachmentsList()};
        if (EqualityChecker.check(a, b)) {
            res = true;
        }

        return res;
    }

    public String toString() {
        return title;
    }

    public String getCreationShort(Context mContext) {
        if (creationDate == 0) {
            return "";
        }
        return DateHelper.getDateTimeShort(mContext, creationDate);
    }

    public String getLastModificationShort(Context mContext) {
        if (lastModificationDate == 0) {
            return "";
        }
        return DateHelper.getDateTimeShort(mContext, lastModificationDate);
    }

    public String getAlarmShort(Context mContext) {
        if (alarmDate == 0) {
            return "";
        }
        return DateHelper.getDateTimeShort(mContext, alarmDate);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(content);
        parcel.writeLong(creationDate);
        parcel.writeLong(lastModificationDate);
        parcel.writeInt(isTrashed ? 1 : 0);
        parcel.writeLong(alarmDate);
        parcel.writeDouble(latitude);
        parcel.writeDouble(longitude);
        parcel.writeString(address);
        parcel.writeInt(isChecklist ? 1 : 0);
        parcel.writeInt(categoryId);
        parcel.writeParcelable(getCategory(), 0);
        parcel.writeList(getAttachmentsList());
    }

    /*
     * Parcelable interface must also have a static field called CREATOR, which is an object implementing the
     * Parcelable.Creator interface. Used to un-marshal or de-serialize object from Parcel.
     */
    public static final Parcelable.Creator<Task> CREATOR = new Parcelable.Creator<Task>() {

        public Task createFromParcel(Parcel in) {
            return new Task(in);
        }

        public Task[] newArray(int size) {
            return new Task[size];
        }
    };
}
