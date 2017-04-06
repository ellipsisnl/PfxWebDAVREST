package nl.ellipsis.webdav.client;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.jackrabbit.webdav.DavException;

import com.google.gson.Gson;

import nl.ellipsis.pfxwebdav.rest.v1.WebDAVPropertyType;
import nl.ellipsis.pfxwebdav.rest.v1.WebDAVResourceType;
import nl.ellipsis.webdav.client.WebDAVClientImpl;
import nl.ellipsis.webdav.client.WebDAVResourceStream;

public class WebDAVClientBaseTests {
	protected final static String DEFAULT_PROPERTIES = "test.properties";

	protected final static String ENV_FILE_PROPERTIES = "WebDAVTestProperties";

	protected final static String PROP_SERVER_DOMAIN = "server.domain";
	protected final static String PROP_SERVER_USER = "server.user";
	protected final static String PROP_SERVER_PWD = "server.password";
	protected final static String PROP_FILE_PATH = "file.path";
	protected final static String PROP_FILE_LARGE_PATH = "file.large.path";
	protected final static String PROP_FOLDER_PATH = "folder.path";

	public static void testDeleteResource(WebDAVClientImpl client, String path, boolean existingResource) {
		Gson gson = new Gson();
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			WebDAVResourceType resource = client.deleteResource(pfxUri);
			if(!existingResource) {
				fail("Nonexisting "+path+" should throw an exception");
			}
			assertNotNull(resource);
			System.out.println(path+": "+gson.toJson(resource));
		} catch(DavException e) {
			if(	existingResource || 
				(!existingResource && HttpStatus.SC_NOT_FOUND != e.getErrorCode())) {
				assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
			}
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		}
	}

	public static void testGetChildResources(WebDAVClientImpl client, String path) {
		Gson gson = new Gson();
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			List<WebDAVResourceType> resources = client.getChildResources(pfxUri);
			assertNotNull(resources);
			System.out.println(path+": "+gson.toJson(resources));
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		}
	}

	public static void testGetResource(WebDAVClientImpl client, String path) {
		Gson gson = new Gson();
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			WebDAVResourceType resource = client.getResource(pfxUri);
			assertNotNull(resource);
			System.out.println(path+": "+gson.toJson(resource));
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		}
	}

	public static void testGetResourceStream(WebDAVClientImpl client, String path, String targetDir) {
		WebDAVResourceStream resource = null;
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			resource = client.getResourceStream(pfxUri);
			assertNotNull(resource);
			
			String newFilePath = targetDir+resource.getFilename();
			File out = new File(newFilePath);
			assertFalse(out.exists());
			
			// Java-7 paradigm
			try (OutputStream os = new BufferedOutputStream(new FileOutputStream(newFilePath))) {
				org.apache.commons.io.IOUtils.copyLarge(resource.getInputStream(),os);
				os.flush();
			} catch (IOException e) {
	            e.printStackTrace();
	            assertTrue(e.getMessage(),false);
	        }
			assertTrue(out.exists());
			assertTrue(out.delete());
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		} finally {
			if(resource!=null) {
				try {
					// close both inputstream and connection
					resource.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void testMakeCollection(WebDAVClientImpl client, String path) {
		Gson gson = new Gson();	
		try {
			// a file
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			WebDAVResourceType resource = client.createCollection(pfxUri);
			assertNotNull(resource);
			
			System.out.println(path+": "+gson.toJson(resource));
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		}
	}
	
	public static void testPutResource(WebDAVClientImpl client, String path, File file, ContentType contentType) {
		Gson gson = new Gson();
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);

			WebDAVResourceType resource = client.putResource(pfxUri,file,contentType);

			assertNotNull(resource);
			System.out.println(path+": "+gson.toJson(resource));
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		}
	}
	
	public static void testPutLargeResource(WebDAVClientImpl client, String path, File file, ContentType contentType) {
		Gson gson = new Gson();
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			InputStream is = new FileInputStream(file);

			WebDAVResourceType resource = client.putResource(pfxUri,is,file.length(),contentType);

			assertNotNull(resource);
			System.out.println(path+": "+gson.toJson(resource));
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(Exception e) {
			assertTrue(e.getMessage(),false);
		}
	}
	
	public static void testSetProperties(WebDAVClientImpl client, String path, Map<String,Object> propertyMap) {
		Gson gson = new Gson();
		try {
			URI pfxUri = new URI(path);
			assertNotNull(pfxUri);
			WebDAVResourceType resource = client.setProperties(pfxUri, propertyMap);

			assertNotNull(resource);
			System.out.println(path+": "+gson.toJson(resource));
			
			assertProperties(resource,propertyMap);
			
		} catch(DavException e) {
			assertTrue(e.getMessage()+" ["+e.getErrorCode()+"]",false);
		} catch(IOException e) {
			assertTrue(e.getMessage(),false);
		} catch (URISyntaxException e) {
			assertTrue(e.getMessage(),false);
		}
	}

	private static void assertProperties(WebDAVResourceType resource, Map<String, Object> propertyMap) {
		if(resource!=null && propertyMap!=null) {
			for (Entry<String, Object> e : propertyMap.entrySet()) {
				assertProperty(resource.getProperties(),e.getKey(),e.getValue());
			}
		}
	}

	private static void assertProperty(List<WebDAVPropertyType> properties, String key, Object value) {
		if(key!=null) {
			assertNotNull(properties);
			WebDAVPropertyType property = null;
			for(WebDAVPropertyType p : properties) {
				if(p.getName().equals(key)) {
					property = p;
					break;
				}
			}
			assertNotNull("Property with key '"+key+"' could not be found",property);
			String sv = value.toString();
			assertTrue(property.getValue().contains(sv));
		}
	}

}
