package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

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

    public static final String ARG_PLAYER_CARDS = "ARG_PLAYER_CARDS";
    public static final String ARG_LEVEL = "ARG_LEVEL";

    private static final String STATE_BATTLE_MANAGER = "STATE_BATTLE_MANAGER";
    private static final int MAX_NO_SUPPORT_CARD_BTN_WIDTH = UiUtils.dpToPixel(190);
    private static final int NO_SUPPORT_CARD_BTN_MARGIN = UiUtils.dpToPixel(10);

    @BindView(R.id.enemy_portrait)
    ImageView mEnemyImageView;

    @BindView(R.id.enemy_card)
    GameCardView mEnemyCardView;

    @BindView(R.id.enemy_support_card)
    GameCardView mEnemySupportCardView;

    @BindView(R.id.select_card_bottom_sheet)
    View mSelectCardBottomSheet;

    @BindView(R.id.select_strategy_bottom_sheet)
    View mSelectStrategyBottomSheet;

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
    private BottomSheetBehavior mSelectCardBottomSheetBehavior;
    private BottomSheetBehavior mSelectStrategyBottomSheetBehavior;
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

        mSelectCardBottomSheetBehavior = BottomSheetBehavior.from(mSelectCardBottomSheet);
        mSelectCardBottomSheetBehavior.setPeekHeight(UiUtils.dpToPixel(100));

        mSelectStrategyBottomSheetBehavior = BottomSheetBehavior.from(mSelectStrategyBottomSheet);
        mSelectStrategyBottomSheetBehavior.setPeekHeight(UiUtils.dpToPixel(100));

        mEnemyImageView.setImageResource(mLevel.getEnemyIcon(getContext()));

        updateUI();

        return layout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(STATE_BATTLE_MANAGER, Parcels.wrap(mBattleManager));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected
    @RawRes
    int getMainMusicResId() {
        if (mLevel != null && mLevel.battleMusicResId != Level.INVALID_ID) {
            return mLevel.battleMusicResId;
        }

        return R.raw.battle_theme;
    }

    @OnClick(R.id.use_card_button)
    public void onUseCardClicked() {
        stopCardHighlighting();

        final Card playedCard = mCurrentlyAnimatedCard.getCard();
        if (playedCard.type == Card.Type.CREATURE) {
            int cardPaddingStart = mCurrentlyAnimatedCard.getPaddingStart() - mSelectCardBottomSheet.getScrollX();
            int cardPaddingTop = mCurrentlyAnimatedCard.getPaddingTop() + mSelectCardBottomSheet.getPaddingTop();
            mCurrentlyAnimatedCard.animate().scaleX(1).scaleY(1).translationX(0).translationY(0)
                    .x(mPlayerCardView.getX() - mSelectCardBottomSheet.getX() - cardPaddingStart).y(mPlayerCardView.getY() - mSelectCardBottomSheet.getY() - cardPaddingTop)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mPlayerCardView.setAlpha(1); // it has maybe been hidden if a creature died
                            mPlayerCardView.setCard(mCurrentlyAnimatedCard.getCard());
                            mCardListLayout.post(new Runnable() { // to avoid flickering
                                @Override
                                public void run() {
                                    mCardListLayout.removeView(mCurrentlyAnimatedCard);
                                    mCurrentlyAnimatedCard = null;
                                    mBattleManager.play(playedCard);
                                    animateNextStep();
                                }
                            });
                        }
                    });
        } else {
            mCurrentlyAnimatedCard.animate().alpha(0)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            getMainActivity().playSound(MainActivity.SOUND_USE_SUPPORT);
                            mCurrentlyAnimatedCard.animate().alpha(0).withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    mCardListLayout.removeView(mCurrentlyAnimatedCard);
                                    mCurrentlyAnimatedCard = null;
                                    mBattleManager.play(playedCard);
                                    animateNextStep();
                                }
                            });
                        }
                    });

        }
    }

    @OnClick(R.id.attack_strategy_btn)
    public void onAttackStrategyButtonClicked() {
        mBattleManager.applyNewStrategy(BattleManager.BattleStrategy.ATTACK);
        onStrategySelected();
    }

    @OnClick(R.id.defense_strategy_btn)
    public void onDefenseStrategyButtonClicked() {
        mBattleManager.applyNewStrategy(BattleManager.BattleStrategy.DEFENSE);
        onStrategySelected();
    }

    @OnClick(R.id.aleatory_strategy_btn)
    public void onAleatoryStrategyButtonClicked() {
        BattleManager.AleatoryResult result = mBattleManager.applyNewStrategy(BattleManager.BattleStrategy.ALEATORY);
        String displayResult = (result.bonusOrPenalty > 0 ? "+" : "") + result.bonusOrPenalty + " of " + (result.affectedField == BattleManager.AleatoryAffectedField.ATTACK ? "attack" : "defense");
        Toast.makeText(getContext(), displayResult, Toast.LENGTH_SHORT).show();
        onStrategySelected();
    }

    private void animateNextStep() {
        if (getMainActivity() == null) {
            return;  // Do nothing if not correctly attached
        }

        BattleManager.BattleStep step = mBattleManager.executeNextStep();
        switch (step) {
            case APPLY_PLAYER_SUPPORT: {
                boolean animationInProgress = mEnemyCardView.animateValueChange(new Runnable() {
                    @Override
                    public void run() {
                        animateNextStep();
                    }
                });

                if (animationInProgress) {
                    mPlayerCardView.animateValueChange(null);
                } else {
                    if (!mPlayerCardView.animateValueChange(new Runnable() {
                        @Override
                        public void run() {
                            animateNextStep();
                        }
                    })) {
                        animateNextStep(); // if no animation at all, let's do the next step already now
                    }
                }
                break;
            }
            case SELECT_STRATEGY: {
                stopCardHighlighting();
                updateBottomSheetUI();
                break;
            }
            case APPLY_ENEMY_SUPPORT: {
                mEnemySupportCardView.setCard(mBattleManager.getLastUsedEnemySupportCard());
                mEnemySupportCardView.setAlpha(0);
                mEnemySupportCardView.setVisibility(View.VISIBLE);
                mEnemySupportCardView.animate().alpha(1).scaleX(1.2f).scaleY(1.2f).setDuration(500).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        getMainActivity().playSound(MainActivity.SOUND_USE_SUPPORT);
                        mEnemySupportCardView.animate().alpha(0).scaleX(1).scaleY(1).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                mEnemySupportCardView.setCard(null);
                                mEnemySupportCardView.setVisibility(View.GONE);

                                boolean animationInProgress = mEnemyCardView.animateValueChange(new Runnable() {
                                    @Override
                                    public void run() {
                                        animateNextStep();
                                    }
                                });

                                if (animationInProgress) {
                                    mPlayerCardView.animateValueChange(null);
                                } else {
                                    if (!mPlayerCardView.animateValueChange(new Runnable() {
                                        @Override
                                        public void run() {
                                            animateNextStep();
                                        }
                                    })) {
                                        animateNextStep(); // If no animation at all, we do it immediately
                                    }
                                }
                            }
                        });
                    }
                });
                break;
            }
            case FIGHT: {
                float cardsXDiff = (mPlayerCardView.getX() - mEnemyCardView.getX() - mPlayerCardView.getWidth()) / 2;
                cardsXDiff = cardsXDiff + mPlayerCardView.getWidth() / 6; // add a small superposition
                float cardsYDiff = (mPlayerCardView.getY() - mEnemyCardView.getY()) / 2;
                cardsYDiff = cardsYDiff - mPlayerCardView.getHeight() / 8; // add a small superposition

                if (mPlayerCardView.getCard().useMagic) {
                    getMainActivity().playSound(MainActivity.SOUND_FIGHT_MAGIC);
                } else if (mPlayerCardView.getCard().useWeapon) {
                    getMainActivity().playSound(MainActivity.SOUND_FIGHT_WEAPON);
                } else {
                    getMainActivity().playSound(MainActivity.SOUND_FIGHT);
                }
                mEnemyCardView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getMainActivity() != null) {
                            if (mEnemyCardView.getCard().useMagic) {
                                getMainActivity().playSound(MainActivity.SOUND_FIGHT_MAGIC);
                            } else if (mEnemyCardView.getCard().useWeapon) {
                                getMainActivity().playSound(MainActivity.SOUND_FIGHT_WEAPON);
                            } else {
                                getMainActivity().playSound(MainActivity.SOUND_FIGHT);
                            }
                        }
                    }
                }, 100);

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
                                                animateNextStep();
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
                break;
            }
            case APPLY_DAMAGES: {
                mEnemyCardView.animateValueChange(null);
                mPlayerCardView.animateValueChange(null);
                animateNextStep();
                break;
            }
            case PLAYER_DEATH: {
                getMainActivity().playSound(MainActivity.SOUND_DEATH);
                mPlayerCardView.animate().alpha(0).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        animateNextStep();
                    }
                });
                break;
            }
            case ENEMY_DEATH: {
                getMainActivity().playSound(MainActivity.SOUND_DEATH);
                mEnemyCardView.animate().alpha(0).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        Card nextEnemyCard = mBattleManager.getNextEnemyCreatureCard();
                        if (nextEnemyCard != null) {
                            mEnemyCardView.setCard(nextEnemyCard);
                            mEnemyCardView.animate().alpha(1).withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    animateNextStep();
                                }
                            });
                        } else {
                            animateNextStep();
                        }
                    }
                });
                break;
            }
            case END_TURN: {
                boolean isPlayerCreatureStillAlive = mBattleManager.isPlayerCreatureStillAlive();
                if (isPlayerCreatureStillAlive) {
                    List<Card> newCards = isPlayerCreatureStillAlive ? mBattleManager.getRemainingPlayerSupportCards() : mBattleManager.getRemainingPlayerCharacterCards();
                    if (newCards.size() <= 0) {
                        // The battle is not finished, but the user can only fight without using support, let's automatically start that
                        mBattleManager.play();
                        animateNextStep();
                    } else {
                        updateUI();
                    }
                } else {
                    updateUI();
                }
                break;
            }
            case PLAYER_WON: {
                if (mBattleManager.getLastUsedPlayerCreatureCard().defense <= 0) {
                    mPlayerCardView.setCard(mBattleManager.getNextPlayerCreatureCard());
                    mPlayerCardView.animate().alpha(1).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            displayVictory();
                        }
                    });
                } else {
                    displayVictory();
                }

                break;
            }
            case ENEMY_WON: {
                getMainActivity().playSound(MainActivity.SOUND_DEFEAT);

                DialogFragment dialog = EndBattleDialogFragment.newInstance(mLevel, mLevel.isCompleted, EndBattleDialogFragment.EndType.ENEMY_WON);
                FragmentTransaction transaction = getFragmentManager().beginTransaction().addToBackStack(null);
                dialog.show(transaction, EndBattleDialogFragment.class.getName());
                break;
            }
            case DRAW: {
                getMainActivity().playSound(MainActivity.SOUND_DEFEAT);

                DialogFragment dialog = EndBattleDialogFragment.newInstance(mLevel, mLevel.isCompleted, EndBattleDialogFragment.EndType.DRAW);
                FragmentTransaction transaction = getFragmentManager().beginTransaction().addToBackStack(null);
                dialog.show(transaction, EndBattleDialogFragment.class.getName());
                break;
            }
        }
    }

    private void displayVictory() {
        if (getMainActivity() != null && isVisible()) {
            getMainActivity().playSound(MainActivity.SOUND_VICTORY);

            DialogFragment dialog = EndBattleDialogFragment.newInstance(mLevel, mLevel.isCompleted, EndBattleDialogFragment.EndType.PLAYER_WON);
            FragmentTransaction transaction = getFragmentManager().beginTransaction().addToBackStack(null);
            dialog.show(transaction, EndBattleDialogFragment.class.getName());
        }
        mLevel.isCompleted = true;
        mLevel.save();
    }

    @OnClick(R.id.back)
    public void onBackButtonClicked() {
        getFragmentManager().popBackStack();
    }

    @OnClick(R.id.dark_layer)
    public void onDarkLayerClicked() {
        if (mCurrentlyAnimatedCard == null) {
            return; // We already clicked on it once and are in the middle of the animation
        }

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
        if (mCurrentlyAnimatedCard != null) {
            return; // We already clicked on it once and are in the middle of the animation
        }

        mSelectCardBottomSheetBehavior.setHideable(true);
        mSelectCardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        mCurrentlyAnimatedCard = cardView;
        cardView.animate()
                .scaleX(1.7f)
                .scaleY(1.7f)
                .translationX((getView().getWidth() / 2f) - (cardView.getWidth() / 2f) - cardView.getX() + mSelectCardBottomSheet.getScrollX())
                .translationY(-(mCardListLayout.getHeight() * 1.8f));

        mDarkLayer.setVisibility(View.VISIBLE);
        mDarkLayer.setClickable(true);
        mDarkLayer.animate().alpha(1);
    }

    private void stopCardHighlighting() {
        mDarkLayer.setClickable(false);
        mDarkLayer.animate().alpha(0).withEndAction(new Runnable() {
            @Override
            public void run() {
                mDarkLayer.setVisibility(View.GONE);
            }
        });
    }

    private void onStrategySelected() {
        mSelectStrategyBottomSheetBehavior.setHideable(true);
        mSelectStrategyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        if (!mPlayerCardView.animateValueChange(new Runnable() {
            @Override
            public void run() {
                animateNextStep();
            }
        })) {
            animateNextStep(); // if no animation at all, let's do the next step already now
        }
    }

    private void updateUI() {
        if (getContext() == null) {
            return; // we have been detached, we shouldn't do anything
        }

        boolean isPlayerCreatureStillAlive = mBattleManager.isPlayerCreatureStillAlive();
        if (isPlayerCreatureStillAlive) {
            mPlayerCardView.setCard(mBattleManager.getLastUsedPlayerCreatureCard());
        } else {
            mPlayerCardView.setCard(null);
        }
        mEnemyCardView.setCard(mBattleManager.getCurrentOrNextAliveEnemyCreatureCard());

        // The "no support" button view needs to be removed before card population
        if (mCardListLayout.getChildCount() > 0 && mCardListLayout.getChildAt(mCardListLayout.getChildCount() - 1) instanceof Button) {
            mCardListLayout.removeViewAt(mCardListLayout.getChildCount() - 1);
        }

        // Create or remove only the necessary number of views and reuse the existing ones
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
            playWithoutSupportBtn.setText("Don't use support card");
            playWithoutSupportBtn.setMaxWidth(MAX_NO_SUPPORT_CARD_BTN_WIDTH);
            playWithoutSupportBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectCardBottomSheetBehavior.setHideable(true);
                    mSelectCardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    mBattleManager.play();
                    animateNextStep();
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(NO_SUPPORT_CARD_BTN_MARGIN, NO_SUPPORT_CARD_BTN_MARGIN, NO_SUPPORT_CARD_BTN_MARGIN, NO_SUPPORT_CARD_BTN_MARGIN);
            mCardListLayout.addView(playWithoutSupportBtn, params);
        }

        updateBottomSheetUI();
    }

    private void updateBottomSheetUI() {
        if (mBattleManager.getCurrentStep() == BattleManager.BattleStep.SELECT_STRATEGY) {
            if (mSelectStrategyBottomSheetBehavior.isHideable()) {
                mSelectStrategyBottomSheetBehavior.setHideable(false);
                mSelectStrategyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }

            if (!mSelectCardBottomSheetBehavior.isHideable()) {
                mSelectCardBottomSheetBehavior.setHideable(true);
                mSelectCardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        } else {
            if (!mSelectStrategyBottomSheetBehavior.isHideable()) {
                mSelectStrategyBottomSheetBehavior.setHideable(true);
                mSelectStrategyBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }

            if (mCardListLayout.getChildCount() > 0) {
                if (mSelectCardBottomSheetBehavior.isHideable()) {
                    mSelectCardBottomSheetBehavior.setHideable(false);
                    mSelectCardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            } else {
                if (!mSelectCardBottomSheetBehavior.isHideable()) {
                    mSelectCardBottomSheetBehavior.setHideable(true);
                    mSelectCardBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        }
    }
}
