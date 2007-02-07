package us.wa.whatcom.co.wenet.client.toolkit;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;

public class AuditLogReader extends DefaultHandler
{
public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
public String currentText = null;
public WENETJavaToolkit theManager;
public AuditLog theDocument;
public AuditLog.LogEntry theEntry;

	public AuditLogReader(WENETJavaToolkit tm) {
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
		
		if(qName.endsWith("LOGENTRY") == true) { theEntry = theDocument.new LogEntry(); }
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase();

		if(qName.endsWith("ORGURI") == true) { theDocument.orgURI = currentText; return; }
		if(qName.endsWith("SERVICEURI") == true) { theDocument.serviceURI = currentText; return; }
		if(qName.endsWith("URL") == true && qName.endsWith("REQUESTURL") == false) { theDocument.theURL = currentText; return; }
		
		if(qName.endsWith("USERRFC822NAME") == true) { theEntry.userRFC822name = currentText; return; }
		if(qName.endsWith("USERCERTSUBJECT") == true) { theEntry.userCertSubject = currentText; return; }
//		if(qName.endsWith("WHEN") == true) { theEntry.when = (new java.util.Date(currentText); return; }
		if(qName.endsWith("REQUESTIPADDRESS") == true) { theEntry.requestIPAddress = currentText; return; }
		if(qName.endsWith("REQUESTURL") == true) { theEntry.requestURL = currentText; return; }
		if(qName.endsWith("METHODNAME") == true) { theEntry.methodName = currentText; return; }
		if(qName.endsWith("POSTDATA") == true) { theEntry.postData = currentText; return; }
		if(qName.endsWith("LOGENTRY") == true) { theDocument.logEntryList.add(theEntry); theEntry = null; return; }
		}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if(length == 0) { return; }
		currentText += (new String(ch, start, length)).trim();
		}
}