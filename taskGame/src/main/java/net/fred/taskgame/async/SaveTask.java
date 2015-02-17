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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import net.fred.taskgame.R;
import net.fred.taskgame.fragment.DetailFragment;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.listeners.OnTaskSaved;
import net.fred.taskgame.receiver.AlarmReceiver;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.StorageManager;

import java.util.Calendar;
import java.util.List;


public class SaveTask extends AsyncTask<Task, Void, Task> {

	private final Activity mActivity;
	private boolean error = false;
	private boolean updateLastModification = true;
	private OnTaskSaved mOnTaskSaved;


	public SaveTask(DetailFragment activity, boolean updateLastModification) {
		this(activity, null, updateLastModification);
	}


	public SaveTask(DetailFragment activity, OnTaskSaved onTaskSaved, boolean updateLastModification) {
		super();
		mActivity = activity.getActivity();
		this.mOnTaskSaved = onTaskSaved;
		this.updateLastModification = updateLastModification;
	}


	@Override
	protected Task doInBackground(Task... params) {
		Task task = params[0];
		purgeRemovedAttachments(task);

		if (!error) {
			// Note updating on database
			task = DbHelper.updateTask(task, updateLastModification);
		} else {
			Toast.makeText(mActivity, mActivity.getString(R.string.error_saving_attachments), Toast.LENGTH_SHORT).show();
		}

		return task;
	}

	private void purgeRemovedAttachments(Task task) {
		List<Attachment> deletedAttachments = task.getAttachmentsListOld();
		for (Attachment attachment : task.getAttachmentsList()) {
			if (attachment.id != 0) {
				// Workaround to prevent deleting attachments if instance is changed (app restart)
				if (deletedAttachments.indexOf(attachment) == -1) {
					attachment = getFixedAttachmentInstance(deletedAttachments, attachment);
				}
				deletedAttachments.remove(attachment);
			}
		}
		// Remove from database deleted attachments
		for (Attachment deletedAttachment : deletedAttachments) {
			StorageManager.delete(mActivity, deletedAttachment.uri.getPath());

		}
	}

	private Attachment getFixedAttachmentInstance(List<Attachment> deletedAttachments, Attachment attachment) {
		for (Attachment deletedAttachment : deletedAttachments) {
			if (deletedAttachment.id == attachment.id) return deletedAttachment;
		}
		return attachment;
	}


	@Override
	protected void onPostExecute(Task task) {
		super.onPostExecute(task);

		// Set reminder if is not passed yet
		long now = Calendar.getInstance().getTimeInMillis();
		if (task.getAlarm() != null && Long.parseLong(task.getAlarm()) >= now) {
			setAlarm(task);
		}

		if (this.mOnTaskSaved != null) {
			mOnTaskSaved.onTaskSaved(task);
		}
	}


	private void setAlarm(Task task) {
		Intent intent = new Intent(mActivity, AlarmReceiver.class);
		intent.putExtra(Constants.INTENT_NOTE, task);
		PendingIntent sender = PendingIntent.getBroadcast(mActivity, task.getCreation().intValue(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		AlarmManager am = (AlarmManager) mActivity.getSystemService(Activity.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, Long.parseLong(task.getAlarm()), sender);
	}


}
