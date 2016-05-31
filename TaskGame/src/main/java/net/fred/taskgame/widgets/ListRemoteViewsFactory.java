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

package net.fred.taskgame.widgets;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import net.fred.taskgame.R;
import net.fred.taskgame.models.Category;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.TextHelper;
import net.fred.taskgame.utils.ThrottledFlowContentObserver;

import org.parceler.Parcels;

import java.util.List;

public class ListRemoteViewsFactory implements RemoteViewsFactory {

    private final Context mContext;
    private final int appWidgetId;
    private List<Task> tasks;

    private final ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
        @Override
        public void onChangeThrottled() {
            AppWidgetManager.getInstance(mContext).notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list);
        }
    };

    public ListRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }


    @Override
    public void onCreate() {
        getTasks();
        mContentObserver.registerForContentChanges(mContext, Task.class);
        mContentObserver.registerForContentChanges(mContext, Category.class);
    }

    @Override
    public void onDataSetChanged() {
        getTasks();
    }

    private void getTasks() {
        long categoryId = PrefUtils.getLong(PrefUtils.PREF_WIDGET_PREFIX + appWidgetId, -1);
        if (categoryId != -1) {
            tasks = DbHelper.getActiveTasksByCategory(categoryId);
        } else {
            tasks = DbHelper.getActiveTasks();
        }
    }


    @Override
    public void onDestroy() {
        mContentObserver.unregisterForContentChanges(mContext);

        PrefUtils.remove(PrefUtils.PREF_WIDGET_PREFIX + String.valueOf(appWidgetId));
    }


    @Override
    public int getCount() {
        return tasks.size();
    }


    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.widget_task_item);

        Task task = tasks.get(position);

        Spanned[] titleAndContent = TextHelper.parseTitleAndContent(task);

        row.setTextViewText(R.id.task_title, titleAndContent[0]);
        if (titleAndContent[1].length() > 0) {
            row.setTextViewText(R.id.task_content, titleAndContent[1]);
            row.setViewVisibility(R.id.task_content, View.VISIBLE);
        } else {
            row.setViewVisibility(R.id.task_content, View.GONE);
        }
        row.setTextViewText(R.id.reward_points, String.valueOf(task.pointReward));

        // Init task and category marker colors
        if (!TextUtils.isEmpty(task.questId)) { // If this is an official quest, let's make it quite visible
            row.setViewVisibility(R.id.quest_icon, View.VISIBLE);

            // Resetting transparent color to the view
            row.setInt(R.id.category_marker, "setBackgroundColor", Color.TRANSPARENT);
        } else {
            row.setViewVisibility(R.id.quest_icon, View.GONE);

            // If category is set the color will be applied on the appropriate target
            if (task.getCategory() != null) {
                row.setInt(R.id.category_marker, "setBackgroundColor", task.getCategory().color);
            } else {
                row.setInt(R.id.category_marker, "setBackgroundColor", Color.TRANSPARENT);
            }
        }

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putParcelable(Constants.INTENT_TASK, Parcels.wrap(task));
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        row.setOnClickFillInIntent(R.id.card_view, fillInIntent);

        return (row);
    }

    @Override
    public RemoteViews getLoadingView() {
        return (null);
    }

    @Override
    public int getViewTypeCount() {
        return (1);
    }

    @Override
    public long getItemId(int position) {
        return (position);
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public static void updateConfiguration(int mAppWidgetId, long categoryId) {
        PrefUtils.putLong(PrefUtils.PREF_WIDGET_PREFIX + String.valueOf(mAppWidgetId), categoryId);
    }
}
