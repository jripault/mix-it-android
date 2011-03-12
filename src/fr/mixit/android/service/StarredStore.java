package fr.mixit.android.service;

import fr.mixit.android.model.SessionStarred;

interface StarredStore {

	public abstract void putSessionStarred(SessionStarred sessionStarred);

	public abstract SessionStarred[] peekSessionStarreds();

	public abstract SessionStarred[] peekSessionStarreds(int i);

	public abstract void deleteSessionStarred(long l);

	public abstract int getNumStoredSessionStarreds();

	public abstract int getStoreId();

	public abstract void closeDataBase();
}
