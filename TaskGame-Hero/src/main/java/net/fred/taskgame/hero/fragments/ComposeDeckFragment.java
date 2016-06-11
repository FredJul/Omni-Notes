package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.models.adapters.ComposeDeckAdapter;
import net.fred.taskgame.hero.models.adapters.RecyclerViewItemListener;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ComposeDeckFragment extends BaseFragment {

    @BindView(R.id.deck_slots)
    TextView mDeckSlots;

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private List<Card> mObtainedCardList;
    private ComposeDeckAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_compose_deck, container, false);
        ButterKnife.bind(this, layout);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mObtainedCardList = Card.getObtainedCardList();
        mAdapter = new ComposeDeckAdapter(mObtainedCardList, new RecyclerViewItemListener() {
            @Override
            public void onItemClicked(int position) {
                Card card = mObtainedCardList.get(position);
                if (card.isInDeck || (!card.isInDeck && getUsedSlots() + card.neededSlots <= Level.getCorrespondingDeckSlots())) {
                    card.isInDeck = !card.isInDeck;
                    card.save();
                    mAdapter.notifyItemChanged(position);

                    updateUI();
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        updateUI();

        return layout;
    }

    private void updateUI() {
        int freeSlots = Level.getCorrespondingDeckSlots();
        int usedSlots = getUsedSlots();
        mDeckSlots.setText("Free slots: " + (freeSlots - usedSlots) + "/" + freeSlots);
    }

    private int getUsedSlots() {
        int usedSlots = 0;
        for (Card card : mObtainedCardList) {
            if (card.isInDeck) {
                usedSlots += card.neededSlots;
            }
        }
        return usedSlots;
    }

    @Override
    protected int getMainMusicResId() {
        return R.raw.main_theme;
    }
}
