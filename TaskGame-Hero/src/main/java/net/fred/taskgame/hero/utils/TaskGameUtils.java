package net.fred.taskgame.hero.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;

public class TaskGameUtils {

    /**
     * Allow you to verify the TaskGame app is installed on the phone or not
     *
     * @param context any Context
     * @return true if the real TaskGame app is installed on the phone, false otherwise
     */
    public static boolean isAppInstalled(@NonNull Context context) {
        try {
            Signature[] signatures = context.getPackageManager().getPackageInfo("net.fred.taskgame", PackageManager.GET_SIGNATURES).signatures;
            if (signatures.length == 1 && signatures[0].hashCode() == -361897285) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }

        return false;
    }

    /**
     * Get the Intent you can launch with startActivityForResult(). The user accepted the request if RESULT_OK is sent as resultCode in onActivityResult()
     *
     * @param context any Context
     * @param points  the number of TaskGame points you request in your game
     * @return a valid Intent you should start with startActivityForResult() or an ActivityNotFoundException exception if TaskGame is not installed
     */
    public static
    @NonNull
    Intent getRequestPointsActivityIntent(@NonNull Context context, long points) {
        if (!isAppInstalled(context)) {
            throw new ActivityNotFoundException("TaskGame app is not installed");
        }

        Intent intent = new Intent("taskgame.intent.action.REQUEST_POINTS");
        intent.setPackage("net.fred.taskgame");
        intent.putExtra("taskgame.intent.extra.POINT_AMOUNT_NEEDED", points);
        return intent;
    }
}
