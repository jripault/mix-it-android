package fr.mixit.android.provider;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import fr.mixit.android.provider.MixItContract.Speakers;
import fr.mixit.android.provider.MixItContract.SpeakersColumns;
import fr.mixit.android.provider.MixItContract.Sessions;
import fr.mixit.android.provider.MixItContract.SessionsColumns;
import fr.mixit.android.provider.MixItContract.Slots;
import fr.mixit.android.provider.MixItContract.SlotsColumns;
import fr.mixit.android.provider.MixItContract.Tracks;
import fr.mixit.android.provider.MixItContract.TracksColumns;
import fr.mixit.android.provider.MixItContract.Sync;
import fr.mixit.android.provider.MixItContract.SyncColumns;


/**
 * Helper for managing {@link SQLiteDatabase} that stores data for
 * {@link MixItProvider}.
 */
public class MixItDatabase extends SQLiteOpenHelper {

	private static final String TAG = "MixItDatabase";

	private static final String DATABASE_NAME = "mixit.db";

	private static final int DATABASE_VERSION = 1;


	interface Tables {
		String SESSIONS = "sessions";
		String SPEAKERS = "speakers";
		String SLOTS = "slots";
		String TRACKS = "tracks";
		String SYNC = "sync";

		String SESSIONS_SPEAKERS = "sessions_speakers";

		String SESSIONS_SEARCH = "sessions_search";
		String SPEAKERS_SEARCH = "speakers_search";

		String SEARCH_SUGGEST = "search_suggest";

		String SESSIONS_JOIN_SLOTS_TRACKS = "sessions "
		    + "LEFT OUTER JOIN slots ON sessions.slot_id=slots.slot_id "
			+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

		String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
		    + "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

		String SESSIONS_SPEAKERS_JOIN_SESSIONS_SLOTS_TRACKS = "sessions_speakers "
		    + "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
		    + "LEFT OUTER JOIN slots ON sessions.slot_id=slots.slot_id "
			+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

		String SESSIONS_SEARCH_JOIN_SESSIONS_SLOTS_TRACKS = "sessions_search "
		    + "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
		    + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
		    + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id "
			+ "LEFT OUTER JOIN tracks ON sessions.track_id=tracks.track_id";

		String SPEAKERS_SEARCH_JOIN_SPEAKERS = "speakers_search "
		    + "LEFT OUTER JOIN speakers ON speakers_search.speaker_id=speakers.speaker_id";

	}

	private interface Triggers {
	    String SESSIONS_SEARCH_INSERT = "sessions_search_insert";
	    String SESSIONS_SEARCH_DELETE = "sessions_search_delete";
	    String SESSIONS_SEARCH_UPDATE = "sessions_search_update";

	    String SPEAKERS_SEARCH_INSERT = "speakers_search_insert";
	    String SPEAKERS_SEARCH_DELETE = "speakers_search_delete";
	    String SPEAKERS_SEARCH_UPDATE = "speakers_search_update";
	}

	public interface SessionsSpeakers {
	    String SESSION_ID = "session_id";
	    String SPEAKER_ID = "speaker_id";
	}

	interface SessionsSearchColumns {
	    String SESSION_ID = "session_id";
	    String BODY = "body";
	}

	interface SpeakersSearchColumns {
	    String SPEAKER_ID = "speaker_id";
	    String BODY = "body";
	}

	/** Fully-qualified field names. */
	private interface Qualified {
	    String SESSIONS_SEARCH_SESSION_ID = Tables.SESSIONS_SEARCH + "."
	            + SessionsSearchColumns.SESSION_ID;

	    String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "(" + SessionsSearchColumns.SESSION_ID
	            + "," + SessionsSearchColumns.BODY + ")";

	    String SPEAKERS_SEARCH_SPEAKER_ID = Tables.SPEAKERS_SEARCH + "."
		    + SpeakersSearchColumns.SPEAKER_ID;

	    String SPEAKERS_SEARCH = Tables.SPEAKERS_SEARCH + "(" + SpeakersSearchColumns.SPEAKER_ID
		    + "," + SpeakersSearchColumns.BODY + ")";
	}

	/**
	 * {@code REFERENCES} clauses.
	 */
	private interface References {
		String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")";
		String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")";
		String SLOT_ID = "REFERENCES " + Tables.SLOTS + "(" + Slots.SLOT_ID + ")";
		String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + Tracks.TRACK_ID + ")";
	}

	private interface Subquery {
	    /**
	     * Subquery used to build the {@link SessionsSearchColumns#BODY} string
	     * used for indexing {@link Sessions} content.
	     */
	    String SESSIONS_BODY = "(new." + Sessions.TITLE + "||'; '||new." + Sessions.SUMMARY + ")";

	    /**
	     * Subquery used to build the {@link SpeakersSearchColumns#BODY} string
	     * used for indexing {@link Speakers} content.
	     */
	    String SPEAKERS_BODY = "(new." + Speakers.FIRST_NAME + "||'; '||new." + Speakers.LAST_NAME
			    + "||'; '||new." + Speakers.COMPANY + "||'; '||" + "new." + Speakers.BIO
			    + ")";
	}


	public MixItDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate()");

		db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
		        + Sessions.SLOT_ID + " TEXT " + References.SLOT_ID + ","
				+ Sessions.TRACK_ID + " TEXT " + References.TRACK_ID + ","
				+ SessionsColumns.ROOM + " TEXT,"
		        + SessionsColumns.TITLE + " TEXT,"
		        + SessionsColumns.SUMMARY + " TEXT,"
		        + SessionsColumns.STARRED + " INTEGER NOT NULL DEFAULT 0,"
		        + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
		        + SpeakersColumns.FIRST_NAME + " TEXT,"
		        + SpeakersColumns.LAST_NAME + " TEXT,"
		        + SpeakersColumns.BIO + " TEXT,"
		        + SpeakersColumns.COMPANY + " TEXT,"
		        + SpeakersColumns.IMAGE_URL + " TEXT,"
		        + "UNIQUE (" + SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SLOTS + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + SlotsColumns.SLOT_ID + " TEXT NOT NULL,"
		        + SlotsColumns.SLOT_START + " INTEGER NOT NULL,"
		        + SlotsColumns.SLOT_END + " INTEGER NOT NULL,"
		        + SlotsColumns.SLOT_TYPE + " TEXT NOT NULL,"
		        + "UNIQUE (" + SlotsColumns.SLOT_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.TRACKS + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + TracksColumns.TRACK_ID + " TEXT NOT NULL,"
		        + TracksColumns.TRACK_NAME + " TEXT,"
		        + TracksColumns.TRACK_COLOR + " TEXT,"
		        + "UNIQUE (" + TracksColumns.TRACK_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SESSIONS_SPEAKERS + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + SessionsSpeakers.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
		        + SessionsSpeakers.SPEAKER_ID + " TEXT NOT NULL " + References.SPEAKER_ID + ","
		        + "UNIQUE (" + SessionsSpeakers.SESSION_ID + ","
		                + SessionsSpeakers.SPEAKER_ID + ") ON CONFLICT REPLACE)");

		db.execSQL("CREATE TABLE " + Tables.SYNC + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + SyncColumns.URI_ID + " TEXT NOT NULL,"
		        + SyncColumns.URI + " TEXT NOT NULL,"
		        + SyncColumns.MD5 + " TEXT NOT NULL,"
		        + "UNIQUE (" + SyncColumns.URI_ID + ") ON CONFLICT REPLACE)");

		createSessionsSearch(db, true);
		createSpeakersSearch(db, true);

		db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
		        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
		        + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL,"
				+ "UNIQUE (" + SearchManager.SUGGEST_COLUMN_TEXT_1 + ") ON CONFLICT REPLACE)");

		createIndices(db);
	}

	private static void createSessionsSearch(SQLiteDatabase db, boolean createTriggers) {
	    // Using the "porter" tokenizer for simple stemming, so that
	    // "frustration" matches "frustrated."

	    db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH + " USING fts3("
	            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
	            + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
	            + SessionsSearchColumns.SESSION_ID
	                    + " TEXT NOT NULL " + References.SESSION_ID + ","
	            + "UNIQUE (" + SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
	            + "tokenize=porter)");

	    if (createTriggers) createSessionsSearchTriggers(db);
	}

	/**
	 * Create triggers that automatically build {@link Tables#SESSIONS_SEARCH}
	 * as values are changed in {@link Tables#SESSIONS}.
	 */
	private static void createSessionsSearchTriggers(SQLiteDatabase db) {
	    db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT + " AFTER INSERT ON "
	            + Tables.SESSIONS + " BEGIN INSERT INTO " + Qualified.SESSIONS_SEARCH + " "
	            + " VALUES(new." + Sessions.SESSION_ID + ", " + Subquery.SESSIONS_BODY + ");"
	            + " END;");

	    db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE + " AFTER DELETE ON "
	            + Tables.SESSIONS + " BEGIN DELETE FROM " + Tables.SESSIONS_SEARCH + " "
	            + " WHERE " + Qualified.SESSIONS_SEARCH_SESSION_ID + "=old." + Sessions.SESSION_ID
	            + ";" + " END;");

	    db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE + " AFTER UPDATE OF "
	            + Sessions.TITLE + ", " + Sessions.SUMMARY
			    + " ON " + Tables.SESSIONS + " BEGIN UPDATE " + Tables.SESSIONS_SEARCH
	            + " SET " + SessionsSearchColumns.BODY + " = " + Subquery.SESSIONS_BODY
	            + " WHERE " + Qualified.SESSIONS_SEARCH_SESSION_ID + "=old." + Sessions.SESSION_ID
	            + ";" + " END;");
	}

	private static void createSpeakersSearch(SQLiteDatabase db, boolean createTriggers) {
	    // Using the "porter" tokenizer for simple stemming, so that
	    // "frustration" matches "frustrated."

	    db.execSQL("CREATE VIRTUAL TABLE " + Tables.SPEAKERS_SEARCH + " USING fts3("
	            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
	            + SpeakersSearchColumns.BODY + " TEXT NOT NULL,"
	            + SpeakersSearchColumns.SPEAKER_ID
	                    + " TEXT NOT NULL " + References.SPEAKER_ID + ","
	            + "UNIQUE (" + SpeakersSearchColumns.SPEAKER_ID + ") ON CONFLICT REPLACE,"
	            + "tokenize=porter)");

	    if (createTriggers) createSpeakersSearchTriggers(db);
	}

	/**
	 * Create triggers that automatically build {@link Tables#SPEAKERS_SEARCH}
	 * as values are changed in {@link Tables#SPEAKERS}.
	 */
	private static void createSpeakersSearchTriggers(SQLiteDatabase db) {
	    db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_INSERT + " AFTER INSERT ON "
	            + Tables.SPEAKERS + " BEGIN INSERT INTO " + Qualified.SPEAKERS_SEARCH + " "
	            + " VALUES(new." + Speakers.SPEAKER_ID + ", " + Subquery.SPEAKERS_BODY + ");"
	            + " END;");

	    db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_DELETE + " AFTER DELETE ON "
	            + Tables.SPEAKERS + " BEGIN DELETE FROM " + Tables.SPEAKERS_SEARCH + " "
	            + " WHERE " + Qualified.SPEAKERS_SEARCH_SPEAKER_ID + "=old." + Speakers.SPEAKER_ID
	            + ";" + " END;");

	    db.execSQL("CREATE TRIGGER " + Triggers.SPEAKERS_SEARCH_UPDATE + " AFTER UPDATE OF "
	            + Speakers.FIRST_NAME + ", " + Speakers.LAST_NAME + ", " + Speakers.COMPANY
	            + ", " + Speakers.BIO + " ON " + Tables.SPEAKERS + " BEGIN UPDATE "
	            + Tables.SPEAKERS_SEARCH + " SET " + SpeakersSearchColumns.BODY + " = "
	            + Subquery.SPEAKERS_BODY + " WHERE " + Qualified.SPEAKERS_SEARCH_SPEAKER_ID
	            + "=old." + Speakers.SPEAKER_ID + ";" + " END;");
	}


	private static void createIndices(SQLiteDatabase db) {
		db.execSQL("CREATE INDEX "
				+ Tables.SESSIONS + "_" + Sessions.SESSION_ID + "_IDX ON "
	    		+ Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")");
		db.execSQL("CREATE INDEX "
				+ Tables.SESSIONS + "_" + Sessions.SLOT_ID + "_IDX ON "
	    		+ Tables.SESSIONS + "(" + Sessions.SLOT_ID + ")");
		db.execSQL("CREATE INDEX "
				+ Tables.SESSIONS + "_" + Sessions.TRACK_ID + "_IDX ON "
	    		+ Tables.SESSIONS + "(" + Sessions.TRACK_ID + ")");
		db.execSQL("CREATE INDEX "
				+ Tables.SESSIONS + "_" + Sessions.STARRED + "_IDX ON "
	    		+ Tables.SESSIONS + "(" + Sessions.STARRED + ")");

		db.execSQL("CREATE INDEX "
				+ Tables.SPEAKERS + "_" + Speakers.SPEAKER_ID + "_IDX ON "
				+ Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")");

		db.execSQL("CREATE INDEX "
				+ Tables.SLOTS + "_" + Slots.SLOT_ID + "_IDX ON "
	    		+ Tables.SLOTS + "(" + Slots.SLOT_ID + ")");

		db.execSQL("CREATE INDEX "
				+ Tables.TRACKS + "_" + Tracks.TRACK_ID + "_IDX ON "
	    		+ Tables.TRACKS + "(" + Tracks.TRACK_ID + ")");

		db.execSQL("CREATE INDEX "
				+ Tables.SESSIONS_SPEAKERS + "_" + SessionsSpeakers.SESSION_ID + "_IDX ON "
	    		+ Tables.SESSIONS_SPEAKERS + "(" + SessionsSpeakers.SESSION_ID + ")");
		db.execSQL("CREATE INDEX "
				+ Tables.SESSIONS_SPEAKERS + "_" + SessionsSpeakers.SPEAKER_ID + "_IDX ON "
	    		+ Tables.SESSIONS_SPEAKERS + "(" + SessionsSpeakers.SPEAKER_ID + ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);
		Log.e(TAG, "TODO : onUpgrade() from " + oldVersion + " to " + newVersion + " I MUST KEEP STARRED DATA !!!");

		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");

		db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SLOTS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SYNC);

		db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_INSERT);
		db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_DELETE);
		db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SESSIONS_SEARCH_UPDATE);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);

		db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SPEAKERS_SEARCH_INSERT);
		db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SPEAKERS_SEARCH_DELETE);
		db.execSQL("DROP TRIGGER IF EXISTS " + Triggers.SPEAKERS_SEARCH_UPDATE);
		db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS_SEARCH);

		db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

		onCreate(db);
	}

}
