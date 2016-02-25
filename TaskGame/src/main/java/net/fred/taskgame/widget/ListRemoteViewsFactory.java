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

package net.fred.taskgame.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task_Table;
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

    private ThrottledFlowContentObserver mContentObserver = new ThrottledFlowContentObserver(100) {
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
        mContentObserver.registerForContentChanges(mContext, Attachment.class);
    }

    @Override
    public void onDataSetChanged() {
        getTasks();
    }

    private void getTasks() {
        long categoryId = PrefUtils.getLong(PrefUtils.PREF_WIDGET_PREFIX + appWidgetId, -1);
        if (categoryId != -1) {
            tasks = DbHelper.getTasks(Task_Table.categoryId.eq(categoryId));
        } else {
            tasks = DbHelper.getTasks();
        }
    }


    @Override
    public void onDestroy() {
        mContentObserver.unregisterForContentChanges(mContext);

        PrefUtils.remove(PrefUtils.PREF_WIDGET_PREFIX
                + String.valueOf(appWidgetId));
    }


    @Override
    public int getCount() {
        return tasks.size();
    }


    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews row = new RemoteViews(mContext.getPackageName(), R.layout.task_layout_widget);

        Task task = tasks.get(position);

        Spanned[] titleAndContent = TextHelper.parseTitleAndContent(task);

        row.setTextViewText(R.id.task_title, titleAndContent[0]);
        row.setTextViewText(R.id.task_content, titleAndContent[1]);

        color(task, row);

//        if (task.getAttachmentsList().size() > 0) {
//            Attachment attachment = task.getAttachmentsList().get(0);
//
//            AppWidgetTarget target = new AppWidgetTarget(mContext, row, R.id.attachmentThumbnail, 80, 80, new int[]{appWidgetId});
//
//            Uri thumbnailUri = attachment.getThumbnailUri(mContext);
//            Glide.with(mContext)
//                    .load(thumbnailUri)
//                    .asBitmap()
//                    .centerCrop()
//                    .into(target);
//
//            row.setInt(R.id.attachmentThumbnail, "setVisibility", View.VISIBLE);
//        } else {
        row.setInt(R.id.attachment_thumbnail, "setVisibility", View.GONE);
//        }

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putParcelable(Constants.INTENT_TASK, Parcels.wrap(task));
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        row.setOnClickFillInIntent(R.id.root, fillInIntent);

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

    private void color(Task task, RemoteViews row) {
        // Resetting transparent color to the view
        row.setInt(R.id.category_marker, "setBackgroundColor", Color.TRANSPARENT);

        // If tag is set the color will be applied on the appropriate target
        if (task.getCategory() != null) {
            row.setInt(R.id.category_marker, "setBackgroundColor", task.getCategory().color);
        } else {
            row.setInt(R.id.category_marker, "setBackgroundColor", Color.TRANSPARENT);
        }
    }
}
