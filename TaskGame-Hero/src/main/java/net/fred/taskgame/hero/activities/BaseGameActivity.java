package net.fred.taskgame.hero.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.api.GoogleApiClient;

import net.fred.taskgame.hero.utils.GameHelper;

@SuppressLint("Registered")
public abstract class BaseGameActivity extends AppCompatActivity implements GameHelper.GameHelperListener {

    // The game helper object. This class is mainly a wrapper around this object.
    protected GameHelper mHelper;

    public GameHelper getGameHelper() {
        if (mHelper == null) {
            mHelper = new GameHelper(this);
        }
        return mHelper;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (mHelper == null) {
            getGameHelper();
        }
        mHelper.setup(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHelper.onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHelper.onStop();
    }

    @Override
    protected void onDestroy() {
        mHelper.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        mHelper.onActivityResult(request, response, data);
    }

    public GoogleApiClient getApiClient() {
        return mHelper.getApiClient();
    }

    public boolean isSignedIn() {
        return mHelper.isSignedIn();
    }

    public void beginUserInitiatedSignIn() {
        mHelper.beginUserInitiatedSignIn();
    }

    public void signOut() {
        mHelper.signOut();
    }

    public boolean hasSignInError() {
        return mHelper.hasSignInError();
    }

    public GameHelper.SignInFailureReason getSignInError() {
        return mHelper.getSignInError();
    }
}
