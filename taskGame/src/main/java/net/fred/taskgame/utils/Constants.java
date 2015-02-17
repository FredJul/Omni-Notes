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
package net.fred.taskgame.utils;

public interface Constants {

	final String TAG = "TaskGame";

	// Tasks swipe
	final int SWIPE_MARGIN = 30;
	final int SWIPE_OFFSET = 100;

	// Floating action button
	final int FAB_ANIMATION_TIME = 200;

	final int THUMBNAIL_SIZE = 300;

	final String DATE_FORMAT_SHORT_DATE = "d MMM yyyy";
	final String DATE_FORMAT_SORTABLE = "yyyyMMdd_HHmmss_S";
	final String DATE_FORMAT_SORTABLE_OLD = "yyyyMMddHHmmss";

	final String INTENT_KEY = "task_id";
	final String INTENT_TASK = "task";
	final int INTENT_ALARM_CODE = 12345;
	final String INTENT_TAG = "tag";
	final String INTENT_GOOGLE_NOW = "com.google.android.gm.action.AUTO_SEND";
	final String INTENT_WIDGET = "widget_id";

	// Custom intent actions
	final String ACTION_START_APP = "action_start_app";
	final String ACTION_DISMISS = "action_dismiss";
	final String ACTION_SNOOZE = "action_snooze";
	final String ACTION_POSTPONE = "action_postpone";
	final String ACTION_SHORTCUT = "action_shortcut";
	final String ACTION_WIDGET = "action_widget";
	final String ACTION_TAKE_PHOTO = "action_widget_take_photo";
	final String ACTION_WIDGET_SHOW_LIST = "action_widget_show_list";
	final String ACTION_NOTIFICATION_CLICK = "action_notification_click";
	final String ACTION_MERGE = "action_merge";
	/**
	 * Used to quickly add a note, save, and perform backPress (eg. Tasker+Pushbullet) *
	 */
	final String ACTION_SEND_AND_EXIT = "action_send_and_exit";

	final String MIME_TYPE_IMAGE = "image/jpeg";
	final String MIME_TYPE_AUDIO = "audio/mp3";
	final String MIME_TYPE_VIDEO = "video/mp4";
	final String MIME_TYPE_SKETCH = "image/png";
	final String MIME_TYPE_FILES = "file/*";

	final String MIME_TYPE_IMAGE_EXT = ".jpeg";
	final String MIME_TYPE_AUDIO_EXT = ".mp3";
	final String MIME_TYPE_VIDEO_EXT = ".mp4";
	final String MIME_TYPE_SKETCH_EXT = ".png";

	final String TIMESTAMP_UNIX_EPOCH = "0";

	final int MENU_SORT_GROUP_ID = 11998811;
}
