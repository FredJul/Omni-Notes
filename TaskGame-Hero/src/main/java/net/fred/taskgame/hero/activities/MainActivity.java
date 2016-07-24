package net.fred.taskgame.hero.activities;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.fragments.BattleFragment;
import net.fred.taskgame.hero.fragments.BuyCardsFragment;
import net.fred.taskgame.hero.fragments.ComposeDeckFragment;
import net.fred.taskgame.hero.fragments.LevelSelectionFragment;
import net.fred.taskgame.hero.fragments.StoryFragment;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.TaskGameUtils;
import net.fred.taskgame.hero.utils.UiUtils;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    public static int SOUND_ENTER_BATTLE;
    public static int SOUND_FIGHT;
    public static int SOUND_USE_SUPPORT;
    public static int SOUND_DEATH;
    public static int SOUND_VICTORY;
    public static int SOUND_DEFEAT;
    public static int SOUND_CHANGE_CARD;
    public static int SOUND_NEW_CARD;
    public static int SOUND_IMPOSSIBLE_ACTION;

    private FragmentManager mFragmentManager;

    private MediaPlayer mMediaPlayer;
    private SoundPool mSoundPool;
    private int mCurrentSoundStreamId;

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

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        mSoundPool = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(2).build();
        SOUND_ENTER_BATTLE = mSoundPool.load(this, R.raw.enter_battle, 1);
        SOUND_FIGHT = mSoundPool.load(this, R.raw.fight, 1);
        SOUND_USE_SUPPORT = mSoundPool.load(this, R.raw.use_support, 1);
        SOUND_DEATH = mSoundPool.load(this, R.raw.death, 1);
        SOUND_VICTORY = mSoundPool.load(this, R.raw.victory, 1);
        SOUND_DEFEAT = mSoundPool.load(this, R.raw.defeat, 1);
        SOUND_CHANGE_CARD = mSoundPool.load(this, R.raw.change_card, 1);
        SOUND_NEW_CARD = mSoundPool.load(this, R.raw.new_card, 1);
        SOUND_IMPOSSIBLE_ACTION = mSoundPool.load(this, R.raw.impossible_action, 1);

        if (!TaskGameUtils.isAppInstalled(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("TaskGame application needed")
                    .setMessage("This game is totally free and use TaskGame points to progress. Without this application installed, you cannot play to it. Do you want to install it now?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            final String appPackageName = "net.fred.taskgame";
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    finish();
                }
            }).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        stopMusic();
        stopSound();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void startBattle(Level level) {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
        if (!TextUtils.isEmpty(level.getStartStory(this))) {
            transaction.replace(R.id.fragment_container, StoryFragment.newInstance(level, false), StoryFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
        } else {
            playSound(SOUND_ENTER_BATTLE);
            transaction.replace(R.id.fragment_container, BattleFragment.newInstance(level, Card.getDeckCardList()), BattleFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
        }
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

    public void stopMusic() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mCurrentMusicResId = 0;
    }

    public void playMusic(final @RawRes int soundResId) {
        if (soundResId == 0 || (mMediaPlayer != null && mMediaPlayer.isPlaying() && mCurrentMusicResId == soundResId)) {
            return;
        }

        mCurrentMusicResId = soundResId;
        stopMusic();

        mMediaPlayer = MediaPlayer.create(this, soundResId);
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // setLooping(true) is buggy on my Nexus5X, does not really understand why... hence this workaround
                playMusic(soundResId);
            }
        });

        mMediaPlayer.start();
    }

    public void playSound(int soundId) {
        mCurrentSoundStreamId = mSoundPool.play(soundId, 0.5f, 0.5f, 1, 0, 1f);
    }

    public void stopSound() {
        if (mSoundPool != null && mCurrentSoundStreamId != 0) {
            mSoundPool.stop(mCurrentSoundStreamId);
        }
    }
}
