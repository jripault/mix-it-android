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

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import fr.mixit.android.R;

/**
 * {@link android.preference.PreferenceActivity} to handle the application settings.
 */
public class SettingsActivity extends PreferenceActivity {
	
	private static final String TAG = "SettingsActivity";

	public static final String SETTINGS_NAME = "MixItScheduleSettings";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(SETTINGS_NAME);
		preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
		
		addPreferencesFromResource(R.xml.preferences);
	}

}
