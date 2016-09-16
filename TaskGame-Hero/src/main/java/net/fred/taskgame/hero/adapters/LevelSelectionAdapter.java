package net.fred.taskgame.hero.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import net.fred.taskgame.hero.MainApplication;
import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.UiUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LevelSelectionAdapter extends RecyclerView.Adapter<LevelSelectionAdapter.LevelViewHolder> {

    private final List<Level> mLevels;
    private final RecyclerViewItemListener mItemListener;
    private static final int LEVEL_NUMBER_PADDING = -UiUtils.dpToPixel(5);
    private static final int BOSS_ICON_PADDING = -UiUtils.dpToPixel(17);

    public static class LevelViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.level_number)
        Button mLevelNumber;
        @BindView(R.id.lock_icon)
        ImageView mLockIcon;
        @BindView(R.id.boss_icon)
        ImageView mBossIcon;

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
        final View v = inflater.inflate(R.layout.item_level, parent, false);
        return new LevelViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final LevelViewHolder holder, int position) {
        Level level = mLevels.get(position);

        holder.mLockIcon.setVisibility(View.GONE);
        if (level.isBossLevel) {
            holder.mLevelNumber.setText("");
            holder.mBossIcon.setVisibility(View.VISIBLE);
            holder.mBossIcon.setImageResource(level.getEnemyIcon(MainApplication.getContext()));
        } else {
            holder.mBossIcon.setVisibility(View.GONE);
            holder.mLevelNumber.setText(String.valueOf(level.levelNumber));
        }
        holder.mLevelNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mItemListener.onItemClicked(holder.getAdapterPosition());
            }
        });

        if (level.isCompleted) {
            holder.mLevelNumber.setSelected(false);
            holder.mLevelNumber.setEnabled(true);
        } else if (position == 0 || mLevels.get(position - 1).isCompleted) {
            holder.mLevelNumber.setSelected(true);
            holder.mLevelNumber.setEnabled(true);
        } else {
            holder.mLevelNumber.setSelected(true);
            holder.mLevelNumber.setEnabled(false);
            holder.mLevelNumber.setText("");
            if (!level.isBossLevel) {
                holder.mLockIcon.setVisibility(View.VISIBLE);
            } else {
                holder.mLockIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mLevels.size();
    }
}
