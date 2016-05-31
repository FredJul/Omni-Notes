package net.fred.taskgame.hero.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fred.taskgame.hero.models.SyncData;
import net.fred.taskgame.hero.providers.FakeProvider;
import net.fred.taskgame.hero.utils.Dog;
import net.fred.taskgame.hero.utils.GameHelper;
import net.fred.taskgame.hero.utils.PrefUtils;

/**
 * Service to handle sync requests.
 * <p/>
 * <p>This service is invoked in response to Intents with action android.content.SyncAdapter, and
 * returns a Binder connection to SyncAdapter.
 * <p/>
 * <p>For performance, only one sync adapter will be initialized within this application's context.
 * <p/>
 * <p>Note: The SyncService itself is not notified when a new sync occurs. It's role is to
 * manage the lifecycle of our {@link SyncAdapter} and provide a handle to said SyncAdapter to the
 * OS on request.
 */
public class SyncService extends Service {

    public static final long SYNC_INTERVAL = 24 * 60L * 60L; // Every day

    private static final Object sSyncAdapterLock = new Object();
    private static SyncAdapter sSyncAdapter = null;

    /**
     * Thread-safe constructor, creates static {@link SyncAdapter} instance.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Dog.i("Service created");
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    /**
     * Logging-only destructor.
     */
    public void onDestroy() {
        super.onDestroy();
        Dog.i("Service destroyed");
    }

    /**
     * Return Binder handle for IPC communication with {@link SyncAdapter}.
     * <p/>
     * <p>New sync requests will be sent directly to the SyncAdapter using this channel.
     *
     * @param intent Calling intent
     * @return Binder handle for {@link SyncAdapter}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }

    public static void triggerSync(Context context) {
        AccountManager manager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = manager.getAccountsByType("com.google");

        if (accounts.length > 0) {
            ContentResolver.setIsSyncable(accounts[0], FakeProvider.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(accounts[0], FakeProvider.AUTHORITY, true);
            ContentResolver.addPeriodicSync(accounts[0], FakeProvider.AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);

            // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
            Bundle extras = new Bundle();
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            ContentResolver.requestSync(accounts[0], FakeProvider.AUTHORITY, extras);
        }
    }

    /**
     * Define a sync adapter for the app.
     * <p/>
     * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
     * SyncAdapter should only be initialized in SyncService, never anywhere else.
     * <p/>
     * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
     * SyncService.
     */
    public static class SyncAdapter extends AbstractThreadedSyncAdapter {

        /**
         * Constructor. Obtains handle to content resolver for later use.
         */
        public SyncAdapter(Context context, boolean autoInitialize) {
            super(context, autoInitialize);
        }

        /**
         * Constructor. Obtains handle to content resolver for later use.
         */
        public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
            super(context, autoInitialize, allowParallelSyncs);
        }

        /**
         * Called by the Android system in response to a request to run the sync adapter. The work
         * required to read data from the network, parse it, and store it in the content provider is
         * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
         * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
         * run <em>in situ</em>, and you don't have to set up a separate thread for them.
         * .
         * <p/>
         * <p>This is where we actually perform any work required to perform a sync.
         * {@link AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
         * so it is safe to perform blocking I/O here.
         * <p/>
         * <p>The syncResult argument allows you to pass information back to the method that triggered
         * the sync.
         */
        @Override
        public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
            Dog.i("Beginning network synchronization");

            if (PrefUtils.getBoolean(PrefUtils.PREF_ALREADY_LOGGED_TO_GAMES, false)) {
                GameHelper helper = new GameHelper(getContext());
                try {
                    helper.setup(null);
                    helper.getApiClient().blockingConnect();

                    // Open the saved game using its name.
                    Snapshots.OpenSnapshotResult result = Games.Snapshots.open(helper.getApiClient(), "save", true, Snapshots.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED).await();

                    Snapshot snapshot = result.getSnapshot();
                    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

                    // Put back the server data locally
                    if (result.getStatus().isSuccess()) {
                        byte[] savedBytes = snapshot.getSnapshotContents().readFully();

                        String json = new String(savedBytes);
                        Dog.i("get back " + json);
                        SyncData syncedData = gson.fromJson(json, SyncData.class);

                        long savedLastSyncDate = PrefUtils.getLong(PrefUtils.PREF_LAST_SYNC_DATE, -1);

                        if (syncedData.lastSyncDate > savedLastSyncDate) {

                            // TODO
                        }

                        SyncData syncData = SyncData.getLastData();
                        json = gson.toJson(syncData);
                        Dog.i("write " + json);

                        // Set the data payload for the snapshot
                        snapshot.getSnapshotContents().writeBytes(json.getBytes());

                        Games.Snapshots.commitAndClose(helper.getApiClient(), snapshot, new SnapshotMetadataChange.Builder().build());
                        PrefUtils.putLong(PrefUtils.PREF_LAST_SYNC_DATE, syncData.lastSyncDate);

                    } else {
                        Dog.e("error status: " + result.getStatus().getStatusCode());
                    }

                } catch (Throwable t) {
                    Dog.e("ERROR", t);
                    syncResult.databaseError = true;
                } finally {
                    try {
                        helper.getApiClient().disconnect();
                    } catch (Throwable ignored) {
                    }
                }

            } else {
                Dog.i("No yet registered to play games");
                syncResult.databaseError = true;
            }

            Dog.i("Network synchronization complete");
        }
    }
}
