/**
* Copyright (c) 2016, Oracle Corporation and/or its affiliates. All rights reserved.
*
* The sample code in this document or accessed through this document is not certified or
* supported by Oracle. It is intended for educational or testing purposes only. Use of this
* sample code implies acceptance of the License Agreement
* (http://www.oracle.com/technetwork/licenses/standard-license-152015.html).package oracle.km.demo;
* 
*/

package oracle.km.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * 	  This class parses the external configuration file and provides convenience methods for the authentication payload, url etc .. 
 * constructed using the values specified in the configuration file.
 * 
 * 	Map contains the value of the required and optional parameter values that can be retrieved by specifying the parameter string.
 */
class ConfigParams {
	
	public enum ParamTypes {
		BASE_URL("baseUrl", true, null),
		SITE_NAME("siteName", true, null),
		API_USER("apiUser", true, null),
		API_PASSWORD("apiPassword", true, null),
		CONSOLE_USER("consoleUser", true, null),
		CONSOLE_PASSWORD("consolePassword", true, null),
		FILESTOUPLOAD("filesToUpload", true, null),
		PAYLOAD_LOCATION("payloadLocation", true, null),	// Location to the compositeContentBO object 
				
		PAYLOAD_TYPE("payloadType", false, "json"),				// either xml or json defaults to json => file containing the bo must be in correct format.
		LOCALE_ID("localeId", false, "en_US"),				// defaults to en_US
		INGNORE_SSL("ignoreSSL", false, "true"),				// defaults to true 
		ENABLE_LOGGING("enableLogging", false, "false"),		// defaults to false, set it to true to get raw Http traffic on the console		
		
		REST_ENDPOINT("restEndpoint", false, "/km/api/v1/content/import");	// will be ignored even if specified in configuration file or on the command line.
		
		private String paramString;
		private boolean isRequired;
		private String defaultValue;
		
		ParamTypes(String paramString, boolean isRequired, String defaultValue) {
			this.paramString = paramString;
			this.isRequired = isRequired;
			this.defaultValue = defaultValue;
		}
		
		public String getString() {
			return paramString;
		}
				
		public boolean getIsRequired() {
			return isRequired;
		}
				
		public String getDefaultValue() { 
			return defaultValue; 
		}
	}

	static private Map<String, String> paramsAndValues = new HashMap<>();
	static private List<File> filesToUploadList = new ArrayList<>();
	
	static private final String FILESTOUPLOADSTRING="filesToUpload";
	private String baseUrl;
	
	boolean getBooleanValue (String paramString) {
		return paramsAndValues.get(paramString).equalsIgnoreCase("true");
	}
	
	String getStringValue(String paramString) {
		return paramsAndValues.get(paramString);
	}
	
	String getRequestUrl() {
		return baseUrl + getStringValue(ParamTypes.REST_ENDPOINT.getString());
	}

	String getIntegrationUrl() {
		return baseUrl + "/km/api/latest/auth/integration/authorize";
	}
	
	String getUserUrl() {
		return baseUrl + "/km/api/latest/auth/authorize";
	}
	
	private String getSiteName() {
		return getStringValue(ParamTypes.SITE_NAME.getString());
	}
	
	private String getLocaleId() {
		return getStringValue(ParamTypes.LOCALE_ID.getString());
	}
	
	public String getApiUserKMHeader() {
		return String.format("{\"siteName\":\"%s\",\"localeId\":\"%s\"}", getSiteName(), getLocaleId() );
	}
	
	public String getUserKMHeader(String authenticationToken) {
		String formatStr = "{\"siteName\":\"%s\",\"localeId\":\"%s\",\"integrationToken\":\"%s\"}";	

	   return String.format(formatStr, getSiteName(), getLocaleId(), authenticationToken);
	}
	
	public String getApiUserPayload() {		
		return String.format("{\"siteName\":\"%s\",\"login\":\"%s\",\"password\":\"%s\"}", getSiteName(), getStringValue(ParamTypes.API_USER.getString()), 
				getStringValue(ParamTypes.API_PASSWORD.getString()));
	}
	
	// Sample application only works for one rest endpoint /content/import => bo needed for this endpoint is compositeContentBO
	String getBOName() {
		return "compositeContentBO";
	}
	
	// Read contents of the file specified in the configuration file for the BO location and return it as String value.
	String getBOObject() {
		String boObject = null;
		
		try {
			boObject = new String (Files.readAllBytes(Paths.get(getStringValue(ParamTypes.PAYLOAD_LOCATION.getString()))));
		}
		catch (IOException e) {
			boObject = null;
			e.printStackTrace();
		}
		return boObject;
	}

	List<File> getFilesToUploadList() {
		return filesToUploadList;
	}
	
	// returns either application/json or application/xml.  The file specified by the boLocation parameter must be the correctly formatted
	// xml or json version of the BO needed for the rest call, for this sample application it is the compositeContentBO.
	String getBOType() {
		if (getStringValue(ParamTypes.PAYLOAD_TYPE.getString()).equalsIgnoreCase("json")) {
			return "application/json";
		}

		return "application/xml";
	}
	
	public String getBoundaryString() {
    	// This just needs to be an arbitrary string used as the boundary marker for the multipart form data.
    	return "Content-upload-delimiter";
	}
	
	public static ConfigParams parseConfigParams(String[] args) {
		ConfigParams params = new ConfigParams();
		
		try {
			for (String paramPair : args) {
				if (paramPair.startsWith("--")) {
					int parameterEndLoc = paramPair.indexOf(':');
					if (parameterEndLoc == -1) {
						throw new RuntimeException("Invalid parameter entered on the command line");
					}
					
					// split paramPair after the first instance of : 
					String parameter = paramPair.substring(2, parameterEndLoc);
					String value = paramPair.substring(parameterEndLoc+1, paramPair.length());
					
					if (parameter.equalsIgnoreCase("configFile")) {
						parseConfigFile(value, params);
					}
					else {
						if (!parameter.equalsIgnoreCase("restEndpoint")) {
							paramsAndValues.put(parameter, value);
							
							if (parameter.equalsIgnoreCase("filesToUpload")) {
								parseFilesToUploadCommandLine(value);
							}							
						}
					}
				}
			}

			// validate all required parameters have been set!
			for (ParamTypes paramType : ParamTypes.values()) {
			
				if (!paramsAndValues.containsKey(paramType.getString())) {
					if (paramType.isRequired) {
						System.out.println ("Please specify the required parameter : " + paramType.getString());
						return null;
					}
					// If optional parameters are not found, just use the default value specified in the enumeration.
					else {					
						paramsAndValues.put(paramType.getString(), paramType.getDefaultValue());
					}
				}
			}
			params.baseUrl = paramsAndValues.get(ParamTypes.BASE_URL.getString());
			if (params.baseUrl.endsWith("/")) {
				params.baseUrl = params.baseUrl.substring(0, params.baseUrl.length()-1);
			}
		}
		
		catch (Exception e) {
			return null;
		}
		return params;
	}

	/*
	 * 	Same logic as the next method, just that the input method is from the command line parameter in the form of --filesToUpload:[file1,file2] 
	 * instead of being a JSON array node. so the format is that the [ ] are the first and last characters of the filesToUpload value.
	 */
	private static void parseFilesToUploadCommandLine (String value) {		
		String[] filesList = value.substring(1, value.length()-1).split(",");
		
		filesToUploadList.clear();
		
		for ( String curFile : filesList) {
			File file = new File(curFile);
			if (!file.exists()) {
				throw new RuntimeException("File specified on the commandline option does not exist : " + file.getAbsolutePath());
			}
			
			if (file.isDirectory()) {
				for (File fileInDir : file.listFiles()) {
					if (!fileInDir.isDirectory()) {
						filesToUploadList.add(fileInDir);
					}
				}
			}
			else {
				filesToUploadList.add(file);
			}			
		}
	}
	
	/* 
	 * 	User will be allowed to specify a list of files in the filesToUpload array, it is also possible that the list point to a directory containing files
	 *  that will be attached to a particular content to be created as result of the POST /content/import call.  
	 *  
	 *  Do note that the content xml must contain each of the attachments one by one to match the list of file(s) that will be generated as a result of the
	 *  parsing of the filesToUpload array.
	 *  
	 */
	private static void parseFilesToUpload(JsonNode root, String nodeName) {
		JsonNode filesToUpload = root.get(nodeName);
		
		for ( JsonNode fileName : filesToUpload) {
			File file = new File(fileName.asText());
			if (!file.exists()) {
				throw new RuntimeException("File specified on the configuartion file does not exist : " + file.getAbsolutePath());
			}
			
			if (file.isDirectory()) {
				for (File fileInDir : file.listFiles()) {
					if (!fileInDir.isDirectory()) {
						filesToUploadList.add(fileInDir);
					}
				}
			}
			else {
				filesToUploadList.add(file);
			}
		}		
	}
	
	public static void parseConfigFile (String configFilePath, ConfigParams params) throws Exception {
	
		JsonNode root = null;
		ObjectMapper mapper = null;
		
		try {
			mapper = new ObjectMapper();
			root = mapper.readTree(new File(configFilePath));				
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}			
		
		try {		
			for (ParamTypes paramType : ParamTypes.values()) {
				String paramStr = paramType.getString();
				if ( root.has(paramStr) == true) {
					// Do not over write any value set in the command line.
					if (!paramsAndValues.containsKey(paramStr)) {
						// Will not support any other rest endpoint than /content/import
						if (!paramStr.equalsIgnoreCase("restEndpoint")) {
							paramsAndValues.put(paramStr, root.get(paramStr).asText());
						}
						
						if (paramStr.equalsIgnoreCase(FILESTOUPLOADSTRING)) {
							 parseFilesToUpload(root, paramType.getString());
						}						
					}
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Please refer to the README.txt file for the correct format of the configuration file");
			throw e;
		}			
	}
}
	