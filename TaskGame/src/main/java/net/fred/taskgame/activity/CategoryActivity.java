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

package net.fred.taskgame.activity;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.afollestad.materialdialogs.MaterialDialog;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Category;
import net.fred.taskgame.utils.Constants;
import net.fred.taskgame.utils.DbHelper;
import net.fred.taskgame.utils.PrefUtils;

import org.parceler.Parcels;

public class CategoryActivity extends Activity {

    Category category;
    EditText title;
    EditText description;
    ColorPicker picker;
    Button deleteBtn;
    Button saveBtn;
    private boolean colorChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // Retrieving intent
        category = Parcels.unwrap(getIntent().getParcelableExtra(Constants.INTENT_CATEGORY));

        // Getting Views from layout
        initViews();

        if (category == null) {
            category = new Category();
        } else {
            populateViews();
        }
    }

    private void initViews() {
        title = (EditText) findViewById(R.id.category_title);
        description = (EditText) findViewById(R.id.category_description);
        picker = (ColorPicker) findViewById(R.id.colorpicker_category);
        picker.setOnColorChangedListener(new OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                picker.setOldCenterColor(picker.getColor());
                colorChanged = true;
            }
        });
        // Long click on color picker to remove color
        picker.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                picker.setColor(Color.WHITE);
                return true;
            }
        });
        picker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                picker.setColor(Color.WHITE);
            }
        });

        // Added invisible saturation and value bars to get achieve pastel colors
        SaturationBar saturationbar = (SaturationBar) findViewById(R.id.saturationbar_category);
        saturationbar.setSaturation(0.4f);
        picker.addSaturationBar(saturationbar);
        ValueBar valuebar = (ValueBar) findViewById(R.id.valuebar_category);
        valuebar.setValue(0.9f);
        picker.addValueBar(valuebar);

        deleteBtn = (Button) findViewById(R.id.delete);
        saveBtn = (Button) findViewById(R.id.save);
//		discardBtn = (Button) findViewById(R.id.discard);

        // Buttons events
        deleteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCategory();
            }
        });
        saveBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // In case category name is not compiled a message will be shown
                if (title.getText().toString().length() > 0) {
                    saveCategory();
                } else {
                    title.setError(getString(R.string.category_missing_title));
                }
            }
        });
    }

    private void populateViews() {
        title.setText(category.name);
        description.setText(category.description);
        // Reset picker to saved color
        String color = category.color;
        if (color != null && color.length() > 0) {
            picker.setColor(Integer.parseInt(color));
            picker.setOldCenterColor(Integer.parseInt(color));
        }
        deleteBtn.setVisibility(View.VISIBLE);
    }


    /**
     * Category saving
     */
    private void saveCategory() {
        category.name = title.getText().toString();
        category.description = description.getText().toString();
        if (colorChanged || category.color == null)
            category.color = String.valueOf(picker.getColor());

        // Saved to DB and new id or update result catched
        category.save();

        // Sets result to show proper message
        getIntent().putExtra(Constants.INTENT_CATEGORY, Parcels.wrap(category));
        setResult(RESULT_OK, getIntent());
        finish();
    }

    private void deleteCategory() {

        // Retrieving how many tasks are categorized with category to be deleted
        long count = DbHelper.getCategorizedCount(category);
        String msg = "";
        if (count > 0) {
            msg = getString(R.string.delete_category_confirmation).replace("$1$", String.valueOf(count));
        }

        new MaterialDialog.Builder(this)
                .title(R.string.delete_unused_category_confirmation)
                .content(msg)
                .positiveText(R.string.confirm)
                .positiveColorRes(R.color.colorAccent)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        // Changes navigation if actually are shown tasks associated with this category
                        String navTasks = getResources().getStringArray(R.array.navigation_list_codes)[0];
                        String navigation = PrefUtils.getString(PrefUtils.PREF_NAVIGATION, navTasks);
                        if (String.valueOf(category.id).equals(navigation))
                            PrefUtils.putString(PrefUtils.PREF_NAVIGATION, navTasks);
                        // Removes category and edit tasks associated with it
                        DbHelper.deleteCategoryAsync(category);

                        // Sets result to show proper message
                        setResult(RESULT_FIRST_USER);
                        finish();
                    }
                }).build().show();
    }
}
