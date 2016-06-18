package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.activities.MainActivity;
import net.fred.taskgame.hero.logic.BattleManager;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.Dog;
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

    @BindView(R.id.enemy_portrait)
    ImageView mEnemyImageView;

    @BindView(R.id.enemy_card)
    GameCardView mEnemyCardView;

    @BindView(R.id.card_list_scroll_view)
    View mCardListScrollView;

    @BindView(R.id.card_list)
    LinearLayout mCardListLayout;

    @BindView(R.id.dark_layer)
    View mDarkLayer;

    @BindView(R.id.use_card_button)
    Button mUseCardButton;

    @BindView(R.id.player_card)
    GameCardView mPlayerCardView;

    private BattleManager mBattleManager = new BattleManager();
    private Level mLevel;
    private BottomSheetBehavior mBottomSheetBehavior;
    private GameCardView mCurrentlyAnimatedCard;

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

        mEnemyImageView.setImageResource(mLevel.enemyIconResId);

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
        if (mLevel != null && mLevel.specialMusicResId != Level.INVALID_ID) {
            return mLevel.specialMusicResId;
        }

        return R.raw.battle_theme;
    }

    @OnClick(R.id.use_card_button)
    public void onUseCardClicked() {
        stopCardHighlighting();

        int cardPadding = mCurrentlyAnimatedCard.getPaddingTop();
        mCurrentlyAnimatedCard.animate().scaleX(1).scaleY(1).translationX(0).translationY(0)
                .x(mPlayerCardView.getX() - mCardListScrollView.getX() - cardPadding).y(mPlayerCardView.getY() - mCardListScrollView.getY() - cardPadding)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        final Card playedCard = mCurrentlyAnimatedCard.getCard();
                        if (playedCard.type == Card.Type.CREATURE) {
                            mPlayerCardView.setAlpha(1); // it has maybe been hidden if a creature died
                            mPlayerCardView.setCard(mCurrentlyAnimatedCard.getCard());
                            mCardListLayout.post(new Runnable() { // to avoid flickering
                                @Override
                                public void run() {
                                    mCardListLayout.removeView(mCurrentlyAnimatedCard);
                                    mCurrentlyAnimatedCard = null;
                                    animateBattle(playedCard);
                                }
                            });
                        } else {
                            getMainActivity().playSound(MainActivity.SOUND_USE_SUPPORT);
                            mCurrentlyAnimatedCard.animate().alpha(0).withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    mCardListLayout.removeView(mCurrentlyAnimatedCard);
                                    mCurrentlyAnimatedCard = null;
                                    animateBattle(playedCard);
                                }
                            });
                        }
                    }
                });
    }

    private void animateBattle() {
        animateBattle(null);
    }

    private void animateBattle(final Card card) {
        final float cardsXDiff = (mPlayerCardView.getX() - mEnemyCardView.getX() - mPlayerCardView.getWidth()) / 2;
        final float cardsYDiff = (mPlayerCardView.getY() - mEnemyCardView.getY() - mPlayerCardView.getHeight()) / 2;

        getMainActivity().playSound(MainActivity.SOUND_FIGHT);
        mPlayerCardView.animate()
                .translationX(-cardsXDiff).translationY(-cardsYDiff)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mPlayerCardView.animate()
                                .translationX(0).translationY(0)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (card != null) {
                                            mBattleManager.play(card);
                                        } else {
                                            mBattleManager.play();
                                        }

                                        boolean hasAnimationInProgress = false;
                                        if (!mBattleManager.isEnemyCreatureStillAlive()) {
                                            getMainActivity().playSound(MainActivity.SOUND_DEATH);
                                            hasAnimationInProgress = true;
                                            mEnemyCardView.animate().alpha(0).withEndAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Card nextEnemyCard = mBattleManager.getNextEnemyCreatureCard();
                                                    if (nextEnemyCard != null) {
                                                        mEnemyCardView.setCard(nextEnemyCard);
                                                        mEnemyCardView.animate().alpha(1).withEndAction(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                updateUI();
                                                            }
                                                        });
                                                    } else {
                                                        updateUI();
                                                    }
                                                }
                                            });
                                        }

                                        if (!mBattleManager.isPlayerCreatureStillAlive()) {
                                            getMainActivity().playSound(MainActivity.SOUND_DEATH);
                                            if (hasAnimationInProgress) {
                                                mPlayerCardView.animate().alpha(0); // If an animation already started, no need to update the UI at the end of this one
                                            } else {
                                                mPlayerCardView.animate().alpha(0).withEndAction(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        updateUI();
                                                    }
                                                });
                                            }
                                            hasAnimationInProgress = true;
                                        }

                                        if (!hasAnimationInProgress) {
                                            updateUI();
                                        }
                                    }
                                });
                    }
                });

        mEnemyCardView.animate()
                .translationX(cardsXDiff).translationY(cardsYDiff)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        mEnemyCardView.animate()
                                .translationX(0).translationY(0);
                    }
                });
    }

    @OnClick(R.id.dark_layer)
    public void onDarkLayerClicked() {
        stopCardHighlighting();

        mCurrentlyAnimatedCard.animate().scaleX(1).scaleY(1).translationX(0).translationY(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                updateBottomSheetUI();
            }
        });
        mCurrentlyAnimatedCard = null;
    }

    private void highlightCard(GameCardView cardView) {
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        mCurrentlyAnimatedCard = cardView;
        cardView.animate()
                .scaleX(1.7f)
                .scaleY(1.7f)
                .translationX(cardView.getWidth() / 2f - cardView.getX())
                .translationY(-(mCardListLayout.getHeight() * 1.35f));
        mDarkLayer.setVisibility(View.VISIBLE);
        mDarkLayer.animate().alpha(1);
    }

    private void stopCardHighlighting() {
        mDarkLayer.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                mDarkLayer.setVisibility(View.GONE);
            }
        });
    }

    private void updateUI() {
        boolean isPlayerCreatureStillAlive = mBattleManager.isPlayerCreatureStillAlive();
        mPlayerCardView.setCard(mBattleManager.getLastUsedPlayerCreatureCard());
        mEnemyCardView.setCard(mBattleManager.getCurrentOrNextAliveEnemyCreatureCard());

        // The "no support" button view needs to be removed before card population
        if (mCardListLayout.getChildCount() > 0 && mCardListLayout.getChildAt(mCardListLayout.getChildCount() - 1) instanceof Button) {
            mCardListLayout.removeViewAt(mCardListLayout.getChildCount() - 1);
        }

        // Create or remove only rhe necessary number of views and reuse the existing ones
        List<Card> newCards = isPlayerCreatureStillAlive ? mBattleManager.getRemainingPlayerSupportCards() : mBattleManager.getRemainingPlayerCharacterCards();
        int diff = newCards.size() - mCardListLayout.getChildCount();
        if (diff > 0) { // We need more cards
            for (int i = 0; i < diff; i++) {
                final GameCardView cardView = new GameCardView(getContext());
                cardView.setPadding(10, 10, 10, 10);
                cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        highlightCard(cardView);
                    }
                });
                mCardListLayout.addView(cardView);
            }
        } else if (diff < 0) { // We need to remove some cards
            mCardListLayout.removeViews(newCards.size(), -diff);
        }

        // Set the new cards to the views
        for (int i = 0; i < newCards.size(); i++) {
            Card card = newCards.get(i);
            Dog.d("Card added to game: " + card.name);
            ((GameCardView) mCardListLayout.getChildAt(i)).setCard(card);
        }

        // Add the "no support" button if necessary
        if (isPlayerCreatureStillAlive && newCards.size() > 0) {
            Button playWithoutSupportBtn = new Button(getContext());
            playWithoutSupportBtn.setText("Don't use support magic");
            playWithoutSupportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    animateBattle();
                }
            });
            mCardListLayout.addView(playWithoutSupportBtn);
        }

        updateBottomSheetUI();

        if (mBattleManager.getBattleStatus() != BattleManager.BattleStatus.NOT_FINISHED) {
            if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.DRAW) {
                getMainActivity().playSound(MainActivity.SOUND_DEFEAT);
                UiUtils.showMessage(getActivity(), "Draw!");
            } else if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.ENEMY_WON) {
                getMainActivity().playSound(MainActivity.SOUND_DEFEAT);
                UiUtils.showMessage(getActivity(), "Enemy won!");
            } else if (mBattleManager.getBattleStatus() == BattleManager.BattleStatus.PLAYER_WON) {
                getMainActivity().playSound(MainActivity.SOUND_VICTORY);
                UiUtils.showMessage(getActivity(), "Player won!");
                mLevel.isCompleted = true;
                mLevel.save();
            }
            getActivity().getSupportFragmentManager().popBackStack();

        } else if (isPlayerCreatureStillAlive && newCards.size() <= 0) {
            // The battle is not finished, but the user can only fight without using support, let's automatically start that
            animateBattle();
        }
    }

    private void updateBottomSheetUI() {
        if (mCardListLayout.getChildCount() > 0) {
            if (mBottomSheetBehavior.isHideable()) {
                mBottomSheetBehavior.setHideable(false);
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        } else {
            if (!mBottomSheetBehavior.isHideable()) {
                mBottomSheetBehavior.setHideable(true);
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }
    }
}
