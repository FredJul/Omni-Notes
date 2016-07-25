package net.fred.taskgame.hero.fragments;

import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.activities.MainActivity;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;
import net.fred.taskgame.hero.utils.UiUtils;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class StoryFragment extends BaseFragment {

    public static final String ARG_LEVEL = "ARG_LEVEL";
    public static final String ARG_IS_END_STORY = "ARG_IS_END_STORY";

    private static final String STATE_SENTENCES = "STATE_SENTENCES";

    @BindView(R.id.right_char)
    ImageView mRightCharImageView;
    @BindView(R.id.right_char_text)
    TextView mRightCharTextView;
    @BindView(R.id.left_char)
    ImageView mLeftCharImageView;
    @BindView(R.id.left_char_text)
    TextView mLeftCharTextView;
    @BindView(R.id.right_char_separator)
    View mRightCharSeparatorView;

    private Level mLevel;
    private boolean mIsEndStory;
    private ArrayList<String> mSentences;

    public static StoryFragment newInstance(Level level, boolean isEndStory) {
        StoryFragment fragment = new StoryFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LEVEL, Parcels.wrap(level));
        args.putBoolean(ARG_IS_END_STORY, isEndStory);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_story, container, false);
        ButterKnife.bind(this, layout);

        mLevel = Parcels.unwrap(getArguments().getParcelable(ARG_LEVEL));
        mIsEndStory = getArguments().getBoolean(ARG_IS_END_STORY);

        if (savedInstanceState != null) {
            mSentences = savedInstanceState.getStringArrayList(STATE_SENTENCES);
        } else {
            if (!mIsEndStory) {
                mSentences = new ArrayList<>(Arrays.asList(mLevel.getStartStory(getContext()).split("\n")));
            } else {
                mSentences = new ArrayList<>(Arrays.asList(mLevel.getEndStory(getContext()).split("\n")));
            }
        }

        updateUI();

        return layout;
    }

    private void updateUI() {
        String sentence = mSentences.get(0);
        String[] split = sentence.split(":");
        int charResId = Level.STORY_CHARS_DRAWABLE_MAP.get(split[0].substring(0, split[0].length() - 2).trim());
        boolean isLeft = "L".equals(split[0].substring(split[0].length() - 1));
        if (isLeft) {
            mLeftCharImageView.setImageResource(charResId);
            mLeftCharTextView.setText(split[1]);
            mLeftCharTextView.setVisibility(View.VISIBLE);
            mRightCharImageView.setImageResource(0);
            mRightCharTextView.setVisibility(View.GONE);
            mRightCharSeparatorView.setVisibility(View.GONE);
        } else {
            mLeftCharImageView.setImageResource(0);
            mLeftCharTextView.setVisibility(View.GONE);
            mRightCharImageView.setImageResource(charResId);
            mRightCharTextView.setText(split[1]);
            mRightCharTextView.setVisibility(View.VISIBLE);
            mRightCharSeparatorView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(STATE_SENTENCES, mSentences);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected
    @RawRes
    int getMainMusicResId() {
        if (mLevel != null && !mIsEndStory && mLevel.startStoryMusicResId != Level.INVALID_ID) {
            return mLevel.startStoryMusicResId;
        } else if (mLevel != null && mIsEndStory && mLevel.endStoryMusicResId != Level.INVALID_ID) {
            return mLevel.endStoryMusicResId;
        }

        return R.raw.story_normal;
    }

    @OnClick(R.id.skip_button)
    public void onSkipButtonClicked() {
        endStory();
    }

    @OnClick(R.id.root_view)
    public void onRootViewClicked() {
        if (mSentences.size() > 1) {
            mSentences.remove(0);
            updateUI();
        } else {
            endStory();
        }
    }

    private void endStory() {
        getFragmentManager().popBackStack();

        if (!mIsEndStory) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN);
            getMainActivity().playSound(MainActivity.SOUND_ENTER_BATTLE);
            transaction.replace(R.id.fragment_container, BattleFragment.newInstance(mLevel, Card.getDeckCardList()), BattleFragment.class.getName()).addToBackStack(null).commitAllowingStateLoss();
        }
    }
}
