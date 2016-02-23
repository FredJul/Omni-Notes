package net.fred.taskgame.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import net.fred.taskgame.utils.UiUtils;

public class EmptyRecyclerView extends RecyclerView {

    private final
    @NonNull
    AdapterDataObserver mObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            checkIfEmpty();
        }
    };

    @Nullable
    private View mEmptyView;

    public EmptyRecyclerView(Context context) {
        super(context);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mObserver);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
        }
        super.setAdapter(adapter);
        checkIfEmpty();
    }

    @Override
    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mObserver);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(mObserver);
        }
        super.swapAdapter(adapter, removeAndRecycleExistingViews);
        checkIfEmpty();
    }

    public void setEmptyView(@Nullable View mEmptyView) {
        this.mEmptyView = mEmptyView;
        checkIfEmpty();
    }

    @Override
    public int getMinFlingVelocity() {
        return UiUtils.dpToPixel(500); // Nothing related to empty view, but the default velocity is clearly TOO sensible!
    }

    private void checkIfEmpty() {
        if (mEmptyView != null && getAdapter() != null) {
            mEmptyView.setVisibility(getAdapter().getItemCount() > 0 ? GONE : VISIBLE);
        }
    }
}
