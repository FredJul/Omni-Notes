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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;

import net.fred.taskgame.utils.Constants;

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

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child(Constants.FIREBASE_USERS_NODE).child(user.getUid()).child(Constants.FIREBASE_CATEGORIES_NODE).child(String.valueOf(id)).setValue(this);
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child(Constants.FIREBASE_USERS_NODE).child(user.getUid()).child(Constants.FIREBASE_CATEGORIES_NODE).child(String.valueOf(id)).removeValue();
        }

        super.delete();
    }

    public void deleteWithoutFirebase() {
        super.delete();
    }
}
