package nl.ellipsis.webdav.client;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.entity.ContentType;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import nl.ellipsis.webdav.client.WebDAVClientImpl;

public class TestWebDAVClient extends WebDAVClientBaseTests {
	private final static String OUT_PATH = "/test/out";

	protected Properties properties;
	
	private WebDAVClientImpl client;
	private String absoluteBasePath;
	
	@Before
	public void setup() {
		// Log4J junit configuration.
		BasicConfigurator.configure();
		try {
			File out = new File("");
			absoluteBasePath = out.getAbsolutePath();

			Map<String, String> env = System.getenv();
			String propPath = (env.containsKey(ENV_FILE_PROPERTIES) ? env.get(ENV_FILE_PROPERTIES) : DEFAULT_PROPERTIES);
			properties = new Properties();
			properties.load(new FileInputStream(absoluteBasePath + "/src/test/resources/" + propPath));
			URI serverUri = new URI(properties.getProperty(PROP_SERVER_DOMAIN));
			assertNotNull(serverUri);
			client = new WebDAVClientImpl(properties.getProperty(PROP_SERVER_USER), properties.getProperty(PROP_SERVER_PWD), serverUri);
			assertNotNull(client);
		} catch (DavException e) {
			assertTrue(e.getMessage() + " [" + e.getErrorCode() + "]", false);
		} catch (Exception e) {
			assertTrue(e.getMessage(), false);
		}
	}

	@Test
	public void testGetChildResourcesForFile() {
		String path = properties.getProperty(PROP_FILE_PATH);
		testGetChildResources(client,path);
	}

	@Test
	public void testGetChildResourcesForFolder() {
		String path = properties.getProperty(PROP_FOLDER_PATH);
		testGetChildResources(client,path);
	}

	@Test
	public void testGetResourceForFile() {
		String path = properties.getProperty(PROP_FILE_PATH);
		testGetResource(client,path);
	}

	@Test
	public void testGetResourceForLargeFile() {
		String path = properties.getProperty(PROP_FILE_LARGE_PATH);
		testGetResource(client,path);
	}


	@Test
	public void testGetResourceForFolder() {
		String path = properties.getProperty(PROP_FOLDER_PATH);
		testGetResource(client,path);
	}

	@Test
	public void testGetResourceStream() {
		String path = properties.getProperty(PROP_FILE_PATH);
		testGetResourceStream(client,path,absoluteBasePath+OUT_PATH);
	}

	@Test
	public void testGetLargeResourceStream() {
		String path = properties.getProperty(PROP_FILE_LARGE_PATH);
		testGetResourceStream(client,path,absoluteBasePath+OUT_PATH);
	}

	@Test
	public void testMakeAndDeleteCollection() {
		String path = PROP_FOLDER_PATH + "/a/b/c/d";
		testMakeCollection(client,path);

		path = PROP_FOLDER_PATH+"/a";
		testDeleteResource(client,path,true);
	}

	@Test
	public void testDeleteNonExistingResource() {
		String path = properties.getProperty(PROP_FILE_PATH)+".sample";
		testDeleteResource(client,path,false);
	}

	@Test
	public void testUploadAndDeleteResource() {
		File f = new File(absoluteBasePath+"/test/data/sample.pdf");
		assertTrue(f.getAbsolutePath(),f.exists());
		
		String path = properties.getProperty(PROP_FOLDER_PATH) + "/sample.pdf";
		testPutResource(client,path,f,ContentType.create("application/pdf"));

		testDeleteResource(client,path,true);
	}

	@Test
	public void testUploadAndDeleteLargeResource() {
		File f = new File(absoluteBasePath+"/test/data/BladeRunnerCD1.mkv");
		assertTrue(f.getAbsolutePath(),f.exists());
		
		String path = properties.getProperty(PROP_FOLDER_PATH) + "/BladeRunnerCD1.mkv";
		testPutLargeResource(client,path,f,ContentType.create("application/mkv"));

		// testDeleteResource(client,path,true);
	}

	@Test
	public void testSetProperties01() {
		String path = properties.getProperty(PROP_FILE_PATH);
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("a-key", "a-value");
		map.put("b-key", "b-value");
		testSetProperties(client,path,map);
	}

	@Test
	public void testSetProperties02() {
		String path = properties.getProperty(PROP_FILE_PATH);
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("a-key", "a-value-2");
		testSetProperties(client,path,map);
	}
}
