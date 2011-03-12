package fr.mixit.android.service;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.BasicHttpParams;

class PipelinedRequester {
	static interface Callbacks {

		public abstract void pipelineModeChanged(boolean flag);

		public abstract void serverError(int i);

		public abstract void requestSent();
	}

	DefaultHttpClientConnection connection;
	Callbacks callbacks;
	int lastStatusCode;
	boolean canPipeline;
	HttpHost host;
	SocketFactory socketFactory;

	public PipelinedRequester(HttpHost httphost) {
		this(httphost, ((SocketFactory) (new PlainSocketFactory())));
	}

	public PipelinedRequester(HttpHost httphost, SocketFactory socketfactory) {
		connection = new DefaultHttpClientConnection();
		canPipeline = true;
		host = httphost;
		socketFactory = socketfactory;
	}

	public void installCallbacks(Callbacks callbacks1) {
		callbacks = callbacks1;
	}

	public void addRequest(HttpRequest httprequest) throws HttpException,
			IOException {
		maybeOpenConnection();
		connection.sendRequestHeader(httprequest);
	}

	public void sendRequests() throws IOException, HttpException {
		connection.flush();
		for (HttpConnectionMetrics httpconnectionmetrics = connection
				.getMetrics(); httpconnectionmetrics.getResponseCount() < httpconnectionmetrics
				.getRequestCount();) {
			HttpResponse httpresponse = connection.receiveResponseHeader();
			if (!httpresponse.getStatusLine().getProtocolVersion()
					.greaterEquals(HttpVersion.HTTP_1_1)) {
				callbacks.pipelineModeChanged(false);
				canPipeline = false;
			}
			Header aheader[] = httpresponse.getHeaders("Connection");
			if (aheader != null) {
				Header aheader1[] = aheader;
				int i = aheader1.length;
				for (int j = 0; j < i; j++) {
					Header header = aheader1[j];
					if ("close".equalsIgnoreCase(header.getValue())) {
						callbacks.pipelineModeChanged(false);
						canPipeline = false;
					}
				}

			}
			lastStatusCode = httpresponse.getStatusLine().getStatusCode();
			if (lastStatusCode != 200) {
				callbacks.serverError(lastStatusCode);
				closeConnection();
				return;
			}
			connection.receiveResponseEntity(httpresponse);
			httpresponse.getEntity().consumeContent();
			callbacks.requestSent();
			if (!canPipeline) {
				closeConnection();
				return;
			}
		}

	}

	public void finishedCurrentRequests() {
		closeConnection();
	}

	private void maybeOpenConnection() throws IOException {
		if (connection == null || !connection.isOpen()) {
			BasicHttpParams basichttpparams = new BasicHttpParams();
			java.net.Socket socket = socketFactory.createSocket();
			socket = socketFactory.connectSocket(socket, host.getHostName(),
					host.getPort(), null, 0, basichttpparams);
			connection.bind(socket, basichttpparams);
		}
	}

	private void closeConnection() {
		if (connection != null && connection.isOpen()) {
			try {
				connection.close();
			} catch (IOException ioexception) {
			}
		}
	}
}
