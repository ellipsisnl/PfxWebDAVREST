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
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.CopyMethod;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.LockMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.client.methods.UnLockMethod;
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
	
	public WebDAVResourceType copyResource(URI sourceUri, URI targetUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(sourceUri!=null && targetUri!=null) {
			HttpClient client = getHttpClient(user,password,sourceUri);
			// continue with copy
			CopyMethod httpMethod = new CopyMethod(getAbsoluteURI(sourceUri).toString(),getAbsoluteURI(targetUri).toString(),true);
			client.executeMethod(httpMethod);
			// get resource, error is thrown if it doesn't exist
			resource = getResourceProperties(client,targetUri);
			httpMethod.releaseConnection();
		}
		return resource;
	}

	public WebDAVResourceType moveResource(URI sourceUri, URI targetUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(sourceUri!=null && targetUri!=null) {
			HttpClient client = getHttpClient(user,password,sourceUri);
			// continue with move
			MoveMethod httpMethod = new MoveMethod(getAbsoluteURI(sourceUri).toString(),getAbsoluteURI(targetUri).toString(),true);
			client.executeMethod(httpMethod);
			// get resource, error is thrown if it doesn't exist
			resource = getResourceProperties(client,targetUri);
			httpMethod.releaseConnection();
		}
		return resource;
	}


	public WebDAVResourceType createCollection(URI relativeUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			HttpClient client = getHttpClient(user,password,relativeUri);
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
		}
		return resource;
	}

	public WebDAVResourceType deleteResource(URI relativeUri) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			HttpClient client = getHttpClient(user,password,relativeUri);
			// get resource, error is thrown if it doesn't exist
			resource = getResourceProperties(client,relativeUri);
			// continue with deletion
			DeleteMethod httpMethod = new DeleteMethod(getAbsoluteURI(relativeUri).toString());
			client.executeMethod(httpMethod);
			httpMethod.releaseConnection();
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
		HttpClient client = getHttpClient(user,password,relativeUri);
		return getCollectionProperties(client,relativeUri,DavConstants.DEPTH_1);
	}

	public WebDAVResourceType getResource(URI relativeUri) throws IOException, DavException  {
		HttpClient client = getHttpClient(user,password,relativeUri);
		return getResourceProperties(client,relativeUri);
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
		WebDAVResourceStream resource = null;
		HttpClient client = getHttpClient(user,password,relativeUri);
		GetMethod method = new GetMethod(getAbsoluteURI(relativeUri).toString());
		client.executeMethod(method);
		if (method.getStatusCode() == HttpStatus.SC_OK) {
			InputStream stream = method.getResponseBodyAsStream();
			resource = new WebDAVResourceStream(relativeUri,method,stream);
		}
		return resource;
	}

	public WebDAVResourceType lockResource(URI uri, String lockToken, int timeout) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(uri!=null) {
			HttpClient client = getHttpClient(user,password,uri);
			// continue with move
			LockMethod httpMethod = new LockMethod(getAbsoluteURI(uri).toString(), timeout, new String[]{lockToken});
			client.executeMethod(httpMethod);
			// get resource, error is thrown if it doesn't exist
			resource = getResourceProperties(client,uri);
			httpMethod.releaseConnection();
		}
		return resource;
	}


	public WebDAVResourceType setProperties(URI relativeUri, Map<String,Object> propertyMap) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null && propertyMap!=null &&!propertyMap.isEmpty()) {
			HttpClient client = getHttpClient(user,password,relativeUri);
		
			
			PropPatchMethod httpMethod = new PropPatchMethod(getAbsoluteURI(relativeUri).toString(),getWebDAVPropertyList(propertyMap));
		
			client.executeMethod(httpMethod);
			httpMethod.releaseConnection();
			
			resource = getResource(relativeUri);
		}
    	return resource;
	}

	public WebDAVResourceType putResource(URI relativeUri, InputStream inputStream, String contentType) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			HttpClient client = getHttpClient(user,password,relativeUri);
		
			PutMethod httpMethod = new PutMethod(getAbsoluteURI(relativeUri).toString());
		
			// RequestEntity requestEntity = new InputStreamRequestEntity(inputStream, contentType); 
			InputStreamRequestEntity requestEntity = new InputStreamRequestEntity(inputStream, contentType); 
			httpMethod.setRequestEntity(requestEntity);
			client.executeMethod(httpMethod);
			httpMethod.releaseConnection();
			
			resource = getResource(relativeUri);
		}
    	return resource;
	}

	public WebDAVResourceType putResource(URI relativeUri, File file, String contentType) throws IOException, DavException  {
		WebDAVResourceType resource = null;
		if(relativeUri!=null) {
			HttpClient client = getHttpClient(user,password,relativeUri);
		
			PutMethod httpMethod = new PutMethod(getAbsoluteURI(relativeUri).toString());
		
			// RequestEntity requestEntity = new InputStreamRequestEntity(inputStream, contentType); 
			RequestEntity requestEntity = new FileRequestEntity(file, contentType); 
			httpMethod.setRequestEntity(requestEntity);
			client.executeMethod(httpMethod);
			httpMethod.releaseConnection();
			
			resource = getResource(relativeUri);
		}
    	return resource;
	}

	public WebDAVResourceType unlockResource(URI uri, String lockToken) throws IOException, DavException {
		WebDAVResourceType resource = null;
		if(uri!=null) {
			HttpClient client = getHttpClient(user,password,uri);
			// continue with move
			UnLockMethod httpMethod = new UnLockMethod(getAbsoluteURI(uri).toString(), lockToken);
			client.executeMethod(httpMethod);
			// get resource, error is thrown if it doesn't exist
			resource = getResourceProperties(client,uri);
			httpMethod.releaseConnection();
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

	/**
	 * 
	 * @param client
	 * @param folderResources
	 * @param relativeUri
	 * @param createIfNotExists
	 * @return
	 * @throws IOException
	 * @throws DavException
	 * 
	 * @deprecated
	 */
	private WebDAVResourceType _getCollection(HttpClient client, List<WebDAVResourceType> folderResources, URI relativeUri, boolean createIfNotExists) throws IOException, DavException {
		WebDAVResourceType resource = getWebDAVResource(folderResources,relativeUri);
		if(resource == null && createIfNotExists) {
			resource = getCollection(client,relativeUri,createIfNotExists);
		}		
		return resource;
	}
	
	private WebDAVResourceType getCollection(HttpClient client, URI relativeUri, boolean createIfNotExists) throws IOException, DavException {
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
				DavMethod method = new MkColMethod(getAbsoluteURI(relativeUri).toString());
				client.executeMethod(method);
				method.releaseConnection();
				// then propfind if no error has been thrown
				resource = getResource(relativeUri);
				logger.info("collection "+relativeUri.toString() + " has been created");
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
	private List<WebDAVResourceType> getCollectionProperties(HttpClient client, URI relativeUri, int depth) throws IOException, DavException {
		List<WebDAVResourceType> resources = new ArrayList<WebDAVResourceType>();
	
		DavMethod method = new PropFindMethod(getAbsoluteURI(relativeUri).toString(), DavConstants.PROPFIND_ALL_PROP, depth);
		client.executeMethod(method);
		method.releaseConnection();
		
		MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();
	
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
		return resources;
	}

	private HttpClient getHttpClient(String user, String password, URI relativeUri) throws DavException {
		HostConfiguration hostConfig = new HostConfiguration();
		hostConfig.setHost(getAbsoluteURI(relativeUri).toString());
	
		HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		params.setMaxConnectionsPerHost(hostConfig, MAX_HOST_CONNECTIONS);
		connectionManager.setParams(params);
	
		HttpClient client = new HttpClient(connectionManager);
		
		client.setHostConfiguration(hostConfig);
	
		if(user!=null) {
			Credentials creds = new UsernamePasswordCredentials(user, password);
			client.getState().setCredentials(AuthScope.ANY, creds);
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

	private WebDAVResourceType getResourceProperties(HttpClient client, URI relativeUri) throws IOException,DavException {
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

	/**
	 * 
	 * @param client
	 * @param uri
	 * @param createIfNotExist
	 * @return
	 * @throws IOException
	 * @throws DavException
	 * 
	 * @deprecated
	 */
	private List<WebDAVResourceType> _getChildResources(HttpClient client, URI uri, boolean createIfNotExist) throws IOException, DavException {
		logger.debug("getChildResources for "+uri.toString());
		List<WebDAVResourceType> childResources = null;
		List<WebDAVResourceType> siblingResources = null;
		try {
			childResources = getChildResources(uri);
		} catch(DavException e) {
			// parent collection not found, therefore up one level
			if (HttpStatus.SC_NOT_FOUND == e.getErrorCode() && createIfNotExist) {
				siblingResources = _getChildResources(client, getParentURI(uri), createIfNotExist);
			} else {
				throw e;
			}
		}
		// if relativeUri is not member of parent collection, createIfNotExist
		if(siblingResources!=null && createIfNotExist && !isMemberOf(siblingResources,uri)) {
			// create the collection
			WebDAVResourceType selfResource = getCollection(client,uri,createIfNotExist);
			siblingResources.add(selfResource);
			childResources = new ArrayList<WebDAVResourceType>();
		}
		return childResources;
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