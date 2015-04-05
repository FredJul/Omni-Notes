package it.feio.android.checklistview.dragging;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import it.feio.android.checklistview.App;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ChecklistViewOnTouchListener implements OnTouchListener {

    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return actionDown(view);
            default:
                return false;
        }
    }

    private boolean actionDown(View view) {
        View v = (View) view.getParent();
        ChecklistViewDragShadowBuilder shadowBuilder = new ChecklistViewDragShadowBuilder(v);
        v.startDrag(null, shadowBuilder, v, 0);
        if (App.getSettings().getDragVibrationEnabled()) {
            ((Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE)).vibrate(App.getSettings()
                    .getDragVibrationDuration());
        }
        return true;
    }
}
