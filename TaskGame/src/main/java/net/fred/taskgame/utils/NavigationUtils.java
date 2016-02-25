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

package net.fred.taskgame.utils;

import net.fred.taskgame.model.Category;

public class NavigationUtils {

    public static final long TASKS = -2;
    public static final long FINISHED_TASKS = -1;

    /**
     * Returns actual navigation status
     */
    public static long getNavigation() {
        return PrefUtils.getLong(PrefUtils.PREF_NAVIGATION, TASKS);
    }

    public static void setNavigation(long newNavigation) {
        PrefUtils.putLong(PrefUtils.PREF_NAVIGATION, newNavigation);
    }

    public static boolean isDisplayingACategory() {
        return getNavigation() >= 0;
    }

    /**
     * Checks if passed parameters is the category user is actually navigating in
     */
    public static boolean isDisplayingCategory(Category category) {
        return (category != null && getNavigation() == category.id);
    }

}
