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

package net.fred.taskgame.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.DbUtils;

import java.util.List;

public class AlarmRestoreOnRebootService extends Service {

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();

        // Retrieves all tasks with reminder set
        List<Task> tasks = DbUtils.getTasksWithReminder(true);

        for (Task task : tasks) {
            task.setupReminderAlarm(context);
        }

        return Service.START_NOT_STICKY;
    }
}
