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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import fr.mixit.android.R;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.ui.widget.SpeakerImageView;
import fr.mixit.android.utils.NotifyingAsyncQueryHandler;
import fr.mixit.android.utils.SyncUtils;
import fr.mixit.android.utils.UIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

import static fr.mixit.android.provider.MixItContract.Speakers.*;

/**
 * {@link TabActivity} to display the related info of {@link fr.mixit.android.provider.MixItContract.Speakers}.
 */
public class SpeakerDetailActivity extends TabActivity implements NotifyingAsyncQueryHandler.AsyncQueryListener {

    private static final String TAG = "SpeakerDetailActivity";

    private static final String TAG_BIO = "bio";
    private static final String TAG_PRESENTATIONS = "presentations";

    private Uri mSpeakerUri;
    private String mSpeakerId;
    
    private TextView mName;
    private TextView mCompany;
    private TextView mBio;
    private SpeakerImageView mImage;

    private File cacheDir;

    private NotifyingAsyncQueryHandler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speaker_detail);
		
		cacheDir = getCacheDir();
		
        ((TextView) findViewById(R.id.title_text)).setText(getTitle());
        
        mName = (TextView) findViewById(R.id.speaker_name);
        mCompany = (TextView) findViewById(R.id.speaker_company);
        mBio = (TextView) findViewById(R.id.speaker_bio);
        mImage = (SpeakerImageView) findViewById(R.id.speaker_image);
        
        final Intent intent = getIntent();
        mSpeakerUri = intent.getData();
        mSpeakerId = getSpeakerId(mSpeakerUri);
        
        setupBioTab();
        setupPresentationsTab();

        mHandler = new NotifyingAsyncQueryHandler(getContentResolver(), this);
        mHandler.startQuery(mSpeakerUri, SpeakersQuery.PROJECTION);
	}

    /** Build and add "bio" tab. */
    private void setupBioTab() {
        final TabHost host = getTabHost();

        // Summary content comes from existing layout
        host.addTab(host.newTabSpec(TAG_BIO)
                .setIndicator(buildIndicator(R.string.speaker_bio))
                .setContent(R.id.tab_speaker_bio));
    }

    /** Build and add "presentations" tab. */
    private void setupPresentationsTab() {
        final TabHost host = getTabHost();
        
        final Intent intent = new Intent(Intent.ACTION_VIEW, buildSessionsDirUri(mSpeakerId));
        intent.addCategory(Intent.CATEGORY_TAB);

        // Sessions content comes from reused activity
        host.addTab(host.newTabSpec(TAG_PRESENTATIONS)
                .setIndicator(buildIndicator(R.string.speaker_sessions))
                .setContent(intent));
    }

    /**
     * Build a {@link View} to be used as a tab indicator, setting the requested
     * string resource as its label.
     */
    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getLayoutInflater().inflate(R.layout.tab_indicator,
                getTabWidget(), false);
        indicator.setText(textRes);
        return indicator;
    }

    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) return;

            mName.setText(cursor.getString(SpeakersQuery.LAST_NAME) + " " + cursor.getString(SpeakersQuery.FIRST_NAME));
            mCompany.setText(cursor.getString(SpeakersQuery.COMPANY));
            mBio.setText(cursor.getString(SpeakersQuery.BIO));

            Bitmap speakerImage = loadImageFromCache();
            if (speakerImage != null) {
            	setSpeakerImage(speakerImage, false);
            } else {
	            final String imageUrl = cursor.getString(SpeakersQuery.IMAGE_URL);
	            new SpeakerImageTask().execute(imageUrl);
            }
        } finally {
            cursor.close();
        }
    }
    
    /** Handle "home" title-bar action. */
    public void onHomeClick(View v) {
        UIUtils.goHome(this);
    }

    /** Handle "search" title-bar action. */
    public void onSearchClick(View v) {
        UIUtils.goSearch(this);
    }
    
    private Bitmap loadImageFromCache() {
    	String speakerId = getSpeakerId(mSpeakerUri);
    	return BitmapFactory.decodeFile(getCacheFile(cacheDir, speakerId).getAbsolutePath());
    }
    
    private void saveImageToCache(Bitmap bitmap) {
    	String speakerId = getSpeakerId(mSpeakerUri);
    	try {
			bitmap.compress(CompressFormat.JPEG, 100, new FileOutputStream(getCacheFile(cacheDir, speakerId)));
		} catch (FileNotFoundException e) {
            Log.w(TAG, "Problem while saving image cache.");
		}
    }
    
    private void setSpeakerImage(Bitmap bitmap, boolean saveToCache) {
    	if (bitmap != null) {
            mImage.setVisibility(View.VISIBLE);
            mImage.setImage(bitmap);
    		if (saveToCache) saveImageToCache(bitmap);
    	}
    	
    }
    
    private File getCacheFile(File cacheDir, String speakerId) {
    	return new File(cacheDir, "speakerImage-" + speakerId);
    }

    private static HttpClient sHttpClient;

    private static synchronized HttpClient getHttpClient(Context context) {
        if (sHttpClient == null) {
            sHttpClient = SyncUtils.getHttpClient(context);
        }
        return sHttpClient;
    }
    
    private class SpeakerImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            final String param = params[0];

            try {
                final Context context = SpeakerDetailActivity.this;
                final HttpClient httpClient = getHttpClient(context);
                final HttpResponse resp = httpClient.execute(new HttpGet(param));
                final HttpEntity entity = resp.getEntity();

                final int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK || entity == null) return null;

                final byte[] respBytes = EntityUtils.toByteArray(entity);
                return BitmapFactory.decodeByteArray(respBytes, 0, respBytes.length);
            } catch (Exception e) {
                Log.w(TAG, "Problem while loading speaker logo: " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
        	setSpeakerImage(result, true);
        }
    }

    /** {@link fr.mixit.android.provider.MixItContract.Speakers} query parameters. */
    private interface SpeakersQuery {
        String[] PROJECTION = {
                MixItContract.Speakers.LAST_NAME,
                MixItContract.Speakers.FIRST_NAME,
                MixItContract.Speakers.BIO,
                MixItContract.Speakers.COMPANY,
                MixItContract.Speakers.IMAGE_URL,
        };

        int FIRST_NAME = 0;
        int LAST_NAME = 1;
        int BIO = 2;
        int COMPANY = 3;
        int IMAGE_URL = 4;
    }

}
