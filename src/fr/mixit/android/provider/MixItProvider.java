package fr.mixit.android.provider;

import android.app.SearchManager;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;
import fr.mixit.android.utils.SelectionBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by mathieu
 * Date: 1/30/11
 * Time: 4:58 PM
 */
public class MixItProvider extends ContentProvider {

	private static final String TAG = "MixItProvider";

	private static final boolean LOGV = Log.isLoggable(TAG, Log.VERBOSE);

	private static final int SESSIONS = 100;
	private static final int SESSIONS_STARRED = 101;
	private static final int SESSIONS_SEARCH = 105;
	private static final int SESSIONS_AT = 106;
	private static final int SESSIONS_PARALLEL = 107;
	private static final int SESSIONS_NEXT = 108;
	private static final int SESSIONS_ID = 109;
	private static final int SESSIONS_ID_SPEAKERS = 110;
	private static final int SESSIONS_ID_SPEAKERS_ID = 111;

	private static final int SPEAKERS = 200;
	private static final int SPEAKERS_STARRED = 201;
	private static final int SPEAKERS_SEARCH = 202;
	private static final int SPEAKERS_ID = 203;
	private static final int SPEAKERS_ID_SESSIONS = 204;

	private static final int SLOTS = 400;
	private static final int SLOTS_BETWEEN = 401;
	private static final int SLOTS_ID = 402;
	private static final int SLOTS_ID_SESSIONS = 403;

	private static final int TRACKS = 600;
	private static final int TRACKS_ID = 601;
	private static final int TRACKS_ID_SESSIONS = 602;

	private static final int SYNC = 700;
	private static final int SYNC_ID = 701;

	private static final int SEARCH_SUGGEST = 800;

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

		matcher.addURI(authority, "sessions", SESSIONS);
		matcher.addURI(authority, "sessions/starred", SESSIONS_STARRED);
		matcher.addURI(authority, "sessions/search/*", SESSIONS_SEARCH);
		matcher.addURI(authority, "sessions/at/*", SESSIONS_AT);
		matcher.addURI(authority, "sessions/parallel/*", SESSIONS_PARALLEL);
		matcher.addURI(authority, "sessions/next/*", SESSIONS_NEXT);
		matcher.addURI(authority, "sessions/*", SESSIONS_ID);
		matcher.addURI(authority, "sessions/*/speakers", SESSIONS_ID_SPEAKERS);
		matcher.addURI(authority, "sessions/*/speakers/*", SESSIONS_ID_SPEAKERS_ID);

		matcher.addURI(authority, "speakers", SPEAKERS);
		matcher.addURI(authority, "speakers/starred", SPEAKERS_STARRED);
		matcher.addURI(authority, "speakers/search/*", SPEAKERS_SEARCH);
		matcher.addURI(authority, "speakers/*", SPEAKERS_ID);
		matcher.addURI(authority, "speakers/*/sessions", SPEAKERS_ID_SESSIONS);

		matcher.addURI(authority, "slots", SLOTS);
		matcher.addURI(authority, "slots/between/*/*", SLOTS_BETWEEN);
		matcher.addURI(authority, "slots/*", SLOTS_ID);
		matcher.addURI(authority, "slots/*/sessions", SLOTS_ID_SESSIONS);

		matcher.addURI(authority, "tracks", TRACKS);
		matcher.addURI(authority, "tracks/*", TRACKS_ID);
		matcher.addURI(authority, "tracks/*/sessions", TRACKS_ID_SESSIONS);

		matcher.addURI(authority, "sync", SYNC);
		matcher.addURI(authority, "sync/*", SYNC_ID);

		matcher.addURI(authority, "search_suggest_query", SEARCH_SUGGEST);

		return matcher;
	}

	/** {@inheritDoc} */
	@Override
	public String getType(Uri uri) {
	    final int match = sUriMatcher.match(uri);
	    switch (match) {
		    case SESSIONS:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SESSIONS_STARRED:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SESSIONS_SEARCH:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SESSIONS_AT:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SESSIONS_PARALLEL:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SESSIONS_NEXT:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SESSIONS_ID:
		        return MixItContract.Sessions.CONTENT_ITEM_TYPE;
		    case SESSIONS_ID_SPEAKERS:
		        return MixItContract.Speakers.CONTENT_TYPE;
		    case SPEAKERS:
		        return MixItContract.Speakers.CONTENT_TYPE;
		    case SPEAKERS_STARRED:
		        return MixItContract.Speakers.CONTENT_TYPE;
		    case SPEAKERS_SEARCH:
		        return MixItContract.Speakers.CONTENT_TYPE;
		    case SPEAKERS_ID:
		        return MixItContract.Speakers.CONTENT_ITEM_TYPE;
		    case SPEAKERS_ID_SESSIONS:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case SLOTS:
		        return MixItContract.Slots.CONTENT_TYPE;
		    case SLOTS_BETWEEN:
		        return MixItContract.Slots.CONTENT_TYPE;
		    case SLOTS_ID:
		        return MixItContract.Slots.CONTENT_ITEM_TYPE;
		    case SLOTS_ID_SESSIONS:
		        return MixItContract.Sessions.CONTENT_TYPE;
		    case TRACKS:
		        return MixItContract.Tracks.CONTENT_TYPE;
		    case TRACKS_ID:
		        return MixItContract.Tracks.CONTENT_ITEM_TYPE;
		    case SYNC:
		        return MixItContract.Sync.CONTENT_TYPE;
		    case SYNC_ID:
		        return MixItContract.Sync.CONTENT_ITEM_TYPE;
		    case TRACKS_ID_SESSIONS:
		        return MixItContract.Sessions.CONTENT_TYPE;
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
			case SEARCH_SUGGEST: {
				final SelectionBuilder builder = new SelectionBuilder();

				// Adjust incoming query to become SQL text match
				selectionArgs[0] = selectionArgs[0] + "%";
				builder.table(MixItDatabase.Tables.SEARCH_SUGGEST);
				builder.where(selection, selectionArgs);
				builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
						SearchManager.SUGGEST_COLUMN_TEXT_1);

				projection = new String[] { BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,
						SearchManager.SUGGEST_COLUMN_QUERY };

				final String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
				return builder.query(db, projection, null, null, MixItContract.SearchSuggest.DEFAULT_SORT, limit);
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
		    case SESSIONS: {
		        db.insertOrThrow(MixItDatabase.Tables.SESSIONS, null, values);
		        return MixItContract.Sessions.buildSessionUri(values.getAsString(MixItContract.Sessions.SESSION_ID));
		    }
		    case SESSIONS_ID_SPEAKERS: {
		        db.insertOrThrow(MixItDatabase.Tables.SESSIONS_SPEAKERS, null, values);
		        return MixItContract.Speakers.buildSpeakerUri(values.getAsString(MixItDatabase.SessionsSpeakers.SPEAKER_ID));
		    }
	        case SPEAKERS: {
	            db.insertOrThrow(MixItDatabase.Tables.SPEAKERS, null, values);
	            return MixItContract.Speakers.buildSpeakerUri(values.getAsString(MixItContract.Speakers.SPEAKER_ID));
	        }
		    case SPEAKERS_ID_SESSIONS: {
		        db.insertOrThrow(MixItDatabase.Tables.SESSIONS_SPEAKERS, null, values);
		        return MixItContract.Sessions.buildSessionUri(values.getAsString(MixItDatabase.SessionsSpeakers.SESSION_ID));
		    }
		    case SLOTS: {
		        db.insertOrThrow(MixItDatabase.Tables.SLOTS, null, values);
		        return MixItContract.Slots.buildSlotUri(values.getAsString(MixItContract.Slots.SLOT_ID));
		    }
		    case TRACKS: {
		        db.insertOrThrow(MixItDatabase.Tables.TRACKS, null, values);
		        return MixItContract.Tracks.buildTrackUri(values.getAsString(MixItContract.Tracks.TRACK_ID));
		    }
		    case SYNC: {
		        db.insertOrThrow(MixItDatabase.Tables.SYNC, null, values);
		        return MixItContract.Sync.buildSyncUri(values.getAsString(MixItContract.Sync.URI_ID));
		    }
		    case SEARCH_SUGGEST: {
		        db.insertOrThrow(MixItDatabase.Tables.SEARCH_SUGGEST, null, values);
		        return MixItContract.SearchSuggest.CONTENT_URI;
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
		    case SESSIONS: {
		        return builder.table(MixItDatabase.Tables.SESSIONS);
		    }
		    case SESSIONS_STARRED: {
		        return builder.table(MixItDatabase.Tables.SESSIONS)
		        .where(MixItContract.Sessions.STARRED + "=1");
		    }
		    case SESSIONS_ID: {
		        final String sessionId = MixItContract.Sessions.getSessionId(uri);
		        return builder.table(MixItDatabase.Tables.SESSIONS)
		                .where(MixItContract.Sessions.SESSION_ID + "=?", sessionId);
		    }
		    case SESSIONS_ID_SPEAKERS: {
		        final String sessionId = MixItContract.Sessions.getSessionId(uri);
		        return builder.table(MixItDatabase.Tables.SESSIONS_SPEAKERS)
		                .where(MixItDatabase.SessionsSpeakers.SESSION_ID + "=?", sessionId);
		    }
		    case SESSIONS_ID_SPEAKERS_ID: {
		        final String sessionId = MixItContract.Sessions.getSessionId(uri);
		        final String speakerId = MixItContract.Sessions.getSpeakerId(uri);
		        return builder.table(MixItDatabase.Tables.SESSIONS_SPEAKERS)
		                .where(MixItDatabase.SessionsSpeakers.SESSION_ID + "=?", sessionId)
		                .where(MixItDatabase.SessionsSpeakers.SPEAKER_ID + "=?", speakerId);
		    }

	        case SPEAKERS: {
	            return builder.table(MixItDatabase.Tables.SPEAKERS);
	        }
	        case SPEAKERS_ID: {
	            final String speakerId = MixItContract.Speakers.getSpeakerId(uri);
	            return builder.table(MixItDatabase.Tables.SPEAKERS)
	                    .where(MixItContract.Speakers.SPEAKER_ID + "=?", speakerId);
	        }
		    case SPEAKERS_ID_SESSIONS: {
		        final String speakerId = MixItContract.Speakers.getSpeakerId(uri);
		        return builder.table(MixItDatabase.Tables.SESSIONS_SPEAKERS)
		                .where(MixItContract.Speakers.SPEAKER_ID + "=?", speakerId);
		    }

		    case SLOTS: {
		        return builder.table(MixItDatabase.Tables.SLOTS);
		    }
		    case SLOTS_ID: {
		        final String blockId = MixItContract.Slots.getSlotId(uri);
		        return builder.table(MixItDatabase.Tables.SLOTS)
		                .where(MixItContract.Slots.SLOT_ID + "=?", blockId);
		    }

		    case TRACKS: {
		        return builder.table(MixItDatabase.Tables.TRACKS);
		    }
		    case TRACKS_ID: {
		        final String trackId = MixItContract.Tracks.getTrackId(uri);
		        return builder.table(MixItDatabase.Tables.TRACKS)
		                .where(MixItContract.Tracks.TRACK_ID + "=?", trackId);
		    }
		    case SYNC: {
		        return builder.table(MixItDatabase.Tables.TRACKS);
		    }
		    case SYNC_ID: {
		        final String syncId = MixItContract.Sync.getSyncId(uri);
		        return builder.table(MixItDatabase.Tables.SYNC)
		                .where(MixItContract.Sync.URI_ID + "=?", syncId);
		    }
		    case TRACKS_ID_SESSIONS: {
		        final String trackId = MixItContract.Tracks.getTrackId(uri);
		        return builder.table(MixItDatabase.Tables.SESSIONS)
		                .where(Qualified.SESSIONS_TRACK_ID + "=?", trackId);
		    }
		    case SEARCH_SUGGEST: {
		        return builder.table(MixItDatabase.Tables.SEARCH_SUGGEST);
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
            case SESSIONS: {
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                		.mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS);
            }
            case SESSIONS_STARRED: {
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(MixItContract.Sessions.STARRED + "=1");
            }
            case SESSIONS_SEARCH: {
                final String query = MixItContract.Sessions.getSearchQuery(uri);
                return builder.table(MixItDatabase.Tables.SESSIONS_SEARCH_JOIN_SESSIONS_SLOTS_TRACKS)
                        .map(MixItContract.Sessions.SEARCH_SNIPPET, Subquery.SESSIONS_SNIPPET)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SESSION_ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(MixItDatabase.SessionsSearchColumns.BODY + " MATCH ?", query);
            }
            case SESSIONS_AT: {
                final List<String> segments = uri.getPathSegments();
                final String time = segments.get(2);
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(MixItContract.Sessions.SLOT_START + "<=?", time)
                        .where(MixItContract.Sessions.SLOT_END + ">=?", time);
            }
            case SESSIONS_PARALLEL: {
                final List<String> segments = uri.getPathSegments();
                final String sessionId = segments.get(2);
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(WhereClause.SESSIONS_PARALLEL, sessionId, sessionId)
                        .where(MixItContract.Sessions.SESSION_ID + "<>?", sessionId);
            }
            case SESSIONS_NEXT: {
                final List<String> segments = uri.getPathSegments();
                final String time = segments.get(2);
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(WhereClause.SESSIONS_NEXT, time);
            }
            case SESSIONS_ID: {
                final String sessionId = MixItContract.Sessions.getSessionId(uri);
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
            }
            case SESSIONS_ID_SPEAKERS: {
                final String sessionId = MixItContract.Sessions.getSessionId(uri);
                return builder.table(MixItDatabase.Tables.SESSIONS_SPEAKERS_JOIN_SPEAKERS)
                        .mapToTable(MixItContract.Speakers._ID, MixItDatabase.Tables.SPEAKERS)
                        .mapToTable(MixItContract.Speakers.SPEAKER_ID, MixItDatabase.Tables.SPEAKERS)
                        .where(Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=?", sessionId);
            }
            case SPEAKERS: {
                return builder.table(MixItDatabase.Tables.SPEAKERS)
                   		.map(MixItContract.Speakers.CONTAINS_STARRED, Subquery.SPEAKER_CONTAINS_STARRED);
            }
            case SPEAKERS_STARRED: {
                return builder.table(MixItDatabase.Tables.SPEAKERS)
                   		.map(MixItContract.Speakers.CONTAINS_STARRED, Subquery.SPEAKER_CONTAINS_STARRED)
                        .where(MixItContract.Speakers.CONTAINS_STARRED + "=1");
            }
            case SPEAKERS_SEARCH: {
                final String query = MixItContract.Sessions.getSearchQuery(uri);
                return builder.table(MixItDatabase.Tables.SPEAKERS_SEARCH_JOIN_SPEAKERS)
                        .map(MixItContract.Speakers.SEARCH_SNIPPET, Subquery.SPEAKERS_SNIPPET)
                   		.map(MixItContract.Speakers.CONTAINS_STARRED, Subquery.SPEAKER_CONTAINS_STARRED)
                        .mapToTable(MixItContract.Speakers._ID, MixItDatabase.Tables.SPEAKERS)
                        .mapToTable(MixItContract.Speakers.SPEAKER_ID, MixItDatabase.Tables.SPEAKERS)
                        .where(MixItDatabase.SpeakersSearchColumns.BODY + " MATCH ?", query);
            }
            case SPEAKERS_ID: {
                final String speakerId = MixItContract.Speakers.getSpeakerId(uri);
                return builder.table(MixItDatabase.Tables.SPEAKERS)
                        .where(MixItContract.Speakers.SPEAKER_ID + "=?", speakerId);
            }
            case SPEAKERS_ID_SESSIONS: {
                final String speakerId = MixItContract.Speakers.getSpeakerId(uri);
                return builder.table(MixItDatabase.Tables.SESSIONS_SPEAKERS_JOIN_SESSIONS_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SESSION_ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                        .mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "=?", speakerId);
            }
            case SLOTS: {
                return builder.table(MixItDatabase.Tables.SLOTS);
            }
            case SLOTS_BETWEEN: {
                final List<String> segments = uri.getPathSegments();
                final String startTime = segments.get(2);
                final String endTime = segments.get(3);
                builder.table(MixItDatabase.Tables.SLOTS)
                        .map(MixItContract.Slots.SESSIONS_COUNT, Subquery.SLOT_SESSIONS_COUNT)
                        .map(MixItContract.Slots.CONTAINS_STARRED, Subquery.SLOT_CONTAINS_STARRED)
                        .where(MixItContract.Slots.SLOT_START + ">=?", startTime)
                        .where(MixItContract.Slots.SLOT_START + "<=?", endTime);
                return builder;
            }
            case SLOTS_ID: {
                final String blockId = MixItContract.Slots.getSlotId(uri);
                return builder.table(MixItDatabase.Tables.SLOTS)
/*                        .map(MixItContract.Slots.SESSIONS_COUNT, Subquery.SLOT_SESSIONS_COUNT)
                        .map(MixItContract.Slots.CONTAINS_STARRED, Subquery.SLOT_CONTAINS_STARRED)*/
                        .where(MixItContract.Slots.SLOT_ID + "=?", blockId);
            }
            case SLOTS_ID_SESSIONS: {
                final String blockId = MixItContract.Slots.getSlotId(uri);
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .map(MixItContract.Slots.SESSIONS_COUNT, Subquery.SLOT_SESSIONS_COUNT)
                        .map(MixItContract.Slots.CONTAINS_STARRED, Subquery.SLOT_CONTAINS_STARRED)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SESSION_ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(Qualified.SESSIONS_SLOT_ID + "=?", blockId);
            }
            case TRACKS: {
                return builder.table(MixItDatabase.Tables.TRACKS)
                        .map(MixItContract.Tracks.SESSIONS_COUNT, Subquery.TRACK_SESSIONS_COUNT);
            }
            case TRACKS_ID: {
                final String trackId = MixItContract.Tracks.getTrackId(uri);
                return builder.table(MixItDatabase.Tables.TRACKS)
                        .where(MixItContract.Tracks.TRACK_ID + "=?", trackId);
            }
            case SYNC: {
                return builder.table(MixItDatabase.Tables.SYNC);
            }
            case SYNC_ID: {
                final String syncId = MixItContract.Sync.getSyncId(uri);
                return builder.table(MixItDatabase.Tables.SYNC)
                        .where(MixItContract.Sync.URI_ID + "=?", syncId);
            }
            case TRACKS_ID_SESSIONS: {
                final String trackId = MixItContract.Tracks.getTrackId(uri);
                return builder.table(MixItDatabase.Tables.SESSIONS_JOIN_SLOTS_TRACKS)
                        .mapToTable(MixItContract.Sessions._ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SESSION_ID, MixItDatabase.Tables.SESSIONS)
                        .mapToTable(MixItContract.Sessions.SLOT_ID, MixItDatabase.Tables.SESSIONS)
                		.mapToTable(MixItContract.Sessions.TRACK_ID, MixItDatabase.Tables.SESSIONS)
                        .map(MixItContract.Sessions.STARRED_IN_SLOT_COUNT, Subquery.SLOT_STARRED_SESSIONS_COUNT)
                		.mapToTable(MixItContract.Tracks.TRACK_COLOR, MixItDatabase.Tables.TRACKS)
                        .where(Qualified.SESSIONS_TRACK_ID + "=?", trackId);
            }
            default: {
                throw new UnsupportedOperationException("Unknown uri: " + uri);
            }
	    }
	}

/* TODO UNUSED ?	private Cursor bundleSessionCountExtras(Cursor cursor, final SQLiteDatabase db,
	        SelectionBuilder sb, String selection, String[] selectionArgs, String sortOrder) {
	    String sortKey;

	    // The sort order suffix could be something like "DESC".
	    // We want to preserve it in the query even though we will change
	    // the sort column itself.
	    String sortOrderSuffix = "";
	    if (sortOrder != null) {
	        int spaceIndex = sortOrder.indexOf(' ');
	        if (spaceIndex != -1) {
	            sortKey = sortOrder.substring(0, spaceIndex);
	            sortOrderSuffix = sortOrder.substring(spaceIndex);
	        } else {
	            sortKey = sortOrder;
	        }
	    } else {
	        sortKey = MixItContract.Sessions.SESSION_ID;
	    }

	    sb.map(SessionsIndexQuery.COUNT, "COUNT(" + MixItContract.Sessions.SESSION_ID + ")");

	    Cursor indexCursor = sb.query(db, SessionsIndexQuery.COLUMNS,
	            SessionsIndexQuery.ORDER_BY, null ,
	            SessionsIndexQuery.ORDER_BY + sortOrderSuffix, null);

	    try {
	        int groupCount = indexCursor.getCount();
	        String weekdays[] = new String[groupCount];
	        int counts[] = new int[groupCount];
	        int indexCount = 0;

	        // Since GET_PHONEBOOK_INDEX is a many-to-1 function, we may end up
	        // with multiple entries for the same title.  The following code
	        // collapses those duplicates.
	        for (int i = 0; i < groupCount; i++) {
	            indexCursor.moveToNext();
	            long millis = indexCursor.getLong(SessionsIndexQuery.BLOCK_START);
	            int count = indexCursor.getInt(SessionsIndexQuery.COLUMN_COUNT);
	            if (indexCount == 0) {
	                counts[indexCount] = count;
	                indexCount++;
	            } else {
	                counts[indexCount - 1] += count;
	            }
	        }

	        if (indexCount < groupCount) {
	            String[] newWeekdays = new String[indexCount];
	            System.arraycopy(weekdays, 0, newWeekdays, 0, indexCount);
	            weekdays = newWeekdays;

	            int[] newCounts = new int[indexCount];
	            System.arraycopy(counts, 0, newCounts, 0, indexCount);
	            counts = newCounts;
	        }

	        final Bundle bundle = new Bundle();
	        bundle.putStringArray(SessionCounts.EXTRA_SESSION_INDEX_WEEKDAYS, weekdays);
	        bundle.putIntArray(SessionCounts.EXTRA_SESSION_INDEX_COUNTS, counts);
	        return new CursorWrapper(cursor) {
				@Override
	            public Bundle getExtras() {
	                return bundle;
	            }
	        };
	    } finally {
	        indexCursor.close();
	    }
	}*/

	private interface Subquery {
	    String SLOT_SESSIONS_COUNT = "(SELECT COUNT(" + Qualified.SESSIONS_SESSION_ID + ") FROM "
	            + MixItDatabase.Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_SLOT_ID + "="
	            + Qualified.SLOTS_SLOT_ID + ")";

	    String SLOT_STARRED_SESSIONS_COUNT = "(SELECT COUNT(" + Qualified.S_SESSION_ID + ") FROM "
		    + MixItDatabase.Tables.SESSIONS + " AS S LEFT OUTER JOIN " + MixItDatabase.Tables.SLOTS + " AS B ON "
		    + Qualified.S_SLOT_ID + "=" + Qualified.B_SLOT_ID + " WHERE "
		    + Qualified.S_STARRED + "=1 AND " + "((" + Qualified.SLOTS_SLOT_START + ">="
		    + Qualified.B_SLOT_START + " AND " + Qualified.SLOTS_SLOT_END + "<="
		    + Qualified.B_SLOT_END + ") OR (" + Qualified.SLOTS_SLOT_END + ">"
		    + Qualified.B_SLOT_START + " AND " + Qualified.SLOTS_SLOT_END + "<="
		    + Qualified.B_SLOT_END + ") OR (" + Qualified.SLOTS_SLOT_START + "<"
		    + Qualified.B_SLOT_END + " AND " + Qualified.SLOTS_SLOT_START + ">="
		    + Qualified.B_SLOT_START + ")))";

	    String SLOT_CONTAINS_STARRED = "(SELECT MAX(" + Qualified.SESSIONS_STARRED + ") FROM "
	            + MixItDatabase.Tables.SESSIONS + " WHERE " + Qualified.SESSIONS_SLOT_ID + "="
	            + Qualified.SLOTS_SLOT_ID + ")";

	    String SPEAKER_CONTAINS_STARRED = "(SELECT MAX(" + Qualified.SESSIONS_STARRED + ") FROM "
			    + MixItDatabase.Tables.SESSIONS + " LEFT OUTER JOIN " + MixItDatabase.Tables.SESSIONS_SPEAKERS + " ON "
			    + Qualified.SESSIONS_SESSION_ID + "=" + Qualified.SESSIONS_SPEAKERS_SESSION_ID
			    + " WHERE " + Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "="
			    + Qualified.SPEAKERS_SPEAKER_ID + ")";

	    String TRACK_SESSIONS_COUNT = "(SELECT COUNT(" + Qualified.SESSIONS_TRACK_ID
	            + ") FROM " + MixItDatabase.Tables.SESSIONS + " WHERE "
	            + Qualified.SESSIONS_TRACK_ID + "=" + Qualified.TRACKS_TRACK_ID + ")";

	    String SESSIONS_SNIPPET = "snippet(" + MixItDatabase.Tables.SESSIONS_SEARCH + ",'{','}','\u2026')";
	    String SPEAKERS_SNIPPET = "snippet(" + MixItDatabase.Tables.SPEAKERS_SEARCH + ",'{','}','\u2026')";
	}

	private interface WhereClause {
		String SESSIONS_PARALLEL = "(" + MixItContract.Sessions.SLOT_START + " >= (SELECT "
				+ MixItContract.Slots.SLOT_START + " FROM " + MixItDatabase.Tables.SLOTS + " LEFT OUTER JOIN "
				+ MixItDatabase.Tables.SESSIONS + " ON " + MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_ID
				+ "=" + MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.SLOT_ID + " WHERE "
				+ MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.SESSION_ID + " = ?) AND "
				+ MixItContract.Sessions.SLOT_END + " <= (SELECT " + MixItContract.Slots.SLOT_END + " FROM "
				+ MixItDatabase.Tables.SLOTS + " LEFT OUTER JOIN " + MixItDatabase.Tables.SESSIONS + " ON "
				+ MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_ID + "="
				+ MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.SLOT_ID + " WHERE "
				+ MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.SESSION_ID + " = ?))";

		String SESSIONS_NEXT = "(" + MixItContract.Sessions.SLOT_START + " IN (SELECT "
				+ MixItContract.Slots.SLOT_START + " FROM " + MixItDatabase.Tables.SLOTS + " WHERE "
				+ MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_START
				+ " >= ? ORDER BY " + MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_START
				+ " LIMIT 1))";
	}

	/**
	 * {@link MixItContract} fields that are fully qualified with a specific
	 * parent {@link MixItDatabase.Tables}. Used when needed to work around SQL ambiguity.
	 */
	private interface Qualified {
	    String SPEAKERS_SPEAKER_ID = MixItDatabase.Tables.SPEAKERS + "." + MixItContract.Speakers.SPEAKER_ID;

	    String SESSIONS_SESSION_ID = MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.SESSION_ID;
	    String SESSIONS_SLOT_ID = MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.SLOT_ID;
	    String SESSIONS_TRACK_ID = MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.TRACK_ID;

	    String SPEAKERS_FIRST_NAME = MixItDatabase.Tables.SPEAKERS + "." + MixItContract.Speakers.FIRST_NAME;

	    String SESSIONS_SPEAKERS_SESSION_ID = MixItDatabase.Tables.SESSIONS_SPEAKERS + "."
	            + MixItDatabase.SessionsSpeakers.SESSION_ID;
	    String SESSIONS_SPEAKERS_SPEAKER_ID = MixItDatabase.Tables.SESSIONS_SPEAKERS + "."
	            + MixItDatabase.SessionsSpeakers.SPEAKER_ID;

	    String SESSIONS_STARRED = MixItDatabase.Tables.SESSIONS + "." + MixItContract.Sessions.STARRED;

	    String TRACKS_TRACK_ID = MixItDatabase.Tables.TRACKS + "." + MixItContract.Tracks.TRACK_ID;

	    String SLOTS_SLOT_ID = MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_ID;
	    String SLOTS_SLOT_START = MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_START;
	    String SLOTS_SLOT_END = MixItDatabase.Tables.SLOTS + "." + MixItContract.Slots.SLOT_END;

	    String S_SESSION_ID = "S." + MixItContract.Sessions.SESSION_ID;
	    String S_SLOT_ID = "S." + MixItContract.Sessions.SLOT_ID;
	    String S_STARRED = "S." + MixItContract.Sessions.STARRED;
	    String B_SLOT_ID = "B." + MixItContract.Slots.SLOT_ID;
	    String B_SLOT_START = "B." + MixItContract.Slots.SLOT_START;
	    String B_SLOT_END = "B." + MixItContract.Slots.SLOT_END;
	}

	interface SessionsIndexQuery {

		static final String COUNT = "count";

		String [] COLUMNS = {
				MixItContract.Sessions.SLOT_START,
				COUNT,
		};

		static final int SLOT_START = 0;
		static final int COLUMN_COUNT = 1;

		static final String ORDER_BY = MixItContract.Sessions.SLOT_START;
	}

}