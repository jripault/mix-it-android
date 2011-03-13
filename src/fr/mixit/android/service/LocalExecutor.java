package fr.mixit.android.service;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import fr.mixit.android.io.JSONHandler;
import fr.mixit.android.model.XmlHandler;
import fr.mixit.android.utils.Lists;
import fr.mixit.android.utils.ParserUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

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

	public void execute(int resId, XmlHandler handler) throws XmlHandler.XmlHandlerException {
	    final XmlResourceParser parser = mRes.getXml(resId);
	    try {
		    handler.setLocalSync(true);
	        handler.parseAndApply(parser, mResolver);
	    } finally {
	        parser.close();
	    }
	}

	public void execute(Context context, String assetName, XmlHandler handler) throws XmlHandler.XmlHandlerException {
		try {
			final InputStream input = context.getAssets().open(assetName);
			final XmlPullParser parser = ParserUtils.newPullParser(input);
		    handler.setLocalSync(true);
			handler.parseAndApply(parser, mResolver);
		} catch (XmlHandler.XmlHandlerException e) {
			throw e;
		} catch (XmlPullParserException e) {
			throw new XmlHandler.XmlHandlerException("Problem parsing local asset: " + assetName, e);
		} catch (IOException e) {
			throw new XmlHandler.XmlHandlerException("Problem parsing local asset: " + assetName, e);
		}
	}

	public void execute(Context context, String assetName, JSONHandler handler)
	        throws JSONHandler.JSONHandlerException {
	    try {
	        final InputStream input = context.getAssets().open(assetName);
	        byte [] buffer = new byte[input.available()];
	        while (input.read(buffer) != -1);
	        String jsontext = new String(buffer);
	        ArrayList<JSONArray> entries = Lists.newArrayList();
	        entries.add(new JSONArray(jsontext));
		    handler.setLocalSync(true);
	        handler.parseAndApply(entries, mResolver);
	    } catch (JSONHandler.JSONHandlerException e) {
	        throw e;
	    } catch (JSONException e) {
	        throw new JSONHandler.JSONHandlerException("Problem parsing local asset: " + assetName, e);
	    } catch (IOException e) {
	        throw new JSONHandler.JSONHandlerException("Problem parsing local asset: " + assetName, e);
	    }
	}

}
