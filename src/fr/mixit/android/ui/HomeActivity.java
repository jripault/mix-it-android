package fr.mixit.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;
import fr.mixit.android.R;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.service.SyncService;
import fr.mixit.android.utils.DetachableResultReceiver;

/**
 * Front-door {@link Activity} that displays high-level features the schedule
 * application offers to users.
 */
public class HomeActivity extends Activity implements DetachableResultReceiver.Receiver {

	private static final String TAG = "HomeActivity";

    /** State held between configuration changes. */
	private State mState;

	private View mNowPlayingLoadingView;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_home);

	    mNowPlayingLoadingView = findViewById(R.id.now_playing_loading);

	    mState = (State) getLastNonConfigurationInstance();
	    final boolean previousState = mState != null;

	    if (previousState) {
	        // Start listening for SyncService updates again
	        mState.mReceiver.setReceiver(this);
	        updateRefreshStatus();
//	        reloadNowPlaying(true);
	    } else {
	        mState = new State();
	        mState.mReceiver.setReceiver(this);
	        onRefreshClick(null);
	    }


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
//            reloadNowPlaying(false);
        } else if (mState.mNoResults) {
//            showNowPlayingNoResults();
        }
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

//	    reloadNowPlaying(true);
	}

	/** Handle "search" title-bar action. */
	public void onSearchClick(View v) {
		//TODO implement it
/*	    UIUtils.goSearch(this);*/
	}

	/** Handle "schedule" action. */
	public void onScheduleClick(View v) {
		//TODO implement it
/*	    // Launch overall conference schedule
	    startActivity(new Intent(this, ScheduleActivity.class));*/
	}

	/** Handle "sessions" action. */
	public void onSessionsClick(View v) {
		//TODO implement it
	    // Launch sessions overview activity
	    final Intent intent = new Intent(this, SessionsOverviewActivity.class);
	    startActivity(intent);
	}

	/** Handle "starred" action. */
	public void onStarredClick(View v) {
		//TODO implement it
/*	    // Launch list of sessions user has starred
		startActivity(new Intent(this, StarredActivity.class));*/
	}

	/** Handle "speakers" action. */
	public void onSpeakersClick(View v) {
	    // Launch list of speakers at conference
	    startActivity(new Intent(Intent.ACTION_VIEW, MixItContract.Speakers.CONTENT_URI));
	}

	/** Handle "now playing" action. */
	public void onNowPlayingClick(View v) {
		//TODO implement it
/*	    if (!mState.mNoResults && mState.mNowPlayingUri != null) {
	        startActivity(new Intent(Intent.ACTION_VIEW, mState.mNowPlayingUri));
	    } else if (mState.mNoResults) {
	        Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.setData(Sessions.buildSessionsNextDirUri(System.currentTimeMillis()));
	        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_next_up));
	        intent.putExtra(SessionsActivity.EXTRA_NO_WEEKDAY_HEADER, true);
	        startActivity(intent);
	    }*/
	}

	/** Handle "now playing > more" action. */
	public void onNowPlayingMoreClick(View v) {
		//TODO implement it
/*	    Intent intent = new Intent(Intent.ACTION_VIEW);
	    intent.setData(Sessions.buildSessionsAtDirUri(System.currentTimeMillis()));
	    intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.title_now_playing));
	    intent.putExtra(SessionsActivity.EXTRA_NO_WEEKDAY_HEADER, true);
	    startActivity(intent);*/
	}

	private void updateRefreshStatus() {
	    findViewById(R.id.btn_title_refresh).setVisibility(
	            mState.mSyncing ? View.GONE : View.VISIBLE);
	    findViewById(R.id.title_refresh_progress).setVisibility(
	            mState.mSyncing ? View.VISIBLE : View.GONE);
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
//	            reloadNowPlaying(mState.mNowPlayingUri == null);
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


}