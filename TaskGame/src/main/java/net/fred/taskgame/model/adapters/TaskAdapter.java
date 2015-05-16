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
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.Spanned;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nhaarman.listviewanimations.util.Insertable;

import net.fred.taskgame.R;
import net.fred.taskgame.async.TextWorkerTask;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.model.Task$Table;
import net.fred.taskgame.model.holders.NoteViewHolder;
import net.fred.taskgame.utils.BitmapHelper;
import net.fred.taskgame.utils.Navigation;
import net.fred.taskgame.utils.PrefUtils;
import net.fred.taskgame.utils.TextHelper;
import net.fred.taskgame.view.SquareImageView;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;


public class TaskAdapter extends ArrayAdapter<Task> implements Insertable {

    private final Activity mActivity;
    private final List<Task> mTasks;
    private final SparseBooleanArray mSelectedItems = new SparseBooleanArray();
    private final LayoutInflater mInflater;


    public TaskAdapter(Activity activity, List<Task> tasks) {
        super(activity, R.layout.note_layout_expanded, tasks);
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
            convertView = mInflater.inflate(R.layout.note_layout_expanded, parent, false);

            holder = new NoteViewHolder();

            holder.root = convertView.findViewById(R.id.root);
            holder.cardLayout = convertView.findViewById(R.id.card_layout);
            holder.categoryMarker = convertView.findViewById(R.id.category_marker);

            holder.title = (TextView) convertView.findViewById(R.id.note_title);
            holder.content = (TextView) convertView.findViewById(R.id.note_content);
            holder.date = (TextView) convertView.findViewById(R.id.note_date);

            holder.locationIcon = (ImageView) convertView.findViewById(R.id.locationIcon);
            holder.alarmIcon = (ImageView) convertView.findViewById(R.id.alarmIcon);

            holder.attachmentThumbnail = (SquareImageView) convertView.findViewById(R.id.attachmentThumbnail);

            convertView.setTag(holder);

        } else {
            holder = (NoteViewHolder) convertView.getTag();
        }

        initText(task, holder);

        initIcons(task, holder);

        initDates(task, holder);


        // Highlighted if is part of multi selection of tasks. Remember to search for child with card ui
        if (mSelectedItems.get(position)) {
            holder.cardLayout.setBackgroundColor(mActivity.getResources().getColor(
                    R.color.list_bg_selected));
        } else {
            restoreDrawable(task, holder.cardLayout, holder);
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
    }


    private void initIcons(Task task, NoteViewHolder holder) {
        // ...the location
        holder.locationIcon.setVisibility(task.longitude != 0 ? View.VISIBLE : View.GONE);

        // ...the presence of an alarm
        holder.alarmIcon.setVisibility(task.alarmDate != 0 ? View.VISIBLE : View.GONE);
    }


    private void initText(Task note, NoteViewHolder holder) {
        try {
            if (note.isChecklist) {
                TextWorkerTask task = new TextWorkerTask(mActivity, holder.title, holder.content);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, note);
            } else {
                Spanned[] titleAndContent = TextHelper.parseTitleAndContent(note);
                holder.title.setText(titleAndContent[0]);
                holder.content.setText(titleAndContent[1]);
                holder.title.setText(titleAndContent[0]);
                if (titleAndContent[1].length() > 0) {
                    holder.content.setText(titleAndContent[1]);
                    holder.content.setVisibility(View.VISIBLE);
                } else {
                    holder.content.setVisibility(View.INVISIBLE);
                }
            }
        } catch (RejectedExecutionException e) {

        }
    }


    /**
     * Choosing which date must be shown depending on sorting criteria
     *
     * @return String ith formatted date
     */
    public static String getDateText(Context mContext, Task task) {
        String dateText;
        String sort_column;

        // Reminder screen forces sorting
        if (Navigation.checkNavigation(Navigation.REMINDERS)) {
            sort_column = Task$Table.ALARMDATE;
        } else {
            sort_column = PrefUtils.getString(PrefUtils.PREF_SORTING_COLUMN, "");
        }

        if (sort_column.equals(Task$Table.ALARMDATE)) {
            String alarmShort = task.getAlarmShort(mContext);

            if (alarmShort.length() == 0) {
                dateText = mContext.getString(R.string.no_reminder_set);
            } else {
                dateText = mContext.getString(R.string.alarm_set_on) + " " + task.getAlarmShort(mContext);
            }
        } else if (task.lastModificationDate != 0) {
            dateText = mContext.getString(R.string.last_update) + " " + task.getLastModificationShort(mContext);
        } else {
            dateText = mContext.getString(R.string.creation) + " " + task.getCreationShort(mContext);
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
        restoreDrawable(task, v, null);
    }


    public void restoreDrawable(Task task, View v, NoteViewHolder holder) {
        final int paddingBottom = v.getPaddingBottom(), paddingLeft = v.getPaddingLeft();
        final int paddingRight = v.getPaddingRight(), paddingTop = v.getPaddingTop();
        v.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        colorNote(task, v, holder);
    }


    @SuppressWarnings("unused")
    private void colorNote(Task task, View v) {
        colorNote(task, v, null);
    }

    /**
     * Color of category marker if note is categorized a function is active in preferences
     */
    private void colorNote(Task task, View v, NoteViewHolder holder) {

        String colorsPref = PrefUtils.getString("settings_colors_app", PrefUtils.PREF_COLORS_APP_DEFAULT);

        // Checking preference
        if (!colorsPref.equals("disabled")) {

            // Resetting transparent color to the view
            v.setBackgroundColor(Color.parseColor("#00000000"));

            // If category is set the color will be applied on the appropriate target
            if (task.getCategory() != null && task.getCategory().color != null) {
                if (colorsPref.equals("complete") || colorsPref.equals("list")) {
                    v.setBackgroundColor(Integer.parseInt(task.getCategory().color));
                } else {
                    if (holder != null) {
                        holder.categoryMarker.setBackgroundColor(Integer.parseInt(task.getCategory().color));
                    } else {
                        v.findViewById(R.id.category_marker).setBackgroundColor(Integer.parseInt(task.getCategory().color));
                    }
                }
            } else {
                v.findViewById(R.id.category_marker).setBackgroundColor(0);
            }
        }
    }
}
