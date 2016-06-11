package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.logic.BattleManager;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.UiUtils;
import net.fred.taskgame.hero.views.GameCardView;

import org.parceler.Parcels;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class BattleFragment extends BaseFragment {

    public static final String STATE_BATTLE_MANAGER = "STATE_BATTLE_MANAGER";

    public static final String ARG_PLAYER_CARDS = "ARG_PLAYER_CARDS";
    public static final String ARG_LEVEL = "ARG_LEVEL";

    @BindView(R.id.player_card)
    GameCardView mPlayerCardView;

    @BindView(R.id.enemy_card)
    GameCardView mEnemyCardView;

    @BindView(R.id.play_button)
    Button mPlayButton;

    @BindView(R.id.card_list_scroll_view)
    View mCardListScrollView;

    @BindView(R.id.card_list)
    LinearLayout mCardListLayout;

    private BattleManager mBattleManager = new BattleManager();
    private Level mLevel;
    private BottomSheetBehavior mBottomSheetBehavior;

    public static BattleFragment newInstance(Level level, List<Card> playerCards) {
        BattleFragment fragment = new BattleFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LEVEL, Parcels.wrap(level));
        args.putParcelable(ARG_PLAYER_CARDS, Parcels.wrap(playerCards));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_battle, container, false);
        ButterKnife.bind(this, layout);

        mLevel = Parcels.unwrap(getArguments().getParcelable(ARG_LEVEL));

        if (savedInstanceState != null) {
            mBattleManager = Parcels.unwrap(savedInstanceState.getParcelable(STATE_BATTLE_MANAGER));
        } else {
            mBattleManager.addEnemyCards(mLevel.enemyCards);
            List<Card> playerCards = Parcels.unwrap(getArguments().getParcelable(ARG_PLAYER_CARDS));
            mBattleManager.addPlayerCards(playerCards);
        }

        mBottomSheetBehavior = BottomSheetBehavior.from(mCardListScrollView);
        mBottomSheetBehavior.setPeekHeight(300);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        updateUI();

        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BATTLE_MANAGER, Parcels.wrap(mBattleManager));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected int getMainMusicResId() {
        return R.raw.battle_theme;
    }

    @OnClick(R.id.play_button)
    public void onPlayClicked() {
        mBattleManager.play();

        updateUI();
    }

    private void updateUI() {
        boolean isPlayerCreatureStillAlive = mBattleManager.isPlayerCreatureStillAlive();
        mPlayButton.setVisibility(isPlayerCreatureStillAlive ? View.VISIBLE : View.GONE);
        mPlayerCardView.setCard(mBattleManager.getLastUsedPlayerCreatureCard());
        mEnemyCardView.setCard(mBattleManager.getCurrentOrNextAliveEnemyCreatureCard());

        mCardListLayout.removeAllViews();
        for (final Card card : isPlayerCreatureStillAlive ? mBattleManager.getRemainingPlayerSupportCards() : mBattleManager.getRemainingPlayerCharacterCards()) {
            final GameCardView cardView = new GameCardView(getContext());
            cardView.setCard(card);
            cardView.setPadding(10, 10, 10, 10);
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCardListLayout.removeView(cardView);
                    mBattleManager.play(card);
                    updateUI();
                }
            });
            mCardListLayout.addView(cardView);
        }

        if (mBattleManager.getBattleStatus() != BattleManager.BattleStatus.NOT_FINISHED) {
            if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.DRAW) {
                UiUtils.showMessage(getActivity(), "Draw!");
            } else if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.ENEMY_WON) {
                UiUtils.showMessage(getActivity(), "Enemy won!");
            } else if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.PLAYER_WON) {
                UiUtils.showMessage(getActivity(), "Player won!");
                mLevel.isCompleted = true;
                mLevel.save();
            }
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
