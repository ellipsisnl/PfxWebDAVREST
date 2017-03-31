package nl.ellipsis.webdav.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.webdav.DavException;

import nl.ellipsis.pfxwebdav.rest.v1.WebDAVResourceType;

public interface WebDAVClientAPI {
	
	/**
	 * createCollection uses WebDAV:MKCOL to create a new collection resource at the location specified by the Request-URI. 
	 * If the Request-URI is already mapped to a resource, then createCollection must fail. 
	 * During createCollection processing, a server must make the Request-URI an internal member of its parent collection, unless the Request-URI is "/". 
	 * If no such ancestor exists, createCollection will create its ancestor. 
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @return WebDAVResource that has been created
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType createCollection(URI uri) throws IOException, DavException;

	/**
	 * deleteResource uses HTTP:DELETE to delete the resource identified by the Request-URI. 
	 * After a successful DELETE operation (and in the absence of other actions), a subsequent GET/HEAD/PROPFIND request to the target Request-URI must return 404 (Not Found).
	 * The deleteResource method on a collection must act as if a "Depth: infinity" header was used on it.
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @return WebDAVResource that has been deleted
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType deleteResource(URI uri) throws IOException, DavException;

	/**
	 * Use WebDAV:PROPFIND to find metadata for all childresources of parent uri.
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @return	List of WebDAVResources
	 * @throws IOException
	 * @throws DavException
	 */
	public List<WebDAVResourceType> getChildResources(URI uri) throws IOException, DavException;
	
	/**
	 * Use WebDAV:PROPFIND to find metadata for a single resource.
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @return WebDAVResource
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType getResource(URI uri) throws IOException, DavException;

	/**
	 * Use HTTP:GET to get the content of a resource as a stream.
	 * Is the resource is a collection, get the zipped content as a stream
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @return WebDAVResourceStream
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceStream getResourceStream(URI uri) throws IOException, DavException;
	
	/**
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @param inputStream	Stream to be written to uri-location on WebDAV server
	 * @param contentType
	 * 
	 * @return
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType putResource(URI uri, InputStream inputStream, String contentType) throws IOException, DavException;
	public WebDAVResourceType putResource(URI uri, File file, String contentType) throws IOException, DavException;

	/**
	 * Use WebDAV:PROPPATCH with processes instructions specified in the request body to set and/or remove properties 
	 * defined on the resource identified by the Request-URI.
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * @param properties	Map of properties
	 * @return	WebDAVResource
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType setProperties(URI uri, Map<String,Object> properties) throws IOException, DavException;
	
	/**
	 * Use WebDAV:COPY to create a duplicate of the source resource identified by the sourceUri, in the destination resource identified by the targetUri. 
	 * The exact behavior of this method depends on the type of the source resource.	
	 * 
	 * @param sourceUri	Relative URI on WebDAV server
	 * @param targetUri	Relative URI on WebDAV server
	 * @return WebDAVResource of the targetUri
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType copyResource(URI sourceUri, URI targetUri) throws IOException, DavException;
	
	/**
	 * Use WebDAV:COPY to move the source resource identified by the sourceUri to the destination resource identified by the targetUri. 
	 * The exact behavior of this method depends on the type of the source resource.	
	 * 
	 * @param sourceUri	Relative URI on WebDAV server
	 * @param targetUri	Relative URI on WebDAV server
	 * @return WebDAVResource of the targetUri
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType moveResource(URI sourceUri, URI targetUri) throws IOException, DavException;
	
	/**
	 * Use WebDAV:LOCK to take out a lock of any access type and to refresh an existing lock. 
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * 
	 * @return WebDAVResource
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType lockResource(URI uri, String lockToken, int timeout) throws IOException, DavException;
	
	/**
	 * Use WebDAV:UNLOCK to remove the lock identified by the lock token in the Lock-Token request header. 
	 * 
	 * @param uri	Relative URI on WebDAV server
	 * 
	 * @return WebDAVResource
	 * 
	 * @throws IOException
	 * @throws DavException
	 */
	public WebDAVResourceType unlockResource(URI uri, String lockToken) throws IOException, DavException;
}
