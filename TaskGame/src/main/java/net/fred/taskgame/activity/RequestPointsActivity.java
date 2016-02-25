package net.fred.taskgame.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

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
            new AlertDialog.Builder(this)
                    .setMessage("You only have " + currentPoints + " points... Sorry")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    }).show();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage("" + pointsNeeded + " points needed. Do you accept?")
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PrefUtils.putLong(PrefUtils.PREF_CURRENT_POINTS, currentPoints - pointsNeeded);
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.not_set, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    }).show();
        }
    }
}
