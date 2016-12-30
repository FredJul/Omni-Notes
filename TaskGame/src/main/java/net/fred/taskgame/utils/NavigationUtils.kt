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

package net.fred.taskgame.utils

import net.fred.taskgame.models.Category

object NavigationUtils {

    val TASKS = "-2"
    val FINISHED_TASKS = "-1"

    /**
     * Returns actual navigation status
     */
    var navigation: String
        get() = PrefUtils.getString(PrefUtils.PREF_NAVIGATION, TASKS)
        set(newNavigation) = PrefUtils.putString(PrefUtils.PREF_NAVIGATION, newNavigation)

    val isDisplayingACategory: Boolean
        get() = navigation.length > 2

    /**
     * Checks if passed parameters is the category user is actually navigating in
     */
    fun isDisplayingCategory(category: Category?): Boolean {
        return category != null && navigation == category.id
    }

}
