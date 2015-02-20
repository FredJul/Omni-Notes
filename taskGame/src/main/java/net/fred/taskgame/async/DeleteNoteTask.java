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

import android.content.Context;
import android.os.AsyncTask;

import net.fred.taskgame.activity.BaseActivity;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.utils.DbHelper;

public class DeleteNoteTask extends AsyncTask<Task, Void, Void> {

	private final Context mContext;


	public DeleteNoteTask(Context mContext) {
		this.mContext = mContext;
	}


	@Override
	protected Void doInBackground(Task... params) {
		Task task = params[0];

		// Deleting note using DbHelper
		DbHelper.deleteTask(task);

		return null;
	}


	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		BaseActivity.notifyAppWidgets(mContext);
	}
}
