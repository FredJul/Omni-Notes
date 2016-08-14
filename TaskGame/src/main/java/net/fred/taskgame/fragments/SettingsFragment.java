/**
 * ****************************************************************************
 * Copyright 2014 Federico Iosue (federico.iosue@gmail.com)
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ****************************************************************************
 */
package net.fred.taskgame.fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.PrefUtils;


public class SettingsFragment extends PreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Checklists
        final ListPreference checklist = (ListPreference) findPreference(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR);
        if (checklist != null) {
            int checklistIndex = checklist.findIndexOfValue(PrefUtils.getString(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR, "0"));
            String checklistString = getResources().getStringArray(R.array.checked_items_behavior)[checklistIndex];
            checklist.setSummary(checklistString);
            checklist.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int checklistIndex = checklist.findIndexOfValue(newValue.toString());
                    String checklistString = getResources().getStringArray(R.array.checked_items_behavior)
                            [checklistIndex];
                    checklist.setSummary(checklistString);
                    PrefUtils.putString(PrefUtils.PREF_SETTINGS_CHECKED_ITEM_BEHAVIOR, newValue.toString());
                    checklist.setValueIndex(checklistIndex);
                    return false;
                }
            });
        }

        // Notification snooze delay
        final EditTextPreference snoozeDelay = (EditTextPreference) findPreference(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY);
        if (snoozeDelay != null) {
            String snoozeDelayValue = PrefUtils.getString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, "10");
            snoozeDelay.setSummary(String.valueOf(snoozeDelayValue) + " " + getString(R.string.minutes));
            snoozeDelay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    snoozeDelay.setSummary(String.valueOf(newValue) + " " + getString(R.string.minutes));
                    PrefUtils.putString(PrefUtils.PREF_SETTINGS_NOTIFICATION_SNOOZE_DELAY, newValue.toString());
                    return false;
                }
            });
        }
    }
}
