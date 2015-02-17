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
package net.fred.taskgame.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import net.fred.taskgame.R;
import net.fred.taskgame.activity.SnoozeActivity;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.date.DateHelper;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context mContext, Intent intent) {
		try {

//			PowerManager pm = (PowerManager) mContext
//					.getSystemService(Context.POWER_SERVICE);
//			PowerManager.WakeLock wl = pm.newWakeLock(
//					PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG);
//			// Acquire the lock
//			wl.acquire();

			try {
				Task task = intent.getExtras().getParcelable(Constants.INTENT_NOTE);

				createNotification(mContext, task);
			}

			// Release the lock
			finally {
//				wl.release();
			}

		} catch (Exception e) {
			Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
		}

	}

	private void createNotification(Context mContext, Task task) {
		// Prepare text contents
		String title = task.getTitle().length() > 0 ? task.getTitle() : task
				.getContent();
		String alarmText = DateHelper.getString(
				Long.parseLong(task.getAlarm()),
				Constants.DATE_FORMAT_SHORT_DATE)
				+ ", "
				+ DateHelper.getDateTimeShort(mContext, Long.parseLong(task.getAlarm()));
		String text = task.getTitle().length() > 0 && task.getContent().length() > 0 ? task.getContent() : alarmText;

		// Notification building
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				mContext).setSmallIcon(R.drawable.ic_stat_notification_icon)
				.setContentTitle(title).setContentText(text)
				.setAutoCancel(true);


		// Ringtone options
		String ringtone = PrefUtils.getString("settings_notification_ringtone", null);
		if (ringtone != null) {
			mBuilder.setSound(Uri.parse(ringtone));
		}


		// Vibration options
		long[] pattern = {500, 500};
		if (PrefUtils.getBoolean("settings_notification_vibration", true))
			mBuilder.setVibrate(pattern);


		// Sets up the Snooze and Dismiss action buttons that will appear in the
		// big view of the notification.
		Intent snoozeIntent = new Intent(mContext, SnoozeActivity.class);
		snoozeIntent.setAction(Constants.ACTION_SNOOZE);
		snoozeIntent.putExtra(Constants.INTENT_NOTE, task);
		snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent piSnooze = PendingIntent.getActivity(mContext, 0, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Intent postponeIntent = new Intent(mContext, SnoozeActivity.class);
		postponeIntent.setAction(Constants.ACTION_POSTPONE);
		postponeIntent.putExtra(Constants.INTENT_NOTE, task);
		snoozeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent piPostpone = PendingIntent.getActivity(mContext, 0, postponeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		String snoozeDelay = PrefUtils.getString("settings_notification_snooze_delay", "10");

		//Sets the big view "big text" style
		mBuilder
//		.addAction (R.drawable.ic_action_cancel_dark,
//       		mContext.getString(R.string.cancel), piDismiss)
				.addAction(R.drawable.ic_snooze_reminder,
						net.fred.taskgame.utils.TextHelper.capitalize(mContext.getString(R.string.snooze)) + ": " + snoozeDelay, piSnooze)
				.addAction(R.drawable.ic_reminder,
						net.fred.taskgame.utils.TextHelper.capitalize(mContext.getString(R.string.add_reminder)), piPostpone);


		// Next create the bundle and initialize it
		Intent intent = new Intent(mContext, SnoozeActivity.class);
		Bundle bundle = new Bundle();
		bundle.putParcelable(Constants.INTENT_NOTE, task);
		intent.putExtras(bundle);

		// Sets the Activity to start in a new, empty task
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// Workaround to fix problems with multiple notifications
		intent.setAction(Constants.ACTION_NOTIFICATION_CLICK + Long.toString(System.currentTimeMillis()));

		// Creates the PendingIntent
		PendingIntent notifyIntent = PendingIntent.getActivity(mContext, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		// Puts the PendingIntent into the notification builder
		mBuilder.setContentIntent(notifyIntent);


		// Notifications are issued by sending them to the
		// NotificationManager system service.
		NotificationManager mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds an anonymous Notification object from the builder, and
		// passes it to the NotificationManager
		mNotificationManager.notify(task.getId(), mBuilder.build());
	}
}
