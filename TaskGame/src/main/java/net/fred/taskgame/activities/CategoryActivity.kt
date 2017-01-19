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

package net.fred.taskgame.activities

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener
import com.larswerkman.holocolorpicker.SaturationBar
import com.larswerkman.holocolorpicker.ValueBar
import kotlinx.android.synthetic.main.activity_category.*
import net.fred.taskgame.R
import net.fred.taskgame.models.Category
import net.fred.taskgame.utils.Constants
import net.fred.taskgame.utils.DbUtils
import net.fred.taskgame.utils.NavigationUtils
import net.frju.androidquery.gen.CATEGORY
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.onClick
import org.parceler.Parcels

class CategoryActivity : Activity() {

    internal var category: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        // Retrieving intent
        category = Parcels.unwrap<Category>(intent.getParcelableExtra<Parcelable>(Constants.EXTRA_CATEGORY))

        // Getting Views from layout
        initViews()

        if (category == null) {
            category = Category()
        } else {
            populateViews()
        }
    }

    private fun initViews() {
        colorpicker_category.onColorChangedListener = OnColorChangedListener { colorpicker_category.oldCenterColor = colorpicker_category.color }
        // Long click on color picker to remove color
        colorpicker_category.setOnLongClickListener {
            colorpicker_category.color = Color.WHITE
            true
        }
        colorpicker_category.onClick { colorpicker_category.color = Color.WHITE }

        // Added invisible saturation and value bars to get achieve pastel colors
        val saturationBar = findViewById(R.id.saturationbar_category) as SaturationBar
        saturationBar.setSaturation(0.4f)
        colorpicker_category.addSaturationBar(saturationBar)
        val valueBar = findViewById(R.id.valuebar_category) as ValueBar
        valueBar.setValue(0.9f)
        colorpicker_category.addValueBar(valueBar)

        //		discardBtn = (Button) findViewById(R.id.discard);

        // Buttons events
        delete.onClick { deleteCategory() }
        save.onClick {
            // In case category name is not compiled a message will be shown
            if (category_title.text.toString().isNotEmpty()) {
                saveCategory()
            } else {
                category_title.error = getString(R.string.category_missing_title)
            }
        }
    }

    private fun populateViews() {
        category_title.setText(category!!.name)
        category_description.setText(category!!.description)
        // Reset picker to saved color
        colorpicker_category.color = category!!.color
        colorpicker_category.oldCenterColor = category!!.color
        delete.visibility = View.VISIBLE
    }


    /**
     * Category saving
     */
    private fun saveCategory() {
        category!!.name = category_title.text.toString()
        category!!.description = category_description.text.toString()
        category!!.color = colorpicker_category.color

        // Saved to DB and new id or update result catched
        CATEGORY.save(category!!).query()
        category!!.saveInFirebase()

        // Sets result to show proper message
        intent.putExtra(Constants.EXTRA_CATEGORY, Parcels.wrap<Category>(category))
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun deleteCategory() {

        // Retrieving how many tasks are categorized with category to be deleted
        val count = DbUtils.getActiveTaskCountByCategory(category!!)
        var msg = ""
        if (count > 0) {
            msg = getString(R.string.delete_category_confirmation).replace("$1$", count.toString())
        }

        AlertDialog.Builder(this)
                .setTitle(R.string.delete_unused_category_confirmation)
                .setMessage(msg)
                .setPositiveButton(R.string.confirm) { dialog, id ->
                    // Changes navigation if actually are shown tasks associated with this category
                    if (NavigationUtils.isDisplayingCategory(category)) {
                        NavigationUtils.navigation = NavigationUtils.TASKS
                    }
                    // Removes category and edit tasks associated with it
                    doAsync {
                        DbUtils.deleteCategory(category!!)
                    }

                    // Sets result to show proper message
                    setResult(Activity.RESULT_FIRST_USER)
                    finish()
                }.show()
    }
}
