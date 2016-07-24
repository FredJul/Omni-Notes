package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.adapters.LevelSelectionAdapter;
import net.fred.taskgame.hero.adapters.RecyclerViewItemListener;
import net.fred.taskgame.hero.models.Level;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class LevelSelectionFragment extends BaseFragment {

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_level_selection, container, false);
        ButterKnife.bind(this, layout);

        LinearLayoutManager layoutManager = new GridLayoutManager(getContext(), 4);
        mRecyclerView.setLayoutManager(layoutManager);


        LevelSelectionAdapter adapter = new LevelSelectionAdapter(Level.getAllLevelsList(), new RecyclerViewItemListener() {
            @Override
            public void onItemClicked(int position) {
                getMainActivity().startBattle(Level.getAllLevelsList().get(position));
            }
        });
        mRecyclerView.setAdapter(adapter);

        return layout;
    }

    @Override
    protected
    @RawRes
    int getMainMusicResId() {
        return R.raw.main_theme;
    }

    @OnClick(R.id.buy_cards)
    public void onBuyCardsButtonClicked() {
        getMainActivity().buyCards();
    }

    @OnClick(R.id.compose_deck)
    public void onComposeDeckButtonClicked() {
        getMainActivity().composeDeck();
    }
}
