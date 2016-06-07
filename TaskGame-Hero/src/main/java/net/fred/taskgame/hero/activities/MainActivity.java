package net.fred.taskgame.hero.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.fragments.BattleFragment;
import net.fred.taskgame.hero.fragments.BuyCardsFragment;
import net.fred.taskgame.hero.fragments.ComposeDeckFragment;
import net.fred.taskgame.hero.fragments.LevelSelectionFragment;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.UiUtils;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends BaseGameActivity {

    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mFragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_container, new LevelSelectionFragment(), LevelSelectionFragment.class.getName()).commit();
        }
    }

    public void startBattle(Level level) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, BattleFragment.newInstance(level, Card.getDeckCardList()), BattleFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    @OnClick(R.id.buy_cards)
    public void onViewMyCardsButtonClicked() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, new BuyCardsFragment(), BuyCardsFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    @OnClick(R.id.compose_deck)
    public void onComposeDeckButtonClicked() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, new ComposeDeckFragment(), ComposeDeckFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {

    }
}
