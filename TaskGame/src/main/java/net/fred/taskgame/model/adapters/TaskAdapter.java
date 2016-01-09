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
package net.fred.taskgame.model.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nhaarman.listviewanimations.util.Insertable;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task_Table;
import net.fred.taskgame.model.holders.NoteViewHolder;
import net.fred.taskgame.utils.BitmapHelper;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.TextHelper;
import net.fred.taskgame.view.SquareImageView;

import java.util.List;


public class TaskAdapter extends ArrayAdapter<Task> implements Insertable {

    private final Activity mActivity;
    private final List<Task> mTasks;
    private final SparseBooleanArray mSelectedItems = new SparseBooleanArray();
    private final LayoutInflater mInflater;


    public TaskAdapter(Activity activity, List<Task> tasks) {
        super(activity, R.layout.task_item, tasks);
        this.mActivity = activity;
        this.mTasks = tasks;

        mInflater = mActivity.getLayoutInflater();
    }

    public List<Task> getTasks() {
        return mTasks;
    }

    public void setTasks(List<Task> tasks) {
        mTasks.clear();
        mTasks.addAll(tasks);
        notifyDataSetChanged();
    }

    /**
     * Replaces tasks
     */
    public void replace(Task task, int index) {
        if (mTasks.indexOf(task) != -1) {
            mTasks.remove(index);
        } else {
            index = mTasks.size();
        }
        mTasks.add(index, task);

        notifyDataSetChanged();
    }


    @Override
    public void add(int i, @NonNull Object o) {
        insert((Task) o, i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Task task = mTasks.get(position);

        NoteViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.task_item, parent, false);

            holder = new NoteViewHolder();

            holder.root = convertView.findViewById(R.id.root);
            holder.cardLayout = convertView.findViewById(R.id.card_layout);
            holder.categoryMarker = convertView.findViewById(R.id.category_marker);

            holder.title = (TextView) convertView.findViewById(R.id.note_title);
            holder.content = (TextView) convertView.findViewById(R.id.note_content);
            holder.reward = (TextView) convertView.findViewById(R.id.reward);
            holder.date = (TextView) convertView.findViewById(R.id.taskModifDate);

            holder.attachmentThumbnail = (SquareImageView) convertView.findViewById(R.id.attachmentThumbnail);

            convertView.setTag(R.id.holder, holder);

        } else {
            holder = (NoteViewHolder) convertView.getTag(R.id.holder);
        }

        initText(task, holder);

        initDates(task, holder);


        // Highlighted if is part of multi selection of tasks. Remember to search for child with card ui
        if (mSelectedItems.get(position)) {
            holder.cardLayout.setBackgroundColor(mActivity.getResources().getColor(R.color.list_bg_selected));
        } else {
            restoreDrawable(task, convertView);
        }
        initThumbnail(task, holder);

        return convertView;
    }

    private void initThumbnail(Task task, NoteViewHolder holder) {
        holder.attachmentThumbnail.setVisibility(View.GONE);

        // Attachment thumbnail
        if (task.getAttachmentsList() != null && !task.getAttachmentsList().isEmpty()) {
            holder.attachmentThumbnail.setVisibility(View.VISIBLE);
            Attachment mAttachment = task.getAttachmentsList().get(0);
            Uri thumbnailUri = BitmapHelper.getThumbnailUri(mActivity, mAttachment);
            Glide.with(mActivity)
                    .load(thumbnailUri)
                    .centerCrop()
                    .crossFade()
                    .into(holder.attachmentThumbnail);
        }
    }

    private void initDates(Task task, NoteViewHolder holder) {
        String dateText = getDateText(mActivity, task);
        holder.date.setText(dateText);
        holder.date.setCompoundDrawablesWithIntrinsicBounds(0, 0, task.alarmDate != 0 ? R.drawable.ic_alarm_grey600_18dp : 0, 0);
    }

    private void initText(Task task, NoteViewHolder holder) {
        Spanned[] titleAndContent = TextHelper.parseTitleAndContent(task);
        holder.title.setText(titleAndContent[0]);
        if (titleAndContent[1].length() > 0) {
            holder.content.setText(titleAndContent[1]);
            holder.content.setVisibility(View.VISIBLE);
        } else {
            holder.content.setVisibility(View.GONE);
        }

        holder.reward.setText(String.valueOf(task.pointReward));
    }

    /**
     * Choosing which date must be shown depending on sorting criteria
     *
     * @return String with formatted date
     */
    public static String getDateText(Context mContext, Task task) {
        String dateText = "";
        String sort_column = PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, "");

        if (sort_column.equals(Task_Table.creationDate.getContainerKey()) || sort_column.equals(Task_Table.lastModificationDate.getContainerKey())) {
            if (task.lastModificationDate != 0) {
                dateText = mContext.getString(R.string.last_update, task.getLastModificationShort(mContext));
            } else {
                dateText = mContext.getString(R.string.creation, task.getCreationShort(mContext));
            }
        } else {
            String alarmShort = task.getAlarmShort(mContext);

            if (!TextUtils.isEmpty(alarmShort)) {
                dateText = mContext.getString(R.string.alarm_set_on, task.getAlarmShort(mContext));
            }
        }

        return dateText;
    }


    public SparseBooleanArray getSelectedItems() {
        return mSelectedItems;
    }

    public void addSelectedItem(Integer selectedItem) {
        this.mSelectedItems.put(selectedItem, true);
    }

    public void removeSelectedItem(Integer selectedItem) {
        this.mSelectedItems.delete(selectedItem);
    }

    public void clearSelectedItems() {
        this.mSelectedItems.clear();
    }

    public void restoreDrawable(Task task, View v) {
        final int paddingBottom = v.getPaddingBottom(), paddingLeft = v.getPaddingLeft();
        final int paddingRight = v.getPaddingRight(), paddingTop = v.getPaddingTop();
        v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        colorNote(task, v);
    }

    /**
     * Color of category marker if note is categorized a function is active in preferences
     */
    private void colorNote(Task task, View v) {
        NoteViewHolder holder = (NoteViewHolder) v.getTag(R.id.holder);

        if (!TextUtils.isEmpty(task.questId)) { // If this is an official quest, let's make it quite visible
            holder.cardLayout.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.quest_color));
            holder.categoryMarker.setVisibility(View.GONE);
        } else {
            // Resetting transparent color to the view
            holder.cardLayout.setBackgroundColor(Color.TRANSPARENT);
            holder.categoryMarker.setVisibility(View.VISIBLE);

            // If category is set the color will be applied on the appropriate target
            if (task.getCategory() != null && task.getCategory().color != null) {
                holder.categoryMarker.setBackgroundColor(Integer.parseInt(task.getCategory().color));
            } else {
                holder.categoryMarker.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }
}
