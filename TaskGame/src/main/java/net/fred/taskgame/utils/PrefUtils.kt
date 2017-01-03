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

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.preference.PreferenceManager
import net.fred.taskgame.App

object PrefUtils {

    val PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR = "settings_checked_items_behavior"
    val PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY = "settings_notification_snooze_delay"
    val PREF_SETTINGS_NOTIFICATION_RINGTONE = "settings_notification_ringtone"
    val PREF_SETTINGS_NOTIFICATION_VIBRATION = "settings_notification_vibration"
    val PREF_NAVIGATION = "navigation"
    val PREF_WIDGET_PREFIX = "widget_"
    val PREF_CURRENT_POINTS = "PREF_CURRENT_POINTS"

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getBoolean(key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getInt(key: String, defValue: Int): Int {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getInt(key, defValue)
    }

    fun putInt(key: String, value: Int) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getLong(key: String, defValue: Long): Long {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getLong(key, defValue)
    }

    fun putLong(key: String, value: Long) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putLong(key, value)
        editor.apply()
    }

    fun getString(key: String, defValue: String): String {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getString(key, defValue)
    }

    fun putString(key: String, value: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getStringSet(key: String, defValue: MutableSet<String>): MutableSet<String> {
        val settings = PreferenceManager.getDefaultSharedPreferences(App.context)
        return settings.getStringSet(key, defValue)
    }

    fun putStringSet(key: String, value: MutableSet<String>) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.putStringSet(key, value)
        editor.apply()
    }

    fun remove(key: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(App.context).edit()
        editor.remove(key)
        editor.apply()
    }

    fun registerOnPrefChangeListener(listener: OnSharedPreferenceChangeListener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(App.context).registerOnSharedPreferenceChangeListener(listener)
        } catch (ignored: Exception) { // Seems to be possible to have a NPE here... Why??
        }

    }

    fun unregisterOnPrefChangeListener(listener: OnSharedPreferenceChangeListener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(App.context).unregisterOnSharedPreferenceChangeListener(listener)
        } catch (ignored: Exception) { // Seems to be possible to have a NPE here... Why??
        }

    }
}
