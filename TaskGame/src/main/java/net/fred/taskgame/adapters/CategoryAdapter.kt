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
import android.widget.ImageView
import android.widget.TextView
import net.fred.taskgame.R
import net.fred.taskgame.activities.MainActivity
import net.fred.taskgame.models.Category
import net.fred.taskgame.utils.NavigationUtils

class CategoryAdapter(private val mActivity: Activity, private val categories: List<Category>) : BaseAdapter() {
    private val layout: Int
    private val inflater: LayoutInflater = mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    init {
        this.layout = R.layout.category_list_item
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

        val holder: CategoryViewHolder
        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false)

            holder = CategoryViewHolder()

            holder.imgIcon = convertView!!.findViewById(R.id.icon) as ImageView
            holder.txtTitle = convertView.findViewById(R.id.title) as TextView
            convertView.tag = holder
        } else {
            holder = convertView.tag as CategoryViewHolder
        }

        // Set the results into TextViews
        holder.txtTitle!!.text = category.name

        if (isSelected(position)) {
            holder.txtTitle!!.setTextColor(Color.BLACK)
            holder.txtTitle!!.setTypeface(null, Typeface.BOLD)
        } else {
            holder.txtTitle!!.setTextColor(ContextCompat.getColor(mActivity, R.color.drawer_text))
            holder.txtTitle!!.setTypeface(null, Typeface.NORMAL)
        }

        // Set the results into ImageView checking if an icon is present before
        val img = ContextCompat.getDrawable(mActivity, R.drawable.square)
        val cf = LightingColorFilter(Color.TRANSPARENT, category.color)
        img.mutate().colorFilter = cf
        holder.imgIcon!!.setImageDrawable(img)
        val padding = 12
        holder.imgIcon!!.setPadding(padding, padding, padding, padding)

        return convertView
    }


    private fun isSelected(position: Int): Boolean {

        // Managing temporary navigation indicator when coming from a widget
        val widgetCatId = if (mActivity is MainActivity) mActivity.widgetCatId else null

        val navigation = widgetCatId ?: NavigationUtils.navigation
        return navigation == categories[position].id
    }

}

internal class CategoryViewHolder {
    var imgIcon: ImageView? = null
    var txtTitle: TextView? = null
}
