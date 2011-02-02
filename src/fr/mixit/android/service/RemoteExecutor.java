package fr.mixit.android.service;

import android.content.ContentResolver;
import com.sun.tools.internal.ws.processor.model.Request;
import fr.mixit.android.model.RequestHash;
import fr.mixit.android.utils.Lists;
import fr.mixit.android.utils.SyncUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import fr.mixit.android.io.JSONHandler;
import fr.mixit.android.io.JSONHandler.*;

/**
 * Created by mathieu
 * Date: 1/30/11
 * Time: 10:00 PM
 */
public class RemoteExecutor {

	private final HttpClient mHttpClient;
	private final ContentResolver mResolver;

	public RemoteExecutor(HttpClient httpClient, ContentResolver resolver) {
	    mHttpClient = httpClient;
	    mResolver = resolver;
	}

	/**
	 * Execute a {@link org.apache.http.client.methods.HttpGet} request, passing a valid response through
	 * {@link JSONHandler//#parseAndApply(org.json.JSONArray, ContentResolver)}.
	 */
	public /*ArrayList<*/RequestHash/*>*/ executeGet(String /*[]*/ url/*s*/, JSONHandler handler) throws JSONHandlerException {
//		final ArrayList<RequestHash> result = Lists.newArrayList();
		final ArrayList<JSONArray> entries = Lists.newArrayList();
//		for (String url : urls) {
	        final HttpUriRequest request = new HttpGet(url);
	        final String md5 = "";//SyncUtils.getRemoteMd5(mHttpClient, url);
	        JSONArray requestEntries = executeRequest(request);
	        entries.add(requestEntries);
//	        result.add(new RequestHash(url, md5));
//		}
		handler.setLocalSync(false);
		handler.parseAndApply(entries, mResolver);
		return new RequestHash(url, md5);
//		return result;
	}

	/**
	 * Execute this {@link HttpUriRequest}, passing a valid response through
	 * {@link JSONHandler//#parseAndApply(JSONArray, ContentResolver)}.
	 */
	public JSONArray executeRequest(HttpUriRequest request) throws JSONHandlerException {
	    try {
		    request.setHeader("Accept", "application/json");
		    request.setHeader("Content-type", "application/json");
		    final HttpResponse resp = mHttpClient.execute(request);
	        final int status = resp.getStatusLine().getStatusCode();
	        if (status != HttpStatus.SC_OK) {
	            throw new JSONHandlerException("Unexpected server response " + resp.getStatusLine()
	                    + " for " + request.getRequestLine());
	        }

	        final InputStream input = resp.getEntity().getContent();
	        try {
		        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		        StringBuilder sb = new StringBuilder();
		        String line;
		        while ((line = reader.readLine()) != null) {
			        sb.append(line);
		        }
	            String jsontext = sb.toString();
	            return new JSONArray(jsontext);
	        } catch (JSONException e) {
	            throw new JSONHandlerException("Malformed response for " + request.getRequestLine(), e);
	        } finally {
	            if (input != null) input.close();
	        }
	    } catch (JSONHandlerException e) {
	        throw e;
	    } catch (IOException e) {
	        throw new JSONHandlerException("Problem reading remote response for "
	                + request.getRequestLine(), e);
	    }
	}


}
