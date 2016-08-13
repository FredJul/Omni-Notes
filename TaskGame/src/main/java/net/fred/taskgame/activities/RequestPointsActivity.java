package net.fred.taskgame.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import net.fred.taskgame.R;
import net.fred.taskgame.utils.DbUtils;
import net.fred.taskgame.utils.PrefUtils;

public class RequestPointsActivity extends Activity {

    public static final String INTENT_EXTRA_POINT_AMOUNT_NEEDED = "taskgame.intent.extra.POINT_AMOUNT_NEEDED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieving intent
        final long pointsNeeded = getIntent().getLongExtra(INTENT_EXTRA_POINT_AMOUNT_NEEDED, 0);
        final long currentPoints = PrefUtils.getLong(PrefUtils.PREF_CURRENT_POINTS, 0);

        if (currentPoints < pointsNeeded) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(currentPoints <= 0 ? getString(R.string.no_points) : getString(R.string.not_enough_points, currentPoints))
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(RequestPointsActivity.this, MainActivity.class));
                            dialog.cancel();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    }).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setIcon(R.mipmap.ic_launcher)
                    .setMessage(getString(R.string.points_needed, pointsNeeded))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            DbUtils.updateCurrentPoints(currentPoints - pointsNeeded);
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    }).show();
        }
    }
}
