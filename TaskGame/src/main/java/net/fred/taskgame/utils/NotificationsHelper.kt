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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.Builder


class NotificationsHelper(private val context: Context) {

    var builder: Builder? = null
        private set

    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    /**
     * Creation of notification on operations completed
     */
    fun createNotification(smallIcon: Int, title: String, notifyIntent: PendingIntent): NotificationsHelper {
        builder = NotificationCompat.Builder(context).setSmallIcon(smallIcon).setContentTitle(title)
                .setAutoCancel(true)
        builder?.setContentIntent(notifyIntent)
        return this
    }


    fun setLargeIcon(largeIconBitmap: Bitmap): NotificationsHelper {
        builder?.setLargeIcon(largeIconBitmap)
        return this
    }


    fun setLargeIcon(largeIconResource: Int): NotificationsHelper {
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources,
                largeIconResource)
        return setLargeIcon(largeIconBitmap)
    }


    fun setRingtone(ringtone: String?): NotificationsHelper {
        // Ringtone options
        if (ringtone != null) {
            builder?.setSound(Uri.parse(ringtone))
        }
        return this
    }


    @JvmOverloads fun setVibration(pattern: LongArray? = null): NotificationsHelper {
        var usedPattern = pattern
        // Vibration options
        if (usedPattern == null || usedPattern.isEmpty()) {
            usedPattern = longArrayOf(500, 500)
        }
        builder?.setVibrate(usedPattern)
        return this
    }


    fun setIcon(icon: Int): NotificationsHelper {
        builder?.setSmallIcon(icon)
        return this
    }


    fun setMessage(message: String): NotificationsHelper {
        builder?.setContentText(message)
        return this
    }


    fun setIndeterminate(): NotificationsHelper {
        builder?.setProgress(0, 0, true)
        return this
    }


    fun setOngoing(): NotificationsHelper {
        builder?.setOngoing(true)
        return this
    }


    fun show(): NotificationsHelper {
        show(0)
        return this
    }


    fun show(id: Long): NotificationsHelper {
        val mNotification = builder?.build()
        if (mNotification?.contentIntent == null) {
            // Creates a dummy PendingIntent
            builder?.setContentIntent(PendingIntent.getActivity(context, 0, Intent(),
                    PendingIntent.FLAG_UPDATE_CURRENT))
        }
        // Builds an anonymous Notification object from the builder, and passes it to the NotificationManager
        notificationManager.notify(id.toInt(), builder?.build())
        return this
    }

}
