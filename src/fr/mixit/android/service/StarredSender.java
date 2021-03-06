package fr.mixit.android.service;

import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import fr.mixit.android.R;
import fr.mixit.android.model.SessionStarred;
import fr.mixit.android.ui.SettingsActivity;

public class StarredSender {

	private static final boolean mDebugMode = false;
	private static final String TAG = "StarredSender";

	private Handler handler;
	final class DispatcherCallbacks implements Dispatcher.Callbacks {

		public void dispatchFinished() {
			handler.post(new Runnable() {
				public void run() {
					StarredSender.this.dispatchFinished();
				}
			});
		}

		public void eventDispatched(long l) {
			if (starredStore == null)
				starredStore = new PersistentStarredStore(new PersistentStarredStore.DataBaseHelper(mContext));
			starredStore.deleteSessionStarred(l);
			starredStore.closeDataBase();
			starredStore = null;
		}

		DispatcherCallbacks() {
		}
	}

	private static StarredSender mStarredSender;
	private Context mContext;
	private ConnectivityManager connectivityManager;
	private int dispatchPeriod;
	private StarredStore starredStore;
	private Dispatcher dispatcher;
	private boolean powerSaveMode;
	private boolean dispatcherIsBusy;
	private Runnable dispatchRunner;
	private static String mUserId;
	private static int DISPATCH_PERIOD = 15;
	private boolean isStopped = true;

	private StarredSender() {
	    super();

		dispatchRunner = new Runnable() {

			final StarredSender starredSender = StarredSender.this;

			public void run() {
				starredSender.dispatch();
			}
		};
	}

	public static StarredSender getInstance() {
		if (mStarredSender == null) {
			mStarredSender = new StarredSender();
		}
		return mStarredSender;
	}

	public void startStarredDispatcher(Context context) {

		if (mDebugMode)
			Log.d(TAG, "startStarredDispatcher(Context context)");

		if (isStopped) {
			mContext = context;

			dispatcher = new NetworkDispatcher();

			final Dispatcher.Callbacks callbacks = new DispatcherCallbacks();

			dispatcher.init(callbacks);
			dispatcherIsBusy = false;
			if (connectivityManager == null) {
				connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			}
			if (handler == null) {
				handler = new Handler(context.getMainLooper());
			} else {
				cancelPendingDispatches();
			}
			setDispatchPeriod(DISPATCH_PERIOD);

			mUserId = initUserId();

			if (handler == null) {
				handler = new Handler(mContext.getMainLooper());
			}
		}
	}

	public String initUserId() {
		// Get userId
		String userId = Settings.Secure.getString(mContext.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

		// if used on emulator, return null, patch
		if (userId == null)
			userId = "EmulatorUserId";

		// TODO : make a hash -> it seems it's already hashed
		return userId;
	}

	public static String getUserId() {
		return mUserId;
	}

	public void newSessionStarred(int sessionId, boolean state) {
		if (mDebugMode) {
			Log.d(TAG, "newSessionStarred(): sessionId : " + sessionId + " at state : " + state);
		}

		if (starredStore == null)
			starredStore = new PersistentStarredStore(new PersistentStarredStore.DataBaseHelper(mContext));
		starredStore.putSessionStarred(new SessionStarred(sessionId, state));
		starredStore.closeDataBase();
		starredStore = null;
		resetPowerSaveMode();
	}

	Dispatcher getDispatcher() {
		return dispatcher;
	}

	public void setDispatchPeriod(int i) {
		int j = dispatchPeriod;
		dispatchPeriod = i;
		if (j <= 0) {
			maybeScheduleNextDispatch();
		} else if (j > 0) {
			cancelPendingDispatches();
			maybeScheduleNextDispatch();
		}
	}

	private void maybeScheduleNextDispatch() {
		if (dispatchPeriod < 0) {
			if (mDebugMode)
				Log.d(TAG, "maybeScheduleNextDispatch() dispatchPeriod < 0");

			return;
		} else {
			if (mDebugMode)
				Log.d(TAG, "maybeScheduleNextDispatch() dispatchPeriod >= 0");
			if (!handler.postDelayed(dispatchRunner, dispatchPeriod * 1000) && mDebugMode) {
				Log.d(TAG, "maybeScheduleNextDispatch() impossible to reschedule the dispatcher");
			}
			return;
		}
	}

	private void cancelPendingDispatches() {
		handler.removeCallbacks(dispatchRunner);
	}

	private void resetPowerSaveMode() {
		if (powerSaveMode) {
			powerSaveMode = false;
			maybeScheduleNextDispatch();
		}
	}

	public boolean dispatch() {

		if (mDebugMode)
			Log.d(TAG, "dispatch() begin");

		if (dispatcherIsBusy) {
			if (mDebugMode)
				Log.d(TAG, "dispatcher busy");
			maybeScheduleNextDispatch();
			return false;
		}
		try {
			if (!performStarredSync(mContext)) {
				if (mDebugMode)
					Log.d(TAG, "Network is not available at this moment");
				maybeScheduleNextDispatch();
				return false;
			}

			if (starredStore == null)
				starredStore = new PersistentStarredStore(new PersistentStarredStore.DataBaseHelper(mContext));

			if (starredStore.getNumStoredSessionStarreds() != 0) {

				if (mDebugMode)
					Log.d(TAG, "Requests to send found, dispatch");

				SessionStarred starredSessions[] = starredStore.peekSessionStarreds();
				starredStore.closeDataBase();
				starredStore = null;
				dispatcher.dispatchSessions(starredSessions);
				dispatcherIsBusy = true;
				maybeScheduleNextDispatch();
				return true;
			} else {
				starredStore.closeDataBase();
				starredStore = null;
				if (mDebugMode)
					Log.d(TAG, "No request to send");

				powerSaveMode = true;
				return false;
			}
		} catch (SecurityException e) {
			Log.e(TAG, "Security exception", e);
			return false;
		}
	}

	void dispatchFinished() {
		dispatcherIsBusy = false;
	}

	public void stop() {
		if (!isStopped) {
			dispatcher.stop();
			cancelPendingDispatches();
//			starredStore.closeDataBase();
//			starredStore = null;
			isStopped = true;
		}
	}

	private static boolean performStarredSync(Context context) {
	    final SharedPreferences settingsPrefs = context.getSharedPreferences(SettingsActivity.SETTINGS_NAME, Context.MODE_PRIVATE);
	    final boolean onlySyncWifi = settingsPrefs.getBoolean(context.getString(R.string.sync_only_wifi_key), false);
		final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (connectivityManager !=null) {
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if (!onlySyncWifi && networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
				return true;
			}
			else {
				networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				return (networkInfo != null && networkInfo.getState().equals(NetworkInfo.State.CONNECTED));
			}
		}

	    return false;
	}

}
