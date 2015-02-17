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

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spanned;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.raizlabs.android.dbflow.sql.builder.ConditionQueryBuilder;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task$Table;
import net.fred.taskgame.model.adapters.NoteAdapter;
import net.fred.taskgame.utils.BitmapHelper;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.TextHelper;

import java.util.List;

public class ListRemoteViewsFactory implements RemoteViewsFactory {

	private final int WIDTH = 80;
	private final int HEIGHT = 80;

	private static boolean showThumbnails = true;

	private MainApplication app;
	private int appWidgetId;
	private List<Task> tasks;


	public ListRemoteViewsFactory(Application app, Intent intent) {
		this.app = (MainApplication) app;
		appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
	}


	@Override
	public void onCreate() {
		getTasks();
	}


	@Override
	public void onDataSetChanged() {
		getTasks();
	}

	private void getTasks() {
		int categoryId = PrefUtils.getInt(PrefUtils.PREF_WIDGET_PREFIX + appWidgetId, -1);
		if (categoryId != -1) {
			ConditionQueryBuilder<Task> queryBuilder = new ConditionQueryBuilder<>(Task.class, com.raizlabs.android.dbflow.sql.builder.Condition.column(Task$Table.CATEGORYID).eq(categoryId));
			tasks = DbHelper.getTasks(queryBuilder);
		} else {
			tasks = DbHelper.getTasks();
		}
	}


	@Override
	public void onDestroy() {
		PrefUtils.remove(PrefUtils.PREF_WIDGET_PREFIX
				+ String.valueOf(appWidgetId));
	}


	@Override
	public int getCount() {
		return tasks.size();
	}


	@Override
	public RemoteViews getViewAt(int position) {
		RemoteViews row = new RemoteViews(app.getPackageName(), R.layout.note_layout_widget);

		Task task = tasks.get(position);

		Spanned[] titleAndContent = TextHelper.parseTitleAndContent(task);

		row.setTextViewText(R.id.note_title, titleAndContent[0]);
		row.setTextViewText(R.id.note_content, titleAndContent[1]);

		color(task, row);

		if (task.getAttachmentsList().size() > 0 && showThumbnails) {
			Attachment mAttachment = task.getAttachmentsList().get(0);
			// Fetch from cache if possible
			String cacheKey = mAttachment.uri.getPath() + WIDTH + HEIGHT;
			Bitmap bmp = app.getBitmapCache().getBitmap(cacheKey);

			if (bmp == null) {
				bmp = BitmapHelper.getBitmapFromAttachment(app, mAttachment,
						WIDTH, HEIGHT);
//				app.getBitmapCache().addBitmap(cacheKey, bmp);
			}
			row.setBitmap(R.id.attachmentThumbnail, "setImageBitmap", bmp);
			row.setInt(R.id.attachmentThumbnail, "setVisibility", View.VISIBLE);
		} else {
			row.setInt(R.id.attachmentThumbnail, "setVisibility", View.GONE);
		}

		row.setTextViewText(R.id.note_date, NoteAdapter.getDateText(app, task));

		// Next, set a fill-intent, which will be used to fill in the pending intent template
		// that is set on the collection view in StackWidgetProvider.
		Bundle extras = new Bundle();
		extras.putParcelable(Constants.INTENT_NOTE, task);
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

	public static void updateConfiguration(int mAppWidgetId, int categoryId, boolean thumbnails) {
		PrefUtils.putInt(PrefUtils.PREF_WIDGET_PREFIX + String.valueOf(mAppWidgetId), categoryId);
		showThumbnails = thumbnails;
	}


	private void color(Task task, RemoteViews row) {

		String colorsPref = PrefUtils.getString("settings_colors_widget",
				PrefUtils.PREF_COLORS_APP_DEFAULT);

		// Checking preference
		if (!colorsPref.equals("disabled")) {

			// Resetting transparent color to the view
			row.setInt(R.id.tag_marker, "setBackgroundColor", Color.parseColor("#00000000"));

			// If tag is set the color will be applied on the appropriate target
			if (task.getCategory() != null && task.getCategory().color != null) {
				if (colorsPref.equals("list")) {
					row.setInt(R.id.card_layout, "setBackgroundColor", Integer.parseInt(task.getCategory().color));
				} else {
					row.setInt(R.id.tag_marker, "setBackgroundColor", Integer.parseInt(task.getCategory().color));
				}
			} else {
				row.setInt(R.id.tag_marker, "setBackgroundColor", 0);
			}
		}
	}

}
