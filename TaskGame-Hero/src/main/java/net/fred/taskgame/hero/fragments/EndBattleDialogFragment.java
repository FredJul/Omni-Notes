package net.fred.taskgame.hero.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.fred.taskgame.hero.R;
import net.fred.taskgame.hero.models.Card;
import net.fred.taskgame.hero.models.Level;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class EndBattleDialogFragment extends ImmersiveDialogFragment {

    public enum EndType {PLAYER_WON, ENEMY_WON, DRAW}

    private static final String ARG_LEVEL_NUMBER = "ARG_LEVEL_NUMBER";
    private static final String ARG_WAS_ALREADY_COMPLETED_ONCE = "ARG_WAS_ALREADY_COMPLETED_ONCE";
    private static final String ARG_END_TYPE = "ARG_END_TYPE";

    @BindView(R.id.crown)
    View mCrown;

    @BindView(R.id.title)
    TextView mTitle;

    @BindView(R.id.content)
    TextView mContent;

    private int mLevelNumber;
    private boolean mWasAlreadyCompletedOnce;
    private EndType mEndType;

    static EndBattleDialogFragment newInstance(int levelNumber, boolean wasAlreadyCompletedOnce, EndType endType) {
        EndBattleDialogFragment f = new EndBattleDialogFragment();

        Bundle args = new Bundle();
        args.putInt(ARG_LEVEL_NUMBER, levelNumber);
        args.putBoolean(ARG_WAS_ALREADY_COMPLETED_ONCE, wasAlreadyCompletedOnce);
        args.putSerializable(ARG_END_TYPE, endType);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLevelNumber = getArguments().getInt(ARG_LEVEL_NUMBER);
        mWasAlreadyCompletedOnce = getArguments().getBoolean(ARG_WAS_ALREADY_COMPLETED_ONCE);
        mEndType = (EndType) getArguments().getSerializable(ARG_END_TYPE);

        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme_Dialog);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_end_battle, container, false);
        ButterKnife.bind(this, v);

        switch (mEndType) {
            case PLAYER_WON: {
                mTitle.setText("VICTORY");

                String content = "Who is the boss now?\n\n";
                if (!mWasAlreadyCompletedOnce) {
                    int previousSlots = Level.getCorrespondingDeckSlots(mLevelNumber - 1);
                    int newSlots = Level.getCorrespondingDeckSlots(mLevelNumber);

                    int previousAvailableCreatures = Card.getNonObtainedCardList(previousSlots).size();
                    int newAvailableCreatures = Card.getNonObtainedCardList(newSlots).size();

                    if (newAvailableCreatures > previousAvailableCreatures) {
                        content += " ● You can now summon more creatures!\n";
                    }

                    if (newSlots > previousSlots) {
                        content += " ● You have earned new deck slots!\n";
                    }
                }

                mContent.setText(content);
                break;
            }
            case ENEMY_WON: {
                mTitle.setText("DEFEAT");
                mContent.setText("Well, that was close, right?");
                mCrown.setVisibility(View.GONE);
                break;
            }
            case DRAW: {
                mTitle.setText("DRAW");
                mContent.setText("Well, that was close, right?");
                mCrown.setVisibility(View.GONE);
                break;
            }

        }
        return v;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getFragmentManager().popBackStack();
        getFragmentManager().popBackStack();
    }

    @OnClick(R.id.ok)
    public void onOkButtonClicked() {
        getDialog().cancel();
    }
}