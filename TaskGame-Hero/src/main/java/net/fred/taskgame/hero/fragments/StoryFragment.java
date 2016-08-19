package net.fred.taskgame.hero.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

    @BindView(R.id.root_view)
    ViewGroup mRootView;
    @BindView(R.id.story_text)
    TextView mStoryTextView;
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
    private boolean mIsInTextAnimation;
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
        int separatorIndex = sentence.indexOf(':');
        String charInfo = sentence.substring(0, separatorIndex);

        if ("story".equals(charInfo.trim())) {
            mRightCharImageView.animate().alpha(0);
            mRightCharTextView.animate().alpha(0);
            mRightCharSeparatorView.animate().alpha(0);
            mLeftCharImageView.animate().alpha(0);
            mLeftCharTextView.animate().alpha(0);

            displayTextCharPerChar(mStoryTextView, new SpannableString(sentence.substring(separatorIndex + 1)), 50);
            mStoryTextView.setAlpha(0);
            mStoryTextView.animate().alpha(1);
        } else {
            mStoryTextView.animate().alpha(0);

            String charId = charInfo.substring(0, charInfo.length() - 2).trim();
            String charName = getString(Level.STORY_CHARS_INFO_MAP.get(charId).first);
            int charResId = Level.STORY_CHARS_INFO_MAP.get(charId).second;
            boolean isLeft = "L".equals(charInfo.substring(charInfo.length() - 1));

            String text = charName + ": " + sentence.substring(separatorIndex + 1);
            SpannableString spannedText = new SpannableString(text);
            spannedText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.colorAccent)), 0, charName.length() + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            if (isLeft) {
                mRightCharImageView.animate().alpha(0);
                mRightCharTextView.animate().alpha(0);
                mRightCharSeparatorView.animate().alpha(0);

                mLeftCharImageView.setImageResource(charResId);
                mLeftCharImageView.animate().alpha(1);
                displayTextCharPerChar(mLeftCharTextView, spannedText, 20);
                mLeftCharTextView.setAlpha(0);
                mLeftCharTextView.animate().alpha(1);
            } else {
                mLeftCharImageView.animate().alpha(0);
                mLeftCharTextView.animate().alpha(0);

                mRightCharImageView.setImageResource(charResId);
                mRightCharImageView.animate().alpha(1);
                mRightCharSeparatorView.animate().alpha(1);
                displayTextCharPerChar(mRightCharTextView, spannedText, 20);
                mRightCharTextView.setAlpha(0);
                mRightCharTextView.animate().alpha(1);
            }
        }
    }

    private void displayTextCharPerChar(final TextView textView, final SpannableString text, final int delay) {
        mIsInTextAnimation = true;

        final int length = text.length();
        textView.setTag(1);
        Runnable displayOneChar = new Runnable() {
            @Override
            public void run() {
                Integer at = (Integer) textView.getTag();
                SpannableString textViewString = new SpannableString(text);
                textViewString.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), at, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textView.setText(textViewString);

                if (at < text.length()) {
                    textView.setTag(at + 1);
                    textView.postDelayed(this, delay);
                } else {
                    mIsInTextAnimation = false;
                }
            }
        };
        textView.postDelayed(displayOneChar, delay * 2);
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
        if (mIsInTextAnimation) {
            return;
        }

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
