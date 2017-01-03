/*
 * Copyright (c) 2012-2017 Frederic Julian
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
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.fred.taskgame.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService.RemoteViewsFactory
import net.fred.taskgame.R
import net.fred.taskgame.models.Task
import net.fred.taskgame.utils.Constants
import net.fred.taskgame.utils.DbUtils
import net.fred.taskgame.utils.Dog
import net.fred.taskgame.utils.PrefUtils
import net.frju.androidquery.gen.Q
import net.frju.androidquery.utils.ThrottledContentObserver
import org.parceler.Parcels

class ListRemoteViewsFactory(private val context: Context, intent: Intent) : RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    private var tasks: List<Task>? = null

    private val contentObserver = object : ThrottledContentObserver(Handler(), 500) {
        override fun onChangeThrottled() {
            Dog.d("change detected, widget updated")
            AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }


    override fun onCreate() {
        getTasks()
        context.contentResolver.registerContentObserver(Q.Task.getContentUri(), true, contentObserver)
        context.contentResolver.registerContentObserver(Q.Category.getContentUri(), true, contentObserver)
    }

    override fun onDataSetChanged() {
        getTasks()
    }

    private fun getTasks() {
        val categoryId = PrefUtils.getString(PrefUtils.PREF_WIDGET_PREFIX + appWidgetId, "")
        if (categoryId.isNotEmpty()) {
            tasks = DbUtils.getActiveTasksByCategory(categoryId)
        } else {
            tasks = DbUtils.activeTasks
        }
    }


    override fun onDestroy() {
        context.contentResolver.unregisterContentObserver(contentObserver)

        PrefUtils.remove(PrefUtils.PREF_WIDGET_PREFIX + appWidgetId.toString())
    }


    override fun getCount(): Int {
        return tasks!!.size
    }


    override fun getViewAt(position: Int): RemoteViews {
        val row = RemoteViews(context.packageName, R.layout.widget_task_item)

        val task = tasks!![position]

        val titleAndContent = task.computeListItemTitleAndContent()

        row.setTextViewText(R.id.task_title, titleAndContent[0])
        if (titleAndContent[1].isNotEmpty()) {
            row.setTextViewText(R.id.task_content, titleAndContent[1])
            row.setViewVisibility(R.id.task_content, View.VISIBLE)
        } else {
            row.setViewVisibility(R.id.task_content, View.GONE)
        }
        row.setTextViewText(R.id.reward_points, task.pointReward.toString())

        // Init task and category marker colors
        // If category is set the color will be applied on the appropriate target
        if (task.category != null) {
            row.setInt(R.id.category_marker, "setBackgroundColor", task.category!!.color)
        } else {
            row.setInt(R.id.category_marker, "setBackgroundColor", Color.TRANSPARENT)
        }

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StackWidgetProvider.
        val extras = Bundle()
        extras.putParcelable(Constants.EXTRA_TASK, Parcels.wrap(task))
        val fillInIntent = Intent()
        fillInIntent.putExtras(extras)
        // Make it possible to distinguish the individual on-click
        // action of a given item
        row.setOnClickFillInIntent(R.id.root, fillInIntent)

        return row
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    companion object {

        fun updateConfiguration(mAppWidgetId: Int, categoryId: String?) {
            if (categoryId == null) {
                PrefUtils.remove(PrefUtils.PREF_WIDGET_PREFIX + mAppWidgetId.toString())
            } else {
                PrefUtils.putString(PrefUtils.PREF_WIDGET_PREFIX + mAppWidgetId.toString(), categoryId)
            }
        }
    }
}
