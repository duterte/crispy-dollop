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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import oracle.km.demo.ConfigParams.ParamTypes;


/**
 * 	This program is provided as a demonstration of making a single POST call to the /content/import REST resource with file attachments.
 *  This code is intended only as an example of the multipart/form-data creation for the POST call.
 *	
 *	Please note that the required request entity compositeContentBO must be supplied as an external file. 
 *  Refer to the API documentation, included sample files and the README for more details.
 *  
 */
public class ContentUploadSample {
	
	static boolean loggingEnabled;

	public static void printUsage() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\nUsage: java -jar contentUploadSample.jar [Parameter pairs]\n");
		sb.append("Parameter pairs are defined as --parameter:value \n");
		sb.append("The order is not important, listed below are required parameters\n\n");
		sb.append(" --baseUrl:http://localhost:7004/km/api/latest \n");
		sb.append(" --siteName:NameOfSite \n");
		sb.append(" --apiUser:apiUserVal \n");
		sb.append(" --apiPassword:apiPasswordVal \n");
		sb.append(" --consoleUser:consoleUserName \n");
		sb.append(" --consolePassword:consoleUserPassword \n");
		sb.append(" --filesToUpload:[\"file1\",\"file2\"] \n");
		sb.append(" --payloadLocation:LocationtoPayloadFile \n\n");
		sb.append(" The following parameter pairs are optional:\n\n");
		sb.append(" --payloadType:json\n");
		sb.append(" --localeId:en_US\n");
		sb.append(" --ignoreSSL:true\n");
		sb.append(" --enableLogging:false\n\n");
		sb.append("\nAny or all of the parameters listed above can be specified in a configuration file, \n");
		sb.append("Command line values have precedence over the values in the configuartion file specified by the next parameter pair\n");
		sb.append(" --configFile:path\n");
		
		System.out.print(sb.toString());
	}
	
    public static void main(String[] args) {
//		hardcoded
			String[] args2 = {
							"--baseUrl:https://does_not_exist-irs.custhelp.com",
							"--siteName:does_not_exist",
							"--apiUser:********",
							"--apiPassword:********",
							"--consoleUser:********",
							"--consolePassword:********",
							"--filesToUpload:[test.txt]",
							"--payloadLocation:payload.test.json",
			};

    	// Read configuration settings either passed in the command line or in a separate file
    	ConfigParams params = ConfigParams.parseConfigParams(args);
    	
    	// Encountered problem, print usage string.
    	if (params == null) {
    		printUsage();
    		return;
    	}
    	
    	// static variable to keep track of how much information to log to the console.
    	loggingEnabled = params.getBooleanValue(ConfigParams.ParamTypes.ENABLE_LOGGING.getString());
    	
    	// When logging is enabled the raw wire traffic on the REST calls are output to the console.
    	if (loggingEnabled) {
    		enableLogging();    			
    	}
    	
    	CloseableHttpClient httpClient = null;
    	
    	try {

	    	httpClient = ConnectionManager.getHttpConnection(params.getBooleanValue(ConfigParams.ParamTypes.INGNORE_SSL.getString()));
	    	
	    	// The kmauthtoken header that is needed to make the POST call to /content/import is built in the authenticate method which makes POST call
	    	// to /auth/integration/authorize and /auth/authorize.
	    	String kmauthtoken = authenticate(params, httpClient);
	    	if (kmauthtoken == null || kmauthtoken.isEmpty()) {
	    		throw new RuntimeException ("Unable to authenticate the user, please check your userName and password values");
	    	}
	    	
	    	// Call constructs the Http post and attaches the multi part form data
	    	HttpUriRequest httpRequest = constructHttpRequest(params, kmauthtoken);
	    	
	    	CloseableHttpResponse response = httpClient.execute(httpRequest);
	    	
	    	// parses the Http response to get the headers and the returned object into string.
	    	String msg = getResponseDetails(response);
	    	
	    	if (response.getStatusLine().getStatusCode() != 201) {
	    		throw new RuntimeException (msg);
	    	}
	    	
	    	outputResponse (loggingEnabled, msg, response.getStatusLine().toString());
	    	
    	} catch (Exception e) {
    		e.printStackTrace();
    	} finally {
    		try {
    			// Closing the http connection to the server, this is in the finally block as exceptions may happen after getting the connection. 
    			if (httpClient != null) {
    				httpClient.close();
    			}
			} catch (IOException e) {
				e.printStackTrace();
			}   	    		
    	}
    }    
   
    private static HttpUriRequest constructHttpRequest (ConfigParams params, String kmauthtoken) throws Exception {
    	HttpEntityEnclosingRequestBase request = null;
    	    	
    	request = new HttpPost(params.getRequestUrl());    	
		request.addHeader("kmauthtoken", kmauthtoken);
		// Boundary string returned from the call to getBoundarystring() is just a delimiter between the multi part forms.
		request.addHeader("Content-Type", "multipart/form-data; charset=utf-8; boundary=" + params.getBoundaryString());
		request.addHeader("Accept", "application/json");
		MultipartEntityBuilder formEntity = MultipartEntityBuilder.create()
											.setBoundary(params.getBoundaryString())
											// This adds the BO part of the multi part form data -> right now the sample supports the /content/import API so
											// the params.getBOName() will return the string "compositeContentBO".
											// the params.getBOObject() will return a string that represents the BO in either json or xml format
											// params.getBOType will return the string "application/json" or "application/xml"
											.addTextBody(params.getBOName(), params.getBOObject(), ContentType.create(params.getBOType()));
			
		for (File fileToAttach : params.getFilesToUploadList()) {
			formEntity.addBinaryBody("filesToUpload", fileToAttach, ContentType.create("application/octet-stream"), fileToAttach.getName());
		}
			
		request.setEntity(formEntity.build());
		
    	return request;
    }
    
    
	private static void outputResponse(boolean enableLogging, String details, String statusLine) {
		// Default wire traffic will show the response messages if logging is enabled.
		if (enableLogging == false ) {
			System.out.println(statusLine);
			System.out.println(details);
		}		
	}

	private static String getResponseDetails(CloseableHttpResponse httpResponse) {
		HttpEntity respEntity = httpResponse.getEntity();
		String msg = null;
		try {
			msg = EntityUtils.toString(respEntity);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return msg;
	}

	private static void enableLogging() {
		java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
		java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
	
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
		System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "debug");
	}

	private static String authenticate(ConfigParams params, CloseableHttpClient httpClient) {
		String kmauthtoken = null;
		
		System.out.println("Trying to authenticate the integration user and then the console user");
		
		// Setup the request headers
		String url = params.getIntegrationUrl();
		HttpPost request = new HttpPost(url);
		request.addHeader("kmauthtoken",params.getApiUserKMHeader());
		request.addHeader("Accept","application/json");

		// Setup the payload

		String apiPayload = params.getApiUserPayload();
		System.out.println(url);
		System.out.println(Arrays.deepToString(request.getAllHeaders()));
		System.out.println(apiPayload);
		StringEntity entity = new StringEntity(apiPayload, ContentType.create("application/json"));
		request.setEntity(entity);
		
		CloseableHttpResponse response = null;		
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			// this call is POST /auth/integration/authorize which returns the integration user token.
			response = httpClient.execute(request);			 				
			String msg = getResponseDetails(response);

			outputResponse (loggingEnabled, msg, response.getStatusLine().toString());
			
			if (response.getStatusLine().getStatusCode() == 200) {
				JsonNode node = mapper.readTree(msg);
				
				String authenticationToken = node.get("authenticationToken").asText();
				System.out.println(authenticationToken);
				request = new HttpPost(params.getUserUrl());
				
				// the getUserKMHeader method inserts the integration user token returned from the first call (POST /auth/integration/authorize) into the
				// kmauthtoken so that we can make the call to /auth/authorize.
				request.addHeader("kmauthtoken", params.getUserKMHeader(authenticationToken));
				request.addHeader("Content-type", "application/x-www-form-urlencoded");
				request.addHeader("Accept", "application/json");
				
 			   	List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
 			    formParameters.add(new BasicNameValuePair("siteName", params.getStringValue(ParamTypes.SITE_NAME.getString())));
 			    formParameters.add(new BasicNameValuePair("userName", params.getStringValue(ParamTypes.CONSOLE_USER.getString())));
 			    formParameters.add(new BasicNameValuePair("password", params.getStringValue(ParamTypes.CONSOLE_PASSWORD.getString())));
 			    formParameters.add(new BasicNameValuePair("userExternalType", "ACCOUNT"));
 			    
 			    request.setEntity(new UrlEncodedFormEntity(formParameters));
 			    response = httpClient.execute(request);

				msg = getResponseDetails(response);
				outputResponse (loggingEnabled, msg, response.getStatusLine().toString());
 			    
 				if (response.getStatusLine().getStatusCode() == 200) {
 					msg = mapper.readTree(msg).get("authenticationToken").asText();
 					msg.replaceAll("\\/","");
 					
 					node = mapper.readTree(msg);
 					// successful call to /auth/authorize returns the user token as part of the kmauthtoken header, but the integrationUserToken value is set to null so
 					// it needs to be copied from a saved value to have the full header.
 					((ObjectNode)node).put ("integrationUserToken",authenticationToken);
 					kmauthtoken = node.toString();
 				} 				
			}
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return kmauthtoken;
	}
}
