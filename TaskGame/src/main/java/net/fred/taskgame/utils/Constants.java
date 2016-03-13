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

    String TAG = "TaskGame";

    String INTENT_KEY = "task_id";
    String INTENT_TASK = "task";
    int INTENT_ALARM_CODE = 12345;
    String INTENT_CATEGORY = "category";
    String INTENT_GOOGLE_NOW = "com.google.android.gm.action.AUTO_SEND";
    String INTENT_WIDGET = "widget_id";

    // Custom intent actions
    String ACTION_DISMISS = "action_dismiss";
    String ACTION_SNOOZE = "action_snooze";
    String ACTION_POSTPONE = "action_postpone";
    String ACTION_SHORTCUT = "action_shortcut";
    String ACTION_WIDGET = "action_widget";
    String ACTION_TAKE_PHOTO = "action_widget_take_photo";
    String ACTION_WIDGET_SHOW_LIST = "action_widget_show_list";
    String ACTION_NOTIFICATION_CLICK = "action_notification_click";

    String LEADERBOARD_ID = "CgkI--6GstwWEAIQAw";
}
