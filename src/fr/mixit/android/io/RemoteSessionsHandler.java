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
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.provider.MixItDatabase;
import fr.mixit.android.utils.Lists;
import fr.mixit.android.utils.Maps;
import fr.mixit.android.utils.Sets;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 * Handle a remote {@link org.json.JSONArray} that defines a set of {@link fr.mixit.android.provider.MixItContract.Sessions}
 * entries.
 */
public class RemoteSessionsHandler extends JSONHandler {
	
    private static final String TAG = "SessionsHandler";


    public RemoteSessionsHandler() {
		super(MixItContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(ArrayList<JSONArray> entries, ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> sessionIds = Sets.newHashSet();
		final HashSet<String> trackIds = Sets.newHashSet();
		final HashMap<String, HashSet<String>> sessionSpeakerIds = Maps.newHashMap();

		int nrEntries = 0;
		for (JSONArray sessions : entries) {
			Log.d(TAG, "Retrieved " + sessions.length() + " session entries.");
			nrEntries += sessions.length();
	
	        for (int i=0; i < sessions.length(); i++) {
	            JSONObject session = sessions.getJSONObject(i);
	            String sessionId = session.getString("id");
	            
//	            final String sessionId = sanitizeId(sessionId);
	            final Uri sessionUri = MixItContract.Sessions.buildSessionUri(sessionId);
	            sessionIds.add(sessionId);
//		        int isStarred = isStarred(sessionUri, resolver);

	            boolean sessionUpdated = false;
	            boolean newSession = false;
	            ContentProviderOperation.Builder builder;
	            if (isRowExisting(sessionUri, SessionsQuery.PROJECTION, resolver)) {
	            	builder = ContentProviderOperation.newUpdate(sessionUri);
	        		sessionUpdated = isSessionUpdated(sessionUri, session, resolver);
	            } else {
	            	newSession = true;
		            builder = ContentProviderOperation.newInsert(MixItContract.Sessions.CONTENT_URI);
		            builder.withValue(MixItContract.Sessions.SESSION_ID, sessionId);
	            }
	            
			    if (newSession || sessionUpdated) {
				    builder.withValue(MixItContract.Sessions.TITLE, session.getString("name"));
				    if (session.has("room")) {
					    String room = session.getString("room");
					    if (room == null || room.length() == 0) {
						    room = "Missing";
					    }
				        builder.withValue(MixItContract.Sessions.ROOM, room);
				    }
				    builder.withValue(MixItContract.Sessions.SUMMARY, session.getString("description"));

				    if (session.has("track")) {
			    	    final String trackId = session.getString("track");
					    builder.withValue(MixItContract.Sessions.TRACK_ID, trackId);
				    }

				    if (session.has("slot")) {
					    // TODO managing slots !!
				    }

				    batch.add(builder.build());
	            }

			    if (session.has("speakers")) {
				    final Uri speakerSessionsUri = MixItContract.Sessions.buildSpeakersDirUri(sessionId);
			    	final JSONArray speakers = session.getJSONArray("speakers");
					final HashSet<String> speakerIds = Sets.newHashSet();
			    	
			    	if (!isLocalSync()) {
	            		final boolean sessionSpeakersUpdated = isSessionSpeakersUpdated(speakerSessionsUri, speakers, resolver);
			    		if (sessionSpeakersUpdated) {
				    		Log.d(TAG, "Speakers of session with sessionId " + sessionId + " was udpated.");
				    		batch.add(ContentProviderOperation.newUpdate(sessionUri).build());
			    		}
			    	}
			    	
			    	for (int j = 0; j < speakers.length(); j++) {
/*			    		JSONObject speaker = speakers.getJSONObject(j);
			    		
		            	final String speakerId = speaker.getString("speakerId");
		            	speakerIds.add(speakerId);*/
	
					    final String speakerId = speakers.getString(j);
		            	speakerIds.add(speakerId);

				    	batch.add(ContentProviderOperation.newInsert(speakerSessionsUri)
				    			.withValue(MixItDatabase.SessionsSpeakers.SPEAKER_ID, speakerId)
				    			.withValue(MixItDatabase.SessionsSpeakers.SESSION_ID, sessionId).build());
			    	}
			    	
			    	sessionSpeakerIds.put(sessionId, speakerIds);
			    }
	        }
		}
        
        if (isRemoteSync() && nrEntries > 0) {
        	for (Map.Entry<String, HashSet<String>> entry : sessionSpeakerIds.entrySet()) {
        		String sessionId = entry.getKey();
        		HashSet<String> speakerIds = entry.getValue();
			    final Uri speakerSessionsUri = MixItContract.Sessions.buildSpeakersDirUri(sessionId);
    	    	HashSet<String> lostSpeakerIds = getLostIds(speakerIds, speakerSessionsUri, SpeakersQuery.PROJECTION, SpeakersQuery.SPEAKER_ID, resolver);
            	for (String lostSpeakerId : lostSpeakerIds) {
	        		final Uri deleteUri = MixItContract.Sessions.buildSessionSpeakerUri(sessionId, lostSpeakerId);
			    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
            	}
        	}

        	HashSet<String> lostSessionIds = getLostIds(sessionIds, MixItContract.Sessions.CONTENT_URI, SessionsQuery.PROJECTION, SessionsQuery.SESSION_ID, resolver);
        	for (String lostSessionId : lostSessionIds) {
		    	Uri deleteUri = MixItContract.Sessions.buildSpeakersDirUri(lostSessionId);
		    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
		    	deleteUri = MixItContract.Sessions.buildSessionUri(lostSessionId);
		    	batch.add(ContentProviderOperation.newDelete(deleteUri).build());
        	}
        }
        
        return batch;
	}

	private static boolean isSessionUpdated(Uri uri, JSONObject session, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, SessionsQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;

            final String curTitle = cursor.getString(SessionsQuery.TITLE).toLowerCase().trim();
	        final String curSummary = cursor.getString(SessionsQuery.SUMMARY).toLowerCase().trim();
	        final String curTrackId = cursor.getString(SessionsQuery.TRACK_ID) != null ? cursor.getString(SessionsQuery.TRACK_ID).toLowerCase().trim() : "";
			final String newTitle = session.getString("name").toLowerCase().trim();
        	final String newSummary = session.getString("description").toLowerCase().trim();
	        final String newTrackId = session.has("track") ? session.getString("track").toLowerCase().trim() : "";

        	return (!curTitle.equals(newTitle) || !curSummary.equals(newSummary) || !curTrackId.equals(newTrackId));
        } finally {
            cursor.close();
        }
	}

	private static boolean isSessionSpeakersUpdated(Uri uri, JSONArray speakers, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, SpeakersQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;
            return cursor.getCount() != speakers.length();
        } finally {
            cursor.close();
        }
	}

    private interface SessionsQuery {
        String[] PROJECTION = {
        		MixItContract.Sessions.SESSION_ID,
        		MixItContract.Sessions.TITLE,
        		MixItContract.Sessions.SUMMARY,
		        MixItContract.Sessions.TRACK_ID,
/*        		MixItContract.Sessions.ROOM,
                MixItContract.Sessions.STARRED,*/
        };

        int SESSION_ID = 0;
        int TITLE = 1;
        int SUMMARY = 2;
        int TRACK_ID = 3;
/*        int ROOM = 4;
        int STARRED = 5;*/
    }

    interface SpeakersQuery {
        String[] PROJECTION = {
        		MixItContract.Speakers.SPEAKER_ID,
        };

        int SPEAKER_ID = 0;
    }

    private interface TracksQuery {
        String[] PROJECTION = {
                MixItContract.Tracks.TRACK_ID,
        };

        int TRACK_ID = 0;
    }

}
