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

package net.fred.taskgame.utils.date;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;

import net.fred.taskgame.models.listeners.OnReminderPickedListener;

import java.util.Calendar;

public class ReminderPickers implements OnDateSetListener, OnTimeSetListener {

    private final FragmentActivity mActivity;
    private final OnReminderPickedListener mOnReminderPickedListener;

    private int mReminderYear;
    private int mReminderMonth;
    private int mReminderDay;

    private long mPresetDateTime;


    public ReminderPickers(FragmentActivity activity, OnReminderPickedListener onReminderPickedListener) {
        mActivity = activity;
        mOnReminderPickedListener = onReminderPickedListener;
    }

    public void pick(long presetDateTime) {
        mPresetDateTime = presetDateTime;
        showDatePickerDialog(mPresetDateTime);
    }

    private void showDatePickerDialog(long presetDateTime) {
        // Use the current date as the default date in the picker
        Calendar cal = DateHelper.getCalendar(presetDateTime);
        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert, this, y, m, d);
        picker.show();
    }

    private void showTimePickerDialog(long presetDateTime) {
        Calendar cal = DateHelper.getCalendar(presetDateTime);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        boolean is24HourMode = DateFormat.is24HourFormat(mActivity);
        TimePickerDialog picker = new TimePickerDialog(mActivity, android.R.style.Theme_Material_Light_Dialog_Alert, this, hour, minute, is24HourMode);
        picker.show();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        // Setting alarm time in milliseconds
        Calendar c = Calendar.getInstance();
        c.set(mReminderYear, mReminderMonth, mReminderDay, hourOfDay, minute);
        if (mOnReminderPickedListener != null) {
            mOnReminderPickedListener.onReminderPicked(c.getTimeInMillis());
        }
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mReminderYear = year;
        mReminderMonth = monthOfYear;
        mReminderDay = dayOfMonth;

        showTimePickerDialog(mPresetDateTime);
    }
}
