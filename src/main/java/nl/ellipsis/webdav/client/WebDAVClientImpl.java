package nl.ellipsis.webdav.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.params.HttpConnectionParams;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.BaseDavRequest;
import org.apache.jackrabbit.webdav.client.methods.HttpCopy;
import org.apache.jackrabbit.webdav.client.methods.HttpDelete;
import org.apache.jackrabbit.webdav.client.methods.HttpLock;
import org.apache.jackrabbit.webdav.client.methods.HttpMkcol;
import org.apache.jackrabbit.webdav.client.methods.HttpMove;
import org.apache.jackrabbit.webdav.client.methods.HttpPropfind;
import org.apache.jackrabbit.webdav.client.methods.HttpProppatch;
import org.apache.jackrabbit.webdav.client.methods.HttpUnlock;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyIterator;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.apache.log4j.Logger;

import nl.ellipsis.pfxwebdav.rest.v1.WebDAVPropertyType;
import nl.ellipsis.pfxwebdav.rest.v1.WebDAVResourceType;
import nl.ellipsis.webdav.rest.PfxDocumentConstants;
import nl.ellipsis.webdav.rest.PfxDocumentConstants.DavStatusCode;

public class WebDAVClientImpl implements WebDAVClientAPI {

	protected static Logger logger = Logger.getLogger(WebDAVClientImpl.class); // Ellipsis

	private final static int MAX_HOST_CONNECTIONS = 200;

	// private HttpClient client;
	private String user;
	private String password;
	private URI serverUri;

	/**
	 * Constructor
	 * 
	 * @param user
	 * @param password
	 * @throws Exception 
	 */
	public WebDAVClientImpl(String user, String password, URI serverUri) throws DavException {
		if(serverUri==null) {
			throw new DavException(HttpStatus.SC_INTERNAL_SERVER_ERROR,"serverUri is mandatory");
		}
		this.user = user;
		this.password = password;
		this.serverUri = serverUri;
	}
	
	public WebDAVResourceType copyResource(URI sourceUri, URI targetUri, boolean overwrite, boolean shallow) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(sourceUri!=null && targetUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,sourceUri);
			try {
				// continue with copy
				HttpCopy httpMethod = new HttpCopy(getAbsoluteURI(sourceUri).toString(),getAbsoluteURI(targetUri).toString(),overwrite,shallow);
				CloseableHttpResponse response = client.execute(httpMethod);
				try {
					// get resource, error is thrown if it doesn't exist
					resource = getResourceProperties(client,targetUri);
				} finally {
					response.close();
				}
			} finally {
				client.close();
			}
		}
		return resource;
	}

	public WebDAVResourceType moveResource(URI sourceUri, URI targetUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(sourceUri!=null && targetUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,sourceUri);
			try {
				// continue with move
				HttpMove httpMethod = new HttpMove(getAbsoluteURI(sourceUri).toString(),getAbsoluteURI(targetUri).toString(),true);
				CloseableHttpResponse response = client.execute(httpMethod);
				try {
					// get resource, error is thrown if it doesn't exist
					resource = getResourceProperties(client,targetUri);
				} finally {
					response.close();
				}
			} finally {
				client.close();
			}
		}
		return resource;
	}


	public WebDAVResourceType createCollection(URI relativeUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				// let's just walk to the ancestors uri's
				String[] parts = relativeUri.toString().split(PfxDocumentConstants.URI_SEPARATOR);
				String ancestorUri = "";
				for(int i=0; i<parts.length; i++) {
					String part = parts[i];
					if(!StringUtils.isEmpty(part)) {
						ancestorUri = ancestorUri + PfxDocumentConstants.URI_SEPARATOR + part;
						try {
							resource = getCollection(client,new URI(ancestorUri),true);
						} catch (URISyntaxException e) {
							throw new DavException(HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
						}
					}
				}
			} finally {
				client.close();
			}
		}
		return resource;
	}

	public WebDAVResourceType deleteResource(URI relativeUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				// get resource, error is thrown if it doesn't exist
				resource = getResourceProperties(client,relativeUri);
				// continue with deletion
				HttpDelete httpMethod = new HttpDelete(getAbsoluteURI(relativeUri).toString());
				CloseableHttpResponse response = client.execute(httpMethod);
				response.close();
			} finally {
				client.close();
			}
		}
		return resource;
	}

	/**
	 * Determines all level 1 resources of a certain webdav resource, referred
	 * by the given uri; this is a method for a folder-like listing
	 * 
	 * @return
	 * @throws DavException 
	 * @throws IOException 
	 */
	public List<WebDAVResourceType> getChildResources(URI relativeUri) throws IOException, DavException  {
		List<WebDAVResourceType> resources = new ArrayList<WebDAVResourceType>();
		if(relativeUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				resources = getCollectionProperties(client,relativeUri,DavConstants.DEPTH_1);
			} finally {
				client.close();
			}
		}
		return resources;
	}

	public WebDAVResourceType getResource(URI relativeUri) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				resource = getResourceProperties(client,relativeUri);
			} finally {
				client.close();
			}
		}
		return resource;
	}

	/**
	 * A method to retrieve a WebDAV resource
	 * 
	 * @param uri
	 * 
	 * @return WebDAVResource
	 * @throws Exception 
	 */
	public WebDAVResourceStream getResourceStream(URI relativeUri) throws IOException, DavException {
		WebDAVResourceStream resourceStream = null;
		CloseableHttpClient client = getHttpClient(user,password,relativeUri);
		HttpGet httpMethod = new HttpGet(getAbsoluteURI(relativeUri).toString());
		CloseableHttpResponse response = client.execute(httpMethod);
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			resourceStream = new WebDAVResourceStream(relativeUri,client,httpMethod,stream);
		}
		return resourceStream;
	}

	public WebDAVResourceType lockResource(URI uri, String lockToken, int timeout) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(uri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,uri);
			try {
				// continue with move
				HttpLock httpMethod = new HttpLock(getAbsoluteURI(uri).toString(),timeout,new String[]{lockToken});
				CloseableHttpResponse response = client.execute(httpMethod);
				try {
					// get resource, error is thrown if it doesn't exist
					resource = getResourceProperties(client,uri);
				} finally {
					response.close();
				}
			} finally {
				client.close();
			}
		}
		return resource;
	}


	public WebDAVResourceType setProperties(URI relativeUri, Map<String,Object> propertyMap) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null && propertyMap!=null &&!propertyMap.isEmpty()) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				HttpProppatch httpMethod = new HttpProppatch(getAbsoluteURI(relativeUri).toString(),getWebDAVPropertyList(propertyMap));
				CloseableHttpResponse response = client.execute(httpMethod);
				response.close();
				resource = getResourceProperties(client,relativeUri);
			} finally {
				client.close();
			}
		}
    	return resource;
	}

	/**
	 * InputStreamEntity is NOT repeatable
	 */
	public WebDAVResourceType putResource(URI relativeUri, InputStream inputStream, long length, ContentType contentType) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				HttpPut httpMethod = new HttpPut(getAbsoluteURI(relativeUri).toString());
				InputStreamEntity entity = new InputStreamEntity(inputStream, length, contentType);
				entity.setChunked(true);
				httpMethod.setEntity(entity);
				CloseableHttpResponse response = client.execute(httpMethod);
				response.close();
				resource = getResourceProperties(client,relativeUri);
			} finally {
				client.close();
			}
		}
    	return resource;
	}

	/**
	 * FileEntity is repeatable
	 * 
	 */
	public WebDAVResourceType putResource(URI relativeUri, File file, ContentType contentType) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,relativeUri);
			try {
				HttpPut httpMethod = new HttpPut(getAbsoluteURI(relativeUri).toString());
				
				// RequestEntity requestEntity = new InputStreamRequestEntity(inputStream, contentType); 
				FileEntity requestEntity = new FileEntity(file, contentType); 
				BufferedHttpEntity bhe = new BufferedHttpEntity(requestEntity); 
				httpMethod.setEntity(bhe);
				
				CloseableHttpResponse response = client.execute(httpMethod);
				response.close();
				resource = getResourceProperties(client,relativeUri);
			} finally {
				client.close();
			}
		}
    	return resource;
	}

	public WebDAVResourceType unlockResource(URI uri, String lockToken) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(uri!=null) {
			CloseableHttpClient client = getHttpClient(user,password,uri);
			try {
				// continue with move
				HttpUnlock httpMethod = new HttpUnlock(getAbsoluteURI(uri).toString(),lockToken);
				CloseableHttpResponse response = client.execute(httpMethod);
				try {
					// get resource, error is thrown if it doesn't exist
					resource = getResourceProperties(client,uri);
				} finally {
					response.close();
				}
			} finally {
				client.close();
			}
		}
		return resource;
	}

	/**
	 * 
	 * @param relativeUri	path relative to mountpoint of server
	 * @param resourcePath	relative path = absolute path minus domain 
	 * @return				true when equals
	 * @throws DavException
	 */
	private boolean assertResourcePathEquals(URI relativeUri, String resourcePath) throws DavException {
		URI refResourcePath = getResourcePath(relativeUri);
		return refResourcePath.toString().equals(resourcePath);
	}

	/**
	 * Return full uri for resourcePath (which is the path-part of the full uri)
	 * 
	 * @param resourcePath	
	 * @return uri
	 * @throws DavException
	 */
	private URI getAbsoluteURI(String resourcePath) throws DavException {
		if(resourcePath!=null) {
			int p = serverUri.getPath().length();
			int eindIndex = serverUri.toString().length() - p;
			try {
				return new URI(serverUri.toString().substring(0, eindIndex)+resourcePath);
			} catch (URISyntaxException e) {
				throw new DavException(HttpStatus.SC_BAD_REQUEST, e);
			}
		}
		return null;
	}

	/**
	 * Return full uri for relativeUri (URI relative to mountpoint of webdav server)
	 * 
	 * @param relativeUri 	
	 * @return uri
	 * @throws DavException
	 */
	private URI getAbsoluteURI(URI relativeUri) throws DavException {
		if(relativeUri == null) {
			throw new DavException(HttpStatus.SC_BAD_REQUEST, "relativeUri is mandatory");
		} else if(relativeUri.isAbsolute()) {
			throw new DavException(DavStatusCode.UNPROCESSABLE_ENTITY, "Only relative paths are supported");
		}
		String serverPath = serverUri.toString();
		String relativePath = relativeUri.toString();
		if(serverPath.endsWith(PfxDocumentConstants.URI_SEPARATOR) && relativePath.startsWith(PfxDocumentConstants.URI_SEPARATOR)) {
			relativePath = relativePath.substring(1);
		}
		try {
			return new URI(serverPath+relativePath);
		} catch (URISyntaxException e) {
			throw new DavException(HttpStatus.SC_BAD_REQUEST, e);
		}
	}

	private WebDAVResourceType getCollection(CloseableHttpClient client, URI relativeUri, boolean createIfNotExists) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(relativeUri != null) {
			// first check if collection already exists
			try {
				resource = getResource(relativeUri);
			} catch(DavException e) {
				// Ignore 404 not found only when createIfNotExists
				if(!(HttpStatus.SC_NOT_FOUND == e.getErrorCode() && createIfNotExists)){
					throw e;
				}
			}
			if(resource==null && createIfNotExists) {
				BaseDavRequest method = new HttpMkcol(getAbsoluteURI(relativeUri).toString());
				CloseableHttpResponse response = client.execute(method);
				try {
					// then propfind if no error has been thrown
					resource = getResource(relativeUri);
					logger.info("collection "+relativeUri.toString() + " has been created");
				} finally {
					response.close();
				}
			}
		}		
		return resource;
	}

	/**
	 * Determines all level 1 resources of a certain webdav resource, referred
	 * by the given uri; this is a method for a folder-like listing
	 * 
	 * @return
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws DavException 
	 * @throws Exception 
	 */
	private List<WebDAVResourceType> getCollectionProperties(CloseableHttpClient client, URI relativeUri, int depth) throws IOException, DavException {
		List<WebDAVResourceType> resources = new ArrayList<WebDAVResourceType>();
	
		HttpPropfind method = new HttpPropfind(getAbsoluteURI(relativeUri).toString(), DavConstants.PROPFIND_ALL_PROP, depth);
		CloseableHttpResponse httpResponse = client.execute(method);
		try {
			MultiStatus multiStatus = method.getResponseBodyAsMultiStatus(httpResponse);
			MultiStatusResponse[] responses = multiStatus.getResponses();
			MultiStatusResponse response;
		
			int status = HttpStatus.SC_OK; 
			for (int i = 0; i < responses.length; i++) {
				response = responses[i];
				
				// href = mountpoint from documentService + relativePath
				String href = response.getHref();
		
				// Ignore the current directory
				if (assertResourcePathEquals(relativeUri,href) && depth > 0) {
					continue;
				}
		
				// prepare json container
				WebDAVResourceType resource = getWebDAVResource(href,response.getProperties(status));
				resources.add(resource);
				Collections.sort(resources, new Comparator<WebDAVResourceType>() {
		            public int compare(WebDAVResourceType lhs, WebDAVResourceType rhs) {
		                return lhs.getDisplayName().compareToIgnoreCase(rhs.getDisplayName());
		            }
		        });
			}			
		} finally {
			httpResponse.close();
		}
		return resources;
	}

	private CloseableHttpClient getHttpClient(String user, String password, URI relativeUri) throws DavException {
		URI serverUri = getAbsoluteURI(relativeUri);
		CloseableHttpClient client = null;
		
		if(user!=null) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(
	                new AuthScope(serverUri.getHost(), serverUri.getPort()),
	                new UsernamePasswordCredentials(user, password));
			client = HttpClients.custom()
	                .setDefaultCredentialsProvider(credsProvider)
	                .build();
		} else {
			client = HttpClients.custom().build();
		}
		return client;
	}
	
	/**
	 * Get the path-URI for relativeUri (which is relative to mountpoint of webdav server)
	 * 
	 * @param relativeUri
	 * @return path
	 * 
	 * @throws DavException
	 */
	private URI getResourcePath(URI relativeUri) throws DavException {
		if(relativeUri == null) {
			throw new DavException(DavStatusCode.UNPROCESSABLE_ENTITY, "Relative URI is mandatory");
		}
		if(relativeUri.isAbsolute()) {
			throw new DavException(DavStatusCode.UNPROCESSABLE_ENTITY, "Only relative paths are supported");
		}
		try {
			String serverPath = serverUri.getPath();
			String relativePath = relativeUri.toString();
			if(serverPath.endsWith(PfxDocumentConstants.URI_SEPARATOR) && relativePath.startsWith(PfxDocumentConstants.URI_SEPARATOR)) {
				relativePath = relativePath.substring(1);
			}
			return new URI(serverPath+relativePath);
		} catch (URISyntaxException e) {
			throw new DavException(HttpStatus.SC_BAD_REQUEST, e);
		}
	}

	private WebDAVResourceType getResourceProperties(CloseableHttpClient client, URI relativeUri) throws IOException,DavException {
		// let's do a generic propfind with depth 0
		List<WebDAVResourceType> resources = getCollectionProperties(client,relativeUri,DavConstants.DEPTH_0);
		// we get either a response with one resource-item or a NOT-FOUND exception
		if(resources.size()>1) {
			throw new DavException(HttpStatus.SC_INTERNAL_SERVER_ERROR,"More than one resource found where one expected");
		} else if(resources.isEmpty()) {
			// should not happen, NOT-FOUND should throw an exception
			throw new DavException(HttpStatus.SC_NOT_IMPLEMENTED,"getResource empty result");
		}
		return resources.get(0);
	
	}

	@SuppressWarnings("rawtypes")
	private List<DefaultDavProperty> getWebDAVPropertyList(Map<String, Object> propertyMap) {
		List<DefaultDavProperty> retval = new ArrayList<DefaultDavProperty>();
		if(propertyMap!=null &&!propertyMap.isEmpty()) {
			for(Entry<String, Object> e : propertyMap.entrySet()) {
				@SuppressWarnings("unchecked")
				DefaultDavProperty pe = new DefaultDavProperty(e.getKey(),e.getValue(),Namespace.EMPTY_NAMESPACE);
				retval.add(pe);
			}
		}
		return retval;
	}

	private WebDAVResourceType getWebDAVResource(String href, DavPropertySet davProperties) throws DavException {
		WebDAVResourceType webdavResource = new WebDAVResourceType();

		webdavResource.setUri(getAbsoluteURI(href).toString());
		webdavResource.setHref(href);

		List<String> mappedProperties = new ArrayList<String>();
		
		mappedProperties.add(org.apache.jackrabbit.webdav.DavConstants.PROPERTY_RESOURCETYPE);
		DavProperty<?> davResourceType = davProperties.get(org.apache.jackrabbit.webdav.DavConstants.PROPERTY_RESOURCETYPE);
		String resourceType = ((davResourceType != null) && (davResourceType.getValue() != null) ? davResourceType.getValue().toString() : null);
		mappedProperties.add(org.apache.jackrabbit.webdav.DavConstants.XML_COLLECTION);
		boolean isDirectory = ((resourceType != null) && (resourceType.indexOf(org.apache.jackrabbit.webdav.DavConstants.XML_COLLECTION) != -1) ? true : false);
		webdavResource.setCollection(isDirectory);
		// webdavResource.setResourceType(resourceType);

		mappedProperties.add(org.apache.jackrabbit.webdav.DavConstants.PROPERTY_DISPLAYNAME);
		DavProperty<?> davDisplayName = davProperties.get(org.apache.jackrabbit.webdav.DavConstants.PROPERTY_DISPLAYNAME);
		String displayName = ((davDisplayName != null) && (davDisplayName.getValue() != null) ? davDisplayName.getValue().toString() : getFilename(href));
		webdavResource.setDisplayName(displayName);

		List<WebDAVPropertyType> properties = webdavResource.getProperties();
		
		DavPropertyIterator it = davProperties.iterator();
		while(it.hasNext()) {
			DavProperty<?> property = it.next();
			if(property.getValue()!=null) {
				String name = property.getName().getName();
				if(!mappedProperties.contains(name)) {
					String value = property.getValue().toString();
					WebDAVPropertyType p = new WebDAVPropertyType();
					p.setName(name);
					p.setValue(value);
					properties.add(p);
				}
				
//				joContainer.addProperty(property.getName().getName(), property.getValue().toString());
			}
		}
		return webdavResource;
	}
	
	private WebDAVResourceType getWebDAVResource(List<WebDAVResourceType> folderResources, URI relativeUri) throws DavException  {
		WebDAVResourceType resource = null;
		if(folderResources!=null && !folderResources.isEmpty() && relativeUri!=null) {
			for(WebDAVResourceType folderResource : folderResources) {
				if(assertResourcePathEquals(relativeUri,folderResource.getHref())){
					resource = folderResource;
					break;
				}
			}
		}
		return resource;
	}

	private boolean isMemberOf(List<WebDAVResourceType> folderResources, URI relativeUri) throws DavException {
		return (getWebDAVResource(folderResources,relativeUri) != null);
	}

	@SuppressWarnings("unused")
	private boolean isRootURI(URI relativeUri) {
		return serverUri.getPath().equals(relativeUri.toString());
	}

	private static String getFilename(String relativePath) {
		String filename = (relativePath.endsWith(PfxDocumentConstants.URI_SEPARATOR) ? relativePath.substring(0, relativePath.length() - 1) : relativePath);
		return (filename.lastIndexOf(PfxDocumentConstants.URI_SEPARATOR) >= 0 ? filename.substring(filename.lastIndexOf(PfxDocumentConstants.URI_SEPARATOR)+1) : filename);
	}

	private static URI getParentURI(URI relativeUri) throws DavException {
		URI parentUri = null;
		if(relativeUri != null) {
			String resourcePath = relativeUri.toString();
			int index = resourcePath.lastIndexOf(PfxDocumentConstants.URI_SEPARATOR);
			try {
				parentUri =  (index > 0 ? new URI(resourcePath.substring(0, index)) : new URI(PfxDocumentConstants.URI_SEPARATOR));
			} catch (URISyntaxException e) {
				throw new DavException(HttpStatus.SC_BAD_REQUEST, e);
			} 
		}
		return parentUri;
	}

}