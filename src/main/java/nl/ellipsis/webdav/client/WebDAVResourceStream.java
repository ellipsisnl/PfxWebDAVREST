package nl.ellipsis.webdav.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class WebDAVResourceStream {

	private final URI uri;
	private final CloseableHttpClient client;
	private final HttpGet httpRequest;
	private final InputStream inputStream;

	/**
	 * Constructor
	 * 
	 * @param relativeUri
	 * @param client 
	 * @param method 
	 * @param inputStream
	 */
	public WebDAVResourceStream(final URI relativeUri, CloseableHttpClient client, final HttpGet request, final InputStream inputStream) {
		this.uri = relativeUri;
		this.client = client;
		this.httpRequest = request;
		this.inputStream = inputStream;
	}

	public void close() throws IOException {
		if(inputStream!=null) {
			inputStream.close();
		}
		httpRequest.releaseConnection();
		client.close();
	}

	/**
	 * Get the InputStream for this WebDAV resource 
	 * 
	 * @return InputStream
	 */
	public InputStream getInputStream() {
		return inputStream;
	}
	
	/**
	 * Get the relative URI for this WebDAV resource 
	 * 
	 * @return URI
	 */
	public URI getURI() {
		return uri;
	}
	
	/**
	 * Get the filename or foldername for this uri
	 * 
	 * @return Filename or Foldername
	 */
	public String getFilename() {
		return (uri.getPath() != null ?  uri.getPath().substring(uri.getPath().lastIndexOf("/")) : null);
	}
}


