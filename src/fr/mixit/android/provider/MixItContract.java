package fr.mixit.android.provider;

import android.app.SearchManager;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import fr.mixit.android.utils.ParserUtils;

/**
 * Created by IntelliJ IDEA.
 * User: mathieu
 * Date: 1/30/11
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class MixItContract {


	interface SyncColumns {
		String URI_ID = "uri_id";
		String URI = "uri";
		String MD5 = "md5";
	}

	interface SlotsColumns {
	    /** Unique string identifying this slot of time. */
	    String SLOT_ID = "slot_id";
	    /** Time when this slot starts. */
	    String SLOT_START = "slot_start";
	    /** Time when this slot ends. */
	    String SLOT_END = "slot_end";
	    /** Type describing this slot. */
	    String SLOT_TYPE = "slot_type";
	}

	interface TracksColumns {
	    /** Unique string identifying this track. */
	    String TRACK_ID = "track_id";
	    /** Name describing this track. */
	    String TRACK_NAME = "track_name";
	    /** Color used to identify this track, in {@link android.graphics.Color#argb} format. */
	    String TRACK_COLOR = "track_color";
	}

	interface SessionsColumns {
	    /** Unique string identifying this session. */
		String SESSION_ID = "session_id";
	    /** Title describing this session. */
		String TITLE = "title";
	    /** Body of text explaining this session in detail. */
		String SUMMARY = "summary";
	    /** Room where the session will happen. */
		String ROOM = "room";
	    /** User-specific flag indicating starred status. */
		String STARRED = "starred";
	}

	interface SpeakersColumns {

		/** Unique string identifying this speaker. */
		String SPEAKER_ID = "speaker_id";
		/** First name of this speaker. */
		String FIRST_NAME = "first_name";
		/** Last name of this speaker. */
		String LAST_NAME = "last_name";
		/** Company this speaker works for. */
		String COMPANY = "company";
		/** Body of text describing this speaker in detail. */
		String BIO = "bio";
		/** URL towards image of speaker. */
		String IMAGE_URL = "image_url";
		/** User-specific flag indicating starred status. */
		String STARRED = "starred";
	}

	interface TagsColumns {
		/** Unique string identifying this tag. */
		String TAG_ID = "tag_id";
		/** Tag name. */
		String TAG_NAME = "tag_name";
	}


	public static final String CONTENT_AUTHORITY = "fr.mixit.android";

	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_SESSIONS = "sessions";
	private static final String PATH_SPEAKERS = "speakers";
	private static final String PATH_SLOTS = "slots";
    private static final String PATH_TRACKS = "tracks";
	private static final String PATH_TAGS = "tags";

	private static final String PATH_STARRED = "starred";
	private static final String PATH_AT = "at";
	private static final String PATH_BETWEEN = "between";
	private static final String PATH_PARALLEL = "parallel";
	private static final String PATH_NEXT = "next";

	private static final String PATH_SEARCH = "search";
	private static final String PATH_SEARCH_SUGGEST = "search_suggest_query";
	private static final String PATH_SYNC = "sync";


	/**
	 * Slots are generic timeslots that {@link Sessions} and other related
	 * events fall into.
	 */
	public static class Slots implements SlotsColumns, BaseColumns {
	    public static final Uri CONTENT_URI =
	            BASE_CONTENT_URI.buildUpon().appendPath(PATH_SLOTS).build();

	    public static final String CONTENT_TYPE =
	            "vnd.android.cursor.dir/vnd.mixit.slot";
	    public static final String CONTENT_ITEM_TYPE =
	            "vnd.android.cursor.item/vnd.mixit.slot";

	    /** Count of {@link Sessions} inside given slot. */
	    public static final String SESSIONS_COUNT = "sessions_count";

	    /**
	     * Flag indicating that at least one {@link Sessions#SESSION_ID} inside
	     * this slot has {@link Sessions#STARRED} set.
	     */
	    public static final String CONTAINS_STARRED = "contains_starred";

	    /** Default "ORDER BY" clause. */
	    public static final String DEFAULT_SORT = SlotsColumns.SLOT_START + " ASC, "
	            + SlotsColumns.SLOT_END + " ASC";

	    /** Build {@link Uri} for requested {@link #SLOT_ID}. */
	    public static Uri buildSlotUri(String slotId) {
	        return CONTENT_URI.buildUpon().appendPath(slotId).build();
	    }

	    /**
	     * Build {@link Uri} that references any {@link Sessions} associated
	     * with the requested {@link #SLOT_ID}.
	     */
	    public static Uri buildSessionsUri(String slotId) {
	        return CONTENT_URI.buildUpon().appendPath(slotId).appendPath(PATH_SESSIONS).build();
	    }

	    /**
	     * Build {@link Uri} that references any {@link Slots} that occur
	     * between the requested time boundaries.
	     */
	    public static Uri buildSlotsBetweenDirUri(long startTime, long endTime) {
	        return CONTENT_URI.buildUpon().appendPath(PATH_BETWEEN).appendPath(
	                String.valueOf(startTime)).appendPath(String.valueOf(endTime)).build();
	    }

	    /** Read {@link #SLOT_ID} from {@link Slots} {@link Uri}. */
	    public static String getSlotId(Uri uri) {
	        return uri.getPathSegments().get(1);
	    }

	    /**
	     * Generate a {@link #SLOT_ID} that will always match the requested
	     * {@link Slots} details.
	     */
	    public static String generateSlotId(String kind, long startTime, long endTime) {
	        startTime /= DateUtils.SECOND_IN_MILLIS;
	        endTime /= DateUtils.SECOND_IN_MILLIS;
	        return ParserUtils.sanitizeId(kind + "-" + startTime + "-" + endTime);
	    }
	}

	/**
	 * Tracks are overall categories for {@link Sessions},
	 * such as "Desktop/RIA/Mobile" or "Cloud/NoSQL."
	 */
	public static class Tracks implements TracksColumns, BaseColumns {
	    public static final Uri CONTENT_URI =
	            BASE_CONTENT_URI.buildUpon().appendPath(PATH_TRACKS).build();

	    public static final String CONTENT_TYPE =
	            "vnd.android.cursor.dir/vnd.mixit.track";
	    public static final String CONTENT_ITEM_TYPE =
	            "vnd.android.cursor.item/vnd.mixit.track";

	    /** Count of {@link Sessions} inside given track. */
	    public static final String SESSIONS_COUNT = "sessions_count";

	    /** Default "ORDER BY" clause. */
	    public static final String DEFAULT_SORT = TracksColumns.TRACK_NAME + " ASC";

	    /** Build {@link Uri} for requested {@link #TRACK_ID}. */
	    public static Uri buildTrackUri(String trackId) {
	        return CONTENT_URI.buildUpon().appendPath(trackId).build();
	    }

	    /**
	     * Build {@link Uri} that references any {@link Sessions} associated
	     * with the requested {@link #TRACK_ID}.
	     */
	    public static Uri buildSessionsUri(String trackId) {
	        return CONTENT_URI.buildUpon().appendPath(trackId).appendPath(PATH_SESSIONS).build();
	    }

	    /** Read {@link #TRACK_ID} from {@link Tracks} {@link Uri}. */
	    public static String getTrackId(Uri uri) {
	        return uri.getPathSegments().get(1);
	    }

	    /**
	     * Generate a {@link #TRACK_ID} that will always match the requested
	     * {@link Tracks} details.
	     */
	    public static String generateTrackId(String title) {
	        return ParserUtils.sanitizeId(title);
	    }
	}

	/**
	 * Tags.
	 */
	public static class Tags implements TagsColumns, BaseColumns {
	    public static final Uri CONTENT_URI =
	            BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAGS).build();

	    public static final String CONTENT_TYPE =
	            "vnd.android.cursor.dir/vnd.mixit.tag";
	    public static final String CONTENT_ITEM_TYPE =
	            "vnd.android.cursor.item/vnd.mixit.tag";

	    /** Count of {@link Sessions} inside given tag. */
	    public static final String SESSIONS_COUNT = "sessions_count";

	    /** Default "ORDER BY" clause. */
	    public static final String DEFAULT_SORT = TagsColumns.TAG_NAME + " ASC";

	    /** Build {@link Uri} for requested {@link #TAG_ID}. */
	    public static Uri buildTagUri(String tagId) {
	        return CONTENT_URI.buildUpon().appendPath(tagId).build();
	    }

	    /**
	     * Build {@link Uri} that references any {@link Sessions} associated
	     * with the requested {@link #TAG_ID}.
	     */
	    public static Uri buildSessionsDirUri(String tagId) {
	        return CONTENT_URI.buildUpon().appendPath(tagId).appendPath(PATH_SESSIONS).build();
	    }

	    /** Read {@link #TAG_ID} from {@link Tags} {@link Uri}. */
	    public static String getTagId(Uri uri) {
	        return uri.getPathSegments().get(1);
	    }

	    /**
	     * Generate a {@link #TAG_ID} that will always match the requested
	     * {@link Tags} details.
	     */
	    public static String generateTagId(String tagName) {
	        return ParserUtils.sanitizeId(tagName);
	    }
	}

	/**
	 * Each session is a slot of time that has a {@link Tracks},
	 * and zero or more {@link Speakers}.
	 */
	public static class Sessions implements SessionsColumns, SlotsColumns, BaseColumns {
	    public static final Uri CONTENT_URI =
	            BASE_CONTENT_URI.buildUpon().appendPath(PATH_SESSIONS).build();
		public static final Uri CONTENT_STARRED_URI =
		    CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();

	    public static final String CONTENT_TYPE =
	            "vnd.android.cursor.dir/vnd.mixit.session";
	    public static final String CONTENT_ITEM_TYPE =
	            "vnd.android.cursor.item/vnd.mixit.session";

		public static final String SLOT_ID = "slot_id";
	    public static final String TRACK_ID = "track_id";

		public static final String STARRED_IN_SLOT_COUNT = "starred_in_slot_count";

        public static final String SEARCH_SNIPPET = "search_snippet";

	    /** Default "ORDER BY" clause. */
	    public static final String DEFAULT_SORT = MixItDatabase.Tables.SESSIONS + "." + SessionsColumns.SESSION_ID + " ASC";

	    /** Build {@link Uri} for requested {@link #SESSION_ID}. */
	    public static Uri buildSessionUri(String sessionId) {
	        return CONTENT_URI.buildUpon().appendPath(sessionId).build();
	    }

	    /**
	     * Build {@link Uri} that references any {@link Speakers} associated
	     * with the requested {@link #SESSION_ID}.
	     */
	    public static Uri buildSpeakersDirUri(String sessionId) {
	        return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_SPEAKERS).build();
	    }

		/**
		 * Build {@link Uri} that references any {@link Tags} associated
		 * with the requested {@link #SESSION_ID}.
		 */
		public static Uri buildTagsDirUri(String sessionId) {
		    return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_TAGS).build();
		}

	    /** Build {@link Uri} for requested {@link #SESSION_ID} for given {@link Speakers} with given {@link #SPEAKER_ID} */
	    public static Uri buildSessionSpeakerUri(String sessionId, String speakerId) {
	        return CONTENT_URI.buildUpon().appendPath(sessionId).appendPath(PATH_SPEAKERS).appendPath(speakerId).build();
	    }

	    public static Uri buildSessionsAtDirUri(long time) {
	        return CONTENT_URI.buildUpon().appendPath(PATH_AT).appendPath(String.valueOf(time))
	                .build();
	    }

	    public static Uri buildSessionsParallelDirUri(String sessionId) {
	        return CONTENT_URI.buildUpon().appendPath(PATH_PARALLEL).appendPath(sessionId).build();
	    }

	    public static Uri buildSessionsNextDirUri(long time) {
	        return CONTENT_URI.buildUpon().appendPath(PATH_NEXT).appendPath(String.valueOf(time))
	        .build();
	    }

		public static Uri buildSearchUri(String query) {
		    return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
		}

		public static boolean isSearchUri(Uri uri) {
		    return uri.getPathSegments().size() > 1
			    && PATH_SESSIONS.equals(uri.getPathSegments().get(0))
			    && PATH_SEARCH.equals(uri.getPathSegments().get(1));
		}

	    /** Read {@link #SESSION_ID} from {@link Sessions} {@link Uri}. */
	    public static String getSessionId(Uri uri) {
	        return uri.getPathSegments().get(1);
	    }

	    public static String getSpeakerId(Uri uri) {
	        return uri.getPathSegments().get(3);
	    }

		public static String getTagId(Uri uri) {
		    return uri.getPathSegments().get(3);
		}

		public static String getSearchQuery(Uri uri) {
		    return uri.getPathSegments().get(2);
		}

	    /**
	     * Generate a {@link #SESSION_ID} that will always match the requested
	     * {@link Sessions} details.
	     */
	    public static String generateSessionId(String title) {
	        return ParserUtils.sanitizeId(title);
	    }

	}

	/**
	 * Speakers are individual people that lead Sessions.
	 */
	public static class Speakers implements SpeakersColumns, BaseColumns {

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKERS).build();
		public static final Uri CONTENT_STARRED_URI =
		    CONTENT_URI.buildUpon().appendPath(PATH_STARRED).build();

		public static final String CONTENT_TYPE =
				"vnd.android.cursor.dir/vnd.mixit.speaker";
		public static final String CONTENT_ITEM_TYPE =
				"vnd.android.cursor.item/vnd.mixit.speaker";

		public static final String CONTAINS_STARRED = "contains_starred";

		public static final String SEARCH_SNIPPET = "search_snippet";

		/**
		 * Default "ORDER BY" clause.
		 */
		public static final String DEFAULT_SORT = "UPPER(" + SpeakersColumns.LAST_NAME + ") ASC, " + SpeakersColumns.FIRST_NAME + " ASC";

		/**
		 * Build {@link Uri} for requested {@link #SPEAKER_ID}.
		 */
		public static Uri buildSpeakerUri(String speakerId) {
			return CONTENT_URI.buildUpon().appendPath(speakerId).build();
		}

		/**
		 * Build {@link Uri} that references any {@link Sessions} associated
		 * with the requested {@link #SPEAKER_ID}.
		 */
		public static Uri buildSessionsDirUri(String speakerId) {
		    return CONTENT_URI.buildUpon().appendPath(speakerId).appendPath(PATH_SESSIONS).build();
		}

		/** Read {@link #SPEAKER_ID} from {@link Speakers} {@link Uri}. */
		public static Uri buildSearchUri(String query) {
		    return CONTENT_URI.buildUpon().appendPath(PATH_SEARCH).appendPath(query).build();
		}

		public static boolean isSearchUri(Uri uri) {
			return (uri.getPathSegments().size() > 1)
				&& PATH_SEARCH.equals(uri.getPathSegments().get(1));
		}

		public static String getSpeakerId(Uri uri) {
		    return uri.getPathSegments().get(1);
		}

		public static String getSearchQuery(Uri uri) {
		    return uri.getPathSegments().get(2);
		}

		/**
		 * Generate a {@link #SPEAKER_ID} that will always match the requested
		 * {@link Speakers} details.
		 */
		public static String generateSpeakerId(String id) {
		    return ParserUtils.sanitizeId(id);
		}

	}


	public static class Sync implements SyncColumns, BaseColumns {
	    public static final Uri CONTENT_URI =
	            BASE_CONTENT_URI.buildUpon().appendPath(PATH_SYNC).build();

	    public static final String CONTENT_TYPE =
	            "vnd.android.cursor.dir/vnd.mixit.sync";
	    public static final String CONTENT_ITEM_TYPE =
	            "vnd.android.cursor.item/vnd.mixit.sync";

	    public static final String DEFAULT_SORT = MixItDatabase.Tables.SYNC + "." + SyncColumns.URI + " ASC";

	    public static Uri buildSyncUri(String uriId) {
	        return CONTENT_URI.buildUpon().appendPath(uriId).build();
	    }

	    public static String getSyncId(Uri uri) {
	        return uri.getPathSegments().get(1);
	    }

	    public static String generateSyncId(String uri) {
	        return ParserUtils.sanitizeId(uri);
	    }

	}

	public static class SearchSuggest {
	    public static final Uri CONTENT_URI =
	            BASE_CONTENT_URI.buildUpon().appendPath(PATH_SEARCH_SUGGEST).build();

	    public static final String DEFAULT_SORT = SearchManager.SUGGEST_COLUMN_TEXT_1
	            + " COLLATE NOCASE ASC";
	}

	public final static class SessionCounts {
	    public static final String SESSION_INDEX_EXTRAS = "session_index_extras";

	    public static final String EXTRA_SESSION_INDEX_COUNTS = "session_index_counts";
	}


	private MixItContract() {
	}

}
