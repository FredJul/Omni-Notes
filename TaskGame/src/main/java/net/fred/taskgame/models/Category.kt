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

package net.fred.taskgame.models

import android.support.annotation.ColorInt
import net.fred.taskgame.models.providers.LocalDatabaseProvider
import net.fred.taskgame.utils.DbUtils
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.database.ModelListener
import org.parceler.Parcel
import java.util.*

@Parcel(Parcel.Serialization.BEAN)
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Category : ModelListener {

    @DbField(primaryKey = true)
    var id: String? = null
    @DbField
    var name = ""
    @DbField
    var description = ""
    @DbField
    @ColorInt
    var color = 0
    @DbField
    var creationDate = 0L

    fun saveInFirebase() {
        DbUtils.firebaseCategoriesNode?.child(id)?.setValue(this)
    }

    fun deleteInFirebase() {
        DbUtils.firebaseCategoriesNode?.child(id)?.removeValue()
    }

    override fun onPreInsert() {
        if (id == null) {
            id = UUID.randomUUID().toString()
        }

        if (creationDate == 0L) {
            creationDate = System.currentTimeMillis()
        }
    }

    override fun onPreUpdate() {

    }

    override fun onPreDelete() {

    }
}
