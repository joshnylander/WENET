package us.wa.whatcom.co.wenet.client.toolkit;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;
import java.util.*;

public class InstanceDocumentReader extends DefaultHandler
{
static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

public String currentText = null;
public WENETJavaToolkit theManager;
public InstanceDocument theDocument;

	public InstanceDocumentReader(WENETJavaToolkit docMan) {
		theManager = docMan;
		theDocument = null;
		}

	public void readXMLString(String theString) throws Exception {
		StringReader sr = new StringReader(theString);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		
		sp.parse(new InputSource(sr), this); 
		return;
		}
		
	public void readXMLFile(String fileName) throws Exception {
		FileInputStream fis = new FileInputStream(fileName);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		
		sp.parse(fis, this); 
		fis.close();
		return;
		}
			
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
		throws SAXException {
		currentText = new String();
		qName = qName.toUpperCase();
		
		if(qName.endsWith("INSTANCE") == true) { theDocument = new InstanceDocument(); }
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase();
		if(currentText == null) { currentText = ""; }

		if(qName.endsWith("SERVICEURI") == true) { theDocument.serviceURI = currentText; return; }
		if(qName.endsWith("URL") == true) { theDocument.instanceURL = currentText; return; }
		if(qName.endsWith("INSTANCEURI") == true) { theDocument.instanceURI = currentText; return; }
  		if(qName.endsWith("ORGURI") == true) { theDocument.orgURI = currentText; return; }
  		if(qName.endsWith("INSTANCEELEMENT") == true) { theDocument.instanceData = currentText; return; }
//		if(qName.endsWith("LASTMODIFIED") == true) { currentItem.lastModified = }

		if(qName.endsWith("INSTANCE") == true) { return; }

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