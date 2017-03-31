package nl.ellipsis.webdav.client;

public enum ApacheTestServer {
	
	AUTH_NONE("http://test.webdav.org/dav/"),
	AUTH_BASIC("http://test.webdav.org/auth-basic/"),
	AUTH_DIGEST("http://test.webdav.org/auth-digest/");
	
	public String value;
	
	private ApacheTestServer(String key) {
		this.value = key;
	}
	
	public static ApacheTestServer fromValue(String v) {
        for (ApacheTestServer c: ApacheTestServer.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
