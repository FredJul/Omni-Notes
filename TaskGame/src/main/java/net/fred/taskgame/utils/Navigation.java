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

import android.content.Context;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.model.Category;

import java.util.ArrayList;
import java.util.Arrays;

public class Navigation {

    public static final int TASKS = 0;
    public static final int FINISHED = 1;
    public static final int CATEGORY = 2;


    /**
     * Returns actual navigation status
     */
    public static int getNavigation() {
        Context mContext = MainApplication.getContext();
        String[] navigationListCodes = mContext.getResources().getStringArray(R.array.navigation_list_codes);
        String navigation = PrefUtils.getString(PrefUtils.PREF_NAVIGATION, navigationListCodes[0]);

        if (navigationListCodes[TASKS].equals(navigation)) {
            return TASKS;
        } else if (navigationListCodes[FINISHED].equals(navigation)) {
            return FINISHED;
        } else {
            return CATEGORY;
        }
    }


    /**
     * Retrieves category currently shown
     *
     * @return id of category or 0 if current navigation is not a category
     */
    public static long getCategory() {
        if (getNavigation() == CATEGORY) {
            String navTasks = MainApplication.getContext().getResources().getStringArray(R.array.navigation_list_codes)[0];
            return Long.parseLong(PrefUtils.getString(PrefUtils.PREF_NAVIGATION, navTasks));
        } else {
            return 0;
        }
    }


    /**
     * Checks if passed parameters is the actual navigation status
     */
    public static boolean checkNavigation(int navigationToCheck) {
        return checkNavigation(new Integer[]{navigationToCheck});
    }

    public static boolean checkNavigation(Integer[] navigationsToCheck) {
        boolean res = false;
        int navigation = getNavigation();
        for (int navigationToCheck : new ArrayList<>(Arrays.asList(navigationsToCheck))) {
            if (navigation == navigationToCheck) {
                res = true;
                break;
            }
        }
        return res;
    }


    /**
     * Checks if passed parameters is the category user is actually navigating in
     */
    public static boolean checkNavigationCategory(Category categoryToCheck) {
        Context mContext = MainApplication.getContext();
        String[] navigationListCodes = mContext.getResources().getStringArray(R.array.navigation_list_codes);
        String navigation = PrefUtils.getString(PrefUtils.PREF_NAVIGATION, navigationListCodes[0]);
        return (categoryToCheck != null && navigation.equals(String.valueOf(categoryToCheck.id)));
    }

}
