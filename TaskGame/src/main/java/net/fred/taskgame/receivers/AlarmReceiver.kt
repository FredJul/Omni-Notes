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

package net.fred.taskgame.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import net.fred.taskgame.R
import net.fred.taskgame.activities.SnoozeActivity
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.*
import net.fred.taskgame.utils.date.DateHelper

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val task = DbUtils.getTask(intent.extras.getString(Constants.EXTRA_TASK_ID))
            if (task != null) {
                createNotification(context, task)
            }
        } catch (e: Exception) {
            Dog.e("Error while creating reminder notification", e)
        }

    }

    private fun createNotification(context: Context, task: Task) {
        // Prepare text contents
        val title = if (task.title.isNotEmpty()) task.title else task.content
        val alarmText = DateHelper.getDateTimeShort(context, task.alarmDate)
        val text = if (task.title.isNotEmpty() && task.content.isNotEmpty()) task.content else alarmText

        val doneIntent = Intent(context, SnoozeActivity::class.java)
        doneIntent.action = Constants.ACTION_DONE
        doneIntent.putExtra(Constants.EXTRA_TASK_ID, task.id) // Do not use parcelable with API 24+ for PendingIntent
        doneIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val piDone = PendingIntent.getActivity(context, 0, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val snoozeIntent = Intent(context, SnoozeActivity::class.java)
        snoozeIntent.action = Constants.ACTION_SNOOZE
        snoozeIntent.putExtra(Constants.EXTRA_TASK_ID, task.id) // Do not use parcelable with API 24+ for PendingIntent
        snoozeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val piSnooze = PendingIntent.getActivity(context, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val postponeIntent = Intent(context, SnoozeActivity::class.java)
        postponeIntent.action = Constants.ACTION_POSTPONE
        postponeIntent.putExtra(Constants.EXTRA_TASK_ID, task.id) // Do not use parcelable with API 24+ for PendingIntent
        snoozeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val piPostpone = PendingIntent.getActivity(context, 0, postponeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val snoozeDelay = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, "10")

        // Next create the bundle and initialize it
        val intent = Intent(context, SnoozeActivity::class.java)
        intent.putExtra(Constants.EXTRA_TASK_ID, task.id) // Do not use parcelable with API 24+ for PendingIntent

        // Sets the Activity to start in a new, empty task
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        // Workaround to fix problems with multiple notifications
        intent.action = Constants.ACTION_NOTIFICATION_CLICK + java.lang.Long.toString(System.currentTimeMillis())

        // Creates the PendingIntent
        val notifyIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationsHelper = NotificationsHelper(context)
                .createNotification(R.drawable.ic_assignment_white_24dp, title, notifyIntent)
                .setLargeIcon(R.mipmap.ic_launcher)
                .setMessage(text)

        notificationsHelper.builder
                ?.addAction(R.drawable.ic_done_white_24dp, context.getString(R.string.button_done), piDone)
                ?.addAction(R.drawable.ic_update_white_24dp, context.getString(R.string.snooze, java.lang.Long.valueOf(snoozeDelay)), piSnooze)
                ?.addAction(R.drawable.ic_alarm_white_24dp, context.getString(R.string.set_reminder), piPostpone)

        // Ringtone options
        val ringtone = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_RINGTONE, "")
        if (!ringtone.isEmpty()) {
            notificationsHelper.setRingtone(ringtone)
        }

        // Vibration options
        val pattern = longArrayOf(500, 500)
        if (PrefUtils.getBoolean(PrefUtils.PREF_SETTINGS_NOTIFICATION_VIBRATION, true))
            notificationsHelper.setVibration(pattern)

        notificationsHelper.show(task.id!!.hashCode().toLong())
    }
}
