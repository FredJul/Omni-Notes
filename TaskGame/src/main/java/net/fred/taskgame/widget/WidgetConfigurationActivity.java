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

package net.fred.taskgame.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.model.adapters.NavDrawerCategoryAdapter;
import net.fred.taskgame.utils.DbHelper;

import java.util.List;

public class WidgetConfigurationActivity extends Activity {

    private Spinner categorySpinner;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private RadioGroup mRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity mActivity = this;

        setResult(RESULT_CANCELED);

        setContentView(R.layout.activity_widget_configuration);

        mRadioGroup = (RadioGroup) findViewById(R.id.widget_config_radiogroup);
        mRadioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.widget_config_tasks:
                        categorySpinner.setEnabled(false);
                        break;

                    case R.id.widget_config_categories:
                        categorySpinner.setEnabled(true);
                        break;
                }
            }
        });

        categorySpinner = (Spinner) findViewById(R.id.widget_config_spinner);
        categorySpinner.setEnabled(false);
        List<Category> categories = DbHelper.getCategories();
        categorySpinner.setAdapter(new NavDrawerCategoryAdapter(mActivity, categories));

        Button configOkButton = (Button) findViewById(R.id.widget_config_confirm);
        configOkButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                long categoryId = -1;
                if (mRadioGroup.getCheckedRadioButtonId() != R.id.widget_config_tasks) {
                    Category tag = (Category) categorySpinner.getSelectedItem();
                    categoryId = tag.id;
                }

                CheckBox showThumbnailsCheckBox = (CheckBox) findViewById(R.id.show_thumbnails);

                // Updating the ListRemoteViewsFactory parameter to get the list of tasks
                ListRemoteViewsFactory.updateConfiguration(mAppWidgetId,
                        categoryId, showThumbnailsCheckBox.isChecked());

                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        mAppWidgetId);
                setResult(RESULT_OK, resultValue);

                finish();
            }
        });

        // Checks if no tags are available and then disable that option
        if (categories.size() == 0) {
            mRadioGroup.setVisibility(View.GONE);
            categorySpinner.setVisibility(View.GONE);
        }

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If they gave us an intent without the widget id, just bail.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

}
