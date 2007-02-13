package us.wa.whatcom.co.wenet.client.browser;
import us.wa.whatcom.co.wenet.client.toolkit.*;
import java.util.*;
import java.text.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.math.*;
import java.io.*;

import org.bouncycastle.asn1.misc.NetscapeRevocationURL;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import javax.net.ssl.*;


public class WENETBrowserCore implements WENETListener
{
public WENETJavaToolkit connection;
public HashMap servicePoints;
public HashMap queryMap;
public HashMap resultsMap;
public HashMap clusterMap;
public ArrayList workspaceList;
public ArrayList savedQueries;
public ArrayList browserErrors;
public ArrayList browserStatus;
public HashMap errorMap;

public ArrayList initialTargetList;
public boolean browserInitialized;

public BrowserImplementation theImpl;
public Certificate proxyCert;
public PrivateKey proxyPrivateKey;
public PublicKey proxyPublicKey;

public long lastAccess = -1;
public long loginTime = -1;
public String userEmailAddy;
public String sessionDataPath;
public HashMap namespaceMap;
public String selectedDisplayScheme = "Default";

	public WENETBrowserCore(String configXML, String corePath, String userEmail, String implName, 
				HashMap nsMap, PublicKey caPublicKey, PrivateKey caPrivateKey, Certificate[] certs,
				String saveCertsDirectory, String saveCertsPassword) {

		sessionDataPath = corePath;
		userEmailAddy = userEmail;	
		servicePoints = new HashMap();
		errorMap = new HashMap();
		clusterMap = new HashMap();
		resultsMap = new HashMap();
		workspaceList = new ArrayList();
		savedQueries = new ArrayList();
		queryMap = new HashMap();
		browserErrors = new ArrayList();
		browserStatus = new ArrayList();
		namespaceMap = nsMap;
		loginTime = System.currentTimeMillis();
		initialTargetList = new ArrayList();
		browserInitialized = false;
		
		// First, load up the bouncy castle provider and create our proxy certificate for this user
		Certificate newCerts[] = new Certificate[certs.length+1];
	try {	Security.addProvider(new BouncyCastleProvider());
		proxyCert = createProxyCertificate(userEmail, certs[0], caPrivateKey); 
		
		proxyCert.verify(caPublicKey);				
		newCerts[0] = proxyCert;
		for(int i=0; i<certs.length; i++) { newCerts[i+1] = certs[i]; }
	
		// If the info is available to serialize the certificates then do it!
		if((saveCertsDirectory != null && saveCertsDirectory.trim().length() > 0) &&
		   (saveCertsPassword != null && saveCertsPassword.trim().length() > 0)) {
		   
			KeyStore store = KeyStore.getInstance("PKCS12", "BC");
		        store.load(null, null);
	        	store.setKeyEntry("WENET Browser Proxy Cert", proxyPrivateKey, null, newCerts);	
			FileOutputStream fOut = new FileOutputStream(saveCertsDirectory + "\\" + userEmail + ".p12");
	        	store.store(fOut, saveCertsPassword.toCharArray());
			fOut.flush();
			fOut.close();
			}
		}
		catch(Exception exc) { exc.printStackTrace(System.out); }



		connection = new WENETJavaToolkit();
	try {	connection.initialize(configXML); 

		KeyStore privateKS = KeyStore.getInstance("PKCS12");
		privateKS.load(null, null);
		privateKS.setKeyEntry("alias1", proxyPrivateKey, "monkey".toCharArray(), newCerts);

		connection.initializeSSL(privateKS, "monkey");
		connection.exeManager.addServiceDocListener(this);
		connection.exeManager.addQueryListener(this);
		connection.exeManager.addResultListener(this);
		}
		catch(Exception exc) {
			exc.printStackTrace(System.out);
			}
		}

	//*****************************************************************************************
	
	public void RestoreSession()  {
	try {	String dataPath = sessionDataPath;
		if(dataPath.endsWith(File.separator) == false) { dataPath += File.separator; }
		
		File dir = new File(dataPath);
		if(dir.exists() == false) { browserErrors.add("Session directory not found, can not restore user session."); return; }
		
		// Find the users work directory
		String fAry[] = dir.list();
		String email = userEmailAddy.toUpperCase();
		for(int i=0; i<fAry.length; i++) {
			if(fAry[i].toUpperCase().endsWith(email) == true) {
			
				File workSessionFile = new File(dataPath + fAry[i] + "\\sessionData.xml");
				if(workSessionFile.exists() == true) {
					SessionReader sr = new SessionReader(this);
					sr.readXMLFile(workSessionFile.getAbsolutePath());
					browserStatus.add("Users session retrieved");
					}
				else
					{
					browserStatus.add("Can not find users saved session");					
					}
					
				return;
				}
			}
			
		browserErrors.add("Users session directory not found");
		return;
		} catch(Exception exc) {
			exc.printStackTrace(System.out);
			}
		}

	public void SaveSession() {
	
	try {
		String dataPath = sessionDataPath;
		if(dataPath.endsWith(File.separator) == false) { dataPath += File.separator; }
		File dir = new File(dataPath);
		if(dir.exists() == false) { browserErrors.add("Session directory not found, can not restore user session."); return; }
		
		// Find the users work directory
		String fAry[] = dir.list();
		String email = userEmailAddy.toUpperCase();
		for(int i=0; i<fAry.length; i++) {
			if(fAry[i].toUpperCase().endsWith(email) == true) {
								
				File workSessionFile = new File(dataPath + fAry[i] + File.separator + "sessionData.xml");				
				if(workSessionFile.exists() == true) { workSessionFile.delete(); }
				
				FileWriter fw = new FileWriter(workSessionFile);
				PrintWriter pw = new PrintWriter(fw);
				pw.println("<userBrowserSession>");
				pw.println("<workspace>");
				
				for(int k=0; k<workspaceList.size(); k++) {
					WorkspaceEntry we = (WorkspaceEntry)workspaceList.get(k);
					pw.println("<entry>");
					pw.println("<orgURI>" + we.orgURI + "</orgURI>");
					pw.println("<serviceURI>" + we.serviceURI + "</serviceURI>");
					pw.println("<instanceURL>" + we.instanceURL + "</instanceURL>");
					pw.println("<entryName>" + we.entryName + "</entryName>");
					pw.println("</entry>");
					}					
					
				pw.println("</workspace>");
				pw.println("<queries>");
				
				for(int k=0; k<savedQueries.size(); k++) {
					Query q = (Query)savedQueries.get(k);
					pw.println("<query>");
					
					pw.println("<services>");
						for(int p=0; p<q.servicePoints.size(); p++) {
							pw.println("<service>" + q.servicePoints.toString() + "</service>");
							}							
					pw.println("</services>");
					
					pw.println("<xPath><![CDATA[" + q.xPath + "]]></xPath>");
					pw.println("<queryName><![CDATA[" + q.queryName + "]]></queryName>");					
					pw.println("</query>");
					}		

				pw.println("</queries>");		
				pw.println("</userBrowserSession>");		
				fw.flush();
				fw.close();				
				return;
				}
			}
			
		// If the path doesent yet exist, then try and create it
		String pathName = sessionDataPath + File.separator + userEmailAddy.toUpperCase();
		File path = new File(pathName);
		if(path.exists() == false) { 
			path.mkdir();
			browserStatus.add("Users session directory: " + pathName + " not found, created.");
			SaveSession();
			}
		} catch(Exception exc) {
			exc.printStackTrace(System.out);
			}
		}
		
	//*****************************************************************************************
	
	public Certificate createProxyCertificate(String emailAddy, Certificate caCert, PrivateKey caPrivateKey) 
		throws Exception {


		PublicKey caPublicKey = caCert.getPublicKey();
		
		// Create a new keypair for our proxy certificate
		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        	proxyPublicKey = keyPair.getPublic();
	        proxyPrivateKey = keyPair.getPrivate();
	                        
	        // signers name table.
	        Hashtable                   sAttrs = new Hashtable();
	        Vector                      sOrder = new Vector();
/*
		
		sAttrs.put(X509Principal.CN, "WENET Browser App CA 1");
	        sAttrs.put(X509Principal.C, "US");
	        sAttrs.put(X509Principal.O, "Whatcom County Law and Justice Council");
	        sAttrs.put(X509Principal.L, "Whatcom County");
	        sAttrs.put(X509Principal.ST, "WA");
	        sAttrs.put(X509Principal.OU, "MIS");
	
	        sOrder.addElement(X509Principal.CN);
		sOrder.addElement(X509Principal.C);
	        sOrder.addElement(X509Principal.O);
	        sOrder.addElement(X509Principal.L);
	        sOrder.addElement(X509Principal.ST);
	        sOrder.addElement(X509Principal.OU);
*/	
	        // subjects name table.
	        Hashtable                   attrs = new Hashtable();
	        Vector                      order = new Vector();
	
		attrs.put(X509Principal.CN, emailAddy);
	        attrs.put(X509Principal.C, "US");
	        attrs.put(X509Principal.O, "Whatcom County Law and Justice Council");
		attrs.put(X509Principal.L, "Whatcom County");
	        attrs.put(X509Principal.ST, "WA");
	        attrs.put(X509Principal.OU, "MIS");
	        attrs.put(X509Principal.EmailAddress, emailAddy);
	
	        order.addElement(X509Principal.CN);
	        order.addElement(X509Principal.C);
	        order.addElement(X509Principal.O);
	        order.addElement(X509Principal.L);
	        order.addElement(X509Principal.ST);
	        order.addElement(X509Principal.OU);
	        order.addElement(X509Principal.EmailAddress);
	
	        // create the certificate - version 3
	        X509V3CertificateGenerator  v3CertGen = new X509V3CertificateGenerator();
	        v3CertGen.reset();
	
	        v3CertGen.setSerialNumber(BigInteger.valueOf(163));
//	        v3CertGen.setIssuerDN(new X509Principal(sOrder, sAttrs));
	        v3CertGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal((X509Certificate)caCert));
	        
	        v3CertGen.setNotBefore(new java.util.Date(System.currentTimeMillis() - ((1000L * 60 * 60 * 24) * 5)));	        
	        v3CertGen.setNotAfter(new java.util.Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24) ));
	        
	        v3CertGen.setSubjectDN(new X509Principal(order, attrs));
	        v3CertGen.setPublicKey(proxyPublicKey);
	        v3CertGen.setSignatureAlgorithm("SHA1WithRSAEncryption");
	
	        // add the extensions
	        v3CertGen.addExtension(
	            X509Extensions.SubjectKeyIdentifier,
	            false,
	            new SubjectKeyIdentifierStructure(proxyPublicKey));
	

	        v3CertGen.addExtension(
	            X509Extensions.AuthorityKeyIdentifier,
	            false,
	            new AuthorityKeyIdentifierStructure((X509Certificate)caCert));


		// This add the NETSCAPE CERT TYPE info	
	        v3CertGen.addExtension(
	            MiscObjectIdentifiers.netscapeCertType,
	            false,
	            new NetscapeCertType(NetscapeCertType.sslClient | NetscapeCertType.smime));

		// Add the subject alternative name as the users email address
	        v3CertGen.addExtension(
	            X509Extensions.SubjectAlternativeName,
	            false,
	            new GeneralNames(new GeneralName(GeneralName.rfc822Name, emailAddy)));

		// This adds the NETSCAPE BASE URL info
	        v3CertGen.addExtension(
	            MiscObjectIdentifiers.netscapeBaseURL,
	            false,
	            new NetscapeRevocationURL(new DERIA5String("URI:http://www.wcljc.org/certificates/wenet/applications/browser/user")));


		// This adds the NETSCAPE Revocation URL info
	        v3CertGen.addExtension(
	            MiscObjectIdentifiers.netscapeRevocationURL,
	            false,
	            new NetscapeRevocationURL(new DERIA5String("URI:http://www.wcljc.org/certificates/revoked.crl")));

		// This adds the NETSCAPE CA Revocation URL info
	        v3CertGen.addExtension(
	            MiscObjectIdentifiers.netscapeCARevocationURL,
	            false,
	            new NetscapeRevocationURL(new DERIA5String("URI:http://www.wcljc.org/certificates/revoked.crl")));

		// This adds the NETSCAPE RENEWAL URL info
	        v3CertGen.addExtension(
	            MiscObjectIdentifiers.netscapeRenewalURL,
	            false,
	            new NetscapeRevocationURL(new DERIA5String("URI:http://www.wcljc.org/certificates/wenet/applications/browser/user")));

		// This adds the NETSCAPE POLICY URL info
	        v3CertGen.addExtension(
	            MiscObjectIdentifiers.netscapeCApolicyURL,
	            false,
	            new NetscapeRevocationURL(new DERIA5String("URI:http://www.wcljc.org/certificates")));


	        X509Certificate cert = v3CertGen.generateX509Certificate(caPrivateKey);
	
	        cert.checkValidity(new java.util.Date());
	
	        cert.verify(caPublicKey);
	
	        PKCS12BagAttributeCarrier   bagAttr = (PKCS12BagAttributeCarrier)cert;
		            
	        bagAttr.setBagAttribute(
	            PKCSObjectIdentifiers.pkcs_9_at_localKeyId,
	            new SubjectKeyIdentifierStructure(proxyPublicKey));
	
	        return cert;
		}
		
	//*****************************************************************************************
	
	public void clearBrowserStatusMessages() {
		browserStatus.clear();
		}

	public void clearBrowserErrorMessages() {
		browserErrors.clear();
		}

	//*****************************************************************************************
		
	public void initialize() throws Exception {

		System.out.println("Initializing browser with " + theImpl.seedServicePoints.size() + " targets");
		browserStatus.add("Initializing browser with " + theImpl.seedServicePoints.size() + " targets");


		for(int i=0; i<theImpl.seedServicePoints.size(); i++) {
			String target = (String)theImpl.seedServicePoints.get(i);
			connection.exeManager.discover(target, "INIT-" + target);
			initialTargetList.add("INIT-" + target);
			
			System.out.println("Initializing browser with " + target);
			browserStatus.add("Initializing browser with " + target);
			}
		
		// Wait at least a minute for the browser to acquire the initial service documents
		long startTime = System.currentTimeMillis();
		long currentTime;
		while(browserInitialized == false) {
			currentTime = System.currentTimeMillis();
			if((currentTime - startTime) > 30000) { break; }
			try { wait(1000); } catch(Exception exc) { }
			}	
		}

	//*****************************************************************************************

	public void discoverClusters(ServiceDocument sd) {

		for(int i=0; i<sd.clusterList.size(); i++) {
			Cluster c = (Cluster)sd.clusterList.get(i);
			
			for(int k=0; k<c.clusterMembers.size(); k++) {
				Cluster.ClusterMember cm = (Cluster.ClusterMember)c.clusterMembers.get(k);

				// See if we allready have a copy of this service document
				boolean bFoundDoc = false;
				if(servicePoints.get(sd.serviceURI) != null) { bFoundDoc = true; }

				// Request one if its not available
				if(bFoundDoc == false) {
					connection.exeManager.discover(cm.serviceDescriptorURL, "INIT-" + cm.serviceDescriptorURL);
					}
				}
			}
		}
		
	//*****************************************************************************************

	public String Login() {		
		return GetSession();
		}

	//*****************************************************************************************
	
	public String AddQuery(String xPath, String namespaces, String queryName, String targets[], int timeoutSeconds) {
		
		lastAccess = System.currentTimeMillis();
			
		if(xPath == null || targets == null || queryName == null || queryName.length() == 0) { 
			browserErrors.add("Invalid parameters passed to AddQuery(); ");
			return GetSession(); 
			}

		if(timeoutSeconds < 30) { timeoutSeconds = 30; }

		Query qry = (Query)queryMap.get(queryName);
		if(qry != null) { 
			browserErrors.add("A query of name: " + queryName + " allready exists, please choose another name. ");
			return GetSession(); 
			}
		
		qry = new Query();
		qry.xPath = xPath;
		qry.namespaces = namespaces;
		qry.queryName = queryName;
		qry.lastRan = new java.util.Date();
		for(int i=0; i<targets.length; i++) { 
			qry.servicePoints.add(targets[i]); 
			}
						

		// Keep a record of the query's progress through the pipeline
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ssss");

		ArrayList status = qry.statusMsgs;
		status.add("Query: " + queryName + " started at " + sdf.format(new java.util.Date()));
		status.add("Xpath: " + xPath);

		//----------------------------------------------------------------------
		// Make sure we have all of the service documents for the query targets

		for(int i=0; i<targets.length; i++) {
			String target = (String)targets[i];
			
			if(clusterMap.get(target) != null) { 
				status.add("Found cluster info for target: " + target);
				continue; 
				}
			
			if(servicePoints.get(target) == null) {
			try {	ServiceDocument sd = connection.connect(target);
				servicePoints.put(sd.serviceURI, sd);
				status.add("Service document for target " + target + " was not cached, but was retrieved.");
				} catch(Exception exc) {
					exc.printStackTrace(System.out);
					
					qry.errorMsgs.add("Service point: " + target + " provides no service document.");

					// Make sure NOT to query a SP we have no service document of
					targets[i] = null;	
					}
				}
			else
				{
				status.add("Target " + target + " service document was found in the cache");
				}
			}

		//----------------------------------------------------
		// Go through the targets and try and run the query

		ServiceDocument theDoc;
		int totalQueried = 0;
		
		for(int i=0; i<targets.length; i++) {
			String target = (String)targets[i];
			if(target == null) { continue; }

			// If the target is the name of a cluster, then run it against each of the members
			if(clusterMap.get(target) != null) {
				ArrayList clusterMembers = (ArrayList)clusterMap.get(target);
				
				// Queue up the query in the threading engine
				for(int j=0; j<clusterMembers.size(); j++) {
					theDoc = (ServiceDocument)clusterMembers.get(j);
					status.add("Querying cluster member service point " + theDoc.serviceURI + " " + theDoc.organizationURI);
					connection.exeManager.query(xPath, namespaces, theDoc, queryName);
					totalQueried++;
					}
					
				continue;
				}
			
			
			// See if the target is a single service point
			theDoc = (ServiceDocument)servicePoints.get(target);
			if(theDoc != null) { 
				connection.exeManager.query(xPath, namespaces, theDoc, queryName); 
				status.add("Querying service point " + theDoc.serviceURI);
				totalQueried++;
				}
			}


		//-----------------------------------------------------------------------------------
		// Now the method waits until the execution threads in the Javatoolkit do their job.
		// While the WJT notifies this browser core through the process() method, this core
		// thread will wait or timeout
		
		ArrayList al = null;
		long startTime = System.currentTimeMillis();
		long currentTime;
		
		status.add("Entering wait loop for the work threads");
		queryMap.put(queryName, qry);
				
		while(al == null || al.size() < totalQueried ) {
			al = (ArrayList)resultsMap.get(queryName);
			currentTime = System.currentTimeMillis();
			
			if((currentTime - startTime) > (timeoutSeconds*1000)) { 
				status.add("Wait loop timed out!");
				break; 
				}

			try { wait(250); } catch(Exception exc) { }
			}
		
		// Save all of this query status info into the map
		status.add("Wait loop finished at " + sdf.format(new java.util.Date()));
		queryMap.put(queryName, qry);

		// Now build and return the session document		
		return GetSession();
		}

	//*****************************************************************************************

	public void RemoveQuery(String queryName) {
		
		lastAccess = System.currentTimeMillis();
		
		resultsMap.remove(queryName);				
		queryMap.remove(queryName);
		errorMap.remove(queryName);
		
		
		return;
		}

	//***************************************************************************************** 
	
	public String AddResultToWorkspace(String target, String instanceURL, String name) { 
		
		lastAccess = System.currentTimeMillis();
		
		if(target == null || instanceURL == null || name == null) { 
			browserErrors.add("Error adding workspace entry. Supply a target, URL, and an entry name."); 
			return GetSession(); 
			} 
		
		// Make sure the target is known 
		ServiceDocument sd = (ServiceDocument)servicePoints.get(target); 
		if(sd == null) { 
			browserErrors.add("Error adding workspace entry. Unkown target."); 
			return GetSession(); 
			} 
		
		// And the entry name is unique and valid 
		for(int k=0; k<workspaceList.size(); k++) { 
			WorkspaceEntry we = (WorkspaceEntry)workspaceList.get(k); 
			if(we.entryName.equals(name) == true) { 
				browserErrors.add("Error adding workspace entry. Entries must have unique names"); 
				return GetSession(); 
				} 
			} 
		
		// Create the new workspace entry object 
		WorkspaceEntry we = new WorkspaceEntry(); 
		we.serviceURI = sd.serviceURI; 
		we.orgURI = sd.organizationURI; 
		we.entryName = name; 
		we.instanceURL = instanceURL; 
		workspaceList.add(we); 
		
		browserStatus.add("Saved workspace entry " + name); 
		return GetSession(); 
		} 

	//***************************************************************************************** 
	
	public String RemoveResultFromWorkspace(String name) { 
		
		lastAccess = System.currentTimeMillis();
		
		if(name == null) { return GetSession(); } 
	
		for(int i=0; i<workspaceList.size(); i++) { 
			WorkspaceEntry we = (WorkspaceEntry)workspaceList.get(i); 
			if(we.entryName.equals(name) == true) { 
				workspaceList.remove(we); 
				return GetSession(); 
				} 
			} 
	
		return GetSession(); 
		} 

	//*****************************************************************************************

	public String AddSavedQuery(String xPath, String queryName, String targets[]) {
		
		lastAccess = System.currentTimeMillis();
		
		if(xPath == null) { 
			browserErrors.add("No xPath query provided for the saved query");
			return GetSession(); 
			}

		if(targets.length == 0) { 
			browserErrors.add("No targets provided for the saved query");
			return GetSession();
			}
		
		for(int i=0; i<targets.length; i++) {
			String target = targets[i];
			if(target == null || servicePoints.get(target) == null) { 
				browserErrors.add("Target " + target + " has no service document cached.");
				targets[i] = null;
				}
			}

		// If there is no query name specified, then strip off the root element from the xPath query
		if(queryName == null) { queryName = queryName.substring(0, xPath.indexOf("[")-1); }
		
		browserStatus.add("Adding saved query with name: " + queryName);

		Query q = new Query();
		q.xPath = xPath;
		q.queryName = queryName;
		
		for(int i=0; i<targets.length; i++) {
			if(targets[i] == null) { continue; }
			q.servicePoints.add(targets[i]);
			}	
			
		savedQueries.add(q);
		return GetSession();
		}

	//*****************************************************************************************
	// A query can be removed by specifying EITHER the name OR the xPath

	public String RemoveSavedQuery(String queryName, String xPath) {
		
		lastAccess = System.currentTimeMillis();
		
		if(queryName == null && xPath == null) { return GetSession(); }
		
		for(int i=0; i<savedQueries.size(); i++) {
			Query q = (Query)savedQueries.get(i);
			if(queryName != null && q.queryName.equals(queryName) == true) { 
				savedQueries.remove(q);
				return GetSession();
				}
			
			if(xPath != null && q.xPath.equals(xPath) == true) {
				savedQueries.remove(q);
				return GetSession();			
				}
			}
			
		return GetSession();
		}

		
	//*****************************************************************************************

	public String ClearSavedQueries() {
		lastAccess = System.currentTimeMillis();
		savedQueries.clear();
		return GetSession();
		}

	//*****************************************************************************************

	public String GetSession() {
		StringBuffer sessionXML = new StringBuffer();
		sessionXML.append("<?xml version=\"1.0\" ?>\n<session>\n");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy hh:mm:ss");		
	
		//------------------------------------------------------------------------------
		// Write out the browser namespace implementations
		
		sessionXML.append("<browserNamespaces>\n");
		
		if(namespaceMap != null) {
			Object ary[] = namespaceMap.keySet().toArray();
			for(int i=0; i<ary.length; i++) {
				String prefix = (String)ary[i];
				String location = (String)namespaceMap.get(prefix);
				sessionXML.append("<supportedNamespace>\n");
				sessionXML.append("<prefix>" + prefix + "</prefix>\n");
				sessionXML.append("<location>" + location + "</location>\n");
				sessionXML.append("</supportedNamespace>\n");
				}
			}
			
		sessionXML.append("</browserNamespaces>\n");

		//------------------------------------------------------------------------------
		// Write out the browser implementation info
		
		sessionXML.append("<implementation>\n");
		sessionXML.append("<implementationName>" + theImpl.implName + "</implementationName>\n");
		sessionXML.append("<largeApplicationIcon>" + theImpl.largeApplicationIcon + "</largeApplicationIcon>\n");
		sessionXML.append("<smallApplicationIcon>" + theImpl.smallApplicationIcon + "</smallApplicationIcon>\n");

		sessionXML.append("<displayScheme>\n");
		sessionXML.append("<schemeName> " + selectedDisplayScheme + "</schemeName>\n");
		HashMap schemeMap = (HashMap)theImpl.displaySchemeMap.get(selectedDisplayScheme.toUpperCase().trim());
		if(schemeMap != null) {
		
			Object cKeys[] = schemeMap.keySet().toArray();
			for(int i=0; i<cKeys.length; i++) {				
				sessionXML.append("<displayEntry>\n");
				sessionXML.append("<entryName> " + cKeys[i] + "</entryName>\n");				
				sessionXML.append("<entryValue> " + schemeMap.get(cKeys[i]) + "</entryValue>\n");
				sessionXML.append("</displayEntry>\n");
				}
			}				
		sessionXML.append("</displayScheme>\n");


		sessionXML.append("<columnAliasMappings>\n");
		Object aliasKeys[] = theImpl.columnAliasMappings.keySet().toArray();
		for(int i=0; i<aliasKeys.length; i++) {	
			sessionXML.append("<mapping>");
			sessionXML.append("<alias> " + theImpl.columnAliasMappings.get(aliasKeys[i]).toString() + "</alias> ");
			sessionXML.append("<xpath> " + aliasKeys[i].toString() + "</xpath>");
			sessionXML.append("</mapping>\n\n");			
			}
		
		sessionXML.append("</columnAliasMappings>\n");


				
		sessionXML.append("<theRoots>\n");
		
		for(int i=0; i<theImpl.supportedRoots.size(); i++) {
			BrowserRoot br = (BrowserRoot)theImpl.supportedRoots.get(i);

			sessionXML.append("<root>\n");
			sessionXML.append("<rootName>" + br.rootName + "</rootName>\n");
			sessionXML.append("<largeRootIcon>" + br.largeRootIcon + "</largeRootIcon>\n");
			sessionXML.append("<smallRootIcon>" + br.smallRootIcon + "</smallRootIcon>\n");			
			
			Object keys[] = br.presentationXSLTs.keySet().toArray();
			for(int k=0; k<keys.length; k++) {
				String key = (String)keys[k];
				String xsltFile = (String)br.presentationXSLTs.get(key);
				sessionXML.append("<presentation>\n");
				sessionXML.append("<type>" + key + "</type>\n");
				sessionXML.append("<file>" + xsltFile + "</file>\n");				
				sessionXML.append("</presentation>\n");
				}
			
			sessionXML.append("</root>\n");
			}
		
		sessionXML.append("</theRoots>\n");
		sessionXML.append("</implementation>\n");
	
		//------------------------------------------------------------------------------
		// Write out the user information
		
		sessionXML.append("<user>\n");
		sessionXML.append("<userFullName></userFullName>\n");
		sessionXML.append("<userEmail>" + userEmailAddy + "</userEmail>\n");
		sessionXML.append("<loginTime>" + sdf.format(new java.util.Date(loginTime)) + "</loginTime>\n");
		sessionXML.append("<lastLogin></lastLogin>\n");
		sessionXML.append("<lastAccess>" + sdf.format(new java.util.Date(lastAccess)) + "</lastAccess>\n");
		sessionXML.append("</user>\n");


		//------------------------------------------------------------------------------
		// Write out the status info on all the service points we've checked into
		sessionXML.append("<services>\n");
		
		Object keys[] = servicePoints.keySet().toArray();
		for(int k=0; k<keys.length; k++) {
			ServiceDocument sd = (ServiceDocument)servicePoints.get(keys[k]);
			String aliasName = (String)theImpl.aliasMap.get(sd.serviceURI);
			
			sessionXML.append("<service>\n");
			sessionXML.append("<orgURI> " + sd.organizationURI + "</orgURI>\n");
			sessionXML.append("<serviceURI> " + sd.serviceURI + "</serviceURI>\n");
			if(aliasName != null) { sessionXML.append("<alias> " + aliasName + "</alias>\n"); }
			sessionXML.append("<status> </status>\n");
			sessionXML.append("<supportedXPaths>\n");						
			sessionXML.append("</supportedXPaths>\n");
			sessionXML.append("</service>\n");
			}
		
		sessionXML.append("</services>\n");
		
		//------------------------------------------------------------------------------
		// Write out all of the query results the user has ran this session
		ResultsMapping rm;
		keys = queryMap.keySet().toArray();
		
		sessionXML.append("<results>\n");
		for(int i=0; i<keys.length; i++) {
			Query qry = (Query)queryMap.get(keys[i]);

			int colInd = qry.xPath.indexOf(":");
			int parmInd = qry.xPath.indexOf("[");
			String rootType = qry.xPath.substring(colInd+1, (parmInd-colInd)+1);
			
			sessionXML.append("<query>\n");
			sessionXML.append("<xPath><![CDATA[ " + qry.xPath + "]]></xPath>\n");
			sessionXML.append("<rootType> " + rootType + " </rootType>\n");
			sessionXML.append("<queryName><![CDATA[" + keys[i] + " ]]></queryName>\n");
			sessionXML.append("<services>\n");
			
			for(int o=0; o<qry.servicePoints.size(); o++) {
				sessionXML.append("<serviceURI>" + qry.servicePoints.get(o) + "</serviceURI>\n");
				}
			
			sessionXML.append("</services>\n");

			sessionXML.append("<queryStatus>\n");
			
			for(int o=0; o<qry.statusMsgs.size(); o++) {
				sessionXML.append("<status><![CDATA[" + qry.statusMsgs.get(o) + "]]></status>\n");
				}			
				
			sessionXML.append("</queryStatus>\n");

			sessionXML.append("<queryErrors>\n");
			
			for(int o=0; o<qry.errorMsgs.size(); o++) {
				sessionXML.append("<error><![CDATA[" + qry.errorMsgs.get(o) + "]]></error>\n");
				}
				
			sessionXML.append("</queryErrors>\n");

			ArrayList resultList = (ArrayList)resultsMap.get(keys[i]);
			if(resultList != null) { 
				for(int j=0; j<resultList.size(); j++) {
					rm = (ResultsMapping)resultList.get(j);							
					sessionXML.append(rm.resultsXML);
					}
				}
								
			sessionXML.append("</query>\n");
			}

		sessionXML.append("</results>\n");
		
		//------------------------------------------------------------------------------
		// Write out all of the persistant saved query information

		sessionXML.append("<savedQueries>\n");
		for(int i=0; i<savedQueries.size(); i++) {
			Query q = (Query)savedQueries.get(i);
			sessionXML.append("<query>\n");
			sessionXML.append("<xPath><![CDATA[ " + q.xPath + "]]></xPath>\n");
			sessionXML.append("<queryName><![CDATA[ " + q.queryName + "]]></queryName>\n");

			if(q.lastRan != null) { sessionXML.append("<lastRan> " + sdf.format(q.lastRan) + "</lastRan>\n"); }
			
			sessionXML.append("<services>\n");
			for(int k=0; k<q.servicePoints.size(); k++) {
				String target = (String)q.servicePoints.get(k);
				ServiceDocument sd = (ServiceDocument)servicePoints.get(target);
				if(sd != null) {
					sessionXML.append("<servicePoint>\n");
					sessionXML.append("<orgURI> " + sd.organizationURI + "</orgURI>\n");
					sessionXML.append("<serviceURI> " + sd.serviceURI + "</serviceURI>\n");
					sessionXML.append("</servicePoint>\n");
					}
				}

			sessionXML.append("</services>\n");
			sessionXML.append("</query>\n");
			}
					
		sessionXML.append("</savedQueries>\n");

		//------------------------------------------------------------------------------
		// Write out all of the persistant workspace information

		sessionXML.append("<workspace>\n");

		for(int i=0; i<workspaceList.size(); i++) {
			WorkspaceEntry we = (WorkspaceEntry)workspaceList.get(i);

			sessionXML.append("<workspaceEntry>\n");
			sessionXML.append("<instanceURL><![CDATA[ " + we.instanceURL + "]]></instanceURL>\n");
			sessionXML.append("<orgURI> " + we.orgURI + "</orgURI>\n");
			sessionXML.append("<serviceURI> " + we.serviceURI + "</serviceURI>\n");
			sessionXML.append("<entryName><![CDATA[" + we.entryName + "]]></entryName>\n");
			sessionXML.append("</workspaceEntry>\n");
			}

		sessionXML.append("</workspace>\n");
	
		//------------------------------------------------------------------------------			
		// Write out all of the browser status 

		sessionXML.append("<browserStatus>\n");
		for(int i=0; i<browserStatus.size(); i++) {
			String status = (String)browserStatus.get(i);
			sessionXML.append("<status><![CDATA[" + status + "]]></status>\n");
			}

		sessionXML.append("</browserStatus>\n");
			
		//------------------------------------------------------------------------------			
		// Write out all of the browser errors 

		sessionXML.append("<browserErrors>\n");
		for(int i=0; i<browserErrors.size(); i++) {
			String error = (String)browserErrors.get(i);
			sessionXML.append("<error><description><![CDATA[ " + error + "]]></description></error>\n");
			}

		sessionXML.append("</browserErrors>\n");							
		sessionXML.append("</session>");

		return sessionXML.toString();
		}

	//*****************************************************************************************
	// GetInstance()
	// Only one url we can call the toolkit directly
	
	public String GetInstance(String theURL) throws Exception {
		
		InstanceDocument id = connection.retrieveInstance(theURL);
		if(id == null) { return ""; }

		if(id.error != null) { 
			if(id.error.exc != null) { id.error.exc.printStackTrace(System.out); }
			return id.error.errorMsg; 
			}

		return id.instanceData;
		}


	//*****************************************************************************************
	// This method is driven by the WENETJavaToolkit so it will process incoming data asyncronously
	// as the various WJToolkit threads dump their data into here.
		
	public void process(Object value, String returnID, int listenerType) {

		System.out.println("ListenerType: " + listenerType + " retID: " + returnID + " time: " + System.currentTimeMillis());	
		
		switch(listenerType) {
		
			case ExecutionManager.RESULTS_LISTENERS: {			
				return;
				}
		
			//--------------------------------------------------------------------------
			
			case ExecutionManager.QUERY_LISTENERS: {
				ResultsDocument rd = (ResultsDocument)value;				

				// We dont need to save the WJT object, just the browser representation of it. Word.				
				ResultsMapping rm = new ResultsMapping();				
				rm.xPathQuery = rd.xPathQuery;
				rm.error = rd.error;
							
				StringBuffer txt = new StringBuffer("<resultList>\n");
				txt.append("<serviceURI> " + rd.serviceURI + "</serviceURI>\n");			
				
				if(rm.error != null) { 
					if(rm.error.errorMsg != null) { txt.append("<resultError><![CDATA[ " + rm.error.errorMsg + "]]></resultError>\n"); }
					if(rm.error.exc != null) { txt.append("<resultError><![CDATA[" + rm.error.exc + "]]></resultError>\n"); }
					}
				
				ResultsDocument.ResultItem currentItem;
				for(int i=0; i<rd.resultItems.size(); i++) {
					currentItem = (ResultsDocument.ResultItem)rd.resultItems.get(i);
					rm.totalResults++;
					txt.append("<result>\n");
					txt.append("<instanceURL><![CDATA[" + currentItem.instanceURL + "]]></instanceURL> \n");
		
					if(currentItem.summaryMap != null) {
						Object column[] = currentItem.summaryMap.keySet().toArray();
						for(int u=0; u<column.length; u++) {
							txt.append("<summary>\n");
							txt.append("<name><![CDATA[" + column[u] + "]]></name>\n");
							txt.append("<value><![CDATA[" + currentItem.summaryMap.get(column[u]) + "]]></value>\n");
							txt.append("</summary>\n");
							}
						}
					txt.append("<summaryXML>");
					txt.append(currentItem.summaryXML);
					txt.append("</summaryXML>");
						
					txt.append("</result>\n");
					}

				txt.append("</resultList>\n");
				rm.resultsXML = txt.toString();
								
				// Save the results into the browsers saved results map
				ArrayList al = (ArrayList)resultsMap.get(returnID);
				if(al == null) { al = new ArrayList(); }
				al.add(rm);
				resultsMap.put(returnID, al);			

				// Save the query status messages
				Query q = (Query)queryMap.get(returnID);
				q.statusMsgs.add("Found result document from " + rd.serviceURI + " with " + rd.resultItems.size() + " results");
				queryMap.put(returnID, q);
				return;
				}
		
			//--------------------------------------------------------------------------
		
			case ExecutionManager.SERVICE_DOCUMENT_LISTENERS: {
					
				//--------------------------------------------------
				// Add all of the initial connections into the list
				
				if(returnID.startsWith("INIT-") == true) {
					ServiceDocument sd = (ServiceDocument)value;
					if(sd.error != null) {
						browserErrors.add(sd.error.errorMsg);
						
						if(sd.error.exc != null) {
							StringWriter trace = new StringWriter();
							sd.error.exc.printStackTrace(new PrintWriter(trace));
							browserErrors.add(trace.toString());
							}
							
												
						// See if the browser is finished initializing the opening service documents
						initialTargetList.remove(returnID);
						if(initialTargetList.size() == 0) { browserInitialized = true; }						
						return;
						}
					
					servicePoints.put(sd.serviceURI, sd);
					browserStatus.add("Browser initialized service point: " + sd.organizationURI + " " + sd.serviceURI);

					discoverClusters(sd);
										
					// Go through each SD's list of clusters to create the global cluster list
					for(int k=0; k<sd.clusterList.size(); k++) {
						Cluster c = (Cluster)sd.clusterList.get(k);
						
						if(clusterMap.containsKey(c.clusterName) == false) {
							ArrayList al = new ArrayList();
							al.add(sd);
							clusterMap.put(c.clusterName, al);
							}
						else
							{
							ArrayList al = (ArrayList)clusterMap.get(c.clusterName);
							if(al.contains(sd) == false) { al.add(sd); }
							clusterMap.put(c.clusterName, al);
							}	
						}

					// See if the browser is finished initializing the opening service documents
					initialTargetList.remove(returnID);
					if(initialTargetList.size() == 0) { browserInitialized = true; }											
					return;
					}					
				}
			}
			
		return;
		}

	//*****************************************************************************************
	// This class serves as a data holder for the browser to dispense the results data.
	// There is no point in storing the WENETJavaToolkit ResultsDocument object. Just take the data!
	
	public class ResultsMapping {
		public int totalResults=0;
		public String resultsXML;
		public String xPathQuery;
		public String queryName;
		public ToolkitError error;
				
		public ResultsMapping() { }
		}

	//*****************************************************************************************

	public class WorkspaceEntry {
		public String orgURI;
		public String serviceURI;
		public String instanceURL;
		public String entryName;
		public java.util.Date lastAccess;
	
		public WorkspaceEntry() {
			}
		}

	//*****************************************************************************************

	public class Query {
		public ArrayList servicePoints;
		public String description;
		public String xPath;
		public String namespaces;
		public java.util.Date lastRan;
		public String queryName;
		public ArrayList statusMsgs;
		public ArrayList errorMsgs;
		
		public Query() {
			servicePoints = new ArrayList();
			statusMsgs = new ArrayList();
			errorMsgs = new ArrayList();
			}
		}
}

