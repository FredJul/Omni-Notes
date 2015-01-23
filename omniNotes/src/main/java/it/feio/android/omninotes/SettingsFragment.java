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


public class SettingsFragment extends PreferenceFragment {

	private SharedPreferences prefs;

	private final int RINGTONE_REQUEST_CODE = 100;
	public final static String XML_NAME = "xmlName";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int xmlId = getXmlId() > 0 ? getXmlId() : R.xml.settings;
		addPreferencesFromResource(xmlId);
		setTitle();
		prefs = getActivity().getSharedPreferences(Constants.PREFS_NAME, Context.MODE_MULTI_PROCESS);
	}


	private int getXmlId() {
		if (getArguments() == null || !getArguments().containsKey(XML_NAME)) return 0;
		String xmlName = getArguments().getString(XML_NAME);
		int settingsXmlId = getActivity().getResources().getIdentifier(xmlName, "xml",
				getActivity().getPackageName());
		return settingsXmlId;
	}


	private void setTitle() {
		String title = getString(R.string.settings_category_preferences);
		if (getArguments() != null && getArguments().containsKey(XML_NAME)) {
			String xmlName = getArguments().getString(XML_NAME);
			if (!TextUtils.isEmpty(xmlName)) {
				int stringResourceId = getActivity().getResources().getIdentifier(xmlName.replace("settings_", "settings_screen_"), "string",
						getActivity().getPackageName());
				title = stringResourceId != 0 ? getString(stringResourceId) : title;
			}
		}
		((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(title);
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
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		super.onPreferenceTreeClick(preferenceScreen, preference);
		if (preference instanceof PreferenceScreen) {
			((SettingsActivity) getActivity()).switchToScreen(preference.getKey());
		}
		return false;
	}


	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();


		// Swiping action
		final CheckBoxPreference swipeToTrash = (CheckBoxPreference) findPreference("settings_swipe_to_trash");
		if (swipeToTrash != null) {
			if (prefs.getBoolean("settings_swipe_to_trash", false)) {
				swipeToTrash.setChecked(true);
				swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_2));
			} else {
				swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_1));
				swipeToTrash.setChecked(false);
			}
			swipeToTrash.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, final Object newValue) {
					if ((Boolean) newValue) {
						swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_2));
					} else {
						swipeToTrash.setSummary(getResources().getString(R.string.settings_swipe_to_trash_summary_1));
					}
					swipeToTrash.setChecked((Boolean) newValue);
					return false;
				}
			});
		}


		// Show uncategorized notes in menu
		final CheckBoxPreference showUncategorized = (CheckBoxPreference) findPreference(Constants
				.PREF_SHOW_UNCATEGORIZED);
		if (showUncategorized != null) {
			showUncategorized.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, final Object newValue) {
					showUncategorized.setChecked((Boolean) newValue);
					return false;
				}
			});
		}


		// Show Automatically adds location to new notes
		final CheckBoxPreference autoLocation = (CheckBoxPreference) findPreference(Constants.PREF_AUTO_LOCATION);
		if (autoLocation != null) {
			autoLocation.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, final Object newValue) {
					autoLocation.setChecked((Boolean) newValue);
					return false;
				}
			});
		}


		// Maximum video attachment size
		final EditTextPreference maxVideoSize = (EditTextPreference) findPreference("settings_max_video_size");
		if (maxVideoSize != null) {
			String maxVideoSizeValue = prefs.getString("settings_max_video_size", getString(R.string.not_set));
			maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + String.valueOf
					(maxVideoSizeValue));
			maxVideoSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					maxVideoSize.setSummary(getString(R.string.settings_max_video_size_summary) + ": " + String
							.valueOf(newValue));
					prefs.edit().putString("settings_max_video_size", newValue.toString()).commit();
					return false;
				}
			});
		}

		// Text size
		final ListPreference textSize = (ListPreference) findPreference("settings_text_size");
		if (textSize != null) {
			int textSizeIndex = textSize.findIndexOfValue(prefs.getString("settings_text_size", "default"));
			String textSizeString = getResources().getStringArray(R.array.text_size)[textSizeIndex];
			textSize.setSummary(textSizeString);
			textSize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int textSizeIndex = textSize.findIndexOfValue(newValue.toString());
					String checklistString = getResources().getStringArray(R.array.text_size)[textSizeIndex];
					textSize.setSummary(checklistString);
					prefs.edit().putString("settings_text_size", newValue.toString()).commit();
					textSize.setValueIndex(textSizeIndex);
					return false;
				}
			});
		}


		// Application's colors
		final ListPreference colorsApp = (ListPreference) findPreference("settings_colors_app");
		if (colorsApp != null) {
			int colorsAppIndex = colorsApp.findIndexOfValue(prefs.getString("settings_colors_app",
					Constants.PREF_COLORS_APP_DEFAULT));
			String colorsAppString = getResources().getStringArray(R.array.colors_app)[colorsAppIndex];
			colorsApp.setSummary(colorsAppString);
			colorsApp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int colorsAppIndex = colorsApp.findIndexOfValue(newValue.toString());
					String colorsAppString = getResources().getStringArray(R.array.colors_app)[colorsAppIndex];
					colorsApp.setSummary(colorsAppString);
					prefs.edit().putString("settings_colors_app", newValue.toString()).commit();
					colorsApp.setValueIndex(colorsAppIndex);
					return false;
				}
			});
		}


		// Checklists
		final ListPreference checklist = (ListPreference) findPreference("settings_checked_items_behavior");
		if (checklist != null) {
			int checklistIndex = checklist.findIndexOfValue(prefs.getString("settings_checked_items_behavior", "0"));
			String checklistString = getResources().getStringArray(R.array.checked_items_behavior)[checklistIndex];
			checklist.setSummary(checklistString);
			checklist.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int checklistIndex = checklist.findIndexOfValue(newValue.toString());
					String checklistString = getResources().getStringArray(R.array.checked_items_behavior)
							[checklistIndex];
					checklist.setSummary(checklistString);
					prefs.edit().putString("settings_checked_items_behavior", newValue.toString()).commit();
					checklist.setValueIndex(checklistIndex);
					return false;
				}
			});
		}


		// Widget's colors
		final ListPreference colorsWidget = (ListPreference) findPreference("settings_colors_widget");
		if (colorsWidget != null) {
			int colorsWidgetIndex = colorsWidget.findIndexOfValue(prefs.getString("settings_colors_widget",
					Constants.PREF_COLORS_APP_DEFAULT));
			String colorsWidgetString = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex];
			colorsWidget.setSummary(colorsWidgetString);
			colorsWidget.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int colorsWidgetIndex = colorsWidget.findIndexOfValue(newValue.toString());
					String colorsWidgetString = getResources().getStringArray(R.array.colors_widget)[colorsWidgetIndex];
					colorsWidget.setSummary(colorsWidgetString);
					prefs.edit().putString("settings_colors_widget", newValue.toString()).commit();
					colorsWidget.setValueIndex(colorsWidgetIndex);
					return false;
				}
			});
		}


		// Notification snooze delay
		final EditTextPreference snoozeDelay = (EditTextPreference) findPreference
				("settings_notification_snooze_delay");
		if (snoozeDelay != null) {
			String snoozeDelayValue = prefs.getString("settings_notification_snooze_delay", "10");
			snoozeDelay.setSummary(String.valueOf(snoozeDelayValue) + " " + getString(R.string.minutes));
			snoozeDelay.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					snoozeDelay.setSummary(String.valueOf(newValue) + " " + getString(R.string.minutes));
					prefs.edit().putString("settings_notification_snooze_delay", newValue.toString()).apply();
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
					prefs.edit().putString("settings_notification_ringtone", uri.toString()).commit();
					break;
			}
		}
	}
}
