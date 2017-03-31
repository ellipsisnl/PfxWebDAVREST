package nl.ellipsis.webdav.rest;

public interface PfxDocumentConstants {
	
	public final static String URI_SEPARATOR = "/";

	public interface DavProperties {
		public static String FILENAME 							= "filename";
		public static String URI 								= "uri";
	}

	public interface DavStatusCode {
		/*
		 * The 422 (Unprocessable Entity) status code means the server understands the content type of the request entity 
		 * (hence a 415(Unsupported Media Type) status code is inappropriate), and the syntax of the request entity is correct 
		 * (thus a 400 (Bad Request) status code is inappropriate) but was unable to process the contained instructions. 
		 * For example, this error condition may occur if an XML request body contains well-formed (i.e., syntactically correct), 
		 * but semantically erroneous XML instructions.
		 */
		public static int UNPROCESSABLE_ENTITY 					= 422;
	}

	public interface API_JSONKeys {
		public static String ACTOR 								= "actor";
		public static String CONTENT 							= "content";
		public static String DATATABLES_AADATA 					= "aaData";
		public static String DATATABLES_ITOTAL_DISPLAYRECORDS 	= "iTotalDisplayRecords";
		public static String DATATABLES_ITOTAL_RECORDS 			= "iTotalRecords";
		public static String DESCRIPTION 						= "description";
		public static String END_DATETIME 						= "endDatetime";
		public static String EFFECTIVE_DATETIME 				= "effectiveDatetime";
		public static String ID 								= "id";
		public static String IDENTIFIER 						= "identifier";
		public static String JQUERY_FILEUPLOAD_DELETE_URL 	= "delete_url"; 
		public static String JQUERY_FILEUPLOAD_DELETE_TYPE = "delete_type";
		public static String JQUERY_FILEUPLOAD_ERROR 		= "error";
		public static String JQUERY_FILEUPLOAD_NAME 			= "name"; 
		public static String JQUERY_FILEUPLOAD_SIZE 			= "size"; 
		public static String JQUERY_FILEUPLOAD_THUMBNAIL_URL	= "thumbnail_url"; 
		public static String JQUERY_FILEUPLOAD_URL 				= "url";
		public static String MIMETYPE 							= "mimetype";
		public static String OWNER 								= "owner";
		public static String PARENT_ID 							= "parentId";
		public static String PARENT_IDENTIFIER					= "parentIdentifier";
		public static String POSITION 							= "position";
		public static String PRIVATE 							= "private"; 
		public static String REFERENCE_OBJECT 					= "referenceObject";
		public static String REFERENCE_OBJECT_ID 				= "referenceObjectId";
		public static String REFERENCE_SUBOBJECT_ID 			= "referenceSubObjectId";
		public static String RESOURCE_ID 						= "resourceId";
		public static String REST_URI 							= "restUri";
		public static String UPDATE_DATETIME 					= "updateDatetime";
	}	
	
	public interface PfxFilePartKeys {
		public static String BASEPATH 					= "basepath";
		public static String DOCUMENT_MODULE 			= "documentModule";
		public static String FILE_BASEPATH 				= "fileBasepath";
		public static String FILENAME 					= "filename";
		public static String SESSION_ID 					= "sessionId";
		public static String SERVLET_CONTEXT 			= "servletContext";
	}

	public interface HttpServletParameterKeys {
		public static String CLEAR_CACHE 		= "clearCache";
		public static String CREATE_THUMBNAIL 	= "createThumbnail";
		public static String DESCRIPTION        = "description";
		public static String ID 				= "id";
		public static String IDS 				= "ids";
		public static String IDENTIFIER 		= "identifier";
		public static String INSTITUTE_ID 		= "instituteId";
		public static String LANGUAGE 			= "language";
		public static String OBJECT_ID 			= "objectId";
		public static String OBJECT_TYPE 		= "objectType";
		public static String PINCODE 			= "pincode";
		public static String PRIVATE 			= "private";
		public static String RESOURCE_ID 		= "resourceId";
		public static String RESOURCE_TYPENAME = "resourceTypeName";
		public static String STATUS 			= "status";
		public static String SUBOBJECT_ID 		= "subObjectId";
		public static String THUMBNAIL 			= "thumbnail";
		public static String URI 				= "uri";
	}

	public interface ObjectType {
		public static String DOSSIER 	= "dossier"; 
		public static String CASE 	= "case"; 
	}

	public interface ResourceType {
		public static String NOTES 		= "NOTES"; 
		public static String FILES 		= "FILES"; 
	}

	public interface PfxMessageTemplateNames {
		public static String DECISION_NOTIFICATION_MAIL		= "decisionNotificationMail"; 
		public static String PROCESS_STEP_NOTIFICATION_MAIL	= "processStepNotificationMail"; 
	}

	public interface PfxMessageTemplateKeys {
		public static String APPLICATION_PATH		= "applicationPath"; 
		
	}

}

