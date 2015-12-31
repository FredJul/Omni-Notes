package net.fred.taskgame.utils;

import android.os.Handler;
import android.os.SystemClock;

import com.raizlabs.android.dbflow.runtime.FlowContentObserver;
import com.raizlabs.android.dbflow.sql.language.SQLCondition;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.Model;

public abstract class ThrottledFlowContentObserver extends FlowContentObserver {

    public static abstract class ThrottledFlowContentListener implements OnModelStateChangedListener {

        private final long mUpdateThrottle;
        private final Handler mHandler;
        private long mLastUpdate = 0;
        private boolean mRerun = false;

        private final Runnable mNotificationRunnable = new Runnable() {
            @Override
            public void run() {
                onChangeThrottled();
            }
        };
        private final Runnable mRerunNotificationRunnable = new Runnable() {
            @Override
            public void run() {
                if (mRerun) {
                    mRerun = false;
                    onChangeThrottled();
                }
            }
        };

        public ThrottledFlowContentListener(long delayMS) {
            mUpdateThrottle = delayMS;
            mHandler = new Handler();
        }

        @Override
        public void onModelStateChanged(Class<? extends Model> table, BaseModel.Action action, SQLCondition[] primaryKeyValues) {
            long now = SystemClock.elapsedRealtime();
            if (now - mLastUpdate > mUpdateThrottle) {
                mLastUpdate = now;
                mHandler.post(mNotificationRunnable);
                mHandler.postDelayed(mRerunNotificationRunnable, mUpdateThrottle + 1);
            } else {
                // Ignore but remember we need to rerun it;
                mRerun = true;
            }
        }

        abstract public void onChangeThrottled();
    }

    private final ThrottledFlowContentListener mModelChangeListener;

    public ThrottledFlowContentObserver(long delayMS) {
        mModelChangeListener = new ThrottledFlowContentListener(delayMS) {
            @Override
            public void onChangeThrottled() {
                ThrottledFlowContentObserver.this.onChangeThrottled();
            }
        };
        addModelChangeListener(mModelChangeListener);
    }

    abstract public void onChangeThrottled();
}
