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

package net.fred.taskgame.utils

interface Constants {
    companion object {
        val ACTION_GOOGLE_NOW = "com.google.android.gm.action.AUTO_SEND"

        val EXTRA_TASK_ID = "task_id"
        val EXTRA_TASK = "task"
        val EXTRA_CATEGORY = "category"
        val EXTRA_WIDGET_ID = "widget_id"

        // Custom intent actions
        val ACTION_SNOOZE = "action_snooze"
        val ACTION_POSTPONE = "action_postpone"
        val ACTION_WIDGET = "action_widget"
        val ACTION_WIDGET_SHOW_LIST = "action_widget_show_list"
        val ACTION_NOTIFICATION_CLICK = "action_notification_click"
    }
}
