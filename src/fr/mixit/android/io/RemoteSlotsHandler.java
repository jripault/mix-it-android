package fr.mixit.android.io;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.SimpleAdapter;
import fr.mixit.android.provider.MixItContract;
import fr.mixit.android.utils.Lists;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by mathieu
 * Date: 2/19/11
 * Time: 2:16 PM
 */
public class RemoteSlotsHandler extends JSONHandler {

	private static final String DATE_FORMAT = "MMM d',' yyyy h':'mm':'ss aa";// like : Apr 5, 2011 9:00:00 AM
	private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.UK);
	private static final String TAG = "SlotsHandler";

	public RemoteSlotsHandler() {
		super(MixItContract.CONTENT_AUTHORITY);
	}

	@Override
	public ArrayList<ContentProviderOperation> parse(ArrayList<JSONArray> entries,
			ContentResolver resolver) throws JSONException {
		final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
		final HashSet<String> slotIds = new HashSet<String>();

		int nrEntries = 0;
		for (JSONArray slots : entries) {
			Log.d(TAG, "Retrieved " + slots.length() + " slot entries.");
			nrEntries += slots.length();

	        for (int i=0; i < slots.length(); i++) {
	            JSONObject slot = slots.getJSONObject(i);
		        String id = slot.getString("id");

		        final Uri slotUri = MixItContract.Slots.buildSlotUri(id);
		        slotIds.add(id);

		        boolean slotUpdated = false;
		        boolean newSlot = false;
		        boolean build = false;
		        ContentProviderOperation.Builder builder;
		        if (isRowExisting(slotUri, SlotsQuery.PROJECTION, resolver)) {
			        builder = ContentProviderOperation.newUpdate(slotUri);
			        slotUpdated = isSlotUpdated(slotUri, slot, resolver);
		        } else {
			        newSlot = true;
			        builder = ContentProviderOperation.newInsert(MixItContract.Slots.CONTENT_URI);
					builder.withValue(MixItContract.Slots.SLOT_ID, id);
					build = true;
		        }

		        if (newSlot || slotUpdated) {
			        long startTime = 0;
			        try {
				        final Date start = sdf.parse(slot.getString("startTime"));
				        startTime = start.getTime();
			        } catch (ParseException e) {
				        e.printStackTrace();
			        }
			        builder.withValue(MixItContract.Slots.SLOT_START, startTime);
			        long endTime = 0;
			        try {
				        final Date end = sdf.parse(slot.getString("endTime"));
				        endTime = end.getTime();
			        } catch (ParseException e) {
				        e.printStackTrace();
			        }
			        builder.withValue(MixItContract.Slots.SLOT_END, endTime);
			        builder.withValue(MixItContract.Slots.SLOT_TYPE, slot.getString("kind"));
					build = true;
		        }
		        if (build) batch.add(builder.build());
	        }
		}

        return batch;
	}

	private static boolean isSlotUpdated(Uri uri, JSONObject slot, ContentResolver resolver) throws JSONException {
        final Cursor cursor = resolver.query(uri, SlotsQuery.PROJECTION, null, null, null);
        try {
            if (!cursor.moveToFirst()) return false;
	        long startTime = 0;
	        try {
		        final Date start = sdf.parse(slot.getString("startTime"));
		        startTime = start.getTime();
	        } catch (ParseException e) {
		        e.printStackTrace();
	        }
	        long endTime = 0;
	        try {
		        final Date end = sdf.parse(slot.getString("endTime"));
		        endTime = end.getTime();
	        } catch (ParseException e) {
		        e.printStackTrace();
	        }

            final long curStart = cursor.getLong(SlotsQuery.SLOT_START);
        	final long curEnd = cursor.getLong(SlotsQuery.SLOT_END);
        	final String curType = cursor.getString(SlotsQuery.SLOT_TYPE).toLowerCase().trim();
	        final long newStart = slot.has("startTime") ? startTime : curStart;
	        final long newEnd = slot.has("endTime") ? endTime : curEnd;
        	final String newType = slot.has("kind") ? slot.getString("kind").toLowerCase().trim() : curType;

        	return (!(curStart != newStart)
        			|| !(curEnd != newEnd)
        			|| !curType.equals(newType));
        } finally {
            cursor.close();
        }
	}


    private interface SlotsQuery {
        String[] PROJECTION = {
                MixItContract.Slots.SLOT_ID,
                MixItContract.Slots.SLOT_START,
                MixItContract.Slots.SLOT_END,
                MixItContract.Slots.SLOT_TYPE,
        };

        int SLOT_ID = 0;
        int SLOT_START= 1;
        int SLOT_END = 2;
        int SLOT_TYPE = 3;
    }

}
