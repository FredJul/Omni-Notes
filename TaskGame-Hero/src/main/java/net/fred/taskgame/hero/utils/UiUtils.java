package net.fred.taskgame.hero.utils;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;

import net.fred.taskgame.hero.MainApplication;
import net.fred.taskgame.hero.R;

public class UiUtils {

    public enum TransitionType {TRANSITION_FADE_IN}

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }

    static public void animateTransition(@NonNull FragmentTransaction transaction, TransitionType transitionType) {
        switch (transitionType) {
            case TRANSITION_FADE_IN:
                transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
                break;
        }
    }
}
