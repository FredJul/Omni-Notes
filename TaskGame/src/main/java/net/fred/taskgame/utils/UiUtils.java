package net.fred.taskgame.utils;

import android.util.TypedValue;
import android.view.View;
import android.widget.ListView;

import net.fred.taskgame.MainApplication;

public class UiUtils {

    static public int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, MainApplication.getContext().getResources().getDisplayMetrics());
    }

    static public void addEmptyFooterView(ListView listView, int dp) {
        View view = new View(listView.getContext());
        view.setMinimumHeight(dpToPixel(dp));
        view.setClickable(true);
        listView.addFooterView(view);
    }
}
