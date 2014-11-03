/*
 * Copyright 2014 PushBullet Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.feio.android.omninotes.extensions;

import com.pushbullet.android.extension.MessagingExtension;

import it.feio.android.omninotes.MainActivity;
import roboguice.util.Ln;

public class PushBulletExtension extends MessagingExtension {
    private static final String TAG = "PushBulletExtension";


    @Override
    protected void onMessageReceived(final String conversationIden, final String message) {
        Ln.i("Pushbullet MessagingExtension: onMessageReceived(" + conversationIden + ", " + message + ")");
        MainActivity runningMainActivity = MainActivity.getInstance();
        if (runningMainActivity != null && !runningMainActivity.isFinishing()) {
            runningMainActivity.onPushBulletReply(message);
        }
    }

    @Override
    protected void onConversationDismissed(final String conversationIden) {
        Ln.i("Pushbullet MessagingExtension: onConversationDismissed(" + conversationIden + ")");
    }
}
