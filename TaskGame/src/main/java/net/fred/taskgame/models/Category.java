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

import net.fred.taskgame.models.providers.LocalDatabaseProvider;
import net.fred.taskgame.utils.DbUtils;
import net.frju.androidquery.annotation.DbField;
import net.frju.androidquery.annotation.DbModel;
import net.frju.androidquery.database.ModelListener;

import org.parceler.Parcel;

import java.util.UUID;

@Parcel
@DbModel(databaseProvider = LocalDatabaseProvider.class)
public class Category implements ModelListener {

    @DbField(primaryKey = true)
    public String id;
    @DbField
    public String name = "";
    @DbField
    public String description = "";
    @DbField
    @ColorInt
    public int color;
    @DbField
    public long creationDate;

    public Category() {
    }

    public void saveInFirebase() {
        DatabaseReference firebase = DbUtils.getFirebaseCategoriesNode();
        if (firebase != null) {
            firebase.child(id).setValue(this);
        }
    }

    public void deleteInFirebase() {
        DatabaseReference firebase = DbUtils.getFirebaseCategoriesNode();
        if (firebase != null) {
            firebase.child(id).removeValue();
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

    }
}
