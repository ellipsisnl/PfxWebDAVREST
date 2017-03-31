package nl.ellipsis.webdav.rest;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class PfxDocumentJSONObject {
	
	// download resource and thumbnail uri sent in JSON response to caller (UI)
	public final static String KEY_UPLOAD_RESOURCE_URL								= "uploadResourceUrl";
	public final static String KEY_RESPONSE_DOWNLOAD_RESOURCE_URL				= "responseDownloadResourceUrl";
	public final static String KEY_RESPONSE_DOWNLOAD_THUMBNAIL_RESOURCE_URL	= "responseDownloadTNResourceUrl";
	// delete uri sent in JSON response to caller (UI)
	public final static String KEY_RESPONSE_DELETE_RESOURCE_URL					= "responseDeleteResourceUrl";
	// resource uri for serialization in temporary local folder
	public final static String KEY_TEMP_UPLOAD_RESOURCE_URL						= "tempUploadResourceUrl";
	//private static final String KEY_TEMP_RESOURCE_THUMBNAIL_URL		= "{sessionid}/{domain}/{assessmentIdentifier}/{surveyIdentifier}/{itembankIdentifier}/"+prefixThumbnail+"{filename}";
	// upload resource and thumnail uri for webdav (DocumentModule 
	public final static String KEY_WEBDAV_UPLOAD_RESOURCE_URL					= "webdavUploadResourceUrl";
	public final static String KEY_WEBDAV_UPLOAD_RESOURCE_THUMBNAIL_URL		= "webdavUploadTNResourceUrl";

	public final static String REQUEST_PARAMETER_KEY_THUMBNAIL = "thumbnail";
	private final static String THUMBNAIL_POSTFIX = "&"+REQUEST_PARAMETER_KEY_THUMBNAIL+"=true";
	
	private Map<String,String> fileparts;   
	private Long filesize;
	private String errortext;
	private boolean createThumbnail = false;
	private boolean thumbnailCreated = false;
	
	public PfxDocumentJSONObject(Map<String,String> fileparts, Long filesize, boolean createThumbnail) {
		this.fileparts = fileparts;
		this.filesize = filesize;
		this.createThumbnail = createThumbnail;
	}
	
	public boolean createThumbnail() {
		return createThumbnail;
	}
	
	public void setThumbnailCreated(boolean thumbnailCreated) {
		this.thumbnailCreated = thumbnailCreated;
	}
	
	public JsonObject getJSONObject(Map<String,String> urlMap) {
		return getUploadResourceResponseAsJSONObject(urlMap,fileparts,filesize,errortext);
	}
	
	public void setErrorText(String errorText) {
		this.errortext = errorText;
	}
	
	public void setFilesize(Long filesize) {
		this.filesize = filesize;
	}
	
	public void setFilepart(String key, String value) {
		if(fileparts == null) {
			fileparts = new HashMap<String,String>();
		}
		fileparts.put(key, value);
	}
	
	public String getResourceUrl(Map<String,String> urlMap) {
		return getUrl(urlMap,KEY_WEBDAV_UPLOAD_RESOURCE_URL,fileparts);
	}

	public String getResourceThumbnailUrl(Map<String,String> urlMap) {
		return getUrl(urlMap,KEY_WEBDAV_UPLOAD_RESOURCE_THUMBNAIL_URL,fileparts);
	}
	
	public String getTempResourceUrl(Map<String,String> urlMap) {
		return getUrl(urlMap,KEY_TEMP_UPLOAD_RESOURCE_URL,fileparts);
	}
	
//	public String getTempResourceThumbnailUrl() {
//		return getUrl(TEMP_RESOURCE_THUMBNAIL_URL,fileparts);
//	}
	
	private JsonObject getUploadResourceResponseAsJSONObject(Map<String,String> urlMap, Map<String,String> fileparts, Long filesize, String errorText) {
		JsonObject jsonObj = new JsonObject();
		jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_NAME, fileparts.get(PfxDocumentConstants.PfxFilePartKeys.FILENAME));
		if(filesize!=null) {
			jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_SIZE, filesize);
		}
		if(!StringUtils.isEmpty(errorText)) {
			jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_ERROR, errorText);
		} else {
			String uploadResourceUrl = getUrl(urlMap,KEY_UPLOAD_RESOURCE_URL,fileparts);
			if(!StringUtils.isEmpty(uploadResourceUrl)) {
				jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_URL, uploadResourceUrl);
			}
			if(createThumbnail && thumbnailCreated) {
				String thumbnailUrl = getUrl(urlMap,KEY_RESPONSE_DOWNLOAD_THUMBNAIL_RESOURCE_URL,fileparts);
				if(!StringUtils.isEmpty(thumbnailUrl)) {
					jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_THUMBNAIL_URL, thumbnailUrl);
				}
			}
			String deleteUrl = getUrl(urlMap,KEY_RESPONSE_DELETE_RESOURCE_URL,fileparts);
			if(createThumbnail && thumbnailCreated) {
				deleteUrl += THUMBNAIL_POSTFIX;
			}
			if(!StringUtils.isEmpty(deleteUrl)) {
				jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_DELETE_URL, deleteUrl);
			}
			jsonObj.addProperty(PfxDocumentConstants.API_JSONKeys.JQUERY_FILEUPLOAD_DELETE_TYPE, "GET");
		}
		return jsonObj;
	}

	private String getUrl(Map<String, String> urlMap, String urlKey, Map<String, String> fileparts) {
		String retval = null;
		if(urlMap!=null && urlMap.containsKey(urlKey)) {
			String url = urlMap.get(urlKey);
			retval = url;
			String[] sl = url.split(PfxDocumentConstants.URI_SEPARATOR);
			if(sl!=null) {
				for(int i=0; i<sl.length; i++) {
					String s = sl[i];
					if(s.indexOf("{")>-1 && fileparts!=null) {
						String placeholder = s.substring(s.indexOf("{")+1,s.indexOf("}"));
						if(fileparts.containsKey(placeholder)) {
							retval = retval.replace("{"+placeholder+"}", fileparts.get(placeholder));
						}
					}
				}
			}
		}
		return retval;
	}
}
