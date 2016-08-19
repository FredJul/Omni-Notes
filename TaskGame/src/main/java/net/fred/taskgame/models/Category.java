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

import android.support.annotation.ColorInt;

import com.google.firebase.database.DatabaseReference;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;

import net.fred.taskgame.utils.DbUtils;

import org.parceler.Parcel;

@Parcel
@Table(database = AppDatabase.class)
public class Category extends IdBasedModel {

    @Column
    public String name = "";
    @Column
    public String description = "";
    @Column
    @ColorInt
    public int color;
    @Column
    public long creationDate;

    public Category() {
    }

    @Override
    public void save() {
        if (creationDate == 0) {
            creationDate = System.currentTimeMillis();
        }

        super.save();

        DatabaseReference firebase = DbUtils.getFirebaseCategoriesNode();
        if (firebase != null) {
            firebase.child(id).setValue(this);
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
        DatabaseReference firebase = DbUtils.getFirebaseCategoriesNode();
        if (firebase != null) {
            firebase.child(id).removeValue();
        }

        super.delete();
    }

    public void deleteWithoutFirebase() {
        super.delete();
    }
}
