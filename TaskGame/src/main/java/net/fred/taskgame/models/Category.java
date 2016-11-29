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

import net.fred.taskgame.models.providers.ContentDatabaseProvider;
import net.fred.taskgame.models.providers.LocalDatabaseProvider;
import net.fred.taskgame.utils.DbUtils;
import net.frju.androidquery.annotation.Column;
import net.frju.androidquery.annotation.Table;
import net.frju.androidquery.database.ModelListener;

import org.parceler.Parcel;

import java.util.UUID;

@Parcel
@Table(localDatabaseProvider = LocalDatabaseProvider.class, contentDatabaseProvider = ContentDatabaseProvider.class)
public class Category implements ModelListener {

    @Column(primaryKey = true)
    public String id;
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
