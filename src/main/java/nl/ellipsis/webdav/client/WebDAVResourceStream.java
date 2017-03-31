package nl.ellipsis.webdav.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;

public class WebDAVResourceStream {

	private final URI uri;
	private HttpMethodBase httpMethod;
	private final InputStream inputStream;

	/**
	 * Constructor
	 * 
	 * @param relativeUri
	 * @param method 
	 * @param inputStream
	 */
	public WebDAVResourceStream(final URI relativeUri, final HttpMethodBase method, final InputStream inputStream) {
		this.uri = relativeUri;
		this.httpMethod = method;
		this.inputStream = inputStream;
	}

	public void close() throws IOException {
		if(inputStream!=null) {
			inputStream.close();
		}
		httpMethod.releaseConnection();
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


