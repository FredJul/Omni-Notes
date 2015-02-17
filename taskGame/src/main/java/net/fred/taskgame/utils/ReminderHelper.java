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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import net.fred.taskgame.model.Task;
import net.fred.taskgame.receiver.AlarmReceiver;

import java.util.Calendar;


public class ReminderHelper {

    public static void addReminder(Context context, Task Task) {
        if (hasFutureReminder(Task)) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(Constants.INTENT_TASK, Task);
            PendingIntent sender = PendingIntent.getBroadcast(context, Task.getCreation().intValue(), intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, Long.parseLong(Task.getAlarm()), sender);
        }
    }


    private static boolean hasFutureReminder(Task Task) {
        boolean hasFutureReminder = false;
        if (!TextUtils.isEmpty(Task.getAlarm()) && Long.parseLong(Task.getAlarm()) > Calendar.getInstance()
                .getTimeInMillis()) {
            hasFutureReminder = true;
        }
        return hasFutureReminder;
    }


    public static void removeReminder(Context context, Task Task) {
        if (!TextUtils.isEmpty(Task.getAlarm())) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent p = PendingIntent.getBroadcast(context, Task.getCreation().intValue(), intent, 0);
            am.cancel(p);
            p.cancel();
        }
    }
}
