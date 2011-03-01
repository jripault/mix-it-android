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
 * Created by mathieu
 * Date: 3/1/11
 * Time: 9:34 PM
 */
public class RemoteTagsHandler extends JSONHandler {

	private static final String TAG = "TagsHandler";

	public RemoteTagsHandler() {
		super(MixItContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(ArrayList<JSONArray> entries,
			ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> tagIds = new HashSet<String>();

		int nrEntries = 0;
		for (JSONArray tags : entries) {
			Log.d(TAG, "Retrieved " + tags.length() + " tag entries.");
			nrEntries += tags.length();

	        for (int i=0; i < tags.length(); i++) {
	            JSONObject tag = tags.getJSONObject(i);
		        String id = tag.getString("id");

		        final Uri tagUri = MixItContract.Tags.buildTagUri(id);
		        tagIds.add(id);

		        boolean tagUpdated = false;
		        boolean newTag = false;
		        boolean build = false;
		        ContentProviderOperation.Builder builder;
		        if (isRowExisting(tagUri, TagsQuery.PROJECTION, resolver)) {
			        builder = ContentProviderOperation.newUpdate(tagUri);
			        tagUpdated = isTagUpdated(tagUri, tag, resolver);
		        } else {
			        newTag = true;
			        builder = ContentProviderOperation.newInsert(MixItContract.Tags.CONTENT_URI);
					builder.withValue(MixItContract.Tags.TAG_ID, id);
					build = true;
		        }

		        if (newTag || tagUpdated) {
			        builder.withValue(MixItContract.Tags.TAG_NAME, tag.getString("name"));
			        build = true;
		        }
		        if (build) batch.add(builder.build());
	        }
		}

        return batch;
	}

	private static boolean isTagUpdated(Uri uri, JSONObject tag, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, TagsQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;

            final String curName = cursor.getString(TagsQuery.TAG_NAME).toLowerCase().trim();
        	final String newName = tag.has("name") ? tag.getString("name").toLowerCase().trim() : curName;

        	return !curName.equals(newName);
        } finally {
            cursor.close();
        }
	}


    private interface TagsQuery {
        String[] PROJECTION = {
                MixItContract.Tags.TAG_ID,
                MixItContract.Tags.TAG_NAME,
        };

        int TAG_ID = 0;
        int TAG_NAME = 1;
    }

}
