package us.wa.whatcom.co.wenet.client.browser;
import us.wa.whatcom.co.wenet.client.toolkit.*;

import javax.swing.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;
import java.lang.*;

import javax.xml.xpath.*;
import javax.xml.parsers.DocumentBuilder; 
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.parsers.FactoryConfigurationError;  
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;  
import org.xml.sax.SAXParseException;
import org.w3c.dom.*;

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
import java.security.spec.PKCS8EncodedKeySpec;

public class WENETBrowserServlet extends HttpServlet implements WENETListener
{
public ArrayList supportedImplementations;
public HashMap namespaceMap;

public ServicePointListener servicePointListener;
public WENETJavaToolkit spPollingToolkit;
public ArrayList spPollingList;
public ArrayList spErrors;
public HashMap servicePointsMap;
public javax.swing.Timer servicePointPoller;
private String strWebInf;

	//*****************************************************************************************

	public String fullPath(String strFile) {
		
		return new String();
	}

	public void init() {

		// Load up the WENET Browser public/private key pairs 		
try {		ServletContext sc = getServletContext();

		// build the path to the WEB-INF for later use
		strWebInf = sc.getRealPath("/WEB-INF/") + File.separator;
			
		// Create the mapping for the browser cores    		
		sc.setAttribute("BrowserCoreMap", new HashMap());
		
		// Load up the browser implementation data
		supportedImplementations = new ArrayList();
		namespaceMap = new HashMap();
		BrowserImplementationsReader bir = new BrowserImplementationsReader(this);
		bir.readXMLFile(sc.getRealPath("browserImplementations.xml"));
		
		// Load an instance of the WJT that the servlet will use to poll the available service points		
		String configXML = "<Config>\n";
		configXML += "<TotalExecutionThreads>1</TotalExecutionThreads>\n";
		configXML += "<ThreadDelayMillis>250</ThreadDelayMillis>\n";
		configXML += "<TimeoutSeconds> 90 </TimeoutSeconds>\n";
		configXML += "<ConnectionTimeout> 90 </ConnectionTimeout>\n";
		configXML += "<ResultsDocTimeout> 90 </ResultsDocTimeout>\n";
		configXML += "<InstanceDocTimeout> 90 </InstanceDocTimeout>\n";		
		configXML += "<PublicTruststoreFile>" + strWebInf + getInitParameter("WENETPublicCertificatesStore") + "</PublicTruststoreFile>\n";				
		configXML += "<KeyManagerType>SunX509</KeyManagerType>\n";
		configXML += "<KeyManagerProvider>SunJSSE</KeyManagerProvider>\n";
		configXML += "<TrustManagementAlgorithm> SunX509 </TrustManagementAlgorithm>\n";
		configXML += "<SSLProtocolName> TLS </SSLProtocolName>\n";
		configXML += "<SSLProtocolProvider> </SSLProtocolProvider>\n";
		configXML += "</Config>";		
		spPollingToolkit = new WENETJavaToolkit();
		spPollingToolkit.initialize(configXML);
		spPollingToolkit.exeManager.addServiceDocListener(this);
		
		// Load and interpret the list of service points the servlet is monitoring
		String spListFile = strWebInf + getInitParameter("ServicePointListXMLFile");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
   		Document document = builder.parse( new File(spListFile) );
   		XPath xpath = XPathFactory.newInstance().newXPath(); 
		NodeList nl = (NodeList) xpath.evaluate("ServicePointList/ServicePointURL", document, XPathConstants.NODESET); 
	
		spErrors = new ArrayList();
		spPollingList = new ArrayList();
		servicePointsMap = new HashMap();
		String strSPurl = new String();
		for(int i=0; i<nl.getLength(); i++) {
			strSPurl = nl.item(i).getTextContent();
			spPollingList.add(strSPurl);
			}

		// Start the polling process that keeps track of the service points
		int pMinutes = 5;
		String periodMinutes = getInitParameter("ServiceDocumentPollerPeriodMinutes");		
		if(periodMinutes != null && periodMinutes.trim().length() > 0) { pMinutes = (new Integer(periodMinutes)).intValue(); }
		servicePointPoller = new javax.swing.Timer((60000 * pMinutes), new ServicePointListener());
		servicePointPoller.setInitialDelay(0);
		servicePointPoller.start();
		
    		} catch(Exception exc) {
    			exc.printStackTrace(System.out);
    			}
		}

	//*****************************************************************************************

	public WENETBrowserCore createUserCore(String implName, String userName, String path) throws Exception {

		String publicTruststoreFile = strWebInf + getInitParameter("WENETPublicCertificatesStore");
		String browserCAFile = strWebInf + getInitParameter("BrowserCAFile");
		String browserCAType = getInitParameter("BrowserCAType");
		String browserCAProvider = getInitParameter("BrowserCAProvider");
		String browserCAPassword = getInitParameter("BrowserCAPassword");
		String browserCACertificateAlias = getInitParameter("BrowserCACertificateAlias");

		String browserCAPrivateKeyAlias = getInitParameter("BrowserCAPrivateKeyAlias");
		String browserCAPrivateKeyPassword = getInitParameter("BrowserCAPrivateKeyPassword");
		String browserPrivateKeyFile = strWebInf + getInitParameter("BrowserCAPrivateKeyFile");
		String browserPrivateKeyType = getInitParameter("BrowserCAPrivateKeyType");				
		String browserPrivateKeyProvider = getInitParameter("BrowserCAPrivateKeyProvider");				
	
		String saveProxyCertsDirectory = strWebInf + getInitParameter("SaveProxyCertificatesDirectory");
		String saveProxyCertsPassword = getInitParameter("SaveProxyCertificatesPassword");
		
		//--------------------------------------------------------------------------------
		// Load up the browser CA information from the file

		KeyStore ks = null;
		FileInputStream fis = new FileInputStream(browserCAFile);

		if(browserCAProvider != null)
    			ks = KeyStore.getInstance(browserCAType, browserCAProvider);
    		else
    			ks = KeyStore.getInstance(browserCAType);

		ks.load(fis, browserCAPassword.toCharArray());
		fis.close();

		KeyStore ks2 = null;
		fis = new FileInputStream(browserPrivateKeyFile);

		if(browserCAProvider != null)
    			ks2 = KeyStore.getInstance(browserPrivateKeyType, browserPrivateKeyProvider);
    		else
    			ks2 = KeyStore.getInstance(browserPrivateKeyType);

		ks2.load(fis, browserCAPrivateKeyPassword.toCharArray());
		fis.close();

		// Extract the certificate/public key and the private key
		Certificate certs[] = ks.getCertificateChain(browserCACertificateAlias);
		KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry)ks2.getEntry(browserCAPrivateKeyAlias, new KeyStore.PasswordProtection(browserCAPrivateKeyPassword.toCharArray()));
		PrivateKey myPrivateKey = pkEntry.getPrivateKey();

		// Create the configuration block that will get passed into the WENETJavaToolkit			
		String configXML = "<Config>\n";
		configXML += "<TotalExecutionThreads>4</TotalExecutionThreads>\n";
		configXML += "<ThreadDelayMillis>250</ThreadDelayMillis>\n";
		configXML += "<TimeoutSeconds> 90 </TimeoutSeconds>\n";
		configXML += "<ConnectionTimeout> 90 </ConnectionTimeout>\n";
		configXML += "<ResultsDocTimeout> 90 </ResultsDocTimeout>\n";
		configXML += "<InstanceDocTimeout> 90 </InstanceDocTimeout>\n";
		configXML += "<LOGFILENAME>WJT-Browser.log</LOGFILENAME>\n";

		configXML += "<PublicTruststoreFile>" + publicTruststoreFile + "</PublicTruststoreFile>\n";
		
//				configXML += "<CertificateFile>" + publicCertFile + "</CertificateFile>\n";
//				configXML += "<CertificateType>" + publicCertType + "</CertificateType>\n";
//				configXML += "<CertificatePassword>" + publicCertPassword + "</CertificatePassword>\n";
//				configXML += "<CertificateProvider>" + publicCertProvider + "</CertificateProvider>\n";
//				configXML += "<KeyStoreFile>" + privateKeyFile + "</KeyStoreFile>\n";
//				configXML += "<KeystoreType>" + privateKeyType + "</KeystoreType>\n";
//				configXML += "<KeystoreProvider>" + privateKeyProvider + "</KeystoreProvider>\n";
//				configXML += "<KeystorePassword>" + ksPassword + "</KeystorePassword>\n";

		configXML += "<KeyManagerType>SunX509</KeyManagerType>\n";
		configXML += "<KeyManagerProvider>SunJSSE</KeyManagerProvider>\n";
		configXML += "<TrustManagementAlgorithm> SunX509 </TrustManagementAlgorithm>\n";
		configXML += "<SSLProtocolName> TLS </SSLProtocolName>\n";
		configXML += "<SSLProtocolProvider> </SSLProtocolProvider>\n";
		configXML += "</Config>";

		return (new WENETBrowserCore(configXML, path, userName, implName, namespaceMap, 
					     certs[0].getPublicKey(), myPrivateKey, certs,
					     saveProxyCertsDirectory, saveProxyCertsPassword));
		}

	//*****************************************************************************************

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		doPost(request, response);
		}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {

	    	response.setHeader("Pragma", "no-cache");
    		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);

		// Try and load up the users browser core if its allready there
		WENETBrowserCore userCore = null;
		ServletContext sc = getServletContext();
		String userName = ""; // "gfrommer@co.whatcom.wa.us"; //request.getRemoteUser();

		HttpSession ses = request.getSession(false);
		if(ses != null) { userName = (String)ses.getAttribute("UserName"); }
		
		HashMap coreMap = (HashMap)sc.getAttribute("BrowserCoreMap");
	
		//---------------------------------------------------------------------------------
			
		String act = request.getParameter("act");
		if(act == null || act.trim().length() == 0) { return; }
				
		if(userName != null && userName.trim().length() > 0)
			System.out.println("[" + (new java.util.Date()).toString() + "] User: " + userName + " - Action: " + act);
		else
			System.out.println("[" + (new java.util.Date()).toString() + "] Action: " + act);
		
		if(act.equalsIgnoreCase("Login") == true) {
			String implName = request.getParameter("impl");
			if (request.getRemoteUser() != null) {
				userName = request.getRemoteUser();
			} else {
				userName = request.getParameter("uname");
			}

		try {	String path = strWebInf + getInitParameter("UserSessionDataPath");		
			if(coreMap.get(userName) != null) {
				userCore = (WENETBrowserCore)coreMap.get(userName);

				// See if the core has expired, and if so create an new core
				int expirationMins = -1;
				String expirationMinutes = getInitParameter("UserSessionExpirationMinutes");
				if(expirationMinutes != null && expirationMinutes.trim().length() > 0) { expirationMins = (new Integer(expirationMinutes)).intValue(); }
								
				long currentTime = System.currentTimeMillis();
				if(expirationMins > -1 && (currentTime - userCore.loginTime) >= (expirationMins*60000)) {
					coreMap.put(userName, null);
					doPost(request, response);
					return;
					}
				

				ses = request.getSession(true);
				ses.setAttribute("UserName", userName);
				}
			else	
				{
				
				userCore = createUserCore(implName, userName, path);
				ses = request.getSession(true);
				ses.setAttribute("UserName", userName);
				}

			//---------------------------------------------------------------------
			// Find the browser implementation the user requested and set the core
						
			for(int i=0; i<supportedImplementations.size(); i++) {
				if(supportedImplementations.get(i).toString().equalsIgnoreCase(implName) == true) {
					userCore.theImpl = (BrowserImplementation)supportedImplementations.get(i);					
					break;
					}
				}


			coreMap.put(userName, userCore);
			sc.setAttribute("BrowserCoreMap", coreMap);
			
			// Prepare the usercore object, restore any previously saved session elements
			userCore.RestoreSession(); 

			// Load all of the available service documents into the user core
			userCore.browserErrors.addAll(spErrors);
			Object keys[] = servicePointsMap.keySet().toArray();
			for(int i=0; i<keys.length; i++) {
				ServiceDocument sd = (ServiceDocument)servicePointsMap.get(keys[i]);
				userCore.servicePoints.put(sd.serviceURI, sd);
				userCore.browserStatus.add("Browser initialized service point: " + sd.organizationURI + " " + sd.serviceURI);					
				}


			} catch(Exception exc) { exc.printStackTrace(System.out); }
									
			// Send the users browser to the application subdirectory			
			response.sendRedirect("../" + userCore.theImpl.appDirectory);
			return;
			}
			
		if(act.equalsIgnoreCase("Logout") == true) {
			coreMap.put(userName, null);
			return;
			}						
		
		
		//*************************************************************************************************
		// Every servlet request below requires the user to be logged in and have a valid WENETBrowserCore
		
		userCore = (WENETBrowserCore)coreMap.get(userName);
		if(userCore == null) { 
			response.sendRedirect("index.html");
			return;
			}

		// See if the core has expired, and if so send the user to the login page
		int expirationMins = -1;
		String expirationMinutes = getInitParameter("UserSessionExpirationMinutes");
		if(expirationMinutes != null && expirationMinutes.trim().length() > 0) { expirationMins = (new Integer(expirationMinutes)).intValue(); }
				
		if(userCore.lastAccess > -1 && expirationMins > -1) {
			long currentTime = System.currentTimeMillis();
			if((currentTime - userCore.lastAccess) >= (expirationMins*60000)) {
				coreMap.put(userName, null);
				response.sendRedirect("index.html");
				return;
				}
			}
				
		// Update the cores last access time, 
		userCore.lastAccess = System.currentTimeMillis();
				
		if(act.equalsIgnoreCase("ReloadImplementations") == true) {

			supportedImplementations.clear();
			namespaceMap.clear();
			BrowserImplementationsReader bir = new BrowserImplementationsReader(this);
		try {	bir.readXMLFile(sc.getRealPath("browserImplementations.xml")); }
			catch(Exception exc) { exc.printStackTrace(System.out); }
			return;
			}
			
		if(act.equalsIgnoreCase("SetDisplayTheme") == true) {
			String theTheme = request.getParameter("theme");			
			userCore.selectedDisplayScheme = theTheme;
			coreMap.put(userName, userCore);
			return;			
			}
			
		if(act.equalsIgnoreCase("GetInstance") == true) {			
			String theURL = request.getQueryString();
			int ind = theURL.indexOf("&URL=");
			if(ind > -1) { theURL = theURL.substring(ind+5); }
			
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
		try {	out.print( userCore.GetInstance(theURL)); 
			} catch(Exception exc) { exc.printStackTrace(System.out); }
		
			out.close();
			return;
			}
				
		if(act.equalsIgnoreCase("AddQuery") == true) {		
			String qryName = request.getParameter("qname");
			String timeout = request.getParameter("timeout");
			String xpath = request.getParameter("xpath");
			String namespaces = request.getParameter("nspaces");
			String targets[] = request.getParameterValues("target");
			
			int tout = 0;
			if(timeout != null) { tout = (new Integer(timeout)).intValue(); }

			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.print( userCore.AddQuery(xpath, namespaces, qryName, targets, tout) );
			out.close();

			return;
			}
			
		if(act.equalsIgnoreCase("QueryStatus") == true) {
			String qryName = request.getParameter("qname");
			
			PrintWriter out = response.getWriter();			
			ArrayList resultList = (ArrayList)userCore.resultsMap.get(qryName);
			if(resultList == null) {
				out.print("<output><ServicePoints>0</ServicePoints><Results>0</Results></output>");
				out.close();
			} else {				
				int totalResults = 0;
				for(int i=0; i<resultList.size(); i++) {
					WENETBrowserCore.ResultsMapping rm = (WENETBrowserCore.ResultsMapping)resultList.get(i);
					totalResults += rm.totalResults;
				}				
				out.print("<output><ServicePoints>" + resultList.size() + "</ServicePoints><Results>" + totalResults + "</Results></output>");
				out.close();
			}
			return;				
			}
			
		if(act.equalsIgnoreCase("RemoveQuery") == true) {
			String qryName = request.getParameter("qname");			
			userCore.RemoveQuery(qryName);
			return;
			}
		
		if(act.equalsIgnoreCase("GetSession") == true) {
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.print( userCore.GetSession() );
			out.close();
			return;
			}
			
		if(act.equalsIgnoreCase("AddWorkspaceResult") == true) {
			String target = request.getParameter("target");
			String instanceURL = request.getParameter("URL");
			String resultName = request.getParameter("name");
			
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.print( userCore.AddResultToWorkspace(target, instanceURL, resultName) );
			out.close();
			
		try {	userCore.SaveSession();	}
			catch(Exception exc) { 
				exc.printStackTrace(System.out);
				userCore.browserErrors.add("Error saving session file");
				}
			return;
			}
			
		if(act.equalsIgnoreCase("RemoveWorkspaceResult") == true) {
			String resultName = request.getParameter("name");

			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.print( userCore.RemoveResultFromWorkspace(resultName) );
			out.close();
			
		try {	userCore.SaveSession();	}
			catch(Exception exc) { 
				exc.printStackTrace(System.out);
				userCore.browserErrors.add("Error saving session file");
				}
				
			return;		
			}
			
		if(act.equalsIgnoreCase("AddSavedQuery") == true) {
			String qryName = request.getParameter("qname");
			String xpath = request.getParameter("xpath");
			String targets[] = request.getParameterValues("target");
			
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.print( userCore.AddSavedQuery(xpath, qryName, targets) );
			out.close();

		try {	userCore.SaveSession();	}
			catch(Exception exc) { 
				exc.printStackTrace(System.out);
				userCore.browserErrors.add("Error saving session file");				
				}
				
			return;		
			}			
			
		if(act.equalsIgnoreCase("RemoveSavedQuery") == true) {
			String qryName = request.getParameter("qname");
			
			response.setContentType("text/xml");
			PrintWriter out = response.getWriter();
			out.print( userCore.RemoveSavedQuery(qryName, null) );
			out.close();

		try {	userCore.SaveSession();	}
			catch(Exception exc) { 
				exc.printStackTrace(System.out);
				userCore.browserErrors.add("Error saving session file");
				}
				
			return;		
			}
			
		}
		
	//*****************************************************************************************
		
	public class ServicePointListener implements ActionListener {
		public ServicePointListener() { }
		public void actionPerformed(ActionEvent e) {

			spErrors.clear();
			for(int i=0; i<spPollingList.size(); i++) {
				String spAddy = (String)spPollingList.get(i);
				spPollingToolkit.exeManager.discover(spAddy, "INIT-" + spAddy);
				}
			}
		}			
	
	public void process(Object value, String returnID, int listenerType) {

		if(listenerType == ExecutionManager.SERVICE_DOCUMENT_LISTENERS) {	
			ServiceDocument sd = (ServiceDocument)value;

			// If there was an error getting this SD, then add the message to the global error list
			if(sd.error != null) { 
				spErrors.add(sd.error.errorMsg);

				if(sd.error.exc != null) {
					StringWriter trace = new StringWriter();
					sd.error.exc.printStackTrace(new PrintWriter(trace));
					spErrors.add(trace.toString());
					}

				return; 
				}

			// Add the service document into our global map
			servicePointsMap.put(sd.serviceURI, sd);
			
					
			// Update all the online users with the available service documents
			ServletContext sc = getServletContext();
			HashMap coreMap = (HashMap)sc.getAttribute("BrowserCoreMap");
			Object keys[] = coreMap.keySet().toArray();
			for(int i=0; i<keys.length; i++) {
				WENETBrowserCore wbc = (WENETBrowserCore)coreMap.get(keys[i]);				
				wbc.servicePoints.put(sd.serviceURI, sd);
				}
	
			}					

		return;
		}

		
}
