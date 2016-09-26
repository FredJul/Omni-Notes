package net.fred.taskgame.hero.views;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BugfixedBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {

    public BugfixedBottomSheetBehavior() {
        super();
    }

    public BugfixedBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (getState() == STATE_HIDDEN || getState() == STATE_SETTLING) {
            return false;
        }
        return super.onTouchEvent(parent, child, event);
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        if (getState() == STATE_HIDDEN || getState() == STATE_SETTLING) {
            return false;
        }
        return super.onInterceptTouchEvent(parent, child, event);
    }
}
