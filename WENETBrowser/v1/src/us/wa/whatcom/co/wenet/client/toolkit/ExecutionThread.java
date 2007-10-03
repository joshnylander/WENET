package us.wa.whatcom.co.wenet.client.toolkit;

import java.lang.*;
import javax.net.ssl.*;
import java.net.*;
import java.io.*;
import java.util.*;

/** This class encapsulates all of the communications functionality for the toolkit. These threads are
* invoked in the WENETJavaToolkit initialize() method. The threads run at a fixed delay, set in the config XML
* under the tag <THREADDELAYMILLIS>. Each time the delay expires, the run() method is called and another attempt
* to process requests begins. The java.util.concurrent.ScheduledExecutorService provides the thread pool functionality.
* 
* @see java.util.concurrent.ScheduledExecutorService java.util.concurrent.ScheduledExecutorService
*/ 

public class ExecutionThread implements Runnable
{
public InstanceDocumentReader idr;
public ResultsDocumentReader rdr;
public ServiceDocumentReader sdr;
public AuthListReader alr;
public AuditLogReader adlr;
public ExecutionManager exeManager;

private char connectionBuffer[];
private char instanceBuffer[];
private char resultBuffer[];
private char auditLogBuffer[];
private char authListBuffer[];

	public ExecutionThread(ExecutionManager em) {
		exeManager = em;
		
		alr = new AuthListReader(exeManager.theManager);
		idr = new InstanceDocumentReader(exeManager.theManager);
		sdr = new ServiceDocumentReader(exeManager.theManager);
		rdr = new ResultsDocumentReader(exeManager.theManager);
		adlr = new AuditLogReader(exeManager.theManager);
		
		connectionBuffer = new char[exeManager.theManager.connectionBufferSize];
		instanceBuffer = new char[exeManager.theManager.instanceBufferSize];
		resultBuffer = new char[exeManager.theManager.resultBufferSize];
		authListBuffer = new char[exeManager.theManager.authListBufferSize];
		auditLogBuffer = new char[exeManager.theManager.auditLogBufferSize];
		}
	
	/** This method continously scans the buffers in the ExecutionManager looking for events to consume. 
	 * @see us.wa.whatcom.co.wenet.client.toolkit.ExecutionManager ExecutionManager
	 */
	public void run() {

		processQuery(exeManager.consumeQuery());
		processResults(exeManager.consumeResults());
		processDiscovery(exeManager.consumeDiscovery());
		processAuthlist(exeManager.consumeAuthlist());
		processAuditLog(exeManager.consumeAuditLog());
		
		}

	//*****************************************************************************************
	/** This method is called from the run() method in response to an available QueryEvent object. This method
	* analyzes the request, builds the appropriate URL with the supplied XPath query, calls the URL and parses
	* the results into a ResultsDocument object. If an error occurs during the processesing, a ResultsDocument
	* object is generated reguardless, but its ToolkitError object is filled in. Once the task is complete
	* the method calls the ExecutionManager.feedListeners method to pass back the results.
	*/
	
	public void processQuery(Object o) {
		if(o == null) { return; }
		QueryEvent qe = (QueryEvent)o;
		
		// Try and find the query method required by all service points
		ServiceDocument.ServiceMethod queryMethod;
		queryMethod = qe.queryService.findMethod("Query", null);

		// If the query method cant be found then return an error
		if(queryMethod == null) { 
			ResultsDocument rd = new ResultsDocument();
			rd.error = new ToolkitError();
			rd.error.errorMsg = "[ProcessQuery] Can not find QUERY method from service: " + qe.queryService.serviceURI;
			exeManager.feedListeners(rd, qe.returnID, ExecutionManager.QUERY_LISTENERS);
			return; 
			}	


		String queryString2 = new String(queryMethod.methodURL);
		String queryString = new String(queryMethod.methodURL);

	try {	queryString2 = queryString2.replaceAll("!xpath!", qe.xPathQuery);
		queryString2 = queryString2.replaceAll("!namespaces!", qe.xPathNamespaces);
		queryString2 = queryString2.replaceAll("!xpath-esc!", qe.xPathQuery);
		queryString2 = queryString2.replaceAll("!namespaces-esc!", qe.xPathQuery);
		System.out.println(queryString2);

		queryString = queryString.replaceAll("!xpath!", URLEncoder.encode(qe.xPathQuery, "UTF-8"));
		queryString = queryString.replaceAll("!namespaces!", URLEncoder.encode(qe.xPathNamespaces, "UTF-8"));
		queryString = queryString.replaceAll("!xpath-esc!", URLEncoder.encode(qe.xPathQuery, "UTF-8"));
		queryString = queryString.replaceAll("!namespaces-esc!", URLEncoder.encode(qe.xPathQuery, "UTF-8"));
		
		System.out.println(queryString);
		
		} catch(Exception exc) { }
		
		// Call the service point and pass the XPath query
		String resultsText = "";
	try {	URL u = new URL(queryString);

		InputStreamReader in;
		if(exeManager.theManager.sslStatus == WENETJavaToolkit.SSL_DISABLED) {				
			HttpURLConnection uCon = (HttpURLConnection)u.openConnection();
			in = new InputStreamReader(uCon.getInputStream());
			}
		else
			{
			HttpsURLConnection uCon = (HttpsURLConnection)u.openConnection();
			uCon.setSSLSocketFactory(exeManager.theManager.sslc.getSocketFactory());
			uCon.setHostnameVerifier(new Verifier()); 						
			in = new InputStreamReader(uCon.getInputStream()); 
			}
    
		// Read in the service document from the URL
		int totalRead = 0;		
		while(totalRead > -1) { 
			totalRead = in.read(resultBuffer, 0, resultBuffer.length);
			if(totalRead > -1) { resultsText += new String(resultBuffer, 0, totalRead); }
			}
			
		in.close();	
	
//	System.out.println(resultsText);
	
		ResultsDocument rd = new ResultsDocument();
		rd.xPathQuery = qe.xPathQuery;
		rdr.theDocument = rd;
		rdr.readXMLString(resultsText); 
		rd.xmlText = resultsText;
						
		exeManager.feedListeners(rd, qe.returnID, ExecutionManager.QUERY_LISTENERS);
	
		} catch(Exception exc) {
			exc.printStackTrace(System.out);
		
			// On an error, attach the exception to the result and send it through the pipeline			
			ResultsDocument rd = new ResultsDocument();
			rd.serviceURI = qe.queryService.serviceURI;
			rd.error = new ToolkitError();
			rd.error.errorMsg = "[ProcessQuery] Communication error to URL: " + queryString;
			rd.error.exc = exc;
			exeManager.feedListeners(rd, qe.returnID, ExecutionManager.QUERY_LISTENERS);
			}
		}
		
	//*****************************************************************************************		
	/** This method is called from the run() method in response to an available ResultsEvent object. This method
	* analyzes the request, builds the appropriate URL with the supplied XPath query, calls the URL and parses
	* the results into a InstanceDocument object. If an error occurs during the processesing, an InstanceDocument
	* object is generated reguardless, but its ToolkitError object is filled in. Once the task is complete
	* the method calls the ExecutionManager.feedListeners method to pass back the results.
	*/
	
	public void processResults(Object o) {
		if(o == null) { return; }
		ResultsEvent re = (ResultsEvent)o;
		
		// Open the URL specified by the result item if its attached, or its attached to the resultEvent
		String instanceDoc = "";
		String instanceURL = re.instanceURL;
		if(instanceURL == null) { instanceURL = re.theItem.instanceURL; }
//		System.out.println(instanceURL);
	try {	URL u = new URL(instanceURL);
	
		InputStreamReader in;
		if(exeManager.theManager.sslStatus == WENETJavaToolkit.SSL_DISABLED) {				
			HttpURLConnection uCon = (HttpURLConnection)u.openConnection();
			in = new InputStreamReader(uCon.getInputStream());
			}
		else
			{
			HttpsURLConnection uCon = (HttpsURLConnection)u.openConnection();
			uCon.setSSLSocketFactory(exeManager.theManager.sslc.getSocketFactory());			
			uCon.setHostnameVerifier(new Verifier()); 						
			in = new InputStreamReader(uCon.getInputStream());
			}
			
		// Read in the instance document from the URL
		int totalRead = 0;		
		while(totalRead > -1) { 
			totalRead = in.read(instanceBuffer, 0, instanceBuffer.length);
			if(totalRead > -1) { instanceDoc += new String(instanceBuffer, 0, totalRead); }
			}
			
		in.close();		
// System.out.println(instanceDoc);
		// Parse the instance document
		idr.readXMLString(instanceDoc);
		InstanceDocument id = idr.theDocument;

		// Strip out the nougety center (xml data)
		String token = ":instanceElement>";
		int startInd = instanceDoc.indexOf(token) + token.length();
		int endInd = instanceDoc.lastIndexOf(token);
		endInd = instanceDoc.lastIndexOf("<", endInd);

		id.instanceData = instanceDoc.substring(startInd, endInd); 

		// Save the complete xml text
		id.xmlText = instanceDoc;
		
		// Extract the instance document object and pipe it to the listeners
		exeManager.feedListeners(id, re.returnID, ExecutionManager.RESULTS_LISTENERS);
		
		} catch(Exception exc) {

			// An exception was thrown, so generate an error and send it through the pipeline
			InstanceDocument id = new InstanceDocument();
			id.error = new ToolkitError();
			id.error.errorMsg = "[ProcessResults] Communication error retrieving instance from URI: " + instanceURL;
			id.error.exc = exc;
			exeManager.feedListeners(id, re.returnID, ExecutionManager.RESULTS_LISTENERS);
			}
		}

	//*****************************************************************************************
	/** This method is called from the run() method in response to an available AuthlistEvent object. */
	
	public void processAuthlist(Object o) {
		if(o == null) { return; }
		AuthlistEvent ae = (AuthlistEvent)o;

		ServiceDocument.ServiceMethod authlistMethod;
		authlistMethod = ae.theService.findMethod("Authlist", null);
		if(authlistMethod == null) { 

			AuthList al = new AuthList();
			al.error = new ToolkitError();
			al.error.errorMsg = "[ProcessAuthList] Cant find Authlist method for service " + ae.theService.serviceURI;
			exeManager.feedListeners(al, ae.returnID, ExecutionManager.AUTHLIST_LISTENERS);
			return; 
			}	
			
		String authlistText = "";
	try {	URL u = new URL(authlistMethod.methodURL);			
		HttpsURLConnection uCon = (HttpsURLConnection)u.openConnection();
		uCon.setSSLSocketFactory(exeManager.theManager.sslc.getSocketFactory());		
		uCon.setHostnameVerifier(new Verifier()); 						
		InputStreamReader in = new InputStreamReader(uCon.getInputStream());
		
		int totalRead = 0;		
		while(totalRead > -1) { 
			totalRead = in.read(authListBuffer, 0, authListBuffer.length);
			if(totalRead > -1) { authlistText += new String(authListBuffer, 0, totalRead); }
			}
			
		in.close();		

		
		AuthList al = new AuthList();
		alr.theDocument = al;
		alr.readXMLString(authlistText); 
		al.xmlText = authlistText;
				
		// It didnt throw exceptions so pass the object onto the listeners
		exeManager.feedListeners(al, ae.returnID, ExecutionManager.AUTHLIST_LISTENERS);

		} catch(Exception exc) {
		
			// An exception was thrown, so generate an error and send it through the pipeline
			AuthList al = new AuthList();
			al.error = new ToolkitError();
			al.error.errorMsg = "[ProcessAuthList] Communication error retrieving Authlist";
			al.error.exc = exc;

			exeManager.feedListeners(al, ae.returnID, ExecutionManager.AUTHLIST_LISTENERS);			
			}
		}
		
	//*****************************************************************************************
	/** This method is called from the run() method in response to an available AuditLogEvent object. */
	
	public void processAuditLog(Object o) {
		if(o == null) { return; }
		AuditLogEvent ale = (AuditLogEvent)o;

		ServiceDocument.ServiceMethod auditLogMethod;
		auditLogMethod = ale.theService.findMethod("AuditLog", null);
		if(auditLogMethod == null) { 

			AuditLog al = new AuditLog();
			al.error = new ToolkitError();
			al.error.errorMsg = "[ProcessAuditLog] Cant find AuditLog method for service " + ale.theService.serviceURI;
			exeManager.feedListeners(al, ale.returnID, ExecutionManager.AUDITLOG_LISTENERS);
			return; 
			}	
		
		String queryString = auditLogMethod.methodURL;			
	try {	queryString = queryString.replaceAll("!xpath!", URLEncoder.encode(ale.xPath, "UTF-8"));
		queryString = queryString.replaceAll("!namespaces!", URLEncoder.encode(ale.xPathNamespaces, "UTF-8"));
		queryString = queryString.replaceAll("!xpath-esc!", URLEncoder.encode(ale.xPath, "UTF-8"));
		queryString = queryString.replaceAll("!namespaces-esc!", URLEncoder.encode(ale.xPathNamespaces, "UTF-8"));
		} catch(Exception exc) { }

		
	try {	URL u = new URL(queryString);			
		HttpsURLConnection uCon = (HttpsURLConnection)u.openConnection();
		uCon.setSSLSocketFactory(exeManager.theManager.sslc.getSocketFactory());		
		uCon.setHostnameVerifier(new Verifier()); 						
		InputStreamReader in = new InputStreamReader(uCon.getInputStream());

		String auditLogText = "";
		int totalRead = 0;		
		while(totalRead > -1) { 
			totalRead = in.read(auditLogBuffer, 0, auditLogBuffer.length);
			if(totalRead > -1) { auditLogText += new String(auditLogBuffer, 0, totalRead); }
			}
			
		in.close();		

		AuditLog al = new AuditLog();
		adlr.theDocument = al;
		adlr.readXMLString(auditLogText); 
		al.xmlText = auditLogText;
				
		// It didnt throw exceptions so pass the object onto the listeners
		exeManager.feedListeners(al, ale.returnID, ExecutionManager.AUDITLOG_LISTENERS);

		} catch(Exception exc) {
		
			// An exception was thrown, so generate an error and send it through the pipeline
			AuditLog al = new AuditLog();
			al.error = new ToolkitError();
			al.error.errorMsg = "[Process AUDITLOG] Communication error retrieving audit log";
			al.error.exc = exc;

			exeManager.feedListeners(al, ale.returnID, ExecutionManager.AUDITLOG_LISTENERS);			
			}
		}

	//*****************************************************************************************
	/** This method is called from the run() method in response to an available DiscoveryEvent object. This method
	* analyzes the request, builds the appropriate URL with the supplied XPath query, calls the URL and parses
	* the results into a ServiceDocument object. If an error occurs during the processesing, a ServiceDocument
	* object is generated reguardless, but its ToolkitError object is filled in. Once the task is complete
	* the method calls the ExecutionManager.feedListeners method to pass back the results.
	*/
	
	public void processDiscovery(Object o) {
		if(o == null) { return; }
		DiscoveryEvent de = (DiscoveryEvent)o;		
		
		// Open a URL connection to the specified server and retrieve the service doc
		// Retrieveing the service document doesent need an SSL connection
		String serviceDoc = "";
	try {	URL u = new URL(de.serverURL);
	
		InputStreamReader in;
		if(de.serverURL.toUpperCase().startsWith("HTTPS") == true) {				
			HttpsURLConnection uCon = (HttpsURLConnection)u.openConnection();	
			uCon.setHostnameVerifier(new Verifier()); 						
			in = new InputStreamReader(uCon.getInputStream());
			}
		else
			{
			HttpURLConnection uCon = (HttpURLConnection)u.openConnection();
			in = new InputStreamReader(uCon.getInputStream());
			}
	


		int totalRead = 0;		
		while(totalRead > -1) { 
			totalRead = in.read(connectionBuffer, 0, connectionBuffer.length);
			if(totalRead > -1) { serviceDoc += new String(connectionBuffer, 0, totalRead); }
			}
			
		in.close();		
			
		// Try parsing the service document XML into a real object
		ServiceDocument sd = new ServiceDocument();
		sdr.theDocument = sd;
		sdr.readXMLString(serviceDoc); 
		sd.xmlText = serviceDoc;
		sd.serviceDocumentURL = de.serverURL;
		
		// It didnt throw exceptions so pass the object onto the listeners
		exeManager.feedListeners(sd, de.returnID, ExecutionManager.SERVICE_DOCUMENT_LISTENERS);

		} catch(Exception exc) {
		
			// An exception was thrown, so generate an error and send it through the pipeline
			ServiceDocument sd = new ServiceDocument();
			sd.error = new ToolkitError();
			sd.error.errorMsg = "[ProcessDiscovery] Communication error retrieving service document from URI: " + de.serverURL;
			sd.error.exc = exc;
			sd.serviceDocumentURL = de.serverURL;
			exeManager.feedListeners(sd, de.returnID, ExecutionManager.SERVICE_DOCUMENT_LISTENERS);
						
			}
		}
}