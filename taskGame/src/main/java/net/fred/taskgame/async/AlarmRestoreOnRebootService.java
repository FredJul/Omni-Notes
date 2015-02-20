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

package net.fred.taskgame.async;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import net.fred.taskgame.activity.BaseActivity;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.receiver.AlarmReceiver;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.ReminderHelper;

import java.util.List;

public class AlarmRestoreOnRebootService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Context context = getApplicationContext();

		// Refresh widgets data
		BaseActivity.notifyAppWidgets(context);

		// Retrieves all tasks with reminder set
			List<Task> tasks = DbHelper.getTasksWithReminder(true);

			for (Task task : tasks) {
				ReminderHelper.addReminder(context, task);
			}

		return Service.START_NOT_STICKY;
	}
}