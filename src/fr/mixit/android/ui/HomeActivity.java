package fr.mixit.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import fr.mixit.android.R;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.service.StarredSender;
import fr.mixit.android.service.SyncService;
import fr.mixit.android.utils.DetachableResultReceiver;
import fr.mixit.android.utils.NotifyingAsyncQueryHandler;
import fr.mixit.android.utils.UIUtils;

import java.util.Random;

/**
 * Front-door {@link Activity} that displays high-level features the schedule
 * application offers to users.
 */
public class HomeActivity extends Activity implements NotifyingAsyncQueryHandler.AsyncQueryListener, DetachableResultReceiver.Receiver {

	private static final String TAG = "HomeActivity";

    /** State held between configuration changes. */
	private State mState;

	private Handler mMessageHandler = new Handler();
	private Random random = new Random();
	private NotifyingAsyncQueryHandler mQueryHandler;

	private TextView mCountdownTextView;
	private View mNowPlayingLoadingView;

	private GoogleAnalyticsTracker tracker;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_home);

	    mNowPlayingLoadingView = findViewById(R.id.now_playing_loading);

	    mState = (State) getLastNonConfigurationInstance();
	    final boolean previousState = mState != null;

        // Set up handler for now playing session query.
        mQueryHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);

	    if (previousState) {
	        // Start listening for SyncService updates again
	        mState.mReceiver.setReceiver(this);
	        updateRefreshStatus();
	        reloadNowPlaying(true);
	    } else {
	        mState = new State();
	        mState.mReceiver.setReceiver(this);
	        onRefreshClick(null);
	    }

	    StarredSender.getInstance().startStarredDispatcher(getApplicationContext());

        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.start("UA-8046496-4", 30, this);
    }

	@Override
    public Object onRetainNonConfigurationInstance() {
        // Clear any strong references to this Activity, we'll reattach to
        // handle events on the other side.
        mState.mReceiver.clearReceiver();
        return mState;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mState.mNowPlayingUri != null) {
            reloadNowPlaying(false);
        } else if (mState.mNoResults) {
            showNowPlayingNoResults();
        }

        tracker.trackPageView("/Home");
    }

	protected void onDestroy() {
		super.onDestroy();
		tracker.stop();
		StarredSender.getInstance().stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);
	    getMenuInflater().inflate(R.menu.options_menu_home, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
		    case R.id.menu_settings:
			    UIUtils.goSettings(this);
			    return true;
	        case R.id.menu_about:
	            UIUtils.goAbout(this);
	            return true;
	    }
	    return false;
	}

	/** Handle "refresh" title-bar action. */
	public void onRefreshClick(View v) {
	    // trigger off background sync
	    final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, SyncService.class);
	    intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, mState.mReceiver);
	    if (v != null) {
	        intent.putExtra(SyncService.EXTRA_FORCE_REFRESH, true);
	    }
	    startService(intent);

	    reloadNowPlaying(true);
	}

	/** Handle "search" title-bar action. */
	public void onSearchClick(View v) {
	    UIUtils.goSearch(this);
	}

	/** Handle "schedule" action. */
	public void onScheduleClick(View v) {
		//TODO implement it
	    // Launch overall conference schedule
	    startActivity(new Intent(this, SlotsActivity.class));
	}

	/** Handle "sessions" action. */
	public void onSessionsClick(View v) {
	    // Launch sessions overview activity
	    final Intent intent = new Intent(this, SessionsOverviewActivity.class);
	    startActivity(intent);
	}

	/** Handle "starred" action. */
	public void onStarredClick(View v) {
	    // Launch list of sessions user has starred
		startActivity(new Intent(this, StarredActivity.class));
	}

	/** Handle "speakers" action. */
	public void onSpeakersClick(View v) {
	    // Launch list of speakers at conference
	    startActivity(new Intent(Intent.ACTION_VIEW, MixItContract.Speakers.CONTENT_URI));
	}

	/** Handle "now playing" action. */
	public void onNowPlayingClick(View v) {
	    if (!mState.mNoResults && mState.mNowPlayingUri != null) {
	        startActivity(new Intent(Intent.ACTION_VIEW, mState.mNowPlayingUri));
	    } else if (mState.mNoResults) {
	        Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.setData(MixItContract.Sessions.buildSessionsNextDirUri(System.currentTimeMillis()));
	        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_next_up));
	        startActivity(intent);
	    }
	}

	/** Handle "now playing > more" action. */
	public void onNowPlayingMoreClick(View v) {
	    Intent intent = new Intent(Intent.ACTION_VIEW);
	    intent.setData(MixItContract.Sessions.buildSessionsAtDirUri(System.currentTimeMillis()));
	    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_now_playing));
	    startActivity(intent);
	}

	private void reloadNowPlaying(boolean forceRelocate) {
	    mMessageHandler.removeCallbacks(mCountdownRunnable);

	    final long currentTimeMillis = System.currentTimeMillis();

	    if (mNowPlayingLoadingView == null) // Landscape orientation
	        return;

	    ViewGroup homeRoot = (ViewGroup) findViewById(R.id.home_root);
	    View nowPlaying = findViewById(R.id.now_playing);
	    if (nowPlaying != null) {
	        homeRoot.removeView(nowPlaying);
	        nowPlaying = null;
	    }

	    // Show Loading... and load the view corresponding to the current state
	    if (forceRelocate) mNowPlayingLoadingView.setVisibility(View.VISIBLE);
	    mState.mNoResults = false;
	    if (currentTimeMillis < UIUtils.CONFERENCE_START_MILLIS) {
	        nowPlaying = createNowPlayingBeforeView();
	    } else if (currentTimeMillis > UIUtils.CONFERENCE_END_MILLIS) {
	        nowPlaying = createNowPlayingAfterView();
	    } else {
	        nowPlaying = createNowPlayingDuringView(forceRelocate);
	    }

	    homeRoot.addView(nowPlaying, new ViewGroup.LayoutParams(
	            ViewGroup.LayoutParams.FILL_PARENT,
	            (int) getResources().getDimension(R.dimen.now_playing_height)));
	}

	private View createNowPlayingBeforeView() {
	    // Before conference, show countdown.
	    final View nowPlaying = getLayoutInflater().inflate(R.layout.now_playing_before, null);
	    final TextView nowPlayingTitle = (TextView) nowPlaying.findViewById(R.id.now_playing_title);

	    mCountdownTextView = nowPlayingTitle;
	    mMessageHandler.post(mCountdownRunnable);
	    mNowPlayingLoadingView.setVisibility(View.GONE);
	    nowPlaying.setVisibility(View.VISIBLE);
	    return nowPlaying;
	}

	private View createNowPlayingAfterView() {
	    // After conference, show canned text.
	    final View nowPlaying = getLayoutInflater().inflate(R.layout.now_playing_after, null);
	    mNowPlayingLoadingView.setVisibility(View.GONE);
	    nowPlaying.setVisibility(View.VISIBLE);
	    return nowPlaying;
	}

	private View createNowPlayingDuringView(boolean forceRelocate) {
	    // Conference in progress, show now playing.
	    final View nowPlaying = getLayoutInflater().inflate(R.layout.now_playing_during, null);
	    if (forceRelocate) nowPlaying.setVisibility(View.GONE);
	    mQueryHandler.startQuery(MixItContract.Sessions.buildSessionsAtDirUri(System.currentTimeMillis()), SessionsQuery.PROJECTION);
	    return nowPlaying;
	}

	/**
	 * Event that updates countdown timer. Posts itself again to
	 * {@link #mMessageHandler} to continue updating time.
	 */
	private Runnable mCountdownRunnable = new Runnable() {
	    public void run() {
	        int remainingSec = (int) Math.max(0,
	                (UIUtils.CONFERENCE_START_MILLIS - System.currentTimeMillis()) / 1000);
	        final boolean conferenceStarted = remainingSec == 0;

	        if (conferenceStarted) {
	            // Conference started while in countdown mode, switch modes and
	            // bail on future countdown updates.
	            mMessageHandler.postDelayed(new Runnable() {
	                public void run() {
	                    reloadNowPlaying(true);
	                }
	            }, 100);
	            return;
	        }

	        final int secs = remainingSec % 86400;
	        final int days = remainingSec / 86400;
	        final String str = getResources().getQuantityString(
	                R.plurals.now_playing_countdown, days, days,
	                DateUtils.formatElapsedTime(secs));
	        mCountdownTextView.setText(str);

	        // Repost ourselves to keep updating countdown
	        mMessageHandler.postDelayed(mCountdownRunnable, 1000);
	    }
	};

	private void showNowPlayingNoResults() {
	    mState.mNoResults = true;
	    runOnUiThread(new Runnable() {
	        public void run() {
	            final View loadingView = findViewById(R.id.now_playing_loading);
	            if (loadingView == null) return;

	            loadingView.setVisibility(View.GONE);
	            findViewById(R.id.now_playing).setVisibility(View.VISIBLE);
	            ((TextView) findViewById(R.id.now_playing_title)).setText(
	                    R.string.now_playing_no_results);
	            ((TextView) findViewById(R.id.now_playing_subtitle)).setText(
	                    R.string.now_playing_next_up);
	            findViewById(R.id.separator_now_playing_more).setVisibility(View.GONE);
	            findViewById(R.id.now_playing_more).setVisibility(View.GONE);
	        }
	    });
	}

	/** {@inheritDoc} */
	public void onQueryComplete(int token, Object cookie, Cursor cursor) {
	    try {
	        if (!cursor.moveToFirst()) {
	            showNowPlayingNoResults();
	            return;
	        }

	        int position = random.nextInt(cursor.getCount());
	        cursor.moveToPosition(position);

	        mState.mNowPlayingUri = MixItContract.Sessions.buildSessionUri(cursor.getString(SessionsQuery.SESSION_ID));

	        // Format time block this session occupies
	        final long slotStart = cursor.getLong(SessionsQuery.SLOT_START);
	        final long slotEnd = cursor.getLong(SessionsQuery.SLOT_END);

	        final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
	        final String subtitle = UIUtils.formatSessionSubtitle(slotStart, slotEnd, roomName, this);

	        findViewById(R.id.now_playing_loading).setVisibility(View.GONE);
	        findViewById(R.id.now_playing).setVisibility(View.VISIBLE);
	        ((TextView) findViewById(R.id.now_playing_title)).setText(cursor.getString(SessionsQuery.TITLE));
	        ((TextView) findViewById(R.id.now_playing_subtitle)).setText(subtitle);
	    } finally {
	        cursor.close();
	    }
	}

	private void updateRefreshStatus() {
	    findViewById(R.id.btn_title_refresh).setVisibility(mState.mSyncing ? View.GONE : View.VISIBLE);
	    findViewById(R.id.title_refresh_progress).setVisibility(mState.mSyncing ? View.VISIBLE : View.GONE);
	}

	@Override
	/** {@inheritDoc} */
	public void onReceiveResult(int resultCode, Bundle resultData) {
	    switch (resultCode) {
	        case SyncService.STATUS_RUNNING: {
	            mState.mSyncing = true;
	            updateRefreshStatus();
	            break;
	        }
	        case SyncService.STATUS_FINISHED: {
	            mState.mSyncing = false;
	            updateRefreshStatus();
	            reloadNowPlaying(mState.mNowPlayingUri == null);
	            break;
	        }
	        case SyncService.STATUS_ERROR: {
	            // Error happened down in SyncService, show as toast.
	            mState.mSyncing = false;
	            updateRefreshStatus();
	            final String errorText = getString(R.string.toast_sync_error, resultData
	                    .getString(Intent.EXTRA_TEXT));
	            Toast.makeText(HomeActivity.this, errorText, Toast.LENGTH_LONG).show();
	            break;
	        }
	    }
	}


	/**
	 * State specific to {@link HomeActivity} that is held between configuration
	 * changes. Any strong {@link Activity} references <strong>must</strong> be
	 * cleared before {@link #onRetainNonConfigurationInstance()}, and this
	 * class should remain {@code static class}.
	 */
	private static class State {
	    public DetachableResultReceiver mReceiver;
	    public Uri mNowPlayingUri = null;
	    public boolean mSyncing = false;
	    public boolean mNoResults = false;

	    private State() {
	        mReceiver = new DetachableResultReceiver(new Handler());
	    }

	}

	/** {@link fr.mixit.android.provider.MixItContract.Sessions} query parameters. */
	private interface SessionsQuery {
	    String[] PROJECTION = {
	            MixItContract.Slots.SLOT_START,
	            MixItContract.Slots.SLOT_END,
	            MixItContract.Sessions.SESSION_ID,
	            MixItContract.Sessions.TITLE,
	            MixItContract.Sessions.ROOM,
	    };

	    int SLOT_START = 0;
	    int SLOT_END = 1;
	    int SESSION_ID = 2;
	    int TITLE = 3;
	    int ROOM_NAME = 4;
	}

}