package fr.mixit.android.service;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import fr.mixit.android.model.SessionStarred;

class PersistentStarredStore implements StarredStore {

	private static final boolean mDebugMode = true;
	private static final String TAG = "PersistentStarredStore";


	static class DataBaseHelper extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "mixit_starred.db";
		private static final int DATABASE_VERSION = 1;

		public void onCreate(SQLiteDatabase sqlitedatabase) {

			if (mDebugMode)
				Log.d(TAG, "onCreate");

			sqlitedatabase.execSQL((new StringBuilder()).append("CREATE TABLE starred_sessions (")
					.append(String.format(" '%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,", new Object[] { EVENT_ID }))
					.append(String.format(" '%s' INTEGER NOT NULL,", new Object[] { STARRED_SESSION_ID }))
					.append(String.format(" '%s' INTEGER NOT NULL)", new Object[] { STARRED_STATE })).toString());
		}

		public void onUpgrade(SQLiteDatabase sqlitedatabase, int oldVersion, int newVersion) {

			if (mDebugMode)
				Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
						+ ", which will destroy all old data");
		}

		public DataBaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
	}

	private static final int MAX_EVENTS = 1000;
	private static final String EVENT_ID = "event_id";
	private static final String STARRED_SESSION_ID = "session_id";
	private static final String STARRED_STATE = "starred_state";

	private static final String REQUEST_SELECT_COUNT = "SELECT COUNT(*) from starred_sessions";
	private static final String ST_SQL_ROW_ID = "Row ID: ";
	private static final String ST_SQL_EVENT_ID = ", Event ID: ";
	private static final String ST_SQL_EVENT_ID_DESC = "event_id DESC";

	private static final String ST_EVENTS = "starred_sessions";
	private static final String ST_EVENT_ID = "event_id";
	private static final String ST_STARRED_SESSION_ID = "session_id";
	private static final String ST_STARRED_STATE = "starred_state";

	private static final String ST_EVENT_ID_EQUAL = "event_id=";

	private DataBaseHelper databaseHelper;

	private int numStoredEvents;
	private SQLiteStatement compiledCountStatement;

	PersistentStarredStore(DataBaseHelper databasehelper) {
		compiledCountStatement = null;
		databaseHelper = databasehelper;
		try {
			databasehelper.getWritableDatabase().close();
		} catch (SQLiteException sqliteexception) {
			Log.e(TAG, sqliteexception.toString());
		}
	}

	public void deleteSessionStarred(long l) {

		if (mDebugMode)
			Log.d(TAG, "deleteSessionStarred(long l)");

		String s = (new StringBuilder()).append(ST_EVENT_ID_EQUAL).append(l).toString();
		try {
			SQLiteDatabase sqlitedatabase = databaseHelper.getWritableDatabase();
			if (sqlitedatabase.delete(ST_EVENTS, s, null) != 0) {
				numStoredEvents--;
			}
		} catch (SQLiteException sqliteexception) {
			Log.e(TAG, sqliteexception.toString());
		}
	}

	public SessionStarred[] peekSessionStarreds() {
		return peekSessionStarreds(MAX_EVENTS);
	}

	public SessionStarred[] peekSessionStarreds(int i) {
		if (mDebugMode)
			Log.d(TAG, "peekSessionStarreds(int i)");

		ArrayList<SessionStarred> arraylist = new ArrayList<SessionStarred>();
		Cursor cursor = null;
		SQLiteDatabase sqlitedatabase = databaseHelper.getReadableDatabase();
		SessionStarred event;
		try {
			for (cursor = sqlitedatabase.query(ST_EVENTS, null, null, null, null, null, ST_EVENT_ID, Integer.toString(i)); cursor
					.moveToNext(); arraylist.add(event)) {
				final int sessionId = cursor.getInt(cursor.getColumnIndex(ST_STARRED_SESSION_ID));
				final int starredStatus = cursor.getInt(cursor.getColumnIndex(ST_STARRED_STATE));
				final long eventId = cursor.getLong(cursor.getColumnIndex(ST_EVENT_ID));
				event = new SessionStarred(sessionId, starredStatus == 1 ? true : false);
				event.setEventId(eventId);
				if (mDebugMode) {
					Log.d(TAG, "get " + eventId + " : sessionid : " + sessionId + " - starredStatus : " + starredStatus);
				}
			}

			if (cursor != null) {
				cursor.close();
			}
		} catch (SQLiteException sqliteexception) {

			Log.e(TAG, sqliteexception.toString());
			SessionStarred[] sessionStarred = new SessionStarred[0];
			if (cursor != null) {
				cursor.close();
			}
			return sessionStarred;
		} catch (Exception exception) {
			if (cursor != null) {
				cursor.close();
			}
		}
		return arraylist.toArray(new SessionStarred[arraylist.size()]);
	}

	public void putSessionStarred(SessionStarred sessionStarred) {
		if (mDebugMode)
			Log.d(TAG, "putSessionStarred(SessionStarred sessionStarred)");

		SQLiteDatabase sqlitedatabase;
		if (numStoredEvents >= 1000) {
			Log.w(TAG, "Store full. Not storing last event.");
			return;
		}

		sqlitedatabase = null;
		sqlitedatabase = databaseHelper.getWritableDatabase();
		sqlitedatabase.beginTransaction();
		ContentValues contentvalues = new ContentValues();
		contentvalues.put(ST_STARRED_SESSION_ID, sessionStarred.getIdSession());
		contentvalues.put(ST_STARRED_STATE, sessionStarred.isSessionStarred() ? 1 : 0);

		try {
			long l = sqlitedatabase.insert(ST_EVENTS, ST_EVENT_ID, contentvalues);
			if (l != -1L) {
				numStoredEvents++;
				Cursor cursor = sqlitedatabase.query(ST_EVENTS, new String[] { ST_EVENT_ID }, null, null, null, null,
						ST_SQL_EVENT_ID_DESC, null);
				cursor.moveToPosition(0);
				long l1 = cursor.getLong(0);
				Log.d(TAG, (new StringBuilder("putSessionStarred")).append(ST_SQL_ROW_ID).append(l).append(ST_SQL_EVENT_ID).append(l1).toString());
				cursor.close();

				sqlitedatabase.setTransactionSuccessful();
			} else {
				Log.d(TAG, "Error when attempting to add event to database.");
			}
			if (sqlitedatabase != null) {
				sqlitedatabase.endTransaction();
			}
		} catch (SQLiteException sqliteexception) {
			Log.e(TAG, sqliteexception.toString());
			if (sqlitedatabase != null) {
				sqlitedatabase.endTransaction();
			}
		} catch (Exception exception) {
			if (sqlitedatabase != null) {
				sqlitedatabase.endTransaction();
			}
		}
	}

	public int getNumStoredSessionStarreds() {

		if (mDebugMode)
			Log.d(TAG, "getNumStoredSessionStarreds()");
		try {
			if (compiledCountStatement == null) {
				compiledCountStatement = databaseHelper.getReadableDatabase().compileStatement(REQUEST_SELECT_COUNT);
			}
			return (int) compiledCountStatement.simpleQueryForLong();
		} catch (SQLiteException sqliteexception) {
			Log.e(TAG, sqliteexception.toString());
		}
		return 0;

	}

	@Override
	public void closeDataBase() {
		if (databaseHelper != null)
			databaseHelper.close();
	}
}
