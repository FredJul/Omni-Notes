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

package net.fred.taskgame.activity;

import android.app.AlarmManager;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.DatePicker;
import android.widget.TimePicker;

import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.listeners.OnReminderPickedListener;
import net.fred.taskgame.receiver.AlarmReceiver;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.date.ReminderPickers;

import java.util.Calendar;

public class SnoozeActivity extends FragmentActivity implements OnReminderPickedListener, OnDateSetListener, OnTimeSetListener {

	private Task task;
	private OnDateSetListener mOnDateSetListener;
	private OnTimeSetListener mOnTimeSetListener;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		task = getIntent().getParcelableExtra(Constants.INTENT_NOTE);

		// If an alarm has been fired a notification must be generated
		if (Constants.ACTION_DISMISS.equals(getIntent().getAction())) {
			finish();
		} else if (Constants.ACTION_SNOOZE.equals(getIntent().getAction())) {
			String snoozeDelay = PrefUtils.getString("settings_notification_snooze_delay", "10");
			long newAlarm = Calendar.getInstance().getTimeInMillis() + Integer.parseInt(snoozeDelay) * 60 * 1000;
			setAlarm(task, newAlarm);
			finish();
		} else if (Constants.ACTION_POSTPONE.equals(getIntent().getAction())) {
			int pickerType = PrefUtils.getBoolean("settings_simple_calendar", false) ? ReminderPickers.TYPE_AOSP : ReminderPickers.TYPE_GOOGLE;
			ReminderPickers reminderPicker = new ReminderPickers(this, this, pickerType);
			reminderPicker.pick(Long.parseLong(task.getAlarm()));
			mOnDateSetListener = reminderPicker;
			mOnTimeSetListener = reminderPicker;
		} else {
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra(Constants.INTENT_KEY, task.getId());
			intent.setAction(Constants.ACTION_NOTIFICATION_CLICK);
			startActivity(intent);
		}
		removeNotification(task);
	}


	private void removeNotification(Task task) {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(task.getId());
	}

	private void setAlarm(Task task, long newAlarm) {
		Intent intent = new Intent(this, AlarmReceiver.class);
		intent.putExtra(Constants.INTENT_NOTE, (android.os.Parcelable) task);
		PendingIntent sender = PendingIntent.getBroadcast(this, Constants.INTENT_ALARM_CODE, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) this.getSystemService(this.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, newAlarm, sender);
	}


	@Override
	public void onReminderPicked(long reminder) {
		task.setAlarm(reminder);
		setAlarm(task, reminder);
		finish();
	}


	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear,
						  int dayOfMonth) {
		mOnDateSetListener.onDateSet(view, year, monthOfYear, dayOfMonth);
	}


	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		mOnTimeSetListener.onTimeSet(view, hourOfDay, minute);
	}


}
