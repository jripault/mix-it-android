package fr.mixit.android.service;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import fr.mixit.android.Constants;
import fr.mixit.android.model.SessionStarred;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class NetworkDispatcher implements Dispatcher {

	private static final String TAG = "StarredNetworkDispatcher";
	private static final boolean mDebugMode = true;

	private static final int MAX_EVENTS_PER_PIPELINE = 30;
	private static final int MAX_SEQUENTIAL_REQUESTS = 5;
	private static final long MIN_RETRY_INTERVAL = 2L;

	private static DispatcherThread.RequesterCallbacks mRequesterCallbacks;

	private static class DispatcherThread extends HandlerThread {
		private class RequesterCallbacks {

			final DispatcherThread mDispatcherThread;

			public void requestSent() {
				if (currentTask == null) {
					return;
				}
				SessionStarred sessionStarred = currentTask.removeNextSessionStarred();
				if (sessionStarred != null) {
					callbacks.eventDispatched(sessionStarred.getEventId());
				}
			}

			public void serverError(int i) {
				lastStatusCode = i;
			}

			private RequesterCallbacks() {
				mDispatcherThread = DispatcherThread.this;
			}

		}

		private class AsyncDispatchTask implements Runnable {

			private final LinkedList<SessionStarred> sessionStarreds = new LinkedList<SessionStarred>();

			public void run() {
				currentTask = this;
				int i = 0;
				do {
					if (i >= MAX_SEQUENTIAL_REQUESTS || sessionStarreds.size() <= 0) {
						break;
					}
					try {
						if (mDebugMode)
							Log.d(TAG, "AsyncDispatchTask running");

						long l = 0L;
						if (lastStatusCode == 500 || lastStatusCode == 503) {
							l = (long) (Math.random() * (double) retryInterval);
							if (retryInterval < 256L) {
								retryInterval *= MIN_RETRY_INTERVAL;
							}
						} else {
							retryInterval = MIN_RETRY_INTERVAL;
						}
						Thread.sleep(l * 1000L);
						dispatchSomePendingEvents();
					} catch (InterruptedException interruptedexception) {
						Log.e(TAG, "Couldn't sleep.", interruptedexception);
						break;
					} catch (IOException ioexception) {
						Log.e(TAG, "Problem with socket or streams.", ioexception);
						break;
					} catch (HttpException httpexception) {
						Log.e(TAG, "Problem with http streams.", httpexception);
						break;
					}
					i++;
				} while (true);

				if (mDebugMode)
					Log.d(TAG, "AsyncDispatchTask stopped");

				callbacks.dispatchFinished();
				currentTask = null;
			}

			private void dispatchSomePendingEvents() throws IOException, ParseException, HttpException {

				int lastStatusCode;

				for (int i = 0; i < sessionStarreds.size() && i < maxEventsPerRequest; i++) {
					SessionStarred sessionStarred = sessionStarreds.get(0);

					if (mDebugMode)
						Log.d(TAG, "trying to send request[" + sessionStarred.getIdSession() + "] :" + sessionStarred.isSessionStarred());

					// Create a new HttpClient and Post Header
					HttpClient httpClient = new DefaultHttpClient();
					final StringBuilder str = new StringBuilder(Constants.STARRED_URL);
					str.append("sessionId=");
					str.append(sessionStarred.getIdSession());
					str.append("&starred=");
					str.append(sessionStarred.isSessionStarred() ? "true" : "false");
					str.append("&login=");
					str.append(StarredSender.getUserId());
					HttpGet httpGet = new HttpGet(str.toString());

					try {
						// Execute HTTP Post Request
						HttpResponse response = httpClient.execute(httpGet);

						lastStatusCode = response.getStatusLine().getStatusCode();

						if (lastStatusCode == 200) {
							mRequesterCallbacks.requestSent();
							if (mDebugMode)
								Log.d(TAG, "Request sent:" + str);

						} else {
							mRequesterCallbacks.serverError(lastStatusCode);
							Log.e(TAG, "error sending request, error code:" + lastStatusCode);
						}
					} catch (ClientProtocolException e) {
						Log.e(TAG, "error sending request", e);
					} catch (IOException e) {
						Log.e(TAG, "error sending request", e);
					}
				}
			}

			public SessionStarred removeNextSessionStarred() {
				return sessionStarreds.poll();
			}

			public AsyncDispatchTask(SessionStarred sessionStarred[]) {
				Collections.addAll(sessionStarreds, sessionStarred);
			}
		}

		private Handler handlerExecuteOnDispatcherThread;
		private int lastStatusCode;
		private int maxEventsPerRequest;
		private long retryInterval;
		private AsyncDispatchTask currentTask;
		private final Dispatcher.Callbacks callbacks;

		protected void onLooperPrepared() {
			handlerExecuteOnDispatcherThread = new Handler();
		}

		public void dispatchEvents(SessionStarred sessionStarred[]) {
			if (handlerExecuteOnDispatcherThread != null) {
				handlerExecuteOnDispatcherThread.post(new AsyncDispatchTask(sessionStarred));
			}
		}

		private DispatcherThread(Dispatcher.Callbacks callbacks1) {
			super("DispatcherThread");
			maxEventsPerRequest = MAX_EVENTS_PER_PIPELINE;
			currentTask = null;
			callbacks = callbacks1;
			mRequesterCallbacks = new RequesterCallbacks();
		}
	}

	private DispatcherThread dispatcherThread;

	public NetworkDispatcher() {
		super();
	}

	public void dispatchSessions(SessionStarred sessionsStarred[]) {
		if (dispatcherThread != null) {
			dispatcherThread.dispatchEvents(sessionsStarred);
		}
	}

	public void waitForThreadLooper() {
		dispatcherThread.getLooper();
	}

	public void stop() {
		if (dispatcherThread != null && dispatcherThread.getLooper() != null) {
			dispatcherThread.getLooper().quit();
			dispatcherThread = null;
		}
	}

	@Override
	public void init(Callbacks callbacks) {
		stop();
		dispatcherThread = new DispatcherThread(callbacks);
		dispatcherThread.start();

	}
}
