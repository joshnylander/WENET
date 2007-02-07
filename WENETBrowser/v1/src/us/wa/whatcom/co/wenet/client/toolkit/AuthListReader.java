package us.wa.whatcom.co.wenet.client.toolkit;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;

public class AuthListReader extends DefaultHandler
{
static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

public String currentText = null;
public WENETJavaToolkit theManager;
public AuthList theDocument;
public AuthList.User currentUser;
public AuthList.CertificateInfo currentCert;

	public AuthListReader(WENETJavaToolkit docMan) {
		theManager = docMan;
		theDocument = null;
		currentUser = null;
		currentCert = null;
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
		
		if(qName.endsWith("AUTHORIZINGCERTIFICATE") == true) { currentCert = theDocument.new CertificateInfo(); return; }
		if(qName.endsWith("USER") == true) { currentUser = theDocument.new User(); return; }		
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase();

		if(qName.endsWith("SERVICEURI") == true) { theDocument.serviceURI = currentText; return; }
		if(qName.endsWith("ORGURI") == true) { theDocument.orgURI = currentText; return; }
		if(qName.endsWith("URL") == true) { theDocument.theURL = currentText; return; }
		if(qName.endsWith("LASTMODIFIED") == true) { theDocument.lastModified = currentText; return; }

		if(qName.endsWith("ISSUERDN") == true) { currentCert.issuerDN = currentText; return; }
		if(qName.endsWith("SUBJECTDN") == true) { currentCert.subjectDN = currentText; return; }
		if(qName.endsWith("SERIALNUMBER") == true) { currentCert.serialNumber = currentText; return; }
		if(qName.endsWith("AUTHORIZINGCERTIFICATE") == true) { theDocument.authCertList.add(currentCert); currentCert = null; return; }

		if(qName.endsWith("RFC822NAME") == true) { currentUser.userName = currentText; return; }
		if(qName.endsWith("USERINFO") == true) { currentUser.userInfo = currentText; return; }
		if(qName.endsWith("ROLE") == true) { currentUser.roleList.add(currentText); return; }
		if(qName.endsWith("USER") == true) { theDocument.userList.add(currentUser); currentUser = null; return; }
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