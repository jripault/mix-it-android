package fr.mixit.android.service;

import android.content.ContentResolver;
import android.content.res.Resources;

/**
 * Created by mathieu
 * Date: 1/30/11
 * Time: 10:00 PM
 */
public class LocalExecutor {

	private Resources mRes;
	private ContentResolver mResolver;

	public LocalExecutor(Resources res, ContentResolver resolver) {
	    mRes = res;
	    mResolver = resolver;
	}

	// TODO add execute methods for local json files
}
