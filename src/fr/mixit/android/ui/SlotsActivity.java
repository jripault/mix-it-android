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
 * Modified by Peter Kuterna to support the blocks and block types of the
 * Devoxx Java Community Conference.
 */
package fr.mixit.android.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import fr.mixit.android.R;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.service.StarredSender;
import fr.mixit.android.ui.widget.SlotView;
import fr.mixit.android.ui.widget.SlotsLayout;
import fr.mixit.android.utils.NotifyingAsyncQueryHandler;
import fr.mixit.android.utils.ParserUtils;
import fr.mixit.android.utils.UIUtils;

/**
 * {@link android.app.Activity} that displays a high-level view of a single day of
 * {@link fr.mixit.android.provider.MixItContract.Slots} across the conference. Shows them lined up against a vertical
 * ruler of times across the day.
 */
public class SlotsActivity extends Activity implements NotifyingAsyncQueryHandler.AsyncQueryListener, View.OnClickListener {
	
    private static final String TAG = "SlotsActivity";

    // TODO: these layouts and views are structured pretty weird, ask someone to
    // review them and come up with better organization.

    // TODO: show slots that don't fall into columns at the bottom

    private ScrollView mScrollView;
    private SlotsLayout mSlots;
    private View mNowView;

    private long mTimeStart = -1;
    private long mTimeEnd = -1;

    private NotifyingAsyncQueryHandler mHandler;

    private static final int DISABLED_SLOT_ALPHA = 160;
	private final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slots_content);

	    ((TextView) findViewById(R.id.title_text)).setText(R.string.title_schedule);

        mTimeStart = ParserUtils.parseTime("2011-04-05T08:00:00.000+01:00");
        mTimeEnd = ParserUtils.parseTime("2011-04-05T20:00:00.000+01:00");

        mScrollView = (ScrollView) findViewById(R.id.slots_scroll);
        mSlots = (SlotsLayout) findViewById(R.id.slots);
        mNowView = findViewById(R.id.slots_now);

        mSlots.setDrawingCacheEnabled(true);
        mSlots.setAlwaysDrawnWithCacheEnabled(true);

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Since we build our views manually instead of using an adapter, we
        // need to manually requery every time launched.
        final Uri slotsBetweenDirUri = MixItContract.Slots.buildSlotsBetweenDirUri(mTimeStart, mTimeEnd);
        mHandler.startQuery(slotsBetweenDirUri, SlotsQuery.PROJECTION, MixItContract.Slots.DEFAULT_SORT);

        // Start listening for time updates to adjust "now" bar. TIME_TICK is
        // triggered once per minute, which is how we move the bar over time.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mReceiver, filter, null, new Handler());

        mNowView.post(new Runnable() {
            public void run() {
                updateNowView(true);
            }
        });
	    tracker.trackPageView("/Schedule");

		StarredSender.getInstance().startStarredDispatcher(getApplicationContext());
	}

	protected void onPause() {
		super.onPause();
		StarredSender.getInstance().stop();
        unregisterReceiver(mReceiver);
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        // Clear out any existing sessions before inserting again
        mSlots.removeAllSlots();

        try {
        	while (cursor.moveToNext()) {
                final String type = cursor.getString(SlotsQuery.SLOT_TYPE);
/*		        final Integer column = ParserUtils.sTypeColumnMap.get(type);
		        // TODO: place random slots at bottom of entire layout
		        if (column == null) continue;*/
		        final int column = 0;
		        final Integer color = ParserUtils.sTypeColumnMap.get(type);
		        // TODO: place random slots at bottom of entire layout
		        if (color == null) continue;

                final String slotId = cursor.getString(SlotsQuery.SLOT_ID);
                final String title = cursor.getString(SlotsQuery.SLOT_TYPE);//SLOT_TITLE
                final long start = cursor.getLong(SlotsQuery.SLOT_START);
                final long end = cursor.getLong(SlotsQuery.SLOT_END);
                final boolean containsStarred = cursor.getInt(SlotsQuery.CONTAINS_STARRED) != 0;

                final SlotView slotView = new SlotView(this, slotId, title, start, end,
                        containsStarred, column, color);

                final int sessionsCount = cursor.getInt(SlotsQuery.SESSIONS_COUNT);
                if (sessionsCount > 0) {
                    slotView.setOnClickListener(this);
                } else {
                    slotView.setFocusable(false);
                    slotView.setEnabled(false);
                    LayerDrawable buttonDrawable = (LayerDrawable) slotView.getBackground();
                    buttonDrawable.getDrawable(0).setAlpha(DISABLED_SLOT_ALPHA);
                    buttonDrawable.getDrawable(2).setAlpha(DISABLED_SLOT_ALPHA);
                }

                mSlots.addSlot(slotView);
        	}
        } finally {
            cursor.close();
        }
    }

    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    public void onRefreshClick(View v) {
    }

    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }

    /** {@inheritDoc} */
    public void onClick(View view) {
        if (view instanceof SlotView) {
            final String slotId = ((SlotView) view).getSlotId();
            final Uri sessionsUri = MixItContract.Slots.buildSessionsUri(slotId);
            final Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
//            final String slotTitle = ((SlotView) view).getTitle();
//            intent.putExtra(Intent.EXTRA_TITLE, slotTitle);
            startActivity(intent);
        }
    }

    /**
     * Update position and visibility of "now" view.
     */
    private void updateNowView(boolean forceScroll) {
        final long now = System.currentTimeMillis();

        final boolean visible = now >= mTimeStart && now <= mTimeEnd;
        mNowView.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible && forceScroll) {
            // Scroll to show "now" in center
            final int offset = mScrollView.getHeight() / 2;
            mNowView.requestRectangleOnScreen(new Rect(0, offset, 0, offset), true);
        }

        mSlots.requestLayout();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    private Intent intent;

	    @Override
        public void onReceive(Context context, Intent intent) {
		    this.intent = intent;
		    Log.d(TAG, "onReceive time update");
            updateNowView(false);
        }
    };

    /** {@link fr.mixit.android.provider.MixItContract.Slots} query parameters. */
    private interface SlotsQuery {
        String[] PROJECTION = {
                BaseColumns._ID,
                MixItContract.Slots.SLOT_ID,
//                MixItContract.Slots.SLOT_TITLE,
                MixItContract.Slots.SLOT_START,
                MixItContract.Slots.SLOT_END,
                MixItContract.Slots.SLOT_TYPE,
                MixItContract.Slots.SESSIONS_COUNT,
                MixItContract.Slots.CONTAINS_STARRED,
        };

        int _ID = 0;
        int SLOT_ID = 1;
//        int SLOT_TITLE = 2;
        int SLOT_START = 2;
        int SLOT_END = 3;
        int SLOT_TYPE = 4;
        int SESSIONS_COUNT = 5;
        int CONTAINS_STARRED = 6;
    }
    
}
