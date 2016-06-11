package net.fred.taskgame.hero.activities;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.RawRes;
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

public class MainActivity extends BaseGameActivity {

    private FragmentManager mFragmentManager;

    private MediaPlayer mMediaPlayer;

    @RawRes
    private int mCurrentMusicResId;

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

    @Override
    protected void onStop() {
        super.onStop();

        stopMusic();
    }

    public void startBattle(Level level) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, BattleFragment.newInstance(level, Card.getDeckCardList()), BattleFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    public void buyCards() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        transaction.replace(R.id.fragment_container, new BuyCardsFragment(), BuyCardsFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
    }

    public void composeDeck() {
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

    public void stopMusic() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public void playMusic(@RawRes int soundResId) {
        if (soundResId == 0 || (mMediaPlayer != null && mMediaPlayer.isPlaying() && mCurrentMusicResId == soundResId)) {
            return;
        }

        mCurrentMusicResId = soundResId;
        stopMusic();

        mMediaPlayer = MediaPlayer.create(this, soundResId);
        mMediaPlayer.setLooping(true);

        mMediaPlayer.start();
    }
}
