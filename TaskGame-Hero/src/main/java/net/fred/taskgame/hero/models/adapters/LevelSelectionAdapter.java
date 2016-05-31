package net.fred.taskgame.hero.models.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.models.Level;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LevelSelectionAdapter extends RecyclerView.Adapter<LevelSelectionAdapter.LevelViewHolder> {

    private final List<Level> mLevels;
    private final RecyclerViewItemListener mItemListener;

    public static class LevelViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.level_number)
        TextView mLevelNumber;

        public LevelViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    public LevelSelectionAdapter(List<Level> levels, RecyclerViewItemListener listener) {
        mLevels = levels;
        mItemListener = listener;

        setHasStableIds(true);
    }

    public List<Level> getLevels() {
        return mLevels;
    }

    @Override
    public long getItemId(int position) {
        return mLevels.get(position).levelNumber;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public LevelViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        final View v = inflater.inflate(R.layout.level_item, parent, false);
        return new LevelViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final LevelViewHolder holder, final int position) {
        Level level = mLevels.get(position);

        holder.mLevelNumber.setText(String.valueOf(level.levelNumber));
        holder.mLevelNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListener.onItemClicked(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mLevels.size();
    }
}
