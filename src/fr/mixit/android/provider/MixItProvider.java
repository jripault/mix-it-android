package fr.mixit.android.provider;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import fr.mixit.android.utils.SelectionBuilder;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by mathieu
 * Date: 1/30/11
 * Time: 4:58 PM
 */
public class MixItProvider extends ContentProvider {

	private static final String TAG = "MixItProvider";

	private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

	private static final int SPEAKERS = 200;
	private static final int SPEAKERS_ID = 203;

	private MixItDatabase mOpenHelper;

	private static final UriMatcher sUriMatcher = buildUriMatcher();


	@Override
	public boolean onCreate() {
		final Context context = getContext();

		mOpenHelper = new MixItDatabase(context);
		return true;
	}

	/**
	 * Build and return a {@link UriMatcher} that catches all {@link Uri}
	 * variations supported by this {@link ContentProvider}.
	 */
	private static UriMatcher buildUriMatcher() {
		final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
		final String authority = MixItContract.CONTENT_AUTHORITY;

		matcher.addURI(authority, "speakers", SPEAKERS);
		matcher.addURI(authority, "speakers/*", SPEAKERS_ID);

		return matcher;
	}

	/** {@inheritDoc} */
	@Override
	public String getType(Uri uri) {
	    final int match = sUriMatcher.match(uri);
	    switch (match) {
	        case SPEAKERS:
	            return MixItContract.Speakers.CONTENT_TYPE;
	        case SPEAKERS_ID:
	            return MixItContract.Speakers.CONTENT_ITEM_TYPE;
	        default:
	            throw new UnsupportedOperationException("Unknown uri: " + uri);
	    }
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
		if (LOGV) Log.v(TAG, "query(uri=" + uri + ", proj=" + Arrays.toString(projection) + ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            default: {
                // Most cases are handled with simple SelectionBuilder
                final SelectionBuilder builder = buildExpandedSelection(uri, match);
                final Cursor cursor = builder.where(selection, selectionArgs).query(db, projection, sort);
                return cursor;
            }
        }

	}

	/** {@inheritDoc} */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
	    if (LOGV) Log.v(TAG, "insert(uri=" + uri + ", values=" + values.toString() + ")");
	    final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
	    final int match = sUriMatcher.match(uri);
	    switch (match) {
	        case SPEAKERS: {
	            db.insertOrThrow(MixItDatabase.Tables.SPEAKERS, null, values);
	            return MixItContract.Speakers.buildSpeakerUri(values.getAsString(MixItContract.Speakers.SPEAKER_ID));
	        }
	        default: {
	            throw new UnsupportedOperationException("Unknown uri: " + uri);
	        }
	    }
	}

	/** {@inheritDoc} */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
	    if (LOGV) Log.v(TAG, "update(uri=" + uri + ", values=" + values.toString() + ")");
	    final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
	    final SelectionBuilder builder = buildSimpleSelection(uri);
	    return builder.where(selection, selectionArgs).update(db, values);
	}

	/** {@inheritDoc} */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
	    if (LOGV) Log.v(TAG, "delete(uri=" + uri + ")");
	    final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
	    final SelectionBuilder builder = buildSimpleSelection(uri);
	    return builder.where(selection, selectionArgs).delete(db);
	}

	/**
	 * Apply the given set of {@link ContentProviderOperation}, executing inside
	 * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
	 * any single one fails.
	 */
	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
	        throws OperationApplicationException {
	    final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
	    db.beginTransaction();
	    try {
	        final int numOperations = operations.size();
	        final ContentProviderResult[] results = new ContentProviderResult[numOperations];
	        for (int i = 0; i < numOperations; i++) {
	            results[i] = operations.get(i).apply(this, results, i);
	        }
	        db.setTransactionSuccessful();
	        return results;
	    } finally {
	        db.endTransaction();
	    }
	}

	/**
	 * Build a simple {@link SelectionBuilder} to match the requested
	 * {@link Uri}. This is usually enough to support {@link #insert},
	 * {@link #update}, and {@link #delete} operations.
	 */
	private SelectionBuilder buildSimpleSelection(Uri uri) {
	    final SelectionBuilder builder = new SelectionBuilder();
	    final int match = sUriMatcher.match(uri);
	    switch (match) {
	        case SPEAKERS: {
	            return builder.table(MixItDatabase.Tables.SPEAKERS);
	        }
	        case SPEAKERS_ID: {
	            final String speakerId = MixItContract.Speakers.getSpeakerId(uri);
	            return builder.table(MixItDatabase.Tables.SPEAKERS)
	                    .where(MixItContract.Speakers.SPEAKER_ID + "=?", speakerId);
	        }
	        default: {
	            throw new UnsupportedOperationException("Unknown uri: " + uri);
	        }
	    }
	}

	/**
	 * Build an advanced {@link SelectionBuilder} to match the requested
	 * {@link Uri}. This is usually only used by {@link #query}, since it
	 * performs table joins useful for {@link Cursor} data.
	 */
	private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
	    final SelectionBuilder builder = new SelectionBuilder();
	    switch (match) {
		    case SPEAKERS: {
		        return builder.table(MixItDatabase.Tables.SPEAKERS);
//				           .map(MixItContract.Speakers.CONTAINS_STARRED, Subquery.SPEAKER_CONTAINS_STARRED);
		    }
		    case SPEAKERS_ID: {
		        final String speakerId = MixItContract.Speakers.getSpeakerId(uri);
		        return builder.table(MixItDatabase.Tables.SPEAKERS)
		                .where(MixItContract.Speakers.SPEAKER_ID + "=?", speakerId);
		    }
	        default: {
	            throw new UnsupportedOperationException("Unknown uri: " + uri);
	        }
	    }
	}
}