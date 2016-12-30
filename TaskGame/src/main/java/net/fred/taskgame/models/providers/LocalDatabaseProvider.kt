/*
 * Copyright (c) 2012-2017 Frederic Julian
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.fred.taskgame.models.providers

import android.content.Context
import android.database.sqlite.SQLiteDatabase

import net.frju.androidquery.database.BaseLocalDatabaseProvider
import net.frju.androidquery.database.Resolver
import net.frju.androidquery.gen.Q

class LocalDatabaseProvider(context: Context) : BaseLocalDatabaseProvider(context) {

    override fun getDbName(): String {
        return "game_data"
    }

    override fun getDbVersion(): Int {
        return 2
    }

    override fun getResolver(): Resolver {
        return Q.getResolver()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        super.onUpgrade(db, oldVersion, newVersion)

        // Put here your migration code
    }
}