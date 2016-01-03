package net.fred.taskgame.activity;

import android.app.Activity;
import android.os.Bundle;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.PrefUtils;

public class RequestPointsActivity extends Activity {

    public static final String INTENT_EXTRA_POINT_AMOUNT_NEEDED = "taskgame.intent.extra.POINT_AMOUNT_NEEDED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_category);

        // Retrieving intent
        final long pointsNeeded = getIntent().getLongExtra(INTENT_EXTRA_POINT_AMOUNT_NEEDED, 0);
        final long currentPoints = PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0);

        if (currentPoints < pointsNeeded) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("You only have " + currentPoints + " points... Sorry")
                    .positiveText(R.string.ok)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    })
                    .build();
            dialog.show();
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("" + pointsNeeded + " points needed. Do you accept?")
                    .positiveText(R.string.ok)
                    .negativeText(R.string.not_set)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, currentPoints - pointsNeeded);
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog dialog, DialogAction which) {
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    })
                    .build();
            dialog.show();
        }
    }
}
