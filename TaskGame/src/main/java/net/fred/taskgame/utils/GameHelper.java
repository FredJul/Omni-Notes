/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fred.taskgame.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;

public class GameHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    /**
     * Listener for sign-in success or failure events.
     */
    public interface GameHelperListener {
        /**
         * Called when sign-in fails. As a result, a "Sign-In" button can be
         * shown to the user; when that button is clicked, call
         *
         * @link{GamesHelper#beginUserInitiatedSignIn . Note that not all calls
         * to this method mean an
         * error; it may be a result
         * of the fact that automatic
         * sign-in could not proceed
         * because user interaction
         * was required (consent
         * dialogs). So
         * implementations of this
         * method should NOT display
         * an error message unless a
         * call to @link{GamesHelper#
         * hasSignInError} indicates
         * that an error indeed
         * occurred.
         */
        void onSignInFailed();

        /**
         * Called when sign-in succeeds.
         */
        void onSignInSucceeded();
    }

    // configuration done?
    private boolean mSetupDone = false;

    // are we currently connecting?
    private boolean mConnecting = false;

    // Are we expecting the result of a resolution flow?
    boolean mExpectingResolution = false;

    // was the sign-in flow cancelled when we tried it?
    // if true, we know not to try again automatically.
    boolean mSignInCancelled = false;

    /**
     * The Activity we are bound to. We need to keep a reference to the Activity
     * because some games methods require an Activity (a Context won't do). We
     * are careful not to leak these references: we release them on onStop().
     */
    Activity mActivity = null;

    // app context
    Context mAppContext = null;

    // Request code we use when invoking other Activities to complete the
    // sign-in flow.
    final static int RC_RESOLVE = 9001;

    // the Google API client builder we will use to create GoogleApiClient
    GoogleApiClient.Builder mGoogleApiClientBuilder = null;

    // Google API client object we manage.
    GoogleApiClient mGoogleApiClient = null;

    // Whether to automatically try to sign in on onStart(). We only set this
    // to true when the sign-in process fails or the user explicitly signs out.
    // We set it back to false when the user initiates the sign in process.
    boolean mConnectOnStart = true;

    /*
     * Whether user has specifically requested that the sign-in process begin.
     * If mUserInitiatedSignIn is false, we're in the automatic sign-in attempt
     * that we try once the Activity is started -- if true, then the user has
     * already clicked a "Sign-In" button or something similar
     */
    boolean mUserInitiatedSignIn = false;

    // The connection result we got from our last attempt to sign-in.
    ConnectionResult mConnectionResult = null;

    // The error that happened during sign-in.
    SignInFailureReason mSignInFailureReason = null;

    // Listener
    GameHelperListener mListener = null;

    /**
     * Construct a GameHelper object, initially tied to the given Activity.
     * After constructing this object, call @link{setup} from the onCreate()
     * method of your Activity.
     */
    public GameHelper(Context context) {
        mAppContext = context.getApplicationContext();
    }

    void assertConfigured(String operation) {
        if (!mSetupDone) {
            String error = "GameHelper error: Operation attempted without setup: "
                    + operation
                    + ". The setup() method must be called before attempting any other operation.";
            Dog.e(error);
            throw new IllegalStateException(error);
        }
    }

    /**
     * Creates a GoogleApiClient.Builder for use with @link{#setup}. Normally,
     * you do not have to do this; use this method only if you need to make
     * nonstandard setup (e.g. adding extra scopes for other APIs) on the
     * GoogleApiClient.Builder before calling @link{#setup}.
     */
    public GoogleApiClient.Builder createApiClientBuilder() {
        if (mSetupDone) {
            String error = "GameHelper: you called GameHelper.createApiClientBuilder() after "
                    + "calling setup. You can only get a client builder BEFORE performing setup.";
            Dog.e(error);
            throw new IllegalStateException(error);
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(mAppContext, this, this);

        builder.addApi(Games.API);
        builder.addScope(Games.SCOPE_GAMES);

        builder.addScope(Drive.SCOPE_APPFOLDER);
        builder.addApi(Drive.API);

        mGoogleApiClientBuilder = builder;
        return builder;
    }

    /**
     * Performs setup on this GameHelper object. Call this from the onCreate()
     * method of your Activity. This will create the clients and do a few other
     * initialization tasks. Next, call @link{#onStart} from the onStart()
     * method of your Activity.
     *
     * @param listener The listener to be notified of sign-in events.
     */
    public void setup(GameHelperListener listener) {
        if (mSetupDone) {
            String error = "GameHelper: you cannot call GameHelper.setup() more than once!";
            Dog.e(error);
            throw new IllegalStateException(error);
        }
        mListener = listener;

        if (mGoogleApiClientBuilder == null) {
            // we don't have a builder yet, so create one
            createApiClientBuilder();
        }

        mGoogleApiClient = mGoogleApiClientBuilder.build();
        mGoogleApiClientBuilder = null;
        mSetupDone = true;
    }

    /**
     * Returns the GoogleApiClient object. In order to call this method, you
     * must have called @link{setup}.
     */
    public GoogleApiClient getApiClient() {
        if (mGoogleApiClient == null) {
            throw new IllegalStateException(
                    "No GoogleApiClient. Did you call setup()?");
        }
        return mGoogleApiClient;
    }

    /**
     * Returns whether or not the user is signed in.
     */
    public boolean isSignedIn() {
        return mGoogleApiClient != null && mGoogleApiClient.isConnected();
    }

    /**
     * Returns whether or not we are currently connecting
     */
    public boolean isConnecting() {
        return mConnecting;
    }

    /**
     * Returns whether or not there was a (non-recoverable) error during the
     * sign-in process.
     */
    public boolean hasSignInError() {
        return mSignInFailureReason != null;
    }

    /**
     * Returns the error that happened during the sign-in process, null if no
     * error occurred.
     */
    public SignInFailureReason getSignInError() {
        return mSignInFailureReason;
    }

    /**
     * Call this method from your Activity's onStart().
     */
    public void onStart(Activity act) {
        mActivity = act;
        mAppContext = act.getApplicationContext();

        Dog.d("onStart");
        assertConfigured("onStart");

        if (mConnectOnStart) {
            if (mGoogleApiClient.isConnected()) {
                Dog.w("GameHelper: client was already connected on onStart()");
            } else {
                Dog.d("Connecting client.");
                mConnecting = true;
                mGoogleApiClient.connect();
            }
        } else {
            Dog.d("Not attempting to connect because mConnectOnStart=false");
            Dog.d("Instead, reporting a sign-in failure.");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyListener(false);
                }
            }, 1000);
        }
    }

    /**
     * Call this method from your Activity's onStop().
     */
    public void onStop() {
        Dog.d("onStop");
        assertConfigured("onStop");
        if (mGoogleApiClient.isConnected()) {
            Dog.d("Disconnecting client due to onStop");
            mGoogleApiClient.disconnect();
        } else {
            Dog.d("Client already disconnected when we got onStop.");
        }
        mConnecting = false;
        mExpectingResolution = false;

        // let go of the Activity reference
        mActivity = null;
    }

    /**
     * Call this method from your Activity's onDestroy().
     */
    public void onDestroy() {
        mListener = null;
    }


    /**
     * Sign out and disconnect from the APIs.
     */
    public void signOut() {
        if (!mGoogleApiClient.isConnected()) {
            // nothing to do
            Dog.d("signOut: was already disconnected, ignoring.");
            return;
        }

        // For the games client, signing out means calling signOut and
        // disconnecting
        Dog.d("Signing out from the Google API Client.");
        Games.signOut(mGoogleApiClient);

        // Ready to disconnect
        Dog.d("Disconnecting client.");
        mConnectOnStart = false;
        mConnecting = false;
        mGoogleApiClient.disconnect();
    }

    /**
     * Handle activity result. Call this method from your Activity's
     * onActivityResult callback. If the activity result pertains to the sign-in
     * process, processes it appropriately.
     */
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        Dog.d("onActivityResult: req="
                + (requestCode == RC_RESOLVE ? "RC_RESOLVE" : String
                .valueOf(requestCode)) + ", resp="
                + activityResponseCodeToString(responseCode));
        if (requestCode != RC_RESOLVE) {
            Dog.d("onActivityResult: request code not meant for us. Ignoring.");
            return;
        }

        // no longer expecting a resolution
        mExpectingResolution = false;

        if (!mConnecting) {
            Dog.d("onActivityResult: ignoring because we are not connecting.");
            return;
        }

        // We're coming back from an activity that was launched to resolve a
        // connection problem. For example, the sign-in UI.
        if (responseCode == Activity.RESULT_OK) {
            // Ready to try to connect again.
            Dog.d("onAR: Resolution was RESULT_OK, so connecting current client again.");
            connect();
        } else if (responseCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
            Dog.d("onAR: Resolution was RECONNECT_REQUIRED, so reconnecting.");
            connect();
        } else if (responseCode == Activity.RESULT_CANCELED) {
            // User cancelled.
            Dog.d("onAR: Got a cancellation result, so disconnecting.");
            mSignInCancelled = true;
            mConnectOnStart = false;
            mUserInitiatedSignIn = false;
            mSignInFailureReason = null; // cancelling is not a failure!
            mConnecting = false;
            mGoogleApiClient.disconnect();

            notifyListener(false);
        } else {
            // Whatever the problem we were trying to solve, it was not
            // solved. So give up and show an error message.
            Dog.d("onAR: responseCode="
                    + activityResponseCodeToString(responseCode)
                    + ", so giving up.");
            giveUp(new SignInFailureReason(mConnectionResult.getErrorCode(),
                    responseCode));
        }
    }

    void notifyListener(boolean success) {
        Dog.d("Notifying LISTENER of sign-in "
                + (success ? "SUCCESS"
                : mSignInFailureReason != null ? "FAILURE (error)"
                : "FAILURE (no error)"));
        if (mListener != null) {
            if (success) {
                mListener.onSignInSucceeded();
            } else {
                mListener.onSignInFailed();
            }
        }
    }

    /**
     * Starts a user-initiated sign-in flow. This should be called when the user
     * clicks on a "Sign In" button. As a result, authentication/consent dialogs
     * may show up. At the end of the process, the GameHelperListener's
     * onSignInSucceeded() or onSignInFailed() methods will be called.
     */
    public void beginUserInitiatedSignIn() {
        Dog.d("beginUserInitiatedSignIn: resetting attempt count.");
        mSignInCancelled = false;
        mConnectOnStart = true;

        if (mGoogleApiClient.isConnected()) {
            // nothing to do
            Dog.w("beginUserInitiatedSignIn() called when already connected. "
                    + "Calling listener directly to notify of success.");
            notifyListener(true);
            return;
        } else if (mConnecting) {
            Dog.w("beginUserInitiatedSignIn() called when already connecting. "
                    + "Be patient! You can only call this method after you get an "
                    + "onSignInSucceeded() or onSignInFailed() callback. Suggestion: disable "
                    + "the sign-in button on startup and also when it's clicked, and re-enable "
                    + "when you get the callback.");
            // ignore call (listener will get a callback when the connection
            // process finishes)
            return;
        }

        Dog.d("Starting USER-INITIATED sign-in flow.");

        // indicate that user is actively trying to sign in (so we know to
        // resolve
        // connection problems by showing dialogs)
        mUserInitiatedSignIn = true;

        if (mConnectionResult != null) {
            // We have a pending connection result from a previous failure, so
            // start with that.
            Dog.d("beginUserInitiatedSignIn: continuing pending sign-in flow.");
            mConnecting = true;
            resolveConnectionResult();
        } else {
            // We don't have a pending connection result, so start anew.
            Dog.d("beginUserInitiatedSignIn: starting new sign-in flow.");
            mConnecting = true;
            connect();
        }
    }

    void connect() {
        if (mGoogleApiClient.isConnected()) {
            Dog.d("Already connected.");
            return;
        }
        Dog.d("Starting connection.");
        mConnecting = true;
        mGoogleApiClient.connect();
    }

    /**
     * Called when we successfully obtain a connection to a client.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Dog.d("onConnected: connected!");

        // we're good to go
        succeedSignIn();
    }

    void succeedSignIn() {
        Dog.d("succeedSignIn");
        mSignInFailureReason = null;
        mConnectOnStart = true;
        mUserInitiatedSignIn = false;
        mConnecting = false;
        notifyListener(true);
    }

    /**
     * Handles a connection failure.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // save connection result for later reference
        Dog.d("onConnectionFailed");

        mConnectionResult = result;
        Dog.d("Connection failure:");
        Dog.d("   - code: "
                + errorCodeToString(mConnectionResult
                .getErrorCode()));
        Dog.d("   - resolvable: " + mConnectionResult.hasResolution());
        Dog.d("   - details: " + mConnectionResult.toString());

        boolean shouldResolve;

        if (mUserInitiatedSignIn) {
            Dog.d("onConnectionFailed: WILL resolve because user initiated sign-in.");
            shouldResolve = true;
        } else if (mSignInCancelled) {
            Dog.d("onConnectionFailed WILL NOT resolve (user already cancelled once).");
            shouldResolve = false;
        } else {
            shouldResolve = false;
            Dog.d("onConnectionFailed: Will NOT resolve; not user-initiated");
        }

        if (!shouldResolve) {
            // Fail and wait for the user to want to sign in.
            Dog.d("onConnectionFailed: since we won't resolve, failing now.");
            mConnectionResult = result;
            mConnecting = false;
            notifyListener(false);
            return;
        }

        Dog.d("onConnectionFailed: resolving problem...");

        // Resolve the connection result. This usually means showing a dialog or
        // starting an Activity that will allow the user to give the appropriate
        // consents so that sign-in can be successful.
        resolveConnectionResult();
    }

    /**
     * Attempts to resolve a connection failure. This will usually involve
     * starting a UI flow that lets the user give the appropriate consents
     * necessary for sign-in to work.
     */
    void resolveConnectionResult() {
        // Try to resolve the problem
        if (mExpectingResolution) {
            Dog.d("We're already expecting the result of a previous resolution.");
            return;
        }

        if (mActivity == null) {
            Dog.d("No need to resolve issue, activity does not exist anymore");
            return;
        }

        Dog.d("resolveConnectionResult: trying to resolve result: "
                + mConnectionResult);
        if (mConnectionResult.hasResolution()) {
            // This problem can be fixed. So let's try to fix it.
            Dog.d("Result has resolution. Starting it.");
            try {
                // launch appropriate UI flow (which might, for example, be the
                // sign-in flow)
                mExpectingResolution = true;
                mConnectionResult.startResolutionForResult(mActivity, RC_RESOLVE);
            } catch (SendIntentException e) {
                // Try connecting again
                Dog.d("SendIntentException, so connecting again.");
                connect();
            }
        } else {
            // It's not a problem what we can solve, so give up and show an
            // error.
            Dog.d("resolveConnectionResult: result has no resolution. Giving up.");
            giveUp(new SignInFailureReason(mConnectionResult.getErrorCode()));
        }
    }

    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            Dog.d("Disconnecting client.");
            mGoogleApiClient.disconnect();
        } else {
            Dog.w("disconnect() called when client was already disconnected.");
        }
    }

    /**
     * Give up on signing in due to an error. Shows the appropriate error
     * message to the user, using a standard error dialog as appropriate to the
     * cause of the error. That dialog will indicate to the user how the problem
     * can be solved (for example, re-enable Google Play Services, upgrade to a
     * new version, etc).
     */
    void giveUp(SignInFailureReason reason) {
        mConnectOnStart = false;
        disconnect();
        mSignInFailureReason = reason;

        if (reason.mActivityResultCode == GamesActivityResultCodes.RESULT_APP_MISCONFIGURED) {
            // print debug info for the developer
            Log.w("GameHelper", "**** APP NOT CORRECTLY CONFIGURED TO USE GOOGLE PLAY GAME SERVICES");
        }

        mConnecting = false;
        notifyListener(false);
    }

    /**
     * Called when we are disconnected from the Google API client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Dog.d("onConnectionSuspended, cause=" + cause);
        disconnect();
        mSignInFailureReason = null;
        Dog.d("Making extraordinary call to onSignInFailed callback");
        mConnecting = false;
        notifyListener(false);
    }

    static String activityResponseCodeToString(int respCode) {
        switch (respCode) {
            case Activity.RESULT_OK:
                return "RESULT_OK";
            case Activity.RESULT_CANCELED:
                return "RESULT_CANCELED";
            case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
                return "RESULT_APP_MISCONFIGURED";
            case GamesActivityResultCodes.RESULT_LEFT_ROOM:
                return "RESULT_LEFT_ROOM";
            case GamesActivityResultCodes.RESULT_LICENSE_FAILED:
                return "RESULT_LICENSE_FAILED";
            case GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED:
                return "RESULT_RECONNECT_REQUIRED";
            case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
                return "SIGN_IN_FAILED";
            default:
                return String.valueOf(respCode);
        }
    }

    static String errorCodeToString(int errorCode) {
        switch (errorCode) {
            case ConnectionResult.DEVELOPER_ERROR:
                return "DEVELOPER_ERROR(" + errorCode + ")";
            case ConnectionResult.INTERNAL_ERROR:
                return "INTERNAL_ERROR(" + errorCode + ")";
            case ConnectionResult.INVALID_ACCOUNT:
                return "INVALID_ACCOUNT(" + errorCode + ")";
            case ConnectionResult.LICENSE_CHECK_FAILED:
                return "LICENSE_CHECK_FAILED(" + errorCode + ")";
            case ConnectionResult.NETWORK_ERROR:
                return "NETWORK_ERROR(" + errorCode + ")";
            case ConnectionResult.RESOLUTION_REQUIRED:
                return "RESOLUTION_REQUIRED(" + errorCode + ")";
            case ConnectionResult.SERVICE_DISABLED:
                return "SERVICE_DISABLED(" + errorCode + ")";
            case ConnectionResult.SERVICE_INVALID:
                return "SERVICE_INVALID(" + errorCode + ")";
            case ConnectionResult.SERVICE_MISSING:
                return "SERVICE_MISSING(" + errorCode + ")";
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                return "SERVICE_VERSION_UPDATE_REQUIRED(" + errorCode + ")";
            case ConnectionResult.SIGN_IN_REQUIRED:
                return "SIGN_IN_REQUIRED(" + errorCode + ")";
            case ConnectionResult.SUCCESS:
                return "SUCCESS(" + errorCode + ")";
            default:
                return "Unknown error code " + errorCode;
        }
    }

    // Represents the reason for a sign-in failure
    public static class SignInFailureReason {
        public static final int NO_ACTIVITY_RESULT_CODE = -100;
        int mServiceErrorCode = 0;
        int mActivityResultCode = NO_ACTIVITY_RESULT_CODE;

        public int getServiceErrorCode() {
            return mServiceErrorCode;
        }

        public int getActivityResultCode() {
            return mActivityResultCode;
        }

        public SignInFailureReason(int serviceErrorCode, int activityResultCode) {
            mServiceErrorCode = serviceErrorCode;
            mActivityResultCode = activityResultCode;
        }

        public SignInFailureReason(int serviceErrorCode) {
            this(serviceErrorCode, NO_ACTIVITY_RESULT_CODE);
        }

        @Override
        public String toString() {
            return "SignInFailureReason(serviceErrorCode:"
                    + errorCodeToString(mServiceErrorCode)
                    + ((mActivityResultCode == NO_ACTIVITY_RESULT_CODE) ? ")"
                    : (",activityResultCode:"
                    + activityResponseCodeToString(mActivityResultCode) + ")"));
        }
    }
}
