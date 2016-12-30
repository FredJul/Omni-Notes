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

package net.fred.taskgame.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import net.fred.taskgame.utils.DbUtils

class AlarmRestoreOnRebootService : Service() {

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val context = applicationContext

        // Retrieves all tasks with reminder set
        val tasks = DbUtils.getTasksWithReminder(true)

        for (task in tasks) {
            task.setupReminderAlarm(context)
        }

        return Service.START_NOT_STICKY
    }
}
