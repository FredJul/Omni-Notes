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

package net.fred.taskgame.utils.date

import android.content.Context
import android.text.format.DateUtils
import java.util.*

object DateHelper {

    fun getCalendar(dateTime: Long): Calendar {
        val cal = Calendar.getInstance()
        if (dateTime > 0) {
            cal.timeInMillis = dateTime
        }
        return cal
    }

    /**
     * @param context
     * *
     * @param date
     * *
     * @return
     */
    fun getDateTimeShort(context: Context, date: Long): String {
        if (date <= 0) {
            return ""
        }

        val now = Calendar.getInstance()
        val c = Calendar.getInstance()
        c.timeInMillis = date

        var flags = DateUtils.FORMAT_ABBREV_MONTH
        if (c.get(Calendar.YEAR) != now.get(Calendar.YEAR))
            flags = flags or DateUtils.FORMAT_SHOW_YEAR
        return DateUtils.formatDateTime(context, date, flags) + " " + DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_TIME)
    }
}
