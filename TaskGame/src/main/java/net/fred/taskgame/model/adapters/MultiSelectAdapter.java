package net.fred.taskgame.model.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;

public abstract class MultiSelectAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private final SparseBooleanArray mSelectedItems = new SparseBooleanArray();

    public void toggleSelection(int position) {
        if (mSelectedItems.get(position, false)) {
            mSelectedItems.delete(position);
        } else {
            mSelectedItems.put(position, true);
        }
        notifyItemChanged(position);
    }

    public boolean isItemSelected(int position) {
        return mSelectedItems.get(position);
    }

    public void selectAll() {
        for (int i = 0; i < getItemCount(); i++) {
            mSelectedItems.put(i, true);
        }
        notifyDataSetChanged();
    }

    public void clearSelections() {
        mSelectedItems.clear();
        notifyDataSetChanged();
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
}
