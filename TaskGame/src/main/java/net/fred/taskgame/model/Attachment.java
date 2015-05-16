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

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(databaseName = AppDatabase.NAME)
public class Attachment extends BaseModel implements Parcelable {

    @Column
    @PrimaryKey(autoincrement = true)
    public int id;
    @Column
    public int taskId;
    @Column
    public String name = "";
    @Column
    public long size;
    @Column
    public long length;
    @Column
    public String mimeType = "";
    @Column
    public Uri uri = Uri.EMPTY;

    public Attachment() {
    }

    private Attachment(Parcel in) {
        id = in.readInt();
        taskId = in.readInt();
        name = in.readString();
        size = in.readLong();
        length = in.readLong();
        mimeType = in.readString();
        uri = Uri.parse(in.readString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeInt(taskId);
        parcel.writeString(name);
        parcel.writeLong(size);
        parcel.writeLong(length);
        parcel.writeString(mimeType);
        parcel.writeString(uri.toString());
    }

    /*
    * Parcelable interface must also have a static field called CREATOR, which is an object implementing the
    * Parcelable.Creator interface. Used to un-marshal or de-serialize object from Parcel.
    */
    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {

        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };
}
