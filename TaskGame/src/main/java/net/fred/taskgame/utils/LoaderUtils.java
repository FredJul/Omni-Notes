package net.fred.taskgame.utils;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.schedulers.Schedulers;

public class LoaderUtils {
    @UiThread
    public static <T> void startAsync(@NonNull Activity activity, @NonNull Observable.OnSubscribe<T> backgroundOnSubscribe, @NonNull RxLoaderObserver<T> uiThreadObserver) {
        RxLoaderManager.get(activity).create(
                Dog.getTag(), // automatically create the tag with the stacktrace
                Observable.create(backgroundOnSubscribe).subscribeOn(Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR)),
                uiThreadObserver
        ).start();
    }

    @UiThread
    public static <T> void startAsync(@NonNull Fragment fragment, @NonNull Observable.OnSubscribe<T> backgroundOnSubscribe, @NonNull RxLoaderObserver<T> uiThreadObserver) {
        RxLoaderManager.get(fragment).create(
                Dog.getTag(), // automatically create the tag with the stacktrace
                Observable.create(backgroundOnSubscribe).subscribeOn(Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR)),
                uiThreadObserver
        ).start();
    }

    @UiThread
    public static <T> void startAsync(@NonNull android.support.v4.app.Fragment fragment, @NonNull Observable.OnSubscribe<T> backgroundOnSubscribe, @NonNull RxLoaderObserver<T> uiThreadObserver) {
        RxLoaderManagerCompat.get(fragment).create(
                Dog.getTag(), // automatically create the tag with the stacktrace
                Observable.create(backgroundOnSubscribe).subscribeOn(Schedulers.from(AsyncTask.THREAD_POOL_EXECUTOR)),
                uiThreadObserver
        ).start();
    }
}
