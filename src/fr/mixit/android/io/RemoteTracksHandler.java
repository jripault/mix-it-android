package fr.mixit.android.io;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Color;
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
 * Created by mathieu
 * Date: 2/19/11
 * Time: 2:03 PM
 */
public class RemoteTracksHandler extends JSONHandler {

	private static final String TAG = "TracksHandler";

	public RemoteTracksHandler() {
		super(MixItContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(ArrayList<JSONArray> entries,
			ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> trackIds = new HashSet<String>();

		int nrEntries = 0;
		for (JSONArray tracks : entries) {
			Log.d(TAG, "Retrieved " + tracks.length() + " track entries.");
			nrEntries += tracks.length();

	        for (int i=0; i < tracks.length(); i++) {
	            JSONObject track = tracks.getJSONObject(i);
		        String id = track.getString("id");

		        final Uri trackUri = MixItContract.Tracks.buildTrackUri(id);
		        trackIds.add(id);

		        boolean trackUpdated = false;
		        boolean newTrack = false;
		        boolean build = false;
		        ContentProviderOperation.Builder builder;
		        if (isRowExisting(trackUri, TracksQuery.PROJECTION, resolver)) {
			        builder = ContentProviderOperation.newUpdate(trackUri);
			        trackUpdated = isTrackUpdated(trackUri, track, resolver);
		        } else {
			        newTrack = true;
			        builder = ContentProviderOperation.newInsert(MixItContract.Tracks.CONTENT_URI);
					builder.withValue(MixItContract.Tracks.TRACK_ID, id);
					build = true;
		        }

		        if (newTrack || trackUpdated) {
			        builder.withValue(MixItContract.Tracks.TRACK_NAME, track.getString("name"));
			        final int color = Color.parseColor(track.getString("color"));
			        builder.withValue(MixItContract.Tracks.TRACK_COLOR, color);
					build = true;
		        }
		        if (build) batch.add(builder.build());
	        }
		}

        return batch;
	}

	private static boolean isTrackUpdated(Uri uri, JSONObject track, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, TracksQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;

            final String curName = cursor.getString(TracksQuery.TRACK_NAME).toLowerCase().trim();
        	final String curColor = cursor.getString(TracksQuery.TRACK_COLOR).toLowerCase().trim();
        	final String newName = track.has("name") ? track.getString("name").toLowerCase().trim() : curName;
        	final String newColor = track.has("color") ? track.getString("color").toLowerCase().trim() : curColor;

        	return (!curName.equals(newName)
        			|| !curColor.equals(newColor));
        } finally {
            cursor.close();
        }
	}


    private interface TracksQuery {
        String[] PROJECTION = {
                MixItContract.Tracks.TRACK_ID,
                MixItContract.Tracks.TRACK_NAME,
                MixItContract.Tracks.TRACK_COLOR,
        };

        int TRACK_ID = 0;
        int TRACK_NAME = 1;
        int TRACK_COLOR = 2;
    }

}
