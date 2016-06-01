package net.fred.taskgame.hero.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.fragments.BattleFragment;
import net.fred.taskgame.hero.fragments.LevelSelectionFragment;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

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
        List<Card> playerCards = new ArrayList<>();
        playerCards.add(Card.getAllCardsMap().get(Card.CREATURE_SKELETON_ARCHER));
        playerCards.add(Card.getAllCardsMap().get(Card.CREATURE_ORC_ARCHER));
        playerCards.add(Card.getAllCardsMap().get(Card.SUPPORT_WEAPON_EROSION));
        playerCards.add(Card.getAllCardsMap().get(Card.SUPPORT_POWER_POTION));

        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, BattleFragment.newInstance(level, playerCards), BattleFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    @Override
    public void onSignInFailed() {

    }

    @Override
    public void onSignInSucceeded() {

    }
}
