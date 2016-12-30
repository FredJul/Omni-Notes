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

package net.fred.taskgame.fragments

import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceFragment
import net.fred.taskgame.R
import net.fred.taskgame.utils.PrefUtils


class SettingsFragment : PreferenceFragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings)
    }

    override fun onResume() {
        super.onResume()

        // Checklists
        val checklist = findPreference(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR) as ListPreference?
        if (checklist != null) {
            val checklistIndex = checklist.findIndexOfValue(PrefUtils.getString(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR, "0"))
            val checklistString = resources.getStringArray(R.array.checked_items_behavior)[checklistIndex]
            checklist.summary = checklistString
            checklist.onPreferenceChangeListener = OnPreferenceChangeListener { preference, newValue ->
                val newChecklistIndex = checklist.findIndexOfValue(newValue.toString())
                val newChecklistString = resources.getStringArray(R.array.checked_items_behavior)[newChecklistIndex]
                checklist.summary = newChecklistString
                PrefUtils.putString(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR, newValue.toString())
                checklist.setValueIndex(newChecklistIndex)
                false
            }
        }

        // Notification snooze delay
        val snoozeDelay = findPreference(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY) as EditTextPreference?
        if (snoozeDelay != null) {
            val snoozeDelayValue = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, "10")
            snoozeDelay.summary = snoozeDelayValue + " " + getString(R.string.minutes)
            snoozeDelay.onPreferenceChangeListener = OnPreferenceChangeListener { preference, newValue ->
                snoozeDelay.summary = newValue.toString() + " " + getString(R.string.minutes)
                PrefUtils.putString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, newValue.toString())
                false
            }
        }
    }
}
