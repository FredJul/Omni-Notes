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
import android.os.AsyncTask;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import net.fred.taskgame.model.Task;
import net.fred.taskgame.utils.TextHelper;

import java.lang.ref.WeakReference;


public class TextWorkerTask extends AsyncTask<Task, Void, Spanned[]> {

    private final WeakReference<Activity> mActivityWeakReference;
    private final TextView titleTextView;
    private final TextView contentTextView;

    public TextWorkerTask(Activity activity, TextView titleTextView,
                          TextView contentTextView) {
        mActivityWeakReference = new WeakReference<>(activity);
        this.titleTextView = titleTextView;
        this.contentTextView = contentTextView;
    }


    @Override
    protected Spanned[] doInBackground(Task... params) {
        Task task = params[0];
        return TextHelper.parseTitleAndContent(task);
    }


    @Override
    protected void onPostExecute(Spanned[] titleAndContent) {

        if (isAlive()) {
            titleTextView.setText(titleAndContent[0]);
            if (titleAndContent[1].length() > 0) {
                contentTextView.setText(titleAndContent[1]);
                contentTextView.setVisibility(View.VISIBLE);
            } else {
                contentTextView.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Cheks if activity is still alive and not finishing
     *
     * @return True or false
     */
    private boolean isAlive() {
        return mActivityWeakReference.get() != null;
    }

}