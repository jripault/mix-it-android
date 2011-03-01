package fr.mixit.android.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import fr.mixit.android.Constants;
import fr.mixit.android.R;
import fr.mixit.android.io.RemoteSessionsHandler;
import fr.mixit.android.io.RemoteSlotsHandler;
import fr.mixit.android.io.RemoteSpeakersHandler;
import fr.mixit.android.io.RemoteTracksHandler;
import fr.mixit.android.model.RequestHash;
import fr.mixit.android.utils.SyncUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by mathieu
 * Date: 1/30/11
 * Time: 9:57 PM
 */
public class SyncService extends IntentService {

	private static final String TAG = "SyncService";

	public static final String EXTRA_STATUS_RECEIVER = "fr.mixit.android.extra.STATUS_RECEIVER";
	public static final String EXTRA_FORCE_REFRESH = "fr.mixit.android.extra.FORCE_REFRESH";

	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_ERROR = 0x2;
	public static final int STATUS_FINISHED = 0x3;

	private static final String [] URLS = {
			Constants.SESSIONS_URL,
			Constants.SPEAKERS_URL,
			Constants.SLOTS_URL,
			Constants.TRACKS_URL,
	};

	private static final int VERSION_NONE = 0;
	private static final int VERSION_LOCAL = 1;
	private static final int VERSION_REMOTE = 5;

	private LocalExecutor mLocalExecutor;
	private RemoteExecutor mRemoteExecutor;
	private HttpClient mHttpClient;
	private ContentResolver mResolver;


	public SyncService() {
	    super(TAG);
	}

	@Override
	public void onCreate() {
	    super.onCreate();

	    mHttpClient = SyncUtils.getHttpClient(this);
		mResolver = getContentResolver();

	    mLocalExecutor = new LocalExecutor(getResources(), mResolver);
	    mRemoteExecutor = new RemoteExecutor(mHttpClient, mResolver);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
		if (receiver != null) receiver.send(STATUS_RUNNING, Bundle.EMPTY);

		final Context context = this;
        final SharedPreferences syncServicePrefs = getSharedPreferences(SyncPrefs.MIXITSCHED_SYNC, Context.MODE_PRIVATE);
        final int localVersion = syncServicePrefs.getInt(SyncPrefs.LOCAL_VERSION, VERSION_NONE);
        final long lastRemoteSync = syncServicePrefs.getLong(SyncPrefs.LAST_REMOTE_SYNC, 0);

        try {
            // Bulk of sync work, performed by executing several fetches from
            // local and online sources.

            final long startLocal = System.currentTimeMillis();
            final boolean localParse = localVersion < VERSION_LOCAL;
            Log.d(TAG, "found localVersion=" + localVersion + " and VERSION_LOCAL=" + VERSION_LOCAL);
            if (localParse) {
	            //TODO integrate cache
                // Parse values from local cache first
/*                mLocalExecutor.execute(R.xml.search_suggest, new LocalSearchSuggestHandler());
            	mLocalExecutor.execute(context, "cache-rooms.json", new RemoteRoomsHandler());
            	mLocalExecutor.execute(context, "cache-presentationtypes.json", new RemoteSessionTypesHandler());
            	mLocalExecutor.execute(context, "cache-speakers.json", new RemoteSpeakersHandler());
            	mLocalExecutor.execute(context, "cache-presentations.json", new RemoteSessionsHandler());
            	mLocalExecutor.execute(context, "cache-schedule.json", new RemoteScheduleHandler());

                // Save local parsed version
            	syncServicePrefs.edit().putInt(SyncPrefs.LOCAL_VERSION, VERSION_LOCAL).commit();*/
            }
            Log.d(TAG, "local sync took " + (System.currentTimeMillis() - startLocal) + "ms");

            final long startRemote = System.currentTimeMillis();
            boolean performRemoteSync = performRemoteSync(mResolver, mHttpClient, intent, context);
            if (performRemoteSync) {
            	// Parse values from REST interface
	            RequestHash result = mRemoteExecutor.executeGet(Constants.TRACKS_URL, new RemoteTracksHandler());
				SyncUtils.updateLocalMd5(mResolver, result.getUrl(), result.getMd5());

	            result = mRemoteExecutor.executeGet(Constants.SLOTS_URL, new RemoteSlotsHandler());
	            SyncUtils.updateLocalMd5(mResolver, result.getUrl(), result.getMd5());

	            result = mRemoteExecutor.executeGet(Constants.SPEAKERS_URL, new RemoteSpeakersHandler());
	            SyncUtils.updateLocalMd5(mResolver, result.getUrl(), result.getMd5());

	            result = mRemoteExecutor.executeGet(Constants.SESSIONS_URL, new RemoteSessionsHandler());
	            SyncUtils.updateLocalMd5(mResolver, result.getUrl(), result.getMd5());

	            // Save last remote sync time
	            syncServicePrefs.edit().putLong(SyncPrefs.LAST_REMOTE_SYNC, startRemote).commit();
	            // Save remote parsed version
	            syncServicePrefs.edit().putInt(SyncPrefs.LOCAL_VERSION, VERSION_REMOTE).commit();
            }
            Log.d(TAG, "remote sync took " + (System.currentTimeMillis() - startRemote) + "ms");

/*            if (!localParse && performRemoteSync) {
            	NotificationUtils.cancelNotifications(context);
            	NotificationUtils.notifyNewSessions(context, getContentResolver());
            	NotificationUtils.notifyChangedStarredSessions(context, getContentResolver());
            }*/
        } catch (Exception e) {
            Log.e(TAG, "Problem while syncing", e);

            if (receiver != null) {
                // Pass back error to surface listener
                final Bundle bundle = new Bundle();
                bundle.putString(Intent.EXTRA_TEXT, e.toString());
                receiver.send(STATUS_ERROR, bundle);
            }
        }

        // Announce success to any surface listener
        Log.d(TAG, "sync finished");
        if (receiver != null) receiver.send(STATUS_FINISHED, Bundle.EMPTY);
	}


	/**
	 * Should we perform a remote sync?
	 */
	private static boolean performRemoteSync(ContentResolver resolver, HttpClient httpClient, Intent intent, Context context) {
	    final SharedPreferences settingsPrefs = context.getSharedPreferences("MixItScheduleSettings", MODE_PRIVATE);
	    final SharedPreferences syncServicePrefs = context.getSharedPreferences(SyncPrefs.MIXITSCHED_SYNC, Context.MODE_PRIVATE);
	    final boolean onlySyncWifi = settingsPrefs.getBoolean(context.getString(R.string.sync_only_wifi_key), false);
	    final int localVersion = syncServicePrefs.getInt(SyncPrefs.LOCAL_VERSION, VERSION_NONE);
	    if (!onlySyncWifi || isWifiConnected(context)) {
	        final boolean remoteParse = localVersion < VERSION_REMOTE;
		    final boolean forceRemoteRefresh = intent.getBooleanExtra(EXTRA_FORCE_REFRESH, false);
		    final boolean hasContentChanged = hasContentChanged(resolver, httpClient);
		    return remoteParse || forceRemoteRefresh || hasContentChanged;
	    }
	    return false;
	}

	/**
	 * Are we connected to a WiFi network?
	 */
	private static boolean isWifiConnected(Context context) {
		final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connectivityManager != null) {
			NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			return (networkInfo != null && networkInfo.getState().equals(NetworkInfo.State.CONNECTED));
		}

		return false;
	}

	/**
	 * Checks for changes to the responses of the Devoxx REST API.
	 */
	private static boolean hasContentChanged(ContentResolver resolver, HttpClient httpClient) {
		for (String url : URLS) {
			if (isContentChanged(resolver, httpClient, url)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if the content of a given url has changed.
	 */
	private static boolean isContentChanged(ContentResolver resolver, HttpClient httpClient, String url) {
		final String localMd5 = SyncUtils.getLocalMd5(resolver, url);
		final String remoteMd5 = SyncUtils.getRemoteMd5(httpClient, url);
		return (remoteMd5 != null && !remoteMd5.equals(localMd5));
	}

	private interface SyncPrefs {
	    String MIXITSCHED_SYNC = "mixitsched_sync";
	    String LOCAL_VERSION = "local_version";
	    String LAST_REMOTE_SYNC = "last_remote_sync";
	}

}

