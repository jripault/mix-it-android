/*
 * Copyright 2010 Peter Kuterna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.mixit.android.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.format.DateUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;


public class SyncUtils {

    private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;
    
    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";
    
    private static final String BASE_MD5_URL = "http://devoxx2010.appspot.com/requestmd5key?requestUri=";

    /**
     * Generate and return a {@link org.apache.http.client.HttpClient} configured for general use,
     * including setting an application-specific user-agent string.
     */
    public static HttpClient getHttpClient(Context context) {
        final HttpParams params = new BasicHttpParams();

        // Use generous timeouts for slow mobile networks
        HttpConnectionParams.setConnectionTimeout(params, 20 * SECOND_IN_MILLIS);
        HttpConnectionParams.setSoTimeout(params, 20 * SECOND_IN_MILLIS);

        HttpConnectionParams.setSocketBufferSize(params, 8192);
//        HttpProtocolParams.setUserAgent(params, buildUserAgent(context));

        final SchemeRegistry schemeReg = new SchemeRegistry();
        schemeReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(params, schemeReg);
        final DefaultHttpClient client = new DefaultHttpClient(connectionManager, params);

/*        client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(HttpRequest request, HttpContext context) {
                // Add header to accept gzip content
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
            }
        });

        client.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(HttpResponse response, HttpContext context) {
                // Inflate any responses compressed with gzip
                final HttpEntity entity = response.getEntity();
                final Header encoding = entity.getContentEncoding();
                if (encoding != null) {
                    for (HeaderElement element : encoding.getElements()) {
                        if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
                            response.setEntity(new InflatingEntity(response.getEntity()));
                            break;
                        }
                    }
                }
            }
        });*/

        return client;
    }

/*    public static String getLocalMd5(ContentResolver resolver, String url) {
    	final String syncId = Sync.generateSyncId(url);
    	final Uri uri = Sync.buildSyncUri(syncId);
    	Cursor cursor = resolver.query(uri, SyncQuery.PROJECTION, null, null, null);
    	try {
    		if (!cursor.moveToFirst()) return "";
    		return cursor.getString(SyncQuery.MD5);
    	} finally {
    		cursor.close();
    	}
    }

    public static String getRemoteMd5(HttpClient httpClient, String url) {
    	try {
	    	final String requestMd5KeyUrl = BASE_MD5_URL + url;
	        final HttpUriRequest request = new HttpGet(requestMd5KeyUrl);
	        final HttpResponse resp = httpClient.execute(request);
	        final int status = resp.getStatusLine().getStatusCode();
	        if (status != HttpStatus.SC_OK) {
	        	return null;
	        }

	        final InputStream input = resp.getEntity().getContent();

	    	try {
	        	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
	        	StringBuilder sb = new StringBuilder();
	        	String line;
	        	while ((line = reader.readLine()) != null) {
	        		sb.append(line);
	        	}
	            String md5 = sb.toString().trim();
	            if (md5.length() > 0 && !"NOK".equals(md5)) {
	            	return md5;
	            }
	    	} finally {
	            if (input != null) input.close();
	    	}
    	} catch (ClientProtocolException e) {
    		return null;
    	} catch (IOException e) {
    		return null;
        }

    	return null;
    }

    public static void updateLocalMd5(ContentResolver resolver, String url, String md5) {
        final String syncId = Sync.generateSyncId(url);
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Sync.URI_ID, syncId);
        contentValues.put(Sync.URI, url);
        contentValues.put(Sync.MD5, md5);
        resolver.insert(Sync.CONTENT_URI, contentValues);
    } */

    /**
     * Build and return a user-agent string that can identify this application
     * to remote servers. Contains the package name and version code.
     */
/*    private static String buildUserAgent(Context context) {
        try {
            final PackageManager manager = context.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);

            // Some APIs require "(gzip)" in the user-agent string.
            return info.packageName + "/" + info.versionName
                    + " (" + info.versionCode + ") (gzip)";
        } catch (NameNotFoundException e) {
            return null;
        }
    }*/

    /**
     * Simple {@link org.apache.http.entity.HttpEntityWrapper} that inflates the wrapped
     * {@link org.apache.http.HttpEntity} by passing it through {@link java.util.zip.GZIPInputStream}.
     */
/*    private static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }*/
    
    /** {@link Sync} query parameters */
/*    private interface SyncQuery {
    	String [] PROJECTION = {
    			Sync.MD5,
    	};
    	
    	int MD5 = 0;
    }*/
    
}
