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

package net.fred.taskgame.activities;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import net.fred.taskgame.listeners.OnReminderPickedListener;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbUtils;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.date.ReminderPickers;

import java.util.Calendar;

public class SnoozeActivity extends FragmentActivity implements OnReminderPickedListener {

    private Task mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTask = DbUtils.getTask(getIntent().getExtras().getString(Constants.INTENT_TASK_ID));

        // If an alarm has been fired a notification must be generated
        if (Constants.ACTION_SNOOZE.equals(getIntent().getAction())) {
            String snoozeDelay = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, "10");
            mTask.alarmDate = Calendar.getInstance().getTimeInMillis() + Integer.parseInt(snoozeDelay) * 60 * 1000;
            mTask.setupReminderAlarm(this);
            finish();
        } else if (Constants.ACTION_POSTPONE.equals(getIntent().getAction())) {
            new ReminderPickers(this, this).pick(mTask.alarmDate);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(Constants.INTENT_TASK_ID, mTask.id);
            intent.setAction(Constants.ACTION_NOTIFICATION_CLICK);
            startActivity(intent);
        }
        removeNotification(mTask);
    }


    private void removeNotification(Task task) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(task.id.hashCode());
    }

    @Override
    public void onReminderPicked(long reminder) {
        mTask.alarmDate = reminder;
        mTask.setupReminderAlarm(this);
        finish();
    }
}
