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

package fr.mixit.android.io;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.utils.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;


/**
 * Handle a remote {@link org.json.JSONArray} that defines a set of {@link MixItContract.Speakers}
 * entries.
 */
public class RemoteSpeakersHandler extends JSONHandler {

    private static final String TAG = "SpeakersHandler";

    public RemoteSpeakersHandler() {
		super(MixItContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(ArrayList<JSONArray> entries,
			ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> speakerIds = new HashSet<String>();
		
		int nrEntries = 0;
		for (JSONArray speakers : entries) {
			Log.d(TAG, "Retrieved " + speakers.length() + " speaker entries.");
			nrEntries += speakers.length();
	
	        for (int i=0; i < speakers.length(); i++) {
	            JSONObject speaker = speakers.getJSONObject(i);
		        String id = speaker.getString("id");

//		        final String speakerId = sanitizeId(id);
		        final Uri speakerUri = MixItContract.Speakers.buildSpeakerUri(id);
		        speakerIds.add(id);

		        boolean speakerUpdated = false;
		        boolean newSpeaker = false;
		        boolean build = false;
		        ContentProviderOperation.Builder builder;
		        if (isRowExisting(speakerUri, SpeakersQuery.PROJECTION, resolver)) {
			        builder = ContentProviderOperation.newUpdate(speakerUri);
			        speakerUpdated = isSpeakerUpdated(speakerUri, speaker, resolver);
		        } else {
			        newSpeaker = true;
			        builder = ContentProviderOperation.newInsert(MixItContract.Speakers.CONTENT_URI);
					builder.withValue(MixItContract.Speakers.SPEAKER_ID, id);
					build = true;
		        }

		        if (newSpeaker || speakerUpdated) {
			        builder.withValue(MixItContract.Speakers.LAST_NAME, speaker.getString("lastName"));
			        builder.withValue(MixItContract.Speakers.FIRST_NAME, speaker.getString("firstName"));
					builder.withValue(MixItContract.Speakers.BIO, speaker.getString("bio"));
//		        String twitter = speaker.getString("twitter");
//		        String linkedin = speaker.getString("linkedin");
//		        String blog = speaker.getString("blog");
					builder.withValue(MixItContract.Speakers.COMPANY, speaker.getString("company"));
					builder.withValue(MixItContract.Speakers.IMAGE_URL, speaker.getString("urlImage"));
					build = true;
		        }
		        if (build) batch.add(builder.build());
	        }
		}

		// delete sessions with speakers deleted
/*        if (isRemoteSync() && nrEntries > 0) {
		    for (String lostId : getLostIds(speakerIds, MixItContract.Speakers.CONTENT_URI, SpeakersQuery.PROJECTION, SpeakersQuery.SPEAKER_ID, resolver)) {
		    	Uri deleteUri = MixItContract.Speakers.buildSessionsDirUri(lostId);
		    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
		    	deleteUri = MixItContract.Speakers.buildSpeakerUri(lostId);
		    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
		    }
        }*/

        return batch;
	}

	private static boolean isSpeakerUpdated(Uri uri, JSONObject speaker, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, SpeakersQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;

            final String curFirstName = cursor.getString(SpeakersQuery.FIRST_NAME).toLowerCase().trim();
        	final String curLastName = cursor.getString(SpeakersQuery.LAST_NAME).toLowerCase().trim();
        	final String curBio = cursor.getString(SpeakersQuery.BIO).toLowerCase().trim();
        	final String curCompany = cursor.getString(SpeakersQuery.COMPANY).toLowerCase().trim();
        	final String newFirstName = speaker.has("firstName") ? speaker.getString("firstName").toLowerCase().trim() : curFirstName;
        	final String newLastName = speaker.has("lastName") ? speaker.getString("lastName").toLowerCase().trim() : curLastName;
        	final String newBio = speaker.has("bio") ? speaker.getString("bio").toLowerCase().trim() : curBio;
        	final String newCompany = speaker.has("company") ? speaker.getString("company").toLowerCase().trim() : curCompany;
        	
        	return (!curFirstName.equals(newFirstName)
        			|| !curLastName.equals(newLastName)
        			|| !curBio.equals(newBio)
        			|| !curCompany.equals(newCompany));
        } finally {
            cursor.close();
        }
	}

	
    private interface SpeakersQuery {
        String[] PROJECTION = {
                MixItContract.Speakers.SPEAKER_ID,
                MixItContract.Speakers.FIRST_NAME,
                MixItContract.Speakers.LAST_NAME,
                MixItContract.Speakers.BIO,
                MixItContract.Speakers.COMPANY,
        };

        int SPEAKER_ID = 0;
        int FIRST_NAME = 1;
        int LAST_NAME = 2;
        int BIO = 3;
        int COMPANY = 4;
    }

}
