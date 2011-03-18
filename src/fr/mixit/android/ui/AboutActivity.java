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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import fr.mixit.android.R;
import fr.mixit.android.service.StarredSender;
import fr.mixit.android.utils.UIUtils;

/**
 * {@link android.app.Activity} that displays an about screen.
 */
public class AboutActivity extends Activity {

	private static final String TAG = "AboutActivity";
	private final GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		((TextView) findViewById(R.id.title_text)).setText(getTitle());
		
		final TextView version = (TextView) findViewById(R.id.about_version);
		version.setText(getString(R.string.about_text5, UIUtils.getAppVersionName(this, getPackageName())));
	}

	protected void onResume() {
		super.onResume();
		tracker.trackPageView("/About");

		StarredSender.getInstance().startStarredDispatcher(getApplicationContext());
	}

	protected void onPause() {
		super.onPause();
		StarredSender.getInstance().stop();
	}

    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

}
