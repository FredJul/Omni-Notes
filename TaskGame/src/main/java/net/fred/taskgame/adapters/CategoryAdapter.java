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
package net.fred.taskgame.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.fred.taskgame.R;
import net.fred.taskgame.activities.MainActivity;
import net.fred.taskgame.models.Category;
import net.fred.taskgame.utils.NavigationUtils;

import java.util.List;

public class CategoryAdapter extends BaseAdapter {

    private final Activity mActivity;
    private final int layout;
    private final List<Category> categories;
    private final LayoutInflater inflater;

    public CategoryAdapter(Activity mActivity, List<Category> categories) {
        this.mActivity = mActivity;
        this.layout = R.layout.category_list_item;
        this.categories = categories;
        inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int position) {
        return categories.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {

        // Finds elements
        Category category = categories.get(position);

        CategoryViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false);

            holder = new CategoryViewHolder();

            holder.imgIcon = (ImageView) convertView.findViewById(R.id.icon);
            holder.txtTitle = (TextView) convertView.findViewById(R.id.title);
            convertView.setTag(holder);
        } else {
            holder = (CategoryViewHolder) convertView.getTag();
        }

        // Set the results into TextViews
        holder.txtTitle.setText(category.name);

        if (isSelected(position)) {
            holder.txtTitle.setTextColor(Color.BLACK);
            holder.txtTitle.setTypeface(null, Typeface.BOLD);
        } else {
            holder.txtTitle.setTextColor(ContextCompat.getColor(mActivity, R.color.drawer_text));
            holder.txtTitle.setTypeface(null, Typeface.NORMAL);
        }

        // Set the results into ImageView checking if an icon is present before
        Drawable img = ContextCompat.getDrawable(mActivity, R.drawable.square);
        ColorFilter cf = new LightingColorFilter(Color.TRANSPARENT, category.color);
        img.mutate().setColorFilter(cf);
        holder.imgIcon.setImageDrawable(img);
        int padding = 12;
        holder.imgIcon.setPadding(padding, padding, padding, padding);

        return convertView;
    }


    private boolean isSelected(int position) {

        // Managing temporary navigation indicator when coming from a widget
        String widgetCatId = mActivity instanceof MainActivity ? ((MainActivity) mActivity).getWidgetCatId() : null;

        String navigation = widgetCatId != null ? widgetCatId : NavigationUtils.getNavigation();
        return (navigation == categories.get(position).id);
    }

}

class CategoryViewHolder {
    ImageView imgIcon;
    TextView txtTitle;
}
