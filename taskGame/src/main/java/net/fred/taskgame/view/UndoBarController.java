/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.fred.taskgame.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.TextView;

import net.fred.taskgame.R;

import java.util.Locale;

public class UndoBarController {
    private View mBarView;
    private TextView mMessageView;
    private ViewPropertyAnimator mBarAnimator;

    private UndoListener mUndoListener;

    // State objects
    private Parcelable mUndoToken;
    private CharSequence mUndoMessage;
    private boolean isVisible;


    public interface UndoListener {
        void onUndo(Parcelable token);
    }

    public UndoBarController(View undoBarView, UndoListener undoListener) {
        mBarView = undoBarView;
        mBarAnimator = mBarView.animate();
        mUndoListener = undoListener;

        mMessageView = (TextView) mBarView.findViewById(R.id.undobar_message);

        Button mButtonView = (Button) mBarView.findViewById(R.id.undobar_button);
        mButtonView.setText(mButtonView.getText().toString().toUpperCase(Locale.getDefault()));
        mButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideUndoBar(false);
                mUndoListener.onUndo(mUndoToken);
            }
        });

        hideUndoBar(false);
    }

    public void showUndoBar(boolean immediate, CharSequence message, Parcelable undoToken) {
        mUndoToken = undoToken;
        mUndoMessage = message;
        mMessageView.setText(mUndoMessage);

        mBarView.setVisibility(View.VISIBLE);
        if (immediate) {
            mBarView.setAlpha(1);
        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(1)
                    .setDuration(
                            mBarView.getResources()
                                    .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(null);
        }
        isVisible = true;
    }

    public void hideUndoBar(boolean immediate) {
        if (immediate) {
            mBarView.setVisibility(View.GONE);
            mBarView.setAlpha(0);
            mUndoMessage = null;
            mUndoToken = null;

        } else {
            mBarAnimator.cancel();
            mBarAnimator
                    .alpha(0)
                    .setDuration(mBarView.getResources()
                            .getInteger(android.R.integer.config_shortAnimTime))
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBarView.setVisibility(View.GONE);
                            mUndoMessage = null;
                            mUndoToken = null;
                        }
                    });
        }
        isVisible = false;
    }

    public boolean isVisible() {
        return isVisible;
    }
}
