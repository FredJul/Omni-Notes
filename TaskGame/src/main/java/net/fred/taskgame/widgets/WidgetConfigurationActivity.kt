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

package net.fred.taskgame.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_widget_configuration.*
import net.fred.taskgame.R
import net.fred.taskgame.adapters.CategoryAdapter
import net.fred.taskgame.models.Category
import net.fred.taskgame.utils.DbUtils
import org.jetbrains.anko.onClick

class WidgetConfigurationActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mActivity = this

        setResult(Activity.RESULT_CANCELED)

        setContentView(R.layout.activity_widget_configuration)

        widget_config_radiogroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.widget_config_tasks -> widget_config_spinner.visibility = View.GONE

                R.id.widget_config_categories -> widget_config_spinner.visibility = View.VISIBLE
            }
        }

        widget_config_spinner.visibility = View.GONE

        val categories = DbUtils.categories
        widget_config_spinner.adapter = CategoryAdapter(mActivity, categories)

        val configOkButton = findViewById(R.id.widget_config_confirm) as Button
        configOkButton.onClick {
            var categoryId: String? = null
            if (widget_config_radiogroup.checkedRadioButtonId != R.id.widget_config_tasks) {
                val tag = widget_config_spinner.selectedItem as Category
                categoryId = tag.id
            }

            // Updating the ListRemoteViewsFactory parameter to get the list of tasks
            ListRemoteViewsFactory.updateConfiguration(appWidgetId, categoryId)

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)

            finish()
        }

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // If they gave us an intent without the widget id, just bail.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        if (categories.isEmpty()) {
            // Updating the ListRemoteViewsFactory parameter to get the list of tasks
            ListRemoteViewsFactory.updateConfiguration(appWidgetId, null)

            val resultValue = Intent()
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)

            finish()
        }
    }

}
