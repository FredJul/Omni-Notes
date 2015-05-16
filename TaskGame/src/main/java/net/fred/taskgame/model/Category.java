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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(databaseName = AppDatabase.NAME)
public class Category extends BaseModel implements Parcelable {

    @Column
    @PrimaryKey(autoincrement = true)
    @Expose
    public int id;
    @Column
    @Expose
    public String name = "";
    @Column
    @Expose
    public String description = "";
    @Column
    @Expose
    public String color = "";

    public Category() {
    }

    private Category(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        color = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeString(description);
        parcel.writeString(color);
    }


    @Override
    public String toString() {
        return name;
    }

    /*
     * Parcelable interface must also have a static field called CREATOR, which is an object implementing the
     * Parcelable.Creator interface. Used to un-marshal or de-serialize object from Parcel.
     */
    public static final Parcelable.Creator<Category> CREATOR = new Parcelable.Creator<Category>() {

        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        public Category[] newArray(int size) {
            return new Category[size];
        }
    };
}
