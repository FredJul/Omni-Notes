package net.fred.taskgame.utils;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import net.fred.taskgame.MainApplication;
import net.fred.taskgame.R;

public class UiUtils {

    static public final int TRANSITION_VERTICAL = 0;
    static public final int TRANSITION_HORIZONTAL = 1;

    public enum MessageType {TYPE_INFO, TYPE_WARN, TYPE_ERROR}

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }

    public static boolean hitTest(View v, int x, int y) {
        final int tx = (int) (ViewCompat.getTranslationX(v) + 0.5f);
        final int ty = (int) (ViewCompat.getTranslationY(v) + 0.5f);
        final int left = v.getLeft() + tx;
        final int right = v.getRight() + tx;
        final int top = v.getTop() + ty;
        final int bottom = v.getBottom() + ty;

        return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
    }

    static public void animateTransition(@NonNull FragmentTransaction transaction, int direction) {
        if (direction == TRANSITION_HORIZONTAL) {
            transaction.setCustomAnimations(R.anim.fade_in_support, R.anim.fade_out_support, R.anim.fade_in_support, R.anim.fade_out_support);
        }
        if (direction == TRANSITION_VERTICAL) {
            transaction.setCustomAnimations(
                    R.anim.anim_in, R.anim.anim_out, R.anim.anim_in_pop, R.anim.anim_out_pop);
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
        Snackbar snackbar = Snackbar.make(activity.getWindow().getDecorView().findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT);
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
