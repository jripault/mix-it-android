/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Modified by Peter Kuterna to support the Devoxx conference.
 */
package fr.mixit.android.ui;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import fr.mixit.android.R;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.service.StarredSender;
import fr.mixit.android.utils.NotifyingAsyncQueryHandler;
import fr.mixit.android.utils.UIUtils;

/**
 * {@link android.app.ListActivity} that displays a set of {@link fr.mixit.android.provider.MixItContract.Tracks}, as requested
 * through {@link android.content.Intent#getData()}.
 */
public class TracksActivity extends ListActivity implements NotifyingAsyncQueryHandler.AsyncQueryListener {

    private TracksAdapter mAdapter;

    private NotifyingAsyncQueryHandler mHandler;
	private final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!getIntent().hasCategory(Intent.CATEGORY_TAB)) {
            setContentView(R.layout.activity_tracks);

            final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
            ((TextView) findViewById(R.id.title_text)).setText(
                    customTitle != null ? customTitle : getTitle());
        } else {
            setContentView(R.layout.activity_tracks_content);
        }

        mAdapter = new TracksAdapter(this);
        setListAdapter(mAdapter);

        final Intent intent = getIntent();
        final Uri tracksUri = intent.getData();

        // Start background query to load tracks
        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(tracksUri, TracksQuery.PROJECTION_WITH_SESSIONS_COUNT, MixItContract.Tracks.SESSIONS_COUNT + ">0", null, MixItContract.Tracks.DEFAULT_SORT);
    }

	protected void onResume() {
		super.onResume();
		tracker.trackPageView("/TracksList");

		StarredSender.getInstance().startStarredDispatcher(getApplicationContext());
	}

	protected void onPause() {
		super.onPause();
		StarredSender.getInstance().stop();
	}

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        startManagingCursor(cursor);
        mAdapter.changeCursor(cursor);
    }

    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    /** {@inheritDoc} */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific track
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String trackId = cursor.getString(TracksQuery.TRACK_ID);
        final String trackTitle = cursor.getString(TracksQuery.TRACK_NAME);
        final int trackColor = cursor.getInt(TracksQuery.TRACK_COLOR);
        final Uri sessionUri = MixItContract.Tracks.buildSessionsUri(trackId)
        	.buildUpon()
        	.appendQueryParameter(MixItContract.SessionCounts.SESSION_INDEX_EXTRAS, Boolean.TRUE.toString())
        	.build();
        final Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
        intent.putExtra(Intent.EXTRA_TITLE, trackTitle);
        intent.putExtra(SessionsActivity.EXTRA_TRACK_COLOR, trackColor);
        intent.putExtra(SessionsActivity.EXTRA_FOCUS_CURRENT_NEXT_SESSION, true);
        startActivity(intent);
    }

    /**
     * {@link android.widget.CursorAdapter} that renders a {@link TracksQuery}.
     */
    private class TracksAdapter extends CursorAdapter {
        public TracksAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getLayoutInflater().inflate(R.layout.list_item_track, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setText(cursor.getString(TracksQuery.TRACK_NAME));

            // Assign track color to visible block
            final TextView countView = (TextView) view.findViewById(android.R.id.text2);
            countView.setText(cursor.getString(TracksQuery.SESSION_COUNT));
            NinePatchDrawable drawable = (NinePatchDrawable) countView.getBackground();
            drawable.setColorFilter(new LightingColorFilter(cursor.getInt(TracksQuery.TRACK_COLOR), 1));
        }
    }

    /** {@link fr.mixit.android.provider.MixItContract.Tracks} query parameters. */
    private interface TracksQuery {
        String[] PROJECTION_WITH_SESSIONS_COUNT = {
                BaseColumns._ID,
                MixItContract.Tracks.TRACK_ID,
                MixItContract.Tracks.TRACK_NAME,
                MixItContract.Tracks.TRACK_COLOR,
                MixItContract.Tracks.SESSIONS_COUNT,
        };

        int _ID = 0;
        int TRACK_ID = 1;
        int TRACK_NAME = 2;
        int TRACK_COLOR = 3;
        int SESSION_COUNT = 4;
    }
}
