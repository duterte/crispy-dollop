/**
* Copyright (c) 2017, Oracle Corporation and/or its affiliates. All rights reserved.
*
* The sample code in this document or accessed through this document is not certified or
* supported by Oracle. This program is intended for educational or testing purposes only. Use of this
* sample code implies acceptance of the License Agreement
* (http://www.oracle.com/technetwork/licenses/standard-license-152015.html).package oracle.km.demo;
* 
*/

This program demonstrates an approach to calling the POST /content/import REST endpoint with file attachments.

The program also shows how to construct the "kmauthtoken" header by authenticating and authorizing the API user and 
console user through the POST /auth/integration/authorize and POST /auth/authorize requests in sequence.

Please refer to the authentication and access portion of the getting started guide of the API documentation 
for further details on the kmauthtoken and API and console users.

To build the project:
  - Requirements:
	JDK 1.7
	Maven 3.3.x and Internet connection to download dependencies from maven repositories
	
  - Tech stack:
	Apache http-components 4.5.2
	Fasterxml jackson-databind 2.7 
	
  - Build command:
	mvn clean install

Run the program:

	java -jar ContentUploadSample-1.0.jar [parameter:value pairs]

1.  Parameter pairs are expected to be in the format:  --parameter:value  for example --siteName:siteName  
2.  Order that the parameters appear is not important.  Parameters passed directly on the command line will have precedence over parameters specified in a JSON configuration file.
3.  Configuration file specification is OPTIONAL and is set as follows -
	--configFile:pathToFile

These are the required parameters
	--baseUrl:  URL path that includes http(s)://hostName:portIfAny/km/api/latest.  
	--siteName:  Site Name to use
	--apiUser:  API user name
	--apiPassword:  API user's password
	--consoleUser:  Console user name
	--consolePassword:  Console user's password
	--payloadLocation: Path to the external payload file that defines the compositeContentBO object in either JSON or XML format.

	--filesToUpload:  Specifies the file(s) to attach to the article being created.  The list of files or directories must be comma separated and be enclosed in [].  
					  For example --filesToUpload:[file1, file2].
					  If a directory is specified all files directly under the directory will be included in the multipart "filesToUpload" part(s).

The content xml provided within the payload by the user has to properly address all file attachments.  The sample application does not validate handling of attachments within the payload.

The following are optional parameters
	--payloadType:  Either json (default) or xml.
	--localeId:  Defaults to en_US. The localeId of the Locale you are trying to access the APIs with. E.g. en_US for English United States.
	--ignoreSSL:  Either true (default) or false. When set to true SSL certificate errors will be ignored when obtaining the Http connection.
	--enableLogging:  Either true or false (default). When set to true the raw http wire traffic of the REST calls will be output to the console.
	--configFile:  No default.  If a config file is specified, the parameter values within will be overridden by any matching parameters specified on the command line.	

Listed below is what the content of a configuration file would look like, a sample configuration file is also included (config.json):
	
{
	"baseUrl" : ""https://day4-17200-sql-10h-irs.dv.lan/km/api/latest", 
	"siteName" : "day4-17200-sql-10h",
	"apiUser" : "apiUserName",
	"apiPassword" :"apiUserPassword",
	"consoleUser" : "consoleUserName",
	"consolePassword" : "consoleUserPassword",
	"localeId" : "en_US",	
 	"payloadLocation" : "path to file", 
	"filesToUpload" : ["d:/dev/files/1.txt", "d:/attachments/1.png"],	
	"ignoreSSL" : true, 
	"enableLogging" : false
}
   
--- Creating content through the /content/import API ----

The payload file that is given for the /content/import REST endpoint must be associated with a specific Content Type in the repository.  

The following example payload json file (import.json) is based on the "TWOFILES" Content Type.
Please refer to the Content Type configuration pdf document that describes the schema attributes.

The sample payload shows the minimum required fields, so a real production case will set additional
 fields, please consult the API documentation for more details.
				   
{	
	"content" : {
	  "isForEdit":true,	
	  "views":[		
		{
		  "recordId":"9B92DE0F2C9A41BCBFB2FADA41AF6CDB",	
		  "referenceKey":"TENANT",
		  "name":"okcs_test"
		}
	  ],
	  "locale":{
		"recordId":"en_US"
	  },
	  "contentType":{	
		"recordId":"0CDC74354EED4DDDBC9666120C1D4E10",
		"referenceKey":"TWOFILES",
		"name":"TWOFILES"
	  },
	  "priority":"PRIORITY_0",
	  "xml":"<TWOFILES><TITLE><![CDATA[Second file is a image]]></TITLE><FILE1 SIZE='40'>1.txt</FILE1><FILE2 SIZE='1453'>1.png</FILE2></TWOFILES>"	
    },
	"contentModificationQualifier":{	
	  "ipAddress":"",
      "publish":true,
      "bypassWorkflowAndPublish":true
    }     
}

The "views" array needs to contain at least one ViewKeyBO item, this information can be retrieved using the following REST endpoint:
	
	GET /views?mode=KEY  https://docs.oracle.com/cloud/latest/servicecs_gs/CXSKA/op-km-api-v1-views-get.html
	
A single "contentType" needs to be assigned a ContentKeyBO item, this information can be retrieved using the following REST endpoint:

	GET /contentTypes?mode=KEY  https://docs.oracle.com/cloud/latest/servicecs_gs/CXSKA/op-km-api-v1-contentTypes-get.html

Notes on the "xml" element:
	
	The "TWOFILES" Content Type specifies the title, file1 and file2 attributes where file1 and file2 attributes are of 'File' type. Since the Content Type can accept
	two files, the filesToUpload field can contain a string list of two files, "1.txt" and "1.png" in this example.  Please note that filename of each of these
	files that are being attached needs to be included as part of the "xml" node as follows:
	
	"xml":"<TWOFILES><TITLE><![CDATA[Second file is a image]]></TITLE><FILE1 SIZE='40'>1.txt</FILE1><FILE2 SIZE='1453'>1.png</FILE2></TWOFILES>"
		
	The required schema attributes that must be included as the value to the "xml" field depends entirely on how the Content Type was created, so the above sample
	will only work for the "TWOFILES" Content Type.  Different Content Types will require a customized version of the xml value.
	 
