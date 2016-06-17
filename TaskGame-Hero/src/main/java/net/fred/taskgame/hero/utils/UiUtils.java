package net.fred.taskgame.hero.utils;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.widget.TextView;

import net.fred.taskgame.hero.MainApplication;
import net.fred.taskgame.hero.R;

public class UiUtils {

    public enum TransitionType {TRANSITION_FADE_IN}

    public enum MessageType {TYPE_INFO, TYPE_WARN, TYPE_ERROR}

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

    static public void showMessage(@NonNull Activity activity, @StringRes int messageId) {
        showMessage(activity, activity.getString(messageId), MessageType.TYPE_INFO);
    }

    static public void showMessage(@NonNull Activity activity, @NonNull String message) {
        showMessage(activity, message, MessageType.TYPE_INFO);
    }

    static public void showMessage(@NonNull Activity activity, @StringRes int messageId, MessageType type) {
        showMessage(activity, activity.getString(messageId), type);
    }

    static public void showMessage(@NonNull Activity activity, @NonNull String message, MessageType type) {
        Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
        switch (type) {
            case TYPE_WARN: {
                TextView textView = (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
                textView.setTextColor(ContextCompat.getColor(activity, R.color.warning));
                break;
            }
            case TYPE_ERROR: {
                TextView textView = (TextView) snackbar.getView().findViewById(R.id.snackbar_text);
                textView.setTextColor(ContextCompat.getColor(activity, R.color.error));
                break;
            }
        }
        snackbar.show();
    }

    static public void showWarningMessage(@NonNull Activity activity, @StringRes int messageId) {
        showMessage(activity, activity.getString(messageId), MessageType.TYPE_WARN);
    }

    static public void showWarningMessage(@NonNull Activity activity, @NonNull String message) {
        showMessage(activity, message, MessageType.TYPE_WARN);
    }

    static public void showErrorMessage(@NonNull Activity activity, @StringRes int messageId) {
        showMessage(activity, activity.getString(messageId), MessageType.TYPE_ERROR);
    }

    static public void showErrorMessage(@NonNull Activity activity, @NonNull String message) {
        showMessage(activity, message, MessageType.TYPE_ERROR);
    }
}
