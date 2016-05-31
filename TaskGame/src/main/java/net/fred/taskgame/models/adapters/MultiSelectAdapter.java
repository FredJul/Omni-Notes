package net.fred.taskgame.models.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.View;

import net.fred.taskgame.utils.recycler.ItemActionAdapter;
import net.fred.taskgame.utils.recycler.ItemActionListener;
import net.fred.taskgame.utils.recycler.ItemActionViewHolder;

public abstract class MultiSelectAdapter<VH extends RecyclerView.ViewHolder & ItemActionViewHolder> extends RecyclerView.Adapter<VH> implements ItemActionAdapter {

    private final SparseBooleanArray mSelectedItems = new SparseBooleanArray();
    private final ItemActionListener mListener;
    private final RecyclerView mRecyclerView;

    public MultiSelectAdapter(ItemActionListener listener, RecyclerView recyclerView) {
        mListener = listener;
        mRecyclerView = recyclerView;
    }

    public boolean isItemSelected(int position) {
        return mSelectedItems.get(position);
    }

    public void selectAll() {
        for (int i = 0; i < getItemCount(); i++) {
            if (!isItemSelected(i)) {
                updateView(mRecyclerView, i, false);
                mSelectedItems.put(i, true);
            }
        }
    }

    public void clearSelections() {
        for (int i = 0; i < getItemCount(); i++) {
            updateView(mRecyclerView, i, true);
        }
        mSelectedItems.clear();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public int[] getSelectedItems() {
        int[] itemsPos = new int[mSelectedItems.size()];
        for (int i = 0; i < mSelectedItems.size(); i++) {
            itemsPos[i] = mSelectedItems.keyAt(i);
        }
        return itemsPos;
    }

    @Override
    public void onBindViewHolder(final VH holder, int position) {
        if (isItemSelected(position)) {
            holder.onItemSelected();
        } else {
            holder.onItemClear();
        }
    }

    protected void toggleSelection(boolean select, int position) {
        updateView(mRecyclerView, position, !select);

        if (!select) {
            mSelectedItems.delete(position);
        } else {
            mSelectedItems.put(position, true);
        }
    }

    private void updateView(RecyclerView recyclerView, int position, boolean isCurrentlySelected) {
        View child = mRecyclerView.getLayoutManager().findViewByPosition(position);
        if (child != null) {
            ItemActionViewHolder viewHolder = (ItemActionViewHolder) recyclerView.getChildViewHolder(child);
            // Let the view holder know that this item is being moved or dragged
            if (isCurrentlySelected) {
                viewHolder.onItemClear();
            } else {
                viewHolder.onItemSelected();
            }
        }
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        return mListener.onItemMove(fromPosition, toPosition);
    }

    @Override
    public void onItemMoveFinished() {
        mListener.onItemMoveFinished();
    }

    @Override
    public void onItemSwiped(int position) {
        mListener.onItemSwiped(position);
    }

    @Override
    public void onItemSelected(int position) {
        toggleSelection(true, position);
        mListener.onItemSelected(position);
    }
}
