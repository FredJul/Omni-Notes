package net.fred.taskgame.hero.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.activities.MainActivity;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.adapters.BuyCardsAdapter;
import net.fred.taskgame.hero.models.adapters.RecyclerViewItemListener;
import net.fred.taskgame.hero.utils.TaskGameUtils;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;


public class BuyCardsFragment extends BaseFragment {

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;

    private List<Card> mNonObtainedCardList;
    private BuyCardsAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_buy_cards, container, false);
        ButterKnife.bind(this, layout);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(layoutManager);

        mNonObtainedCardList = Card.getNonObtainedCardList();
        mAdapter = new BuyCardsAdapter(mNonObtainedCardList, new RecyclerViewItemListener() {
            @Override
            public void onItemClicked(int position) {
                Card card = mNonObtainedCardList.get(position);
                try {
                    startActivityForResult(TaskGameUtils.getRequestPointsActivityIntent(getContext(), card.price), card.id);
                } catch (ActivityNotFoundException e) {
                    // TaskGame application is not installed
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        return layout;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            // the user accepted, let's refresh the DB and UI

            for (int i = 0; i < mNonObtainedCardList.size(); i++) {
                Card card = mNonObtainedCardList.get(i);
                if (card.id == requestCode) { // requestCode is card id
                    getMainActivity().playSound(MainActivity.SOUND_NEW_CARD);
                    card.isObtained = true;
                    card.save();
                    mNonObtainedCardList.remove(i);
                    mAdapter.notifyItemRemoved(i);
                    break;
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getMainMusicResId() {
        return R.raw.main_theme;
    }
}
