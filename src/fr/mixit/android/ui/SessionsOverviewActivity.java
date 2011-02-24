/*
 * Copyright 2010 Peter Kuterna
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

package fr.mixit.android.ui;

import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import fr.mixit.android.R;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.utils.UIUtils;

public class SessionsOverviewActivity extends TabActivity {

	private static final String TAG = "SessionsOverviewActivity";

    public static final String TAG_TRACK = "track";
    public static final String TAG_ALL = "all";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions_overview);

        ((TextView) findViewById(R.id.title_text)).setText(getTitle());
        
        setupTrackTab();
        setupAllTab();
    }

    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }
    
    /** Build and add "track" tab. */
    private void setupTrackTab() {
        final TabHost host = getTabHost();

        final Intent intent = new Intent(Intent.ACTION_VIEW, MixItContract.Tracks.CONTENT_URI);
        intent.addCategory(Intent.CATEGORY_TAB);

        host.addTab(host.newTabSpec(TAG_TRACK)
                .setIndicator(buildIndicator(R.string.sessions_track))
                .setContent(intent));
    }

    /** Build and add "all" tab. */
    private void setupAllTab() {
        final TabHost host = getTabHost();

        final Uri uri = MixItContract.Sessions.CONTENT_URI
        	.buildUpon()
        	.appendQueryParameter(MixItContract.SessionCounts.SESSION_INDEX_EXTRAS, Boolean.TRUE.toString())
        	.build();
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_TAB);
        intent.putExtra(SessionsActivity.EXTRA_FOCUS_CURRENT_NEXT_SESSION, true);

        host.addTab(host.newTabSpec(TAG_ALL)
                .setIndicator(buildIndicator(R.string.sessions_all))
                .setContent(intent));
    }
    
    /**
     * Build a {@link android.view.View} to be used as a tab indicator, setting the requested
     * string resource as its label.
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
                getTabWidget(), false);
        indicator.setText(textRes);
        return indicator;
    }
    
}
