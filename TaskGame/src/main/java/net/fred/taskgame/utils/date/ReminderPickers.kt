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

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.support.v4.app.FragmentActivity
import android.text.format.DateFormat
import android.widget.DatePicker
import android.widget.TimePicker
import net.fred.taskgame.listeners.OnReminderPickedListener
import java.util.*

class ReminderPickers(private val mActivity: FragmentActivity, private val mOnReminderPickedListener: OnReminderPickedListener?) : OnDateSetListener, OnTimeSetListener {

    private var mReminderYear: Int = 0
    private var mReminderMonth: Int = 0
    private var mReminderDay: Int = 0

    private var mPresetDateTime: Long = 0

    fun pick(presetDateTime: Long) {
        mPresetDateTime = presetDateTime
        showDatePickerDialog(mPresetDateTime)
    }

    private fun showDatePickerDialog(presetDateTime: Long) {
        // Use the current date as the default date in the picker
        val cal = DateHelper.getCalendar(presetDateTime)
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH)
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert, this, y, m, d)
        picker.setOnDismissListener {
            mOnReminderPickedListener?.onReminderDismissed()
        }
        picker.show()
    }

    private fun showTimePickerDialog(presetDateTime: Long) {
        val cal = DateHelper.getCalendar(presetDateTime)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        // Create a new instance of TimePickerDialog and return it
        val is24HourMode = DateFormat.is24HourFormat(mActivity)
        val picker = TimePickerDialog(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert, this, hour, minute, is24HourMode)
        picker.setOnDismissListener {
            mOnReminderPickedListener?.onReminderDismissed()
        }
        picker.show()
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        // Setting alarm time in milliseconds
        val c = Calendar.getInstance()
        c.set(mReminderYear, mReminderMonth, mReminderDay, hourOfDay, minute)
        mOnReminderPickedListener?.onReminderPicked(c.timeInMillis)
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        mReminderYear = year
        mReminderMonth = monthOfYear
        mReminderDay = dayOfMonth

        showTimePickerDialog(mPresetDateTime)
    }
}
