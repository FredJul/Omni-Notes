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


import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.widget.RemoteViews

import net.fred.taskgame.R
import net.fred.taskgame.activities.MainActivity
import net.fred.taskgame.utils.Constants


abstract class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Get all ids
        val thisWidget = ComponentName(context, javaClass)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (appWidgetId in allWidgetIds) {

            // Get the layout for and attach an on-click listener to views
            setLayout(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }


    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {

        setLayout(context, appWidgetManager, appWidgetId)
    }

    private fun setLayout(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {

        // Create an Intent to launch DetailActivity
        val intentDetail = Intent(context, MainActivity::class.java)
        intentDetail.action = Constants.ACTION_WIDGET
        intentDetail.putExtra(Constants.INTENT_WIDGET, widgetId)
        val pendingIntentDetail = PendingIntent.getActivity(context, widgetId, intentDetail,
                PendingIntent.FLAG_UPDATE_CURRENT)

        // Create an Intent to launch ListActivity
        val intentList = Intent(context, MainActivity::class.java)
        intentList.action = Constants.ACTION_WIDGET_SHOW_LIST
        intentList.putExtra(Constants.INTENT_WIDGET, widgetId)
        val pendingIntentList = PendingIntent.getActivity(context, widgetId, intentList,
                PendingIntent.FLAG_UPDATE_CURRENT)

        // Check various dimensions aspect of widget to choose between layouts
        val isSmall: Boolean
        val isSingleLine: Boolean
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        // Width check
        isSmall = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) < 110
        // Height check
        isSingleLine = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) < 110

        // Creation of a map to associate PendingIntent(s) to views
        val map = SparseArray<PendingIntent>()
        map.put(R.id.list, pendingIntentList)
        map.put(R.id.add, pendingIntentDetail)

        val views = getRemoteViews(context, widgetId, isSmall, isSingleLine, map)

        // Tell the AppWidgetManager to perform an update on the current app
        // widget
        appWidgetManager.updateAppWidget(widgetId, views)
    }


    protected abstract fun getRemoteViews(context: Context, widgetId: Int, isSmall: Boolean, isSingleLine: Boolean, pendingIntentsMap: SparseArray<PendingIntent>): RemoteViews

}