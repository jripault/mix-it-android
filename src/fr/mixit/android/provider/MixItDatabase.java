package fr.mixit.android.provider;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import fr.mixit.android.provider.MixItContract.Speakers;
import fr.mixit.android.provider.MixItContract.SpeakersColumns;


/**
 * Created by IntelliJ IDEA.
 * User: mathieu
 * Date: 1/30/11
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class MixItDatabase extends SQLiteOpenHelper {

	private static final String TAG = "MixItDatabase";

	private static final String DATABASE_NAME = "mixit.db";

	// NOTE: carefully update onUpgrade() when bumping database versions to make
	// sure user data is saved.

	private static final int DATABASE_VERSION = 1;


	interface Tables {

		String SPEAKERS = "speakers";
	}


	/**
	 * {@code REFERENCES} clauses.
	 */
	private interface References {

		String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")";
	}


	public MixItDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "onCreate()");

		db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " ("
				+ BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ SpeakersColumns.SPEAKER_ID + " TEXT NOT NULL,"
				+ SpeakersColumns.FIRST_NAME + " TEXT NOT NULL,"
				+ SpeakersColumns.LAST_NAME + " TEXT NOT NULL,"
				+ SpeakersColumns.BIO + " TEXT NOT NULL,"
				+ SpeakersColumns.COMPANY + " TEXT NOT NULL,"
				+ SpeakersColumns.IMAGE_URL + " TEXT NOT NULL,"
				+ "UNIQUE (" + SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");

//		createIndices(db);
	}


	private static void createIndices(SQLiteDatabase db) {
		db.execSQL("CREATE INDEX "
				+ Tables.SPEAKERS + "_" + Speakers.SPEAKER_ID + "_IDX ON "
				+ Tables.SPEAKERS + "(" + Speakers.SPEAKER_ID + ")");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);
		Log.e(TAG, "TODO : onUpgrade() from " + oldVersion + " to " + newVersion);

		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
		            + newVersion + ", which will destroy all old data");

		db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
		onCreate(db);
		// NOTE: This switch statement is designed to handle cascading database
		// updates, starting at the current version and falling through to all
		// future upgrade cases. Only use "break;" when you want to drop and
		// recreate the entire database.
/*        int version = oldVersion;
        switch (version) {
            case VER_LAUNCH:
                Log.d(TAG, "performing upgrade coming from VER_LAUNCH");

                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " ADD COLUMN "
                        + SessionsColumns.NOTE + " TEXT");
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE);

                createSessionsSearchTriggers(db);

                version = VER_ADD_NOTE_ON_SESSION;
            case VER_ADD_NOTE_ON_SESSION:
                Log.d(TAG, "performing upgrade coming from VER_ADD_NOTE_ON_SESSION");

                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT);
                db.execSQL("DROP TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE);

                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " RENAME TO tmp_"
                        + Tables.SESSIONS);

                db.execSQL("CREATE TABLE " + Tables.SESSIONS + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + SessionsColumns.SESSION_ID + " TEXT NOT NULL,"
                        + Sessions.SLOT_ID + " TEXT " + References.SLOT_ID + ","
                        + Sessions.ROOM_ID + " TEXT " + References.ROOM_ID + ","
                        + Sessions.TRACK_ID + " TEXT " + References.TRACK_ID + ","
                        + SessionsColumns.TITLE + " TEXT NOT NULL,"
                        + SessionsColumns.SUMMARY + " TEXT NOT NULL,"
                        + SessionsColumns.EXPERIENCE + " TEXT NOT NULL,"
                        + SessionsColumns.TYPE + " TEXT,"
                        + SessionsColumns.NOTE + " TEXT NOT NULL DEFAULT '',"
                        + SessionsColumns.STARRED + " INTEGER NOT NULL DEFAULT 0,"
                        + SessionsColumns.NEW + " INTEGER NOT NULL DEFAULT 0,"
                        + SessionsColumns.UPDATED + " INTEGER NOT NULL DEFAULT 0,"
                        + "UNIQUE (" + SessionsColumns.SESSION_ID + ") ON CONFLICT REPLACE)");

                createSessionsSearchTriggers(db);

                db.execSQL("INSERT INTO " + Tables.SESSIONS + "("
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.SLOT_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.NOTE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED + ")"
                        + " SELECT "
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.SLOT_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.NOTE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED
                        + " FROM tmp_" + Tables.SESSIONS
                        + " WHERE " + SessionsColumns.NOTE + " IS NOT NULL");

                db.execSQL("INSERT INTO " + Tables.SESSIONS + "("
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.SLOT_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED + ")"
                        + " SELECT "
                        + SessionsColumns.SESSION_ID + ", "
                        + Sessions.SLOT_ID + ", "
                        + Sessions.ROOM_ID + ", "
                        + Sessions.TRACK_ID + ", "
                        + SessionsColumns.TITLE + ", "
                        + SessionsColumns.SUMMARY + ", "
                        + SessionsColumns.EXPERIENCE + ", "
                        + SessionsColumns.TYPE + ", "
                        + SessionsColumns.STARRED + ", "
                        + SessionsColumns.NEW + ", "
                        + SessionsColumns.UPDATED
                        + " FROM tmp_" + Tables.SESSIONS
                        + " WHERE " + SessionsColumns.NOTE + " IS NULL");

                db.execSQL("DROP TABLE tmp_" + Tables.SESSIONS);

                version = VER_ALTER_NOTE_ON_SESSION;
            case VER_ALTER_NOTE_ON_SESSION:
                Log.d(TAG, "performing upgrade coming from VER_ALTER_NOTE_ON_SESSION");

                db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);

                createSessionsSearch(db, false);

                db.execSQL("INSERT INTO " + Qualified.SESSIONS_SEARCH
                        + " SELECT "
                        + SessionsColumns.SESSION_ID
                        + ", "
                        + SessionsColumns.TITLE
                        + "||'; '||"
                        + SessionsColumns.SUMMARY
                        + "||'; '||"
                        + SessionsColumns.EXPERIENCE
                        + "||'; '||"
                        + Sessions.NOTE
                        + " FROM " + Tables.SESSIONS);

                db.execSQL("INSERT INTO " + Tables.SEARCH_SUGGEST
                        + "(" + SearchManager.SUGGEST_COLUMN_TEXT_1 + ")"
                        + " VALUES('flex')");

                version = VER_RECREATE_FULLTEXT_TABLE;
            case VER_RECREATE_FULLTEXT_TABLE:
                Log.d(TAG, "performing upgrade coming from VER_RECREATE_FULLTEXT_TABLE");

                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'break-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Break')");
                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'breakfast-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Breakfast')");
                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'coffeebreak-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Coffee Break')");
                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'keynote-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Keynote')");
                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'lunch-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Lunch')");
                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'registration-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Registration')");
                db.execSQL("UPDATE " + Tables.SESSIONS
                        + " SET " + Sessions.SLOT_ID + "="
                        + "'talk-'||" + Sessions.SLOT_ID
                        + " WHERE " + Sessions.SLOT_ID + " IN "
                        + "(SELECT " + Slots.SLOT_ID + " FROM "
                        + Tables.SLOTS + " WHERE "
                        + Slots.SLOT_TYPE + "='Talk')");

                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'break-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Break'");
                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'breakfast-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Breakfast'");
                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'coffeebreak-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Coffee Break'");
                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'keynote-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Keynote'");
                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'lunch-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Lunch'");
                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'registration-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Registration'");
                db.execSQL("UPDATE " + Tables.SLOTS
                        + " SET " + Slots.SLOT_ID + "="
                        + "'talk-'||" + Slots.SLOT_ID
                        + " WHERE " + Slots.SLOT_TYPE + "='Talk'");

                version = VER_ADD_LABS_SESSIONS;
            case VER_ADD_LABS_SESSIONS:
                Log.d(TAG, "performing upgrade coming from VER_ADD_LABS_SESSIONS");

                db.execSQL("CREATE TABLE " + Tables.TAGS + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + TagsColumns.TAG_ID + " TEXT NOT NULL,"
                        + TagsColumns.TAG_NAME + " TEXT NOT NULL,"
                        + "UNIQUE (" + TagsColumns.TAG_ID + ") ON CONFLICT REPLACE)");

                db.execSQL("CREATE TABLE " + Tables.SESSIONS_TAGS + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + SessionsTags.SESSION_ID + " TEXT NOT NULL " + References.SESSION_ID + ","
                        + SessionsTags.TAG_ID + " TEXT NOT NULL " + References.TAG_ID + ","
                        + "UNIQUE (" + SessionsTags.SESSION_ID + ","
                                + SessionsTags.TAG_ID + ") ON CONFLICT REPLACE)");

                version = VER_ADD_TAGS_TABLES;
            case VER_ADD_TAGS_TABLES:
                Log.d(TAG, "performing upgrade coming from VER_ADD_TAGS_TABLES");

                db.execSQL("CREATE INDEX " + SessionsTags.TAG_ID + "_IDX ON "
                        + Tables.SESSIONS_TAGS + "(" + SessionsTags.TAG_ID + ")");

                version = VER_ADD_SESSION_TAGS_INDEX;
            case VER_ADD_SESSION_TAGS_INDEX:
                Log.d(TAG, "performing upgrade coming from VER_ADD_SESSION_TAGS_INDEX");

                db.execSQL("ALTER TABLE " + Tables.SEARCH_SUGGEST + " RENAME TO tmp_"
                        + Tables.SEARCH_SUGGEST);

                db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL,"
                        + "UNIQUE (" + SearchManager.SUGGEST_COLUMN_TEXT_1 + ") ON CONFLICT REPLACE)");

                db.execSQL("INSERT INTO " + Tables.SEARCH_SUGGEST + "("
                        + SearchManager.SUGGEST_COLUMN_TEXT_1 + ")"
                        + " SELECT "
                        + SearchManager.SUGGEST_COLUMN_TEXT_1
                        + " FROM tmp_" + Tables.SEARCH_SUGGEST);

                db.execSQL("DROP TABLE tmp_" + Tables.SEARCH_SUGGEST);

                version = VER_ALTER_SEARCH_SUGGEST_TABLE;
            case VER_ALTER_SEARCH_SUGGEST_TABLE:
                Log.d(TAG, "performing upgrade coming from VER_ALTER_SEARCH_SUGGEST_TABLE");

                db.execSQL("DROP INDEX " + SessionsTags.TAG_ID + "_IDX");

                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS + "_" + Sessions.SESSION_ID + "_IDX ON "
                        + Tables.SESSIONS + "(" + Sessions.SESSION_ID + ")");
                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS + "_" + Sessions.SLOT_ID + "_IDX ON "
                        + Tables.SESSIONS + "(" + Sessions.SLOT_ID + ")");
                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS + "_" + Sessions.ROOM_ID + "_IDX ON "
                        + Tables.SESSIONS + "(" + Sessions.ROOM_ID + ")");
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
                        + Tables.ROOMS + "_" + Rooms.ROOM_ID + "_IDX ON "
                        + Tables.ROOMS + "(" + Rooms.ROOM_ID + ")");

                db.execSQL("CREATE INDEX "
                        + Tables.SLOTS + "_" + Slots.SLOT_ID + "_IDX ON "
                        + Tables.SLOTS + "(" + Slots.SLOT_ID + ")");

                db.execSQL("CREATE INDEX "
                        + Tables.TRACKS + "_" + Tracks.TRACK_ID + "_IDX ON "
                        + Tables.TRACKS + "(" + Tracks.TRACK_ID + ")");

                db.execSQL("CREATE INDEX "
                        + Tables.TAGS + "_" + Tags.TAG_ID + "_IDX ON "
                        + Tables.TAGS + "(" + Tags.TAG_ID + ")");

                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS_SPEAKERS + "_" + SessionsSpeakers.SESSION_ID + "_IDX ON "
                        + Tables.SESSIONS_SPEAKERS + "(" + SessionsSpeakers.SESSION_ID + ")");
                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS_SPEAKERS + "_" + SessionsSpeakers.SPEAKER_ID + "_IDX ON "
                        + Tables.SESSIONS_SPEAKERS + "(" + SessionsSpeakers.SPEAKER_ID + ")");

                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS_TAGS + "_" + SessionsTags.SESSION_ID + "_IDX ON "
                        + Tables.SESSIONS_TAGS + "(" + SessionsTags.SESSION_ID + ")");
                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS_TAGS + "_" + SessionsTags.TAG_ID + "_IDX ON "
                        + Tables.SESSIONS_TAGS + "(" + SessionsTags.TAG_ID + ")");

                db.execSQL("CREATE INDEX "
                        + Tables.NOTES + "_" + Notes.SESSION_ID + "_IDX ON "
                        + Tables.NOTES + "(" + Notes.SESSION_ID + ")");

                version = VER_ADD_INDICES;
            case VER_ADD_INDICES:
                Log.d(TAG, "performing upgrade coming from VER_ADD_INDICES");

                db.execSQL("ALTER TABLE " + Tables.SESSIONS + " ADD COLUMN "
                        + Sessions.TYPE_ID + " TEXT " + References.TYPE_ID);

                db.execSQL("CREATE TABLE " + Tables.TYPES + " ("
                        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + TypesColumns.TYPE_ID + " TEXT NOT NULL,"
                        + TypesColumns.TYPE_NAME + " TEXT NOT NULL,"
                        + TypesColumns.TYPE_DESCRIPTION + " TEXT NOT NULL,"
                        + "UNIQUE (" + TypesColumns.TYPE_ID + ") ON CONFLICT REPLACE)");

                db.execSQL("CREATE INDEX "
                        + Tables.SESSIONS + "_" + Sessions.TYPE_ID + "_IDX ON "
                        + Tables.SESSIONS + "(" + Sessions.TYPE_ID + ")");

                version = VER_ADD_SESSION_TYPES;
        }

        Log.d(TAG, "after upgrade logic, at version " + version);
        if (version != DATABASE_VERSION) {
            Log.w(TAG, "Destroying old data during upgrade");

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
        }   */
	}

}
