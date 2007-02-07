package us.wa.whatcom.co.wenet.client.toolkit;

import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

public class WENETJavaToolkit implements WENETListener
{
/** This object coordinates the schedualed execution of the toolkit threads. */
public ScheduledExecutorService threadPool;

/** This is the data producer object for the entire toolkit, all threads feed from this object. */
public ExecutionManager exeManager;
public int totalThreads = 1;

/** This specifies the period an ExecutionThread object will wait while polling for consumable Event objects */
public int threadDelayMillis = 100;

public static final int TOOLKIT_NOT_INITIALIZED = 1;
public static final int TOOLKIT_INITIALIZED = 2;	
public static final int TOOLKIT_PROCESSING_REQUEST = 3;
public int toolkitStatus = 0;

public static final int SSL_ENABLED = 1;
public static final int SSL_DISABLED = 2;
public int sslStatus = SSL_DISABLED;

/** The timeoutMillis is the default communication operations timeout period for all operations unless otherwise specified  */
public long timeoutMillis = 30000;

/** How many milliseconds before a connect() operation will timeout, timeoutMillis will default if serviceDocTimeoutMillis is not found */
public long serviceDocTimeoutMillis = 30000;

/** How many milliseconds before a query operation will timeout */
public long resultsDocTimeoutMillis = 30000;

/** How many milliseconds before the retrieveInstance operation will timeout */
public long instanceDocTimeoutMillis = 30000;

/** How many milliseconds before a query of the auditLog will timeout */
public long auditDocTimeoutMillis = 30000;

/** How many milliseconds before the authlist document retrieval will timeout */
public long authlistDocTimeoutMillis = 30000;

public int connectionBufferSize = 2048;
public int resultBufferSize = 2048;
public int instanceBufferSize = 2048;
public int authListBufferSize = 2048;
public int auditLogBufferSize = 2048;

public Object errorResultsDump;

/** This map is filled with service document objects when the toolkit is used in command-line mode */
public HashMap discoverMap;

/** This map is filled with result document objects when the toolkit is used in command-line mode */
public HashMap queryMap;

/** This map is filled with InstanceDocument objects when the toolkit is used in command-line mode */
public HashMap resultsMap;

/** This map is filled with Authlist objects when the toolkit is used in command-line mode */
public HashMap authlistMap;

/** This map is filled with AuditLog objects when the toolkit is used in command-line mode */
public HashMap auditLogMap;

/** This loads the certificates into the toolkit */
public CertificateFactory cf;
public SSLContext sslc;
public TrustManagerFactory tmf;
public SSLSocketFactory sslFactory;
public KeyManagerFactory keyFactory;
public KeyStore ks;

/** This file contains the X509 public certificates of the sites the toolkit will connect to */
public String publicTruststoreFile;

/** This file is the specified user PUBLIC credentials certificate */
public String certificateFile;

/** The user credential file type */
public String certificateType;

/** What cryptographic provider will interpret the certificate file */
public String certificateProvider;

/** If the public certificate is password protected */
public String certificatePassword = "";

/** This file contains the PRIVATE KEY that pairs with the users public certificate */ 
public String keystoreFile;
public String keystorePassword = "";
public String keystoreType;

/** What cryptographic provider will interpret the keystore file? */
public String keystoreProvider;

public String keymanagerType;
public String keymanagerProvider;
public String trustManagementAlgorithm;
public String sslProtocolName;
public String sslProtocolProvider;

public ConfigXMLReader cfgXMLReader;
public PrintWriter logWriter = null;

/** By turning on the batchMode flag, programmers can call the high-level query methods and they will return
 * a null value immediatly, but queue up the request until the programmer eventually calls the commit() method.
 * The commit() method places all requests into the execution engine, and waits till each request has finished.
 * In this way, a programmer can use the high-level methods to queue up batches of queries and then execute
 * a single blocking commit() method call to wait for all requests in the batch.
 */ 
public boolean batchMode = false;

/** This is the map the high-level methods use to store requests to in batch mode. It gets emptied after a call to commit() */
public HashMap batchMap;

	public WENETJavaToolkit() {
		discoverMap = new HashMap();
		queryMap = new HashMap();
		resultsMap = new HashMap();
		auditLogMap = new HashMap();
		authlistMap = new HashMap();
		batchMap = new HashMap();
		}
		
	//*****************************************************************************************
	
	/** Reads parameters from an XML string and sets up the toolkit accordingly 
	* @param xmlConfigString the xml configuration */			
	
	public void initialize(String xmlConfigString) throws Exception {		
		
	
		exeManager = new ExecutionManager(this);
		cfgXMLReader = new ConfigXMLReader(this);
		cfgXMLReader.readXMLString(xmlConfigString);

		//---------------------------------------------------------------------------------
		// Start the worker threads running
		exeManager.workThreads = Collections.synchronizedList(new ArrayList());
		threadPool = Executors.newScheduledThreadPool(totalThreads);
		for(int i=0; i<totalThreads; i++) {
			ExecutionThread et = new ExecutionThread(exeManager);
			exeManager.workThreads.add(et);
			threadPool.scheduleWithFixedDelay(et, 0, threadDelayMillis, TimeUnit.MILLISECONDS);
			}

		//---------------------------------------------------------------------------------
		// Load up the truststore containing all the PUBLIC certificates of the sites we will connect to
		System.setProperty("javax.net.ssl.trustStore", publicTruststoreFile); 

	try {	if(certificateFile != null) {
			Security.addProvider(new BouncyCastleProvider());
	    		initializeSSL();
			}
    		    		
		} catch(Exception exc) {
		
//			if(logWriter != null) 
//				exc.printStackTrace(logWriter);	
//			else
				exc.printStackTrace(System.out);
			}		
					
		
		//-----------------------------------------------------------------------------------
		
		exeManager.addServiceDocListener(this);
		exeManager.addQueryListener(this);
		exeManager.addResultListener(this);
		exeManager.addAuthlistListener(this);
		exeManager.addAuditLogListener(this);
		toolkitStatus = TOOLKIT_INITIALIZED;
		}

	//*****************************************************************************************
	/** This method initializes the secure communications by accepting the public and private
	* keys as method parameters.
	* @param publicKey The users public key loaded into a java KeyStore object
	* @param publicPassword The password that encrypts the public keys
	* @param privateKey The users private key loaded into a java KeyStore object
	*/
	
	public void initializeSSL(KeyStore privateKey, String password) throws Exception {

		if(keymanagerProvider != null)
			keyFactory = KeyManagerFactory.getInstance(keymanagerType, keymanagerProvider);
		else
			keyFactory = KeyManagerFactory.getInstance(keymanagerType);
			
       		keyFactory.init(privateKey, password.toCharArray()); 
       		
       		
       		
//       		tmf = TrustManagerFactory.getInstance(trustManagementAlgorithm);
  //		tmf.init(publicKey);
    		
    		//-------------------------------------------------------------------------------------
    		// Bind our certificate to all newly created SSL connections
    		
    		if(sslProtocolProvider != null)
    			sslc = SSLContext.getInstance(sslProtocolName, sslProtocolProvider);
    		else
    			sslc = SSLContext.getInstance(sslProtocolName);
    		
    		sslc.init(keyFactory.getKeyManagers(), null, null);
    		sslStatus = SSL_ENABLED;    		    	
		}

	//*****************************************************************************************
	/** This method initializes the secure communications by accepting the public and private
	* keys as files specified in the configuration XML
	*/
	
	public void initializeSSL() throws Exception {
    		
    		//--------------------------------------------------------------------------------
		// Load up the certificate/pubic key into the primary keyFactory    		
    		if(certificateProvider != null)
    			ks = KeyStore.getInstance(certificateType, certificateProvider);
    		else
    			ks = KeyStore.getInstance(certificateType);

		ks.load(new FileInputStream(certificateFile), certificatePassword.toCharArray());
				
		if(keymanagerProvider != null)
			keyFactory = KeyManagerFactory.getInstance(keymanagerType, keymanagerProvider);
		else
			keyFactory = KeyManagerFactory.getInstance(keymanagerType);
			
		keyFactory.init(ks, certificatePassword.toCharArray()); 
/*		
       		//-----------------------------------------------------------------------------------
       		// Now load up the certificates private key into the store
       		KeyStore signStore = null;
    		if(keystoreProvider != null)
    			signStore = KeyStore.getInstance(keystoreType, keystoreProvider);
    		else
    			signStore = KeyStore.getInstance(keystoreType);

		signStore.load(new FileInputStream(keystoreFile), keystorePassword.toCharArray());    		
    		tmf = TrustManagerFactory.getInstance(trustManagementAlgorithm);
  		tmf.init(signStore);
*/       		
    		//-------------------------------------------------------------------------------------
    		// Bind our certificate to all newly created SSL connections
    		
    		if(sslProtocolProvider != null)
    			sslc = SSLContext.getInstance(sslProtocolName, sslProtocolProvider);
    		else
    			sslc = SSLContext.getInstance(sslProtocolName);
    		
    		sslc.init(keyFactory.getKeyManagers(), null, null);
    		sslStatus = SSL_ENABLED;    		
		}
			
	//*****************************************************************************************
	/** This high-level connect method retrieves the xml from a wenet:service document and
	 * encapsulates the data into a ServiceDocument object. This method executes sychronously (blocking call)
	 * so the method wont return until the operation has completed, timed-out or throws an exception
	 * @param serviceURL the URL where the wenet:service document resides 
	 */
	 
	public ServiceDocument connect(String serviceURL) throws Exception {
		if(serviceURL == null) { throw new Exception("No URL Specified"); }
		
		// Queue up the discover action into the threading engine
		String retID = String.valueOf(System.currentTimeMillis()) + ((int)(Math.random()+Math.random()*100)) + "-TOOLKIT";		
		discoverMap.put(retID, null);		
		exeManager.discover(serviceURL, retID);
		
		// If the toolkit is in batch mode, then start it processing and return results on commit
		if(batchMode == true) {
			ArrayList al = (ArrayList)batchMap.get("DISCOVER");
			if(al == null) { al = new ArrayList(); }
			al.add(retID);
			batchMap.put("DISCOVER", al);
			return null;
			}
		
		// Put the thread in wait mode until a worker thread finishes
		ServiceDocument finalDoc = null;
		long workTime = 0;
		long timeoutValue = serviceDocTimeoutMillis;
		long startTime = System.currentTimeMillis();
		while(finalDoc == null && workTime < timeoutValue) {
		try {	Thread.sleep(500); } catch(Exception exc) { }
			finalDoc = (ServiceDocument)discoverMap.get(retID);
			workTime = System.currentTimeMillis() - startTime;
			}
		
		discoverMap.remove(retID);
	
		// Throw an exception if the program times out, or there was an error in communications
		if(finalDoc == null) { throw new Exception("[Connect] Null ServiceDocument for address " + serviceURL); }
		if(finalDoc != null && finalDoc.error != null && finalDoc.error.exc != null) { throw finalDoc.error.exc; }
		if(finalDoc != null && finalDoc.error != null) { throw new Exception(finalDoc.error.errorMsg); }

		return finalDoc;
		}

	//*****************************************************************************************
	/** This high-level method scans the cluster list of a seed service document and attempts to
	* connect to each of its entries and builds a ServiceDocument object from it, which in turn
	* gets passed into another recursion of the discover() method. This continues until no new
	* ServiceDocuments are acquired and the whole lot is passed back to the user.
	*
	* @param seed This is the initial ServiceDocument with its valuable cluster list information 
	* @param prevAcquiredDocs This HashMap contains the results from each recursion of the function, it starts empty
	*/
	
	public HashMap discover(ServiceDocument seed, HashMap prevAcquiredDocs) throws Exception {
		if(seed == null) { throw new Exception("No seed service document specified"); }
		if(prevAcquiredDocs == null) { prevAcquiredDocs = new HashMap(); }
		
		// Find out which documents we allready have and which ones we need to acquire
		ArrayList findDocList = new ArrayList();
		for(int i=0; i<seed.clusterList.size(); i++) {
			Cluster c = (Cluster)seed.clusterList.get(i);
			for(int k=0; k<c.clusterMembers.size(); k++) {
				Cluster.ClusterMember cm = (Cluster.ClusterMember)c.clusterMembers.get(k);

				if(cm.serviceURI == null || cm.serviceDescriptorURL == null) { continue; }					
				if(prevAcquiredDocs.get(cm.serviceURI) == null) {
					findDocList.add(cm.serviceDescriptorURL);
					} 				
				}			
			}			
			
		// Load up all of the requested documents into the threading engine
		ArrayList retValues = new ArrayList(findDocList.size());
		for(int i=0; i<findDocList.size(); i++) {
			String retID = String.valueOf(System.currentTimeMillis()) + ((int)(Math.random()+Math.random()*100)) + "-TOOLKIT";
			retValues.add(retID);
			discoverMap.put(retID, null);
			exeManager.discover((String)findDocList.get(i), retID);
			}
			
		// If the toolkit is in batch mode, then start it processing and return results on commit
		if(batchMode == true) {
			ArrayList al = (ArrayList)batchMap.get("DISCOVER");
			if(al == null) { al = new ArrayList(); }			
			al.addAll(retValues);
			batchMap.put("DISCOVER", al);
			return null;
			}			
		
		// Put the thread in wait mode until a worker thread finishes		
		long workTime = 0;
		long timeoutValue = serviceDocTimeoutMillis;
		long startTime = System.currentTimeMillis();
		
		while(retValues.size() > 0 && workTime < timeoutValue) {

		try { Thread.sleep(500); } catch(Exception exc) { }
			
			Object ary[] = retValues.toArray();
			for(int k=0; k<ary.length; k++) {
				String retID = (String)ary[k];
				if(discoverMap.get(retID) != null) {
					ServiceDocument sd = (ServiceDocument)discoverMap.get(retID);
					prevAcquiredDocs.put(sd.serviceURI, sd);
					retValues.remove(retID);
					
					// Now send the newly acquired document recursively back through this method,
					// if all of its clusters are allready found then it will pass through
					discover(sd, prevAcquiredDocs);
					}
				}
			
			
			workTime = System.currentTimeMillis() - startTime;
			}
				
		return prevAcquiredDocs;
		}

	//*****************************************************************************************
	/** A high-level method that requests AuditLog objects from a service point. Unless the toolkit
	* is operating in batch mode, this method blocks and wont return until a timeout or the operation has
	* completed. 
	*
	* @param sd The service document object representing the service point to request auditLog entries from
	* @param xpath An xpath requesting specific AuditLog(s). This xpath is formed from the wenet:auditLog schema.
	* @param namespaces The namespaces that the xpath parameter makes use of. See the wenet protocol for specific details.
	*/
		
	public AuditLog retrieveAuditLog(ServiceDocument sd, String xPath, String namespaces) throws Exception {
		if(sd == null || xPath == null || namespaces == null) { throw new Exception("Bad Parameters"); }
		
		// Queue up the authlist action into the threading engine
		String retID = String.valueOf(System.currentTimeMillis()) + ((int)Math.random()+Math.random()*100) + "-TOOLKIT";
		auditLogMap.put(retID, null);		
		exeManager.auditLog(sd, xPath, namespaces, retID);
		
		// If the toolkit is in batch mode, then start it processing and return results on commit
		if(batchMode == true) {
			ArrayList al = (ArrayList)batchMap.get("AUDITLOG");
			if(al == null) { al = new ArrayList(); }
			al.add(retID);
			batchMap.put("AUDITLOG", al);
			return null;
			}
			
		// Put the thread in wait mode until a worker thread finishes
		AuditLog finalDoc = null;
		long workTime = 0;
		long timeoutValue = auditDocTimeoutMillis;
		long startTime = System.currentTimeMillis();
		while(finalDoc == null && workTime < timeoutValue) {
		try {	Thread.sleep(500); } catch(Exception exc) { }
			finalDoc = (AuditLog)auditLogMap.get(retID);
			workTime = System.currentTimeMillis() - startTime;
			}
		
		auditLogMap.remove(retID);
		return finalDoc;
		}

	//*****************************************************************************************
	/** This method retrieves the authlist information from a requested service point and encapsulates the
	 * results into an AuthList object. This is a high-level method that (unless in batch mode) blocks execution 
	 * until data is retrieved or a timeout value has been reached.
	 * @param sd The service document of the point to retrieve Authlist entries from
	 */
	
	public AuthList retrieveAuthList(ServiceDocument sd) throws Exception {
		if(sd == null) { throw new Exception("No service document"); }
		
		// Queue up the authlist action into the threading engine
		String retID = String.valueOf(System.currentTimeMillis()) + ((int)Math.random()+Math.random()*100) + "-TOOLKIT";
		authlistMap.put(retID, null);		
		exeManager.authlist(sd, retID);
		
		// If the toolkit is in batch mode, then start it processing and return results on commit
		if(batchMode == true) {
			ArrayList al = (ArrayList)batchMap.get("AUTHLIST");
			if(al == null) { al = new ArrayList(); }
			al.add(retID);
			batchMap.put("AUTHLIST", al);
			return null;
			}
		
		// Put the thread in wait mode until a worker thread finishes
		AuthList finalDoc = null;
		long workTime = 0;
		long timeoutValue = authlistDocTimeoutMillis;
		long startTime = System.currentTimeMillis();
		while(finalDoc == null && workTime < timeoutValue) {
		try {	Thread.sleep(500); } catch(Exception exc) { }
			finalDoc = (AuthList)authlistMap.get(retID);
			workTime = System.currentTimeMillis() - startTime;
			}
		
		authlistMap.remove(retID);
		return finalDoc;
		}

	//*****************************************************************************************
	/** This method retrieves an intance document from a URL supplied as a parameter. Unless the toolkit
	 * is operating in batch mode, this method will block and not return execution until the operation completes
	 * or times out.
	 * @param instanceURL the URL of the document to be retrieved.
	 */
	
	public InstanceDocument retrieveInstance(String instanceURL) throws Exception {

		ResultsDocument rd = new ResultsDocument();
		ResultsDocument.ResultItem ri = rd.new ResultItem();
		ri.instanceURL = instanceURL;

		// Queue up the retrieve action into the threading engine
		String retID = String.valueOf(System.currentTimeMillis()) + ((int)Math.random()+Math.random() * 100) + "-TOOLKIT";
		resultsMap.put(retID, null);
		exeManager.results(ri, retID);
		
		// If the toolkit is in batch mode, then start it processing and return results on commit
		if(batchMode == true) {
			ArrayList al = (ArrayList)batchMap.get("INSTANCE");
			if(al == null) { al = new ArrayList(); }
			al.add(retID);
			batchMap.put("INSTANCE", al);
			return null;
			}
				
		// Put the thread in wait mode until a worker thread finishes
		InstanceDocument instanceDoc = null;
		long workTime = 0;
		long timeoutValue = instanceDocTimeoutMillis;
		long startTime = System.currentTimeMillis();
		while(instanceDoc == null && workTime < timeoutValue) {

		try {	Thread.sleep(500); } catch(Exception exc) { }
			
			instanceDoc = (InstanceDocument)resultsMap.get(retID);
			workTime = System.currentTimeMillis() - startTime;
			}
		
		resultsMap.remove(retID);
		return instanceDoc;
		}	

	//*****************************************************************************************
	/** This method accepts an XPath query, runs it and retrieves a ResultDocument object
	 * by using the softQuery method, then iterates through the list of ResultItem objects
	 * passing them to the threading engine, and collecting the resulting InstanceDocument 
	 * objects into an ArrayList returnted to the user. This is a high-level method and will block until
	 * the operation completes or times out. This method does NOT queue items for BATCH-MODE processing.
	 * 
	 * @param theQuery the XPath query the user wants run
	 * @param namespaces the xml namespaces that theQuery makes use of
	 * @param theService the service point the user wants the information queried from.
	 */
		
	public ArrayList fullQuery(String theQuery, String namespaces, ServiceDocument theService) throws Exception {
		if(theQuery == null) { return null; }
				
		ResultsDocument.ResultItem dataItem;
		ResultsDocument resultDoc = halfQuery(theQuery, namespaces, theService);
		
		ArrayList resultIDList = new ArrayList();
		for(int i=0; i<resultDoc.resultItems.size(); i++) {
			dataItem = (ResultsDocument.ResultItem)resultDoc.resultItems.get(i);
			
			// Queue up the results action into the threading engine
			String retID = String.valueOf(System.currentTimeMillis()) + ((int)Math.random()+Math.random()*100) + "-TOOLKIT";
			resultIDList.add(retID);
			resultsMap.put(retID, null);		
			exeManager.results(resultDoc, retID);						
			}		
							
		// Put the thread in wait mode until all the worker threads finish their result asking
		// or wait until the timeoutValue is passed
		ArrayList instanceList = new ArrayList();
		long workTime = 0;
		long timeoutValue = instanceDocTimeoutMillis;
		long startTime = System.currentTimeMillis();

		while(resultIDList.size() > 0 && workTime < timeoutValue) {

			try { Thread.sleep(200); } catch(Exception exc) { }
			
			// Keep scanning through the list of resultID's until we have retrieved all of them
			Object resAry[] = resultIDList.toArray();
			for(int i=0; i<resAry.length; i++) {
				String retID = (String)resAry[i];
				Object value = resultsMap.get(retID);
				if(value != null) {
					InstanceDocument id = (InstanceDocument)value;
					instanceList.add(id);
					resultsMap.remove(retID);
					resultIDList.remove(retID);
					}
				}
				
			workTime = System.currentTimeMillis() - startTime;
			}

		// Throw an exception letting the user know a timeout occured, no results get returned
		if(workTime >= timeoutValue) { errorResultsDump = instanceList; throw new Exception("[FullQuery] Timeout waiting for all instance documents from Query: " + theQuery); }			

		return instanceList;	
		}

	/** This is a convienence method that calls serveral service points with the same query.
	 * @param theQuery the XPath query to pass on
	 * @param namespaces the xml namespaces the query makes use of
	 * @param services an array of ServiceDocument objects to be queried
	 */	
	public ArrayList fullQuery(String theQuery, String namespaces, ServiceDocument services[]) throws Exception {
	
		ArrayList theList = new ArrayList();
		for(int i=0; i<services.length; i++) {
			ServiceDocument sd = services[i];
			theList.addAll(fullQuery(theQuery, namespaces, sd));
			}
			
		return theList;
		}
	/** This is a convienence method that iterates through a list of clusters passing the query to each.
	 * @param theQuery the XPath query to pass on
	 * @param namespaces the xml namespaces the query makes use of
	 * @param clusters an array of Cluster objects, each of which in turn contain a service document */	 
	public ArrayList fullQuery(String theQuery, String namespaces, Cluster clusters[]) throws Exception {
	
		ArrayList theList = new ArrayList();
		for(int i=0; i<clusters.length; i++) {
			theList.addAll(fullQuery(theQuery, namespaces, clusters[i]));
			}
			
		return theList;
		}
	
	/** This is a convienence method that accepts a single cluster and iterates through its members, passing
	 * in the supplied query. */	 
	public ArrayList fullQuery(String theQuery, String namespaces, Cluster theCluster) throws Exception {
	
		ArrayList theList = new ArrayList();
		for(int i=0; i<theCluster.clusterMembers.size(); i++) {
			Cluster.ClusterMember cm = (Cluster.ClusterMember)theCluster.clusterMembers.get(i);
			if(cm.serviceDoc != null) {
				theList.addAll(fullQuery(theQuery, namespaces, cm.serviceDoc));
				}
			}
			
		return theList;			
		}
		
	//*****************************************************************************************
	/** This method accepts an XPath query and a service descriptor then passes them into
	 * the threading engine and awaits a response. If the toolkit is NOT operating in BATCH mode
	 * this method will block until the operation is complete or has timed out. If the toolkit IS
	 * operating in batch mode, this method will return null immediatly after the user call. 
	 *
	 * @param theQuery the XPath query to pass to the service point
	 * @param namespaces the xml namespaces used by the XPath parameter
	 * @param theService the service document representing the service point that will be queried
	 */
	
	public ResultsDocument halfQuery(String theQuery, String namespaces, ServiceDocument theService) throws Exception {

		// Queue up the discover action into the threading engine
		String retID = String.valueOf(System.currentTimeMillis()) + ((int)Math.random()+Math.random()*100) + "-TOOLKIT";
		queryMap.put(retID, null);		
		exeManager.query(theQuery, namespaces, theService, retID);
		
		// If the toolkit is in batch mode, then start it processing and return results on commit
		if(batchMode == true) {
			ArrayList al = (ArrayList)batchMap.get("RESULT");
			if(al == null) { al = new ArrayList(); }
			al.add(retID);
			batchMap.put("RESULT", al);
			return null;
			}
		
		// Put the thread in wait mode until a worker thread finishes
		ResultsDocument finalDoc = null;
		long workTime = 0;
		long timeoutValue = resultsDocTimeoutMillis;
		long startTime = System.currentTimeMillis();
		while(finalDoc == null && workTime < timeoutValue) {
		try { 	Thread.sleep(500); } catch(Exception exc) { }
			finalDoc = (ResultsDocument)queryMap.get(retID);
			workTime = System.currentTimeMillis() - startTime;
			}
		
		queryMap.remove(retID);
		
		// See if the document is there, and no errors were generated
		if(finalDoc == null && workTime >= timeoutValue) { throw new Exception("[HalfQuery] Timeout waiting for result document from URI: " + theService.serviceURI); }
		if(finalDoc == null) { throw new Exception(); }
		if(finalDoc.error != null && finalDoc.error.exc != null) { throw finalDoc.error.exc; }
		if(finalDoc.error != null) { throw new Exception(finalDoc.error.errorMsg); }
		
		// A valid, non-errored ResultDocument was generated
		return finalDoc;
		}
		
	public ResultsDocument[] halfQuery(String theQuery, String namespaces, Cluster theCluster) throws Exception {

		ArrayList theList = new ArrayList();
		for(int i=0; i<theCluster.clusterMembers.size(); i++) {
			Cluster.ClusterMember cm = (Cluster.ClusterMember)theCluster.clusterMembers.get(i);
			if(cm.serviceDoc != null) {
				theList.add(halfQuery(theQuery, namespaces, cm.serviceDoc));
				}
			}
			
		return (ResultsDocument[]) theList.toArray();			
		}


	//*****************************************************************************************
	/** When operating in BATCH mode, the toolkit will return (non-blocking) from all high-level methods.
	* Instead of running the operation in those high-level methods, the toolkit queues the request in the
	* public batchMap variable. Once the user has finished queuing up requests, they call this commit() method
	* which will block and wait for all the queued operations to complete or time-out. There is a single timeout
	* value used for this method which overrides any specific values set in the config XML string.
	* @param timeoutSeconds the number of SECONDS this method will block for
	*/
	
	public HashMap commit(int timeoutSeconds) {
		if(batchMode == false || batchMap == null) { return null; }

		HashMap returnMap = new HashMap();
		ArrayList conList = (ArrayList)batchMap.get("DISCOVER");
		ArrayList resultList = (ArrayList)batchMap.get("RESULT");
		ArrayList instanceList = (ArrayList)batchMap.get("INSTANCE");
		ArrayList authList = (ArrayList)batchMap.get("AUTHLIST");
		ArrayList auditList = (ArrayList)batchMap.get("AUDITLOG");
		
		Object listAry[] = {conList, resultList, instanceList, authList, auditList};
		String idAry[] = {"DISCOVER", "RESULT", "INSTANCE", "AUTHLIST", "AUDITLOG"};
		
		long workTime = 0;
		long timeoutValue = timeoutSeconds * 1000;
		long startTime = System.currentTimeMillis();
		int listSize = 1;
		
		// Keep looping while there are still documents needing retrieval and time left to do it...
		while(	listSize > 0 && workTime < timeoutValue) {

			listSize = 0;
			try { Thread.sleep(200); } catch(Exception exc) { }
						
			for(int w=0; w<listAry.length; w++) {
				ArrayList ary = (ArrayList)listAry[w];
				if(ary == null) { continue; }
				
				Object resAry[] = ary.toArray();
				for(int i=0; i<resAry.length; i++) {
					String retID = (String)resAry[i];

					//---------------------------------------------------------------
					
					if(idAry[w].equals("DISCOVER") == true) {	
						if(discoverMap.get(retID) != null) {
						
							ServiceDocument sd = (ServiceDocument) discoverMap.get(retID);

							ArrayList al = (ArrayList)returnMap.get("DISCOVER");
							if(al == null) { al = new ArrayList(); }
							al.add(sd);
							returnMap.put("DISCOVER", al);
					
							discoverMap.remove(retID);
							ary.remove(retID);
							}
						}
						
					//---------------------------------------------------------------
					
					if(idAry[w].equals("RESULT") == true) {	
						if(queryMap.get(retID) != null) {
							ResultsDocument rd = (ResultsDocument) queryMap.get(retID);

							ArrayList al = (ArrayList)returnMap.get("RESULT");
							if(al == null) { al = new ArrayList(); }
							al.add(rd);
							returnMap.put("RESULT", al);
					
							queryMap.remove(retID);
							ary.remove(retID);
							}
						}
						
					//---------------------------------------------------------------
					
					if(idAry[w].equals("INSTANCE") == true) {	
						if(resultsMap.get(retID) != null) {
							InstanceDocument id = (InstanceDocument) resultsMap.get(retID);

							ArrayList al = (ArrayList)returnMap.get("INSTANCE");
							if(al == null) { al = new ArrayList(); }
							al.add(id);
							returnMap.put("INSTANCE", al);
					
							resultsMap.remove(retID);
							ary.remove(retID);
							}
						}					

					//---------------------------------------------------------------
					
					if(idAry[w].equals("AUTHLIST") == true) {	
						if(authlistMap.get(retID) != null) {
							AuthList atl = (AuthList) authlistMap.get(retID);

							ArrayList al = (ArrayList)returnMap.get("AUTHLIST");
							if(al == null) { al = new ArrayList(); }
							al.add(atl);
							returnMap.put("AUTHLIST", al);
					
							authlistMap.remove(retID);
							ary.remove(retID);
							}
						}
						
					//---------------------------------------------------------------
					
					if(idAry[w].equals("AUDITLOG") == true) {	
						if(auditLogMap.get(retID) != null) {
							AuditLog adl = (AuditLog) auditLogMap.get(retID);

							ArrayList al = (ArrayList)returnMap.get("AUDITLOG");
							if(al == null) { al = new ArrayList(); }
							al.add(adl);
							returnMap.put("AUDITLOG", al);
					
							auditLogMap.remove(retID);
							ary.remove(retID);
							}
						}						
					}								

				listSize += ary.size();							
				}
				
			workTime = System.currentTimeMillis() - startTime;
			}
			
		return returnMap;
		}

	//*****************************************************************************************
	/** This is the implementation of WENETListener interface. Whenever any of the high-level methods preform
	* their operations, the data is returned from the execution engine into this method via the execution thread.
	* This means this method gets called (via the work thread) while user execution is still blocked in the 
	* high-level method.
	* @param value the value being passed in by the execution engine
	* @param returnID the returnID that specifies which high-level operation the return value corresponds to
	* @param listenerType what type of result is this
	*/

	public void process(Object value, String returnID, int listenerType) {

		// Only listen for values we generated from high level methods
		if(returnID == null || returnID.endsWith("-TOOLKIT") == false) { return; }

		switch(listenerType) {
			case ExecutionManager.SERVICE_DOCUMENT_LISTENERS: {
				discoverMap.put(returnID, value);
				return;
				}
				
			case ExecutionManager.QUERY_LISTENERS: {
				queryMap.put(returnID, value);
				return;
				}
				
			case ExecutionManager.RESULTS_LISTENERS: {
				resultsMap.put(returnID, value);
				return;
				}
				
			case ExecutionManager.AUTHLIST_LISTENERS: {
				authlistMap.put(returnID, value);
				return;
				}
			
			case ExecutionManager.AUDITLOG_LISTENERS: {
				auditLogMap.put(returnID, value);
				return;
				}
			}
		}
		
	//*****************************************************************************************
	/** The JAVA entry point when the toolkit is run as an application. There are a wide variety
	* of parameters that are accepted from the command line. These parameters can direct the toolkit
	* to connect to a service point(s) and perform a number of operations. The results from these
	* operations are sent to the Standard OUT wrapped in the XML tags <output></output>.<p>
	* Command line arguments are not case sensitive.
	*/
	
	public static void main(String argv[]) {

		WENETJavaToolkit tk = new WENETJavaToolkit();
		HashMap serviceDocMap = new HashMap();
		ArrayList targetPoints = new ArrayList();
		ServiceDocument activeDoc = null;
		ResultsDocument resultsDoc = null;
		int batchModeTimeoutSeconds = 90;
		
		System.out.println("<output>");
		
		long startTime = System.currentTimeMillis();

		for(int i=0; i<argv.length; i++) {
			String s = argv[i];

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/CONFIG") == true || s.equalsIgnoreCase("-CONFIG") == true) {
				String cfgFile = argv[i+1];
				i++;
				
			try {	File cFile = new File(cfgFile);
				if(cFile.exists() == true) {

					String configFileText = new String();
					BufferedReader br = new BufferedReader(new FileReader(cFile));
					while(br.ready() == true) { configFileText += br.readLine(); }
					br.close();
					
					tk.initialize(configFileText); 
					
					if(tk.logWriter != null) { 
						tk.logWriter.println("WENET JAVA TOOLKIT - " + (new java.util.Date())); 
						tk.logWriter.println("Initializing toolkit with config file: " + cfgFile); 
						}
					}
				else
					{					
					System.exit(0);
					}
				}
				catch(Exception exc) {
					if(tk.logWriter != null)
						exc.printStackTrace(tk.logWriter);
					else
						exc.printStackTrace(System.out);					
					}

				continue;
				}

			//---------------------------------------------------------------
				
			if(s.equalsIgnoreCase("/CONFIGXML") == true || s.equalsIgnoreCase("-CONFIGXML") == true) {
				String cfgXML = argv[i+1];
				i++;
				
				try { tk.initialize(cfgXML); 

					if(tk.logWriter != null) { 
						tk.logWriter.println("WENET JAVA TOOLKIT - " + (new java.util.Date())); 
						tk.logWriter.println("Initializing toolkit with command line XML"); 
						}
					}
				
				catch(Exception exc) { 
					if(tk.logWriter != null)
						exc.printStackTrace(tk.logWriter);
					else
						exc.printStackTrace(System.out);
					}
			
				continue;
				}


			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/BATCHMODE") == true || s.equalsIgnoreCase("-BATCHMODE") == true) {
				if(tk.logWriter != null) { tk.logWriter.println("Turning ON batch mode"); }
				tk.batchMode = true;
				tk.batchMap.clear();
				continue;		
				}
				
			//---------------------------------------------------------------
			
			if(s.equalsIgnoreCase("/STANDARDMODE") == true || s.equalsIgnoreCase("-STANDARDMODE") == true) {
			
				if(tk.logWriter != null) { tk.logWriter.println("Turning OFF batch mode"); }
				tk.batchMode = false;
				tk.batchMap.clear();
				continue;
				}
				

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/COMMIT") == true || s.equalsIgnoreCase("-COMMIT") == true) {
				String timeoutSecs = argv[i+1];
				i++;

				if(tk.logWriter != null) { tk.logWriter.println("Attempting to COMMIT to the results"); }
				batchModeTimeoutSeconds = (new Integer(timeoutSecs)).intValue();
				HashMap retMap = tk.commit(batchModeTimeoutSeconds);
				
				ArrayList resAry = (ArrayList)retMap.get("RESULT");
				if(resAry != null) {
					for(int k=0; k<resAry.size(); k++) {
						ResultsDocument rd = (ResultsDocument)resAry.get(k);
						System.out.println(rd.xmlText);
						}
					}
					
				resAry = (ArrayList)retMap.get("DISCOVER");
				if(resAry != null) {
					for(int k=0; k<resAry.size(); k++) {
						ServiceDocument sd = (ServiceDocument)resAry.get(k);
						serviceDocMap.put(sd.serviceURI, sd);
						if(tk.logWriter != null) { tk.logWriter.println("Found new service point: " + sd.serviceURI); }
						}
					}
					
				resAry = (ArrayList)retMap.get("INSTANCE");
				if(resAry != null) {
					for(int k=0; k<resAry.size(); k++) {
						InstanceDocument id = (InstanceDocument)resAry.get(k);
						System.out.println(id.xmlText);						
						}
					}
					
				resAry = (ArrayList)retMap.get("AUTHLIST");
				if(resAry != null) {
					for(int k=0; k<resAry.size(); k++) {
						AuthList al = (AuthList)resAry.get(k);
						System.out.println(al.xmlText);						
						}
					}				
				
				resAry = (ArrayList)retMap.get("AUDITLOG");
				if(resAry != null) {
					for(int k=0; k<resAry.size(); k++) {
						AuditLog al = (AuditLog)resAry.get(k);
						System.out.println(al.xmlText);						
						}
					}
					
				continue;
				}

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/SEED") == true || s.equalsIgnoreCase("-SEED") == true) {
				String addy = argv[i+1];
				i++;
				
				if(tk.logWriter != null) { tk.logWriter.println("Attempting DISCOVERY with seed address: " + addy); }
					
			try {	activeDoc = tk.connect(addy); 
				if(tk.batchMode == true && activeDoc == null) { continue; }
				if(activeDoc.error != null && tk.logWriter != null) { tk.logWriter.println("Error retrieving seed service document"); }
				if(activeDoc.error != null && activeDoc.error.exc == null) { throw new Exception(activeDoc.error.errorMsg); }
				if(activeDoc.error != null && activeDoc.error.exc != null) { throw activeDoc.error.exc; }
				
				serviceDocMap.put(activeDoc.serviceURI, activeDoc);
				tk.discover(activeDoc, serviceDocMap);

				if(tk.logWriter != null) { 
					Object keyMap[] = serviceDocMap.keySet().toArray();
					for(int w=0; w<keyMap.length; w++) { tk.logWriter.println("DISCOVERY found address: " + keyMap[w]); }
					}
						
				} catch(Exception exc) {
					if(tk.logWriter != null)
						exc.printStackTrace(tk.logWriter);
					else
						exc.printStackTrace(System.out);
					}			

				continue;
				}
				
			//---------------------------------------------------------------
			
			if(s.equalsIgnoreCase("/CONNECT") == true || s.equalsIgnoreCase("-CONNECT") == true) {
				String addy = argv[i+1];
				i++;
				
				if(tk.logWriter != null) { tk.logWriter.println("Attempting to retrieve SERVICE DOCUMENT from address: " + addy); }
				
			try {	activeDoc = tk.connect(addy); 

				if(tk.batchMode == true && activeDoc == null) { continue; }
				if(activeDoc.error != null && activeDoc.error.exc != null) { throw activeDoc.error.exc; }
				if(activeDoc.error != null && activeDoc.error.exc == null) { throw new Exception(activeDoc.error.errorMsg); }

				if(tk.logWriter != null) { tk.logWriter.println("FOUND the service document!"); }				
				serviceDocMap.put(activeDoc.serviceURI, activeDoc);

				} catch(Exception exc) {
					if(tk.logWriter != null)
						exc.printStackTrace(tk.logWriter);
					else
						exc.printStackTrace(System.out);
					}			

				continue;
				}
			
			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/TARGETS") == true ||
			   s.equalsIgnoreCase("/TARGET") == true || 
			   s.equalsIgnoreCase("-TARGETS") == true ||
			   s.equalsIgnoreCase("-TARGET") == true) {
				
				if(tk.logWriter != null) { tk.logWriter.println("Assigning TARGET service points"); }
				targetPoints.clear();
				
				String[] targetAry = argv[i+1].split("\\s");
				i++;
				
			     	for(int x=0; x<targetAry.length; x++) {
			     		activeDoc = (ServiceDocument)serviceDocMap.get(targetAry[x]);
			     		
			     		if(activeDoc == null || activeDoc.error != null) { 
			     			if(tk.logWriter != null) { tk.logWriter.println("ERROR - TARGET Service Document: " + targetAry[x] + " is not found or has errors"); }
			     			}
			     		else
			     			{
			     			if(tk.logWriter != null) { tk.logWriter.println("TARGET Service Document: " + targetAry[x] + " found"); }
			     			}

			     		targetPoints.add(activeDoc);
         				}
				
				continue;
				}

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/SERVICEDOCUMENTS") == true ||
			   s.equalsIgnoreCase("/SERVICEDOCUMENT") == true ||
			   s.equalsIgnoreCase("-SERVICEDOCUMENTS") == true ||
			   s.equalsIgnoreCase("-SERVICEDOCUMENT") == true) {

				if(targetPoints.size() == 0) {
					Object ary[] = serviceDocMap.values().toArray();
					for(int g=0; g<ary.length; g++) {
						activeDoc = (ServiceDocument)ary[g];
						if(activeDoc != null) { targetPoints.add(activeDoc); }
						}
					}				

				for(int g=0; g<targetPoints.size(); g++) {
					ServiceDocument sd = (ServiceDocument)targetPoints.get(g);					
					if(tk.logWriter != null) { tk.logWriter.println("[OUTPUT] Service document xml for serviceURI: " + sd.serviceURI); }
					System.out.println(sd.xmlText);					
					}

				continue;					
				}
			
			//---------------------------------------------------------------
			if(s.equalsIgnoreCase("/AUTHLIST") == true ||
			   s.equalsIgnoreCase("/AUTHLISTS") == true ||
			   s.equalsIgnoreCase("-AUTHLIST") == true ||
			   s.equalsIgnoreCase("-AUTHLISTS") == true ) {

				if(targetPoints.size() == 0) {
					Object ary[] = serviceDocMap.values().toArray();
					for(int g=0; g<ary.length; g++) {
						activeDoc = (ServiceDocument)ary[g];
						if(activeDoc != null) { targetPoints.add(activeDoc); }
						}
					}

				if(tk.logWriter != null) { tk.logWriter.println("Attempting to retrieve AUTHLIST objects from target service points"); }
				
				AuthList al = null;
				ServiceDocument sd = null;
				for(int g=0; g<targetPoints.size(); g++) {
					sd = (ServiceDocument)targetPoints.get(g);
					
					if(tk.logWriter != null) { tk.logWriter.println("Attempting AUTHLIST from service point " + sd.serviceURI); }
					
					try { 	al = tk.retrieveAuthList(sd); 
						if(tk.batchMode == true && al == null) { continue; }
						}
					
					catch(Exception exc) { 
						if(tk.logWriter != null)
							exc.printStackTrace(tk.logWriter);
						else
							exc.printStackTrace(System.out);
						}
				
					// As long as there is an object then print it out
					if(al.error == null) { System.out.println(al.xmlText); }
					if(tk.logWriter != null && al.error == null) { tk.logWriter.println("[OUTPUT] AUTHLIST object XML"); }
					if(tk.logWriter != null && al.error != null) { 
						tk.logWriter.println("[ERROR] AUTHLIST has errors"); 
						if(al.error.exc != null) { al.error.exc.printStackTrace(tk.logWriter); }
						}
					}
					
				continue;
			   	}

			//---------------------------------------------------------------
			if(s.equalsIgnoreCase("/AUDITLOG") == true ||
			   s.equalsIgnoreCase("/AUDITLOGS") == true ||
			   s.equalsIgnoreCase("-AUDITLOG") == true ||
			   s.equalsIgnoreCase("-AUDITLOGS") == true) {

				String xPath = argv[i+1];
				String namespaces = argv[i+2];
				i+=2;
				
				if(tk.logWriter != null) { tk.logWriter.println("Attempting to retrieve AUDITLOG objects from target service points"); }
				if(tk.logWriter != null) { tk.logWriter.println("AUDITLOG XPath: " +  xPath); }
				if(tk.logWriter != null) { tk.logWriter.println("AUDITLOG Namespaces: " +  namespaces); }
				
				if(targetPoints.size() == 0) {
					Object ary[] = serviceDocMap.values().toArray();
					for(int g=0; g<ary.length; g++) {
						activeDoc = (ServiceDocument)ary[g];
						if(activeDoc != null) { targetPoints.add(activeDoc); }
						}
					}

				
				AuditLog al = null;
				ServiceDocument sd = null;
				for(int g=0; g<targetPoints.size(); g++) {
					sd = (ServiceDocument)targetPoints.get(g);			
					
					try { 	al = tk.retrieveAuditLog(sd, xPath, namespaces); 
						if(tk.batchMode == true && al == null) { continue; }
						
						}
					catch(Exception exc) { 
						if(tk.logWriter != null)
							exc.printStackTrace(tk.logWriter);
						else
							exc.printStackTrace(System.out);
						}
				
					// As long as there is an object then print it out
					if(al.error == null) { System.out.println(al.xmlText); }
					if(tk.logWriter != null && al.error == null) { tk.logWriter.println("[OUTPUT] AUDITLOG object XML"); }
					if(tk.logWriter != null && al.error != null) { 
						tk.logWriter.println("[ERROR] AUDITLOG has errors"); 
						if(al.error.exc != null) { al.error.exc.printStackTrace(tk.logWriter); }
						}
					}
					
				continue;
			   	}

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/QUERY") == true ||
			   s.equalsIgnoreCase("-QUERY") == true) {
			   
				String xPath = argv[i+1];
				String namespaces = argv[i+2];
				i+=2;
				
			try {	if(targetPoints.size() == 0) {
					Object ary[] = serviceDocMap.values().toArray();
					for(int g=0; g<ary.length; g++) {
						activeDoc = (ServiceDocument)ary[g];
						if(activeDoc != null) { targetPoints.add(activeDoc); }
						}
					}

				if(tk.logWriter != null) { 
					tk.logWriter.println("Attempting QUERY for target service points");
					tk.logWriter.println("Passing XPATH: " + xPath);
					tk.logWriter.println("Passing Namespaces: " + namespaces);
					}
					
				for(int g=0; g<targetPoints.size(); g++) {
					ServiceDocument sd = (ServiceDocument)targetPoints.get(g);

					if(sd == null && tk.logWriter != null) { tk.logWriter.println("Can not find service document to perform the query"); }
					if(sd != null) { 
						resultsDoc = tk.halfQuery(xPath, namespaces, sd); 
						if(tk.batchMode == true && resultsDoc == null) { continue; }
						}
					
					if(resultsDoc.error != null) { 
						if(tk.logWriter != null) { tk.logWriter.println("[ERROR] The results document has an error and cant be displayed"); }
						}
					else
						{
						System.out.println(resultsDoc.xmlText);
						}
					}	
														
				} catch(Exception exc) {
					if(tk.logWriter != null)
						exc.printStackTrace(tk.logWriter);
					else
						exc.printStackTrace(System.out);
					}

				continue;
				}

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/QUERYINSTANCE") == true ||
			   s.equalsIgnoreCase("-QUERYINSTANCE") == true) {
				String xPath = argv[i+1];
				String namespaces = argv[i+2];
				i+=2;
				
				if(targetPoints.size() == 0) {
					Object ary[] = serviceDocMap.values().toArray();
					for(int g=0; g<ary.length; g++) {
						activeDoc = (ServiceDocument)ary[g];
						if(activeDoc != null) { targetPoints.add(activeDoc); }
						}
					}
					
				if(tk.logWriter != null) { 
					tk.logWriter.println("Attempting QUERYINSTANCE for target service points");
					tk.logWriter.println("Passing XPATH: " + xPath);
					tk.logWriter.println("Passing Namespaces: " + namespaces);
					}					

				for(int g=0; g<targetPoints.size(); g++) {
					ServiceDocument sd = (ServiceDocument)targetPoints.get(g);
					
				try {	ArrayList results = tk.fullQuery(xPath, namespaces, sd);
					if(results != null) {

						for(int j=0; j<results.size(); j++) {
							InstanceDocument id = (InstanceDocument)results.get(j);
							System.out.println(id.xmlText);							
							}
							
						}
						
					} catch(Exception exc) {
						if(tk.logWriter != null)
							exc.printStackTrace(tk.logWriter);
						else
							exc.printStackTrace(System.out);
						}						
					}	
															
				continue;
				}

			//---------------------------------------------------------------

			if(s.equalsIgnoreCase("/INSTANCEURL") == true ||
			   s.equalsIgnoreCase("-INSTANCEURL") == true) {
				String theURL = argv[i+1];		
				i++;
				
				if(tk.logWriter != null) { 
					tk.logWriter.println("Attempting INSTANCEURL for service point URL: " + theURL);
					}					
			
			try {	InstanceDocument id = tk.retrieveInstance(theURL);				
				if(tk.batchMode == true && id == null) { continue; }
				
				if(id.error != null) { 
					if(tk.logWriter != null) {
						tk.logWriter.println(id.error);
						if(id.error.exc != null) { id.error.exc.printStackTrace(tk.logWriter); }
						}
					}
				else
					{
					System.out.println(id.xmlText);
					}
					
				} catch(Exception exc) {
					if(tk.logWriter != null)
						exc.printStackTrace(tk.logWriter);
					else
						exc.printStackTrace(System.out);
					}
						
				continue;	
				}
				
				
			}
	
		// Close out the log writer
		if(tk.logWriter != null) { tk.logWriter.println("Work Time: " + (System.currentTimeMillis() - startTime) + " milliseconds"); }
		if(tk.logWriter != null) { tk.logWriter.flush(); tk.logWriter.close(); }	

		System.out.println("</output>");
		System.exit(0);
		}		
}