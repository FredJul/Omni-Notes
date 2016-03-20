TaskGame
==========

TaskGame is a gamified todo-list Android application. It's also open-sourced and based on OmniNotes.
All derivative works need to respect the GPLv3 license of this software and publish their source code under the same license.

![](https://lh3.googleusercontent.com/fvMvVPiu2fTVA5FWUNvhr2iwXfE9WCDxQ0C2I5fYozEY-NQ8d8ZGU7AjVrps3BEU3Q=h310) _
![](https://lh3.googleusercontent.com/-GuoW_H1RJScFMhh36zXnYQmqWRwZtDLcOFfdM3Pgkd--k6iHhhDy3qxBdgawCS6e6Q=h310) _
![](https://lh3.googleusercontent.com/qFC0H8zPJRTvz7nQ9OpujExqGwmvqTLD8S1UnH7nSnzTnIx4iDVndNZcXcc2jIr6vA=h310) _
![](https://lh3.googleusercontent.com/9T_ILE4FrvDZyT0FRM6l9Y9FsnG3nYItUPK8d5PI7O7KCyE9o2fLfL5kU2AepTGEOb9F=h310) _

### Motivate yourself to do borring tasks and make your life funnier!

Each time you finish a task, you'll earn some points and will be able to spend them into a game as a reward. Several Android games can support TaskGame points, you are not limited to one and can regularly change.

### Make our old Earth a better place! 

In addition of your own task, you can accept some public-interest quests and do them in exchange of a large quantity of points. You may just cheat to get the points, but if only few people do it for real it's already a great victory. Try it!

### How to install TaskGame?

To get access to the alpha version on PlayStore just go here: https://play.google.com/apps/testing/net.fred.taskgame

To install a demo game consuming TaskGame points you can manually install this APK: https://github.com/FredJul/memory-game/blob/master/TaskGame-memory.apk

### How to use TaskGame points into my game?

***Important notice: I'm currently searching for people who would like to modify their game to support TaskGame points. I'll refer to their app into TaskGame.***

To support TaskGame points into your game, you just need to add this small utils file into your Android project:
```
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;

public class TaskGameUtils {

    /**
     * Allow you to verify the TaskGame app is installed on the phone or not
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
     * @param context any Context
     * @param points the number of TaskGame points you request in your game
     * @return a valid Intent you should start with startActivityForResult() or an ActivityNotFoundException exception if TaskGame is not installed
     */
    public static @NonNull Intent getRequestPointsActivityIntent(@NonNull Context context, long points) {
        if (!isAppInstalled(context)) {
            throw new ActivityNotFoundException("TaskGame app is not installed");
        }

        Intent intent = new Intent("taskgame.intent.action.REQUEST_POINTS");
        intent.setPackage("net.fred.taskgame");
        intent.putExtra("taskgame.intent.extra.POINT_AMOUNT_NEEDED", points);
        return intent;
    }
}
```

Then simply call from your activity or fragment the `startActivityForResult()` method. Example:
```
public void onClick() {
  try {
  	startActivityForResult(TaskGameUtils.getRequestPointsActivityIntent(getContext(), 50L), TASKGAME_CODE);
  } catch (ActivityNotFoundException e) {
    // TaskGame application is not installed
  }
}
				
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
	switch(requestCode) {
	case TASKGAME_CODE:
		if (resultCode == Activity.RESULT_OK) {
			// the user accepted, continue in your game
		} else {
			// the user refused
		}
		break;
	default:
    break;
	}
	
	super.onActivityResult(requestCode, resultCode, data);
}
```
