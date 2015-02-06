/*******************************************************************************
 * Copyright 2014 Federico Iosue (federico.iosue@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.feio.android.omninotes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import it.feio.android.omninotes.utils.Constants;
import it.feio.android.omninotes.utils.PrefUtils;


public class SettingsFragment extends PreferenceFragment {

	private final int RINGTONE_REQUEST_CODE = 100;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				getActivity().onBackPressed();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Maximum video attachment size
		final EditTextPreference maxVideoSize = (EditTextPreference) findPreference("settings_max_video_size");
		if (maxVideoSize != null) {
			String maxVideoSizeValue = PrefUtils.getString("settings_max_video_size", getString(R.string.not_set));
			maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + String.valueOf
					(maxVideoSizeValue));
			maxVideoSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + String
							.valueOf(newValue));
					PrefUtils.putString("settings_max_video_size", newValue.toString());
					return false;
				}
			});
		}

		// Application's colors
		final ListPreference colorsApp = (ListPreference) findPreference("settings_colors_app");
		if (colorsApp != null) {
			int colorsAppIndex = colorsApp.findIndexOfValue(PrefUtils.getString("settings_colors_app",
					PrefUtils.PREF_COLORS_APP_DEFAULT));
			String colorsAppString = getResources().getStringArray(R.array.colors_app)[colorsAppIndex];
			colorsApp.setSummary(colorsAppString);
			colorsApp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int colorsAppIndex = colorsApp.findIndexOfValue(newValue.toString());
					String colorsAppString = getResources().getStringArray(R.array.colors_app)[colorsAppIndex];
					colorsApp.setSummary(colorsAppString);
					PrefUtils.putString("settings_colors_app", newValue.toString());
					colorsApp.setValueIndex(colorsAppIndex);
					return false;
				}
			});
		}


		// Checklists
		final ListPreference checklist = (ListPreference) findPreference("settings_checked_items_behavior");
		if (checklist != null) {
			int checklistIndex = checklist.findIndexOfValue(PrefUtils.getString("settings_checked_items_behavior", "0"));
			String checklistString = getResources().getStringArray(R.array.checked_items_behavior)[checklistIndex];
			checklist.setSummary(checklistString);
			checklist.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int checklistIndex = checklist.findIndexOfValue(newValue.toString());
					String checklistString = getResources().getStringArray(R.array.checked_items_behavior)
							[checklistIndex];
					checklist.setSummary(checklistString);
					PrefUtils.putString("settings_checked_items_behavior", newValue.toString());
					checklist.setValueIndex(checklistIndex);
					return false;
				}
			});
		}


		// Widget's colors
		final ListPreference colorsWidget = (ListPreference) findPreference("settings_colors_widget");
		if (colorsWidget != null) {
			int colorsWidgetIndex = colorsWidget.findIndexOfValue(PrefUtils.getString("settings_colors_widget",
					PrefUtils.PREF_COLORS_APP_DEFAULT));
			String colorsWidgetString = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex];
			colorsWidget.setSummary(colorsWidgetString);
			colorsWidget.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int colorsWidgetIndex = colorsWidget.findIndexOfValue(newValue.toString());
					String colorsWidgetString = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex];
					colorsWidget.setSummary(colorsWidgetString);
					PrefUtils.putString("settings_colors_widget", newValue.toString());
					colorsWidget.setValueIndex(colorsWidgetIndex);
					return false;
				}
			});
		}


		// Notification snooze delay
		final EditTextPreference snoozeDelay = (EditTextPreference) findPreference
				("settings_notification_snooze_delay");
		if (snoozeDelay != null) {
			String snoozeDelayValue = PrefUtils.getString("settings_notification_snooze_delay", "10");
			snoozeDelay.setSummary(String.valueOf(snoozeDelayValue) + " " + getString(R.string.minutes));
			snoozeDelay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					snoozeDelay.setSummary(String.valueOf(newValue) + " " + getString(R.string.minutes));
					PrefUtils.putString("settings_notification_snooze_delay", newValue.toString());
					return false;
				}
			});
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {

				case RINGTONE_REQUEST_CODE:
					Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
					PrefUtils.putString("settings_notification_ringtone", uri.toString());
					break;
			}
		}
	}
}
