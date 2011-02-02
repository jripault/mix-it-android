package fr.mixit.android.model;

public class RequestHash {
	
	private final String url;
	private final String md5;

	public RequestHash(String url, String md5) {
		this.url = url;
		this.md5 = md5;
	}

	public String getUrl() {
		return url;
	}

	public String getMd5() {
		return md5;
	}

}
