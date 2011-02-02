package fr.mixit.android.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import fr.mixit.android.utils.ParserUtils;

/**
 * Created by IntelliJ IDEA.
 * User: mathieu
 * Date: 1/30/11
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class MixItContract {

	interface SpeakersColumns {

		/**
		 * Unique string identifying this speaker.
		 */
		String SPEAKER_ID = "speaker_id";
		/**
		 * First name of this speaker.
		 */
		String FIRST_NAME = "first_name";
		/**
		 * Last name of this speaker.
		 */
		String LAST_NAME = "last_name";
		/**
		 * Company this speaker works for.
		 */
		String COMPANY = "company";
		/**
		 * Body of text describing this speaker in detail.
		 */
		String BIO = "bio";
		/**
		 * URL towards image of speaker.
		 */
		String IMAGE_URL = "image_url";
		/**
		 * User-specific flag indicating starred status.
		 */
		String STARRED = "starred";
	}


	public static final String CONTENT_AUTHORITY = "fr.mixit.android";

	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	private static final String PATH_SPEAKERS = "speakers";


	/**
	 * Speakers are individual people that lead Sessions.
	 */
	public static class Speakers implements SpeakersColumns, BaseColumns {

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_SPEAKERS).build();

		public static final String CONTENT_TYPE =
				"vnd.android.cursor.dir/vnd.mixit.speaker";
		public static final String CONTENT_ITEM_TYPE =
				"vnd.android.cursor.item/vnd.mixit.speaker";

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

		public static String getSpeakerId(Uri uri) {
			return uri.getPathSegments().get(1);
		}

		/**
		 * Generate a {@link #SPEAKER_ID} that will always match the requested
		 * {@link Speakers} details.
		 */
		public static String generateSpeakerId(String id) {
		    return ParserUtils.sanitizeId(id);
		}

	}


	private MixItContract() {
	}

}
