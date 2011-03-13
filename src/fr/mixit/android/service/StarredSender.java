package fr.mixit.android.service;

import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import fr.mixit.android.model.SessionStarred;

public class StarredSender {

	private static final boolean mDebugMode = true;
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
			starredStore.deleteSessionStarred(l);
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

		mContext = context;

		starredStore = new PersistentStarredStore(new PersistentStarredStore.DataBaseHelper(context));

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
		starredStore.putSessionStarred(new SessionStarred(sessionId, state));
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
			NetworkInfo networkinfo = connectivityManager.getActiveNetworkInfo();
			if (networkinfo == null || !networkinfo.isAvailable()) {
				if (mDebugMode)
					Log.d(TAG, "Network is not available at this moment");
				maybeScheduleNextDispatch();
				return false;
			}
			if (starredStore.getNumStoredSessionStarreds() != 0) {

				if (mDebugMode)
					Log.d(TAG, "Requests to send found, dispatch");

				SessionStarred starredSessions[] = starredStore.peekSessionStarreds();
				dispatcher.dispatchSessions(starredSessions);
				dispatcherIsBusy = true;
				maybeScheduleNextDispatch();
				return true;
			} else {
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
		dispatcher.stop();
		cancelPendingDispatches();
	}

}
