package net.fred.taskgame.adapters;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fred.taskgame.R;
import net.fred.taskgame.models.Task;
import net.fred.taskgame.utils.recycler.ItemActionListener;
import net.fred.taskgame.utils.recycler.ItemActionViewHolder;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TaskAdapter extends MultiSelectAdapter<TaskAdapter.TaskViewHolder> {

    private List<Task> mTasks;
    private final ItemActionListener mItemActionListener;

    public static class TaskViewHolder extends RecyclerView.ViewHolder implements ItemActionViewHolder {

        @BindView(R.id.root)
        View mRoot;
        @BindView(R.id.category_marker)
        View mCategoryMarker;
        @BindView(R.id.task_title)
        TextView mTitle;
        @BindView(R.id.task_content)
        TextView mContent;
        @BindView(R.id.reward_points)
        TextView mReward;

        public TaskViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }

        @Override
        public void onItemSelected() {
            // Highlighted if is part of multi selection of tasks. Remember to search for child with card ui
            mRoot.setBackgroundResource(R.color.gray_bg);
            mRoot.setElevation(1);
        }

        @Override
        public void onItemClear() {
            mRoot.setBackgroundColor(Color.WHITE);
            mRoot.setElevation(0);
        }
    }

    public TaskAdapter(ItemActionListener listener, RecyclerView recyclerView, List<Task> tasks) {
        super(listener, recyclerView);
        mItemActionListener = listener;
        mTasks = tasks;

        setHasStableIds(true);
        recyclerView.setAdapter(this);
    }

    public List<Task> getTasks() {
        return mTasks;
    }

    public void setTasks(List<Task> tasks) {
        mTasks = tasks;
        notifyDataSetChanged();
    }

    @Override
    public long getItemId(int position) {
        return mTasks.get(position).id.hashCode();
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
        super.onBindViewHolder(holder, position);

        Task task = mTasks.get(position);

        // Init texts
        String[] titleAndContent = task.computeListItemTitleAndContent();
        holder.mTitle.setText(titleAndContent[0]);
        if (titleAndContent[1].length() > 0) {
            holder.mContent.setText(titleAndContent[1]);
            holder.mContent.setVisibility(View.VISIBLE);
        } else {
            holder.mContent.setVisibility(View.GONE);
        }
        holder.mReward.setText(String.valueOf(task.pointReward));

        // If category is set the color will be applied on the appropriate target
        if (task.getCategory() != null) {
            holder.mCategoryMarker.setBackgroundColor(task.getCategory().color);
        } else {
            holder.mCategoryMarker.setBackgroundColor(Color.TRANSPARENT);
        }

        // set listeners
        holder.itemView.setOnClickListener(new View.OnClickListener() {
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

        // This is a small hack to avoid clicking on the item just after a D&D long press...
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mTasks != null ? mTasks.size() : 0;
    }
}
