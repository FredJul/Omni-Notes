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

package net.fred.taskgame.adapters

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.graphics.Typeface
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import kotlinx.android.synthetic.main.item_category.view.*
import net.fred.taskgame.R
import net.fred.taskgame.activities.MainActivity
import net.fred.taskgame.models.Category
import net.fred.taskgame.utils.NavigationUtils

class CategoryAdapter(private val activity: Activity, private val categories: List<Category>) : BaseAdapter() {
    private val layout: Int
    private val inflater: LayoutInflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        this.layout = R.layout.item_category
    }

    override fun getCount(): Int {
        return categories.size
    }

    override fun getItem(position: Int): Any {
        return categories[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, _convertView: View?, parent: ViewGroup): View {
        var convertView = _convertView

        // Finds elements
        val category = categories[position]

        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false)
        }

        // Set the results into TextViews
        convertView!!.title.text = category.name

        if (isSelected(position)) {
            convertView.title.setTextColor(Color.BLACK)
            convertView.title.setTypeface(null, Typeface.BOLD)
        } else {
            convertView.title.setTextColor(ContextCompat.getColor(activity, R.color.drawer_text))
            convertView.title.setTypeface(null, Typeface.NORMAL)
        }

        // Set the results into ImageView checking if an icon is present before
        val img = ContextCompat.getDrawable(activity, R.drawable.square)
        val cf = LightingColorFilter(Color.TRANSPARENT, category.color)
        img.mutate().colorFilter = cf
        convertView.icon.setImageDrawable(img)
        val padding = 12
        convertView.icon.setPadding(padding, padding, padding, padding)

        return convertView
    }


    private fun isSelected(position: Int): Boolean {

        // Managing temporary navigation indicator when coming from a widget
        val widgetCatId = if (activity is MainActivity) activity.widgetCatId else null

        val navigation = widgetCatId ?: NavigationUtils.navigation
        return navigation == categories[position].id
    }

}

