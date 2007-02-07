package us.wa.whatcom.co.wenet.client.toolkit;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;

public class ConfigXMLReader extends DefaultHandler
{
public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
public String currentText = null;
public WENETJavaToolkit theManager;

	public ConfigXMLReader(WENETJavaToolkit tm) {
		theManager = tm;
		}

	public void readXMLString(String theString) throws Exception {
		StringReader sr = new StringReader(theString);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);

		SAXParser sp = spf.newSAXParser();
		sp.parse(new InputSource(sr), this); 
		}
		
	public void readXMLFile(String fileName) throws Exception {
		FileInputStream fis = new FileInputStream(fileName);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);

		SAXParser sp = spf.newSAXParser();
		sp.parse(fis, this); 
		fis.close();
		}
			
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
		throws SAXException {

		currentText = new String();
		qName = qName.toUpperCase();
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase();
		if(currentText == null || currentText.length() == 0) { return; }

		if(qName.equals("LOGFILENAME") == true) { 
			try { theManager.logWriter = new PrintWriter(currentText); }
			catch(Exception exc) { exc.printStackTrace(System.out); }
			}
		
		if(qName.equals("TIMEOUTSECONDS") == true) { 
			theManager.timeoutMillis = (new Long(currentText)).longValue() * 1000;
			theManager.serviceDocTimeoutMillis = theManager.timeoutMillis;
			theManager.resultsDocTimeoutMillis = theManager.timeoutMillis;
			theManager.instanceDocTimeoutMillis = theManager.timeoutMillis;
			theManager.authlistDocTimeoutMillis = theManager.timeoutMillis;
			theManager.auditDocTimeoutMillis = theManager.timeoutMillis;
			return;
			}
			
		if(qName.equals("CONNECTIONTIMEOUT") == true) { theManager.serviceDocTimeoutMillis = (new Long(currentText)).longValue() * 1000; return; }
		if(qName.equals("RESULTSDOCTIMEOUT") == true) { theManager.resultsDocTimeoutMillis = (new Long(currentText)).longValue() * 1000; return; }
		if(qName.equals("INSTANCEDOCTIMEOUT") == true) { theManager.instanceDocTimeoutMillis = (new Long(currentText)).longValue() * 1000; return; }
		if(qName.equals("AUDITDOCTIMEOUT") == true) { theManager.auditDocTimeoutMillis = (new Long(currentText)).longValue() * 1000; return; }
		if(qName.equals("AUTHLISTDOCTIMEOUT") == true) { theManager.authlistDocTimeoutMillis = (new Long(currentText)).longValue() * 1000; return; }

		if(qName.equals("PUBLICTRUSTSTOREFILE") == true) { theManager.publicTruststoreFile = currentText; return; }
		if(qName.equals("KEYSTOREFILE") == true) { theManager.keystoreFile = currentText; return; }				
		if(qName.equals("KEYSTOREPASSWORD") == true) { theManager.keystorePassword = currentText; return; }						
		if(qName.equals("KEYSTORETYPE") == true) { theManager.keystoreType = currentText; return; }
		if(qName.equals("KEYSTOREPROVIDER") == true) { theManager.keystoreProvider = currentText; return; }
		if(qName.equals("KEYMANAGERTYPE") == true) { theManager.keymanagerType = currentText; return; }
		if(qName.equals("KEYMANAGERPROVIDER") == true) { theManager.keymanagerProvider = currentText; return; }
		
		if(qName.equals("CERTIFICATETYPE") == true) { theManager.certificateType = currentText; return; }
		if(qName.equals("CERTIFICATEPROVIDER") == true) { theManager.certificateProvider = currentText; return; }
		if(qName.equals("CERTIFICATEFILE") == true) { theManager.certificateFile = currentText; return; }
		if(qName.equals("CERTIFICATEPASSWORD") == true) { theManager.certificatePassword = currentText; return; }
			
		if(qName.equals("TRUSTMANAGEMENTALGORITHM") == true) { theManager.trustManagementAlgorithm = currentText; return; }
		if(qName.equals("SSLPROTOCOLNAME") == true) { theManager.sslProtocolName = currentText; return; }
		if(qName.equals("SSLPROTOCOLPROVIDER") == true) { theManager.sslProtocolProvider = currentText; return; }		
		
		if(qName.equals("TOTALEXECUTIONTHREADS") == true) { theManager.totalThreads = (new Integer(currentText)).intValue(); return; }
		if(qName.equals("THREADDELAYMILLIS") == true) { theManager.threadDelayMillis = (new Integer(currentText)).intValue(); return; }
		
		if(qName.equals("CONNECTIONBUFFERSIZE") == true) { theManager.connectionBufferSize = (new Integer(currentText)).intValue(); return; }
		if(qName.equals("INSTANCEBUFFERSIZE") == true) { theManager.instanceBufferSize = (new Integer(currentText)).intValue(); return; }
		if(qName.equals("RESULTBUFFERSIZE") == true) { theManager.resultBufferSize = (new Integer(currentText)).intValue(); return; }
		if(qName.equals("AUTHLISTBUFFERSIZE") == true) { theManager.authListBufferSize = (new Integer(currentText)).intValue(); return; }
		if(qName.equals("AUDITLOGBUFFERSIZE") == true) { theManager.auditLogBufferSize = (new Integer(currentText)).intValue(); return; }		
		}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if(length == 0) { return; }
		currentText += (new String(ch, start, length)).trim();
		}

	public void error(SAXParseException err) throws SAXParseException {
		throw err;
		}

	public void warning(SAXParseException err) throws SAXParseException {
		}		
}