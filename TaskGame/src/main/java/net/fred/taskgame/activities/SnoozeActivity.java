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

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import net.fred.taskgame.models.Task;
import net.fred.taskgame.models.listeners.OnReminderPickedListener;
import net.fred.taskgame.receivers.AlarmReceiver;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.date.ReminderPickers;

import org.parceler.Parcels;

import java.util.Calendar;

public class SnoozeActivity extends FragmentActivity implements OnReminderPickedListener {

    private static final int INTENT_ALARM_CODE = 12345;

    private Task mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTask = Parcels.unwrap(getIntent().getParcelableExtra(Constants.INTENT_TASK));

        // If an alarm has been fired a notification must be generated
        if (Constants.ACTION_SNOOZE.equals(getIntent().getAction())) {
            String snoozeDelay = PrefUtils.getString("settings_notification_snooze_delay", "10");
            long newAlarm = Calendar.getInstance().getTimeInMillis() + Integer.parseInt(snoozeDelay) * 60 * 1000;
            setAlarm(mTask, newAlarm);
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
        manager.cancel((int) task.id);
    }

    private void setAlarm(Task task, long newAlarm) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra(Constants.INTENT_TASK, Parcels.wrap(task));
        PendingIntent sender = PendingIntent.getBroadcast(this, INTENT_ALARM_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) this.getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, newAlarm, sender);
    }

    @Override
    public void onReminderPicked(long reminder) {
        mTask.alarmDate = reminder;
        setAlarm(mTask, reminder);
        finish();
    }
}
