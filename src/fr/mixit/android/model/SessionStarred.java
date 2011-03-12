package fr.mixit.android.model;

/**
 * Created by mathieu
 * Date: 3/11/11
 * Time: 7:03 PM
 */
public class SessionStarred {

	private long mEventId;
	private int mIdSession;
	private boolean mIsSessionStarred;


	public SessionStarred(int idSession, boolean isSessionStarred) {
		super();
		mIdSession = idSession;
		mIsSessionStarred = isSessionStarred;
	}

	public long getEventId() {
		return mEventId;
	}

	public void setEventId(long eventId) {
		mEventId = eventId;
	}

	public int getIdSession() {
		return mIdSession;
	}

	public void setIdSession(int idSession) {
		mIdSession = idSession;
	}

	public boolean isSessionStarred() {
		return mIsSessionStarred;
	}

	public void setIsSessionStarred(boolean isSessionStarred) {
		mIsSessionStarred = isSessionStarred;
	}
}
