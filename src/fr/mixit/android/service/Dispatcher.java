package fr.mixit.android.service;

import fr.mixit.android.model.SessionStarred;

interface Dispatcher {
	public static interface Callbacks {

		public abstract void eventDispatched(long l);

		public abstract void dispatchFinished();
	}

	public abstract void dispatchSessions(SessionStarred sessionsStarred[]);

	public abstract void init(Callbacks callbacks);

	public abstract void stop();
}
