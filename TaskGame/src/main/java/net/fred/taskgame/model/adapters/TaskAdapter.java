package net.fred.taskgame.model.adapters;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemConstants;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.action.SwipeResultActionRemoveItem;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;

import net.fred.taskgame.R;
import net.fred.taskgame.model.Attachment;
import net.fred.taskgame.model.Task;
import net.fred.taskgame.utils.TextHelper;
import net.fred.taskgame.utils.UiUtils;
import net.fred.taskgame.view.SquareImageView;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TaskAdapter extends MultiSelectAdapter<TaskAdapter.TaskViewHolder>
        implements SwipeableItemAdapter<TaskAdapter.TaskViewHolder>, DraggableItemAdapter<TaskAdapter.TaskViewHolder> {

    private final Activity mActivity;
    private final List<Task> mTasks;
    private EventListener mEventListener;

    public interface EventListener {
        void onItemRemoved(int position);

        void onItemMoved(int fromPosition, int toPosition);

        void onItemViewClicked(View v, int position);

        void onItemViewLongClicked(View v, int position);
    }

    public static class TaskViewHolder extends AbstractDraggableSwipeableItemViewHolder {

        @Bind(R.id.container)
        View mContainer;
        @Bind(R.id.card_layout)
        View mCardLayout;
        @Bind(R.id.drag_handle)
        View mDragHandle;
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
        public View getSwipeableContainerView() {
            return mContainer;
        }
    }

    public TaskAdapter(Activity activity, List<Task> tasks) {
        mActivity = activity;
        mTasks = tasks;

        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    public List<Task> getTasks() {
        return mTasks;
    }

    public void setTasks(List<Task> tasks) {
        mTasks.clear();
        mTasks.addAll(tasks);
        notifyDataSetChanged();
    }

    private void onItemViewClick(View v, int position) {
        if (mEventListener != null && position != -1) { // seems -1 is sometimes possible...
            mEventListener.onItemViewClicked(v, position);
        }
    }

    private void onItemViewLongClick(View v, int position) {
        if (mEventListener != null && position != -1) { // seems -1 is sometimes possible...
            mEventListener.onItemViewLongClicked(v, position);
        }
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
            holder.mCardLayout.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.quest_color));
            holder.mDragHandle.setBackgroundColor(Color.TRANSPARENT);
        } else {
            // Resetting transparent color to the view
            holder.mCardLayout.setBackgroundColor(Color.TRANSPARENT);

            // If category is set the color will be applied on the appropriate target
            if (task.getCategory() != null && task.getCategory().color != null) {
                holder.mDragHandle.setBackgroundColor(Integer.parseInt(task.getCategory().color));
            } else {
                holder.mDragHandle.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        // Highlighted if is part of multi selection of tasks. Remember to search for child with card ui
        if (isItemSelected(position)) {
            holder.mCardLayout.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.list_bg_selected));
        }

        // set listeners
        holder.mContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemViewClick(v, holder.getAdapterPosition());
            }
        });
        holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onItemViewLongClick(v, holder.getAdapterPosition());
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    @Override
    public int onGetSwipeReactionType(TaskViewHolder holder, int position, int x, int y) {
        if (onCheckCanStartDrag(holder, position, x, y)) {
            return SwipeableItemConstants.REACTION_CAN_NOT_SWIPE_BOTH_H;
        } else {
            return SwipeableItemConstants.REACTION_CAN_SWIPE_BOTH_H;
        }
    }

    @Override
    public void onSetSwipeBackground(TaskViewHolder holder, int position, int type) {
//        int bgRes = 0;
//        switch (type) {
//            case Swipeable.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_neutral;
//                break;
//            case Swipeable.DRAWABLE_SWIPE_LEFT_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_left;
//                break;
//            case Swipeable.DRAWABLE_SWIPE_RIGHT_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_right;
//                break;
//        }
//
//        holder.itemView.setBackgroundResource(bgRes);
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        if (mEventListener != null) {
            mEventListener.onItemMoved(fromPosition, toPosition);
        }

        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public boolean onCheckCanStartDrag(TaskViewHolder holder, int position, int x, int y) {
        // x, y --- relative from the itemView's top-left

        final int offsetX = holder.mContainer.getLeft() + (int) (ViewCompat.getTranslationX(holder.mContainer) + 0.5f);
        final int offsetY = holder.mContainer.getTop() + (int) (ViewCompat.getTranslationY(holder.mContainer) + 0.5f);

        return UiUtils.hitTest(holder.mDragHandle, x - offsetX, y - offsetY);
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(TaskViewHolder holder, int position) {
        // no drag-sortable range specified
        return null;
    }

    @Override
    public SwipeResultAction onSwipeItem(TaskViewHolder holder, final int position, int result) {
        switch (result) {
            // swipe
            case SwipeableItemConstants.RESULT_SWIPED_LEFT:
            case SwipeableItemConstants.RESULT_SWIPED_RIGHT:
                return new SwipeResultAction(this, position);
            // other --- do nothing
            default:
                return null;
        }
    }

    public EventListener getEventListener() {
        return mEventListener;
    }

    public void setEventListener(EventListener eventListener) {
        mEventListener = eventListener;
    }

    private static class SwipeResultAction extends SwipeResultActionRemoveItem {
        private TaskAdapter mAdapter;
        private final int mPosition;

        SwipeResultAction(TaskAdapter adapter, int position) {
            mAdapter = adapter;
            mPosition = position;
        }

        @Override
        protected void onPerformAction() {
            super.onPerformAction();
        }

        @Override
        protected void onSlideAnimationEnd() {
            super.onSlideAnimationEnd();
            mAdapter.notifyItemRemoved(mPosition);

            if (mAdapter.mEventListener != null) {
                mAdapter.mEventListener.onItemRemoved(mPosition);
            }
        }

        @Override
        protected void onCleanUp() {
            super.onCleanUp();
            // clear the references
            mAdapter = null;
        }
    }
}
