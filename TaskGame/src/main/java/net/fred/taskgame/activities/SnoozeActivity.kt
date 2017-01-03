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

package net.fred.taskgame.activities

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import net.fred.taskgame.listeners.OnReminderPickedListener
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.Constants
import net.fred.taskgame.utils.DbUtils
import net.fred.taskgame.utils.PrefUtils
import net.fred.taskgame.utils.date.ReminderPickers
import java.util.*

class SnoozeActivity : FragmentActivity(), OnReminderPickedListener {

    private var task: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        task = DbUtils.getTask(intent.extras.getString(Constants.EXTRA_TASK_ID))

        // If an alarm has been fired a notification must be generated
        if (Constants.ACTION_SNOOZE == intent.action) {
            val snoozeDelay = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, "10")
            task!!.alarmDate = Calendar.getInstance().timeInMillis + Integer.parseInt(snoozeDelay) * 60 * 1000
            task!!.setupReminderAlarm(this)
            finish()
        } else if (Constants.ACTION_POSTPONE == intent.action) {
            ReminderPickers(this, this).pick(task!!.alarmDate)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(Constants.EXTRA_TASK_ID, task!!.id)
            intent.action = Constants.ACTION_NOTIFICATION_CLICK
            startActivity(intent)
        }
        removeNotification(task!!)
    }


    private fun removeNotification(task: Task) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(task.id!!.hashCode())
    }

    override fun onReminderPicked(reminder: Long) {
        task!!.alarmDate = reminder
        task!!.setupReminderAlarm(this)
        finish()
    }

    override fun onReminderDismissed() {
        finish()
    }
}
