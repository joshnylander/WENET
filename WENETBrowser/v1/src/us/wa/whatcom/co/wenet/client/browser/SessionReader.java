package us.wa.whatcom.co.wenet.client.browser;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;

public class SessionReader extends DefaultHandler
{
public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
public String currentText = null;
public WENETBrowserCore theManager;

	public SessionReader(WENETBrowserCore browserCore) {
		theManager = browserCore;
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
		
		}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if(length == 0) { return; }
		currentText += (new String(ch, start, length)).trim();
		}

	public void error(SAXParseException err) throws SAXParseException {
		System.out.println("** ERROR" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
		System.out.println(" " + err.getMessage());		
		throw err;
		}

	public void warning(SAXParseException err) throws SAXParseException {
		System.out.println("** Warning" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
		System.out.println(" " + err.getMessage());
		}		
}