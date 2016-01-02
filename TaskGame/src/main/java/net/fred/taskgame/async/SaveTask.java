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

import android.os.AsyncTask;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.IdBasedModel;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.listeners.OnTaskSaved;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.ReminderHelper;

import java.util.Calendar;
import java.util.List;


public class SaveTask extends AsyncTask<Void, Void, Void> {

    private final Task mTask;
    private final List<Attachment> mOldAttachments;
    private boolean mUpdateLastModification = true;
    private final OnTaskSaved mOnTaskSaved;

    public SaveTask(Task task, List<Attachment> oldAttachments, OnTaskSaved onTaskSaved, boolean updateLastModification) {
        super();
        mTask = task;
        mOldAttachments = oldAttachments;
        mOnTaskSaved = onTaskSaved;
        mUpdateLastModification = updateLastModification;
    }

    @Override
    protected Void doInBackground(Void... params) {
        purgeRemovedAttachments();

        // Note updating on database
        DbHelper.updateTask(mTask, mUpdateLastModification);

        return null;
    }

    private void purgeRemovedAttachments() {
        if (mOldAttachments != null && !mOldAttachments.isEmpty()) {
            List<Attachment> deletedAttachments = mOldAttachments;
            for (Attachment attachment : mTask.getAttachmentsList()) {
                if (attachment.id != IdBasedModel.INVALID_ID) {
                    // Workaround to prevent deleting attachments if instance is changed (app restart)
                    if (deletedAttachments.indexOf(attachment) == -1) {
                        attachment = getFixedAttachmentInstance(deletedAttachments, attachment);
                    }
                    deletedAttachments.remove(attachment);
                }
            }
            // Remove from database deleted attachments
            for (Attachment deletedAttachment : deletedAttachments) {
                DbHelper.deleteAttachment(deletedAttachment);
            }
        }
    }

    private Attachment getFixedAttachmentInstance(List<Attachment> deletedAttachments, Attachment attachment) {
        for (Attachment deletedAttachment : deletedAttachments) {
            if (deletedAttachment.id == attachment.id) return deletedAttachment;
        }
        return attachment;
    }


    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);

        // Set reminder if is not passed yet
        long now = Calendar.getInstance().getTimeInMillis();
        if (mTask.alarmDate >= now) {
            ReminderHelper.addReminder(MainApplication.getContext(), mTask);
        }

        if (this.mOnTaskSaved != null) {
            mOnTaskSaved.onTaskSaved(mTask);
        }
    }
}
