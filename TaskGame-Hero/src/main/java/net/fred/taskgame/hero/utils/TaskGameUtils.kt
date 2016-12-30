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

package net.fred.taskgame.hero.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object TaskGameUtils {

    /**
     * Allow you to verify if the TaskGame app is installed on the phone or not

     * @param context any Context
     * *
     * @return true if the real TaskGame app is installed on the phone, false otherwise
     */
    fun isAppInstalled(context: Context): Boolean {
        try {
            val signatures = context.packageManager.getPackageInfo("net.fred.taskgame", PackageManager.GET_SIGNATURES).signatures
            if (signatures.size == 1 && signatures[0].hashCode() == -361897285) { // We check the signature to be sure that's not a fake one
                return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }

        return false
    }

    /**
     * Get the Intent you can launch with startActivityForResult(). The user accepted the request if RESULT_OK is sent as resultCode in onActivityResult()

     * @param context any Context
     * *
     * @param points  the number of TaskGame points you request in your game
     * *
     * @return a valid Intent you should start with startActivityForResult() or an ActivityNotFoundException exception if TaskGame is not installed
     */
    fun getRequestPointsActivityIntent(context: Context, points: Long): Intent {
        if (!isAppInstalled(context)) {
            throw ActivityNotFoundException("TaskGame app is not installed")
        }

        val intent = Intent("taskgame.intent.action.REQUEST_POINTS")
        intent.`package` = "net.fred.taskgame"
        intent.putExtra("taskgame.intent.extra.POINT_AMOUNT_NEEDED", points)
        return intent
    }
}
