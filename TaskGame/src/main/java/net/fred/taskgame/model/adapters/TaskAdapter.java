package net.fred.taskgame.model.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.utils.TextHelper;
import net.fred.taskgame.utils.recycler.ItemActionListener;
import net.fred.taskgame.utils.recycler.ItemActionViewHolder;
import net.fred.taskgame.view.SquareImageView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TaskAdapter extends MultiSelectAdapter<TaskAdapter.TaskViewHolder> {

    private final Activity mActivity;
    private final List<Task> mTasks;
    private ItemActionListener mItemActionListener;

    public static class TaskViewHolder extends RecyclerView.ViewHolder implements ItemActionViewHolder {

        @Bind(R.id.card_view)
        CardView mCardView;
        @Bind(R.id.category_marker)
        View mCategoryMarker;
        @Bind(R.id.task_title)
        TextView mTitle;
        @Bind(R.id.task_content)
        TextView mContent;
        @Bind(R.id.reward)
        TextView mReward;
        @Bind(R.id.attachment_thumbnail)
        SquareImageView mAttachmentThumbnail;

        public TaskViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }

        @Override
        public void onItemSelected() {
            // Highlighted if is part of multi selection of tasks. Remember to search for child with card ui
            mCardView.setCardBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            mCardView.setCardBackgroundColor(Color.WHITE);
        }
    }

    public TaskAdapter(Activity activity, ItemActionListener listener, RecyclerView recyclerView, List<Task> tasks) {
        super(listener, recyclerView);
        mItemActionListener = listener;
        mActivity = activity;
        mTasks = tasks;

        setHasStableIds(true);
        recyclerView.setAdapter(this);
    }

    public List<Task> getTasks() {
        return mTasks;
    }

    public void setTasks(List<Task> tasks) {
        mTasks.clear();
        mTasks.addAll(tasks);
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return mTasks.get(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final TaskViewHolder holder, int position) {
        Task task = mTasks.get(position);

        // Init texts
        Spanned[] titleAndContent = TextHelper.parseTitleAndContent(task);
        holder.mTitle.setText(titleAndContent[0]);
        if (titleAndContent[1].length() > 0) {
            holder.mContent.setText(titleAndContent[1]);
            holder.mContent.setVisibility(View.VISIBLE);
        } else {
            holder.mContent.setVisibility(View.GONE);
        }
        holder.mReward.setText(String.valueOf(task.pointReward));

        // Init thumbnail
        if (task.getAttachmentsList() != null && !task.getAttachmentsList().isEmpty()) {
            holder.mAttachmentThumbnail.setVisibility(View.VISIBLE);
            Attachment mAttachment = task.getAttachmentsList().get(0);
            Uri thumbnailUri = mAttachment.getThumbnailUri(mActivity);
            Glide.with(mActivity)
                    .load(thumbnailUri)
                    .centerCrop()
                    .crossFade()
                    .into(holder.mAttachmentThumbnail);
        } else {
            holder.mAttachmentThumbnail.setVisibility(View.GONE);
        }

        // Init task and category marker colors
        if (!TextUtils.isEmpty(task.questId)) { // If this is an official quest, let's make it quite visible
            holder.mCardView.setCardBackgroundColor(ContextCompat.getColor(mActivity, R.color.quest_color));
            holder.mCategoryMarker.setBackgroundColor(Color.TRANSPARENT);
        } else {
            // If category is set the color will be applied on the appropriate target
            if (task.getCategory() != null && task.getCategory().color != null) {
                holder.mCategoryMarker.setBackgroundColor(Integer.parseInt(task.getCategory().color));
            } else {
                holder.mCategoryMarker.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        // set listeners
        holder.mCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.getAdapterPosition() != -1) { // seems -1 is sometimes possible...
                    int position = holder.getAdapterPosition();
                    if (getSelectedItemCount() > 0) {
                        if (isItemSelected(position)) {
                            toggleSelection(false, position);
                            mItemActionListener.onItemUnselected(position);
                        } else {
                            toggleSelection(true, position);
                            mItemActionListener.onItemSelected(position);
                        }
                    } else {
                        mItemActionListener.onItemClicked(position);
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }
}
