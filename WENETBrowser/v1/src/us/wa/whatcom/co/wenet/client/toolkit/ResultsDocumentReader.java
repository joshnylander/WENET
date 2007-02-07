package us.wa.whatcom.co.wenet.client.toolkit;

import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;

/** This class turns wenet:result documents into the java toolkits ResultDocument objects. */
public class ResultsDocumentReader extends DefaultHandler
{
static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

public boolean bSummaryMode = false;
public HashMap summaryMap = null;
public String currentText = null;
public WENETJavaToolkit theManager;
public ResultsDocument theDocument;
public ResultsDocument.ResultItem currentItem;
public ArrayList summaryColumnStack;
//public String summaryXML = null;

	public ResultsDocumentReader(WENETJavaToolkit docMan) {
		theManager = docMan;
		theDocument = null;
		summaryColumnStack = new ArrayList();
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
		String qNameOrig = new String(qName);
		currentText = new String();
		qName = qName.toUpperCase();
		
		if(qName.endsWith("RESULT") == true) { currentItem = theDocument.new ResultItem(); return; }
		//[TO DO] Need to modify this section so that we can grab the summary as is.
		if(qName.endsWith("SUMMARY") == true) { 
			currentItem.summaryMap = new HashMap(); 
			//summaryXML = new String("");
			bSummaryMode = true;
			return;
			}
			
		if(bSummaryMode == true) {
			//Add qName to summaryColumnStack
			summaryColumnStack.add(qName);
			
			//Build summaryXML and add attributes both to summaryXML and to stack
			//summaryXML = summaryXML + "\r" + "<" + qNameOrig;
			if (attributes.getLength() > 0) {
				for (int i=0; i<attributes.getLength(); i++) {
					currentItem.summaryMap.put(attributes.getQName(i), attributes.getValue(i));
					//summaryXML = summaryXML + " " + attributes.getQName(i);
					//summaryXML = summaryXML + "=\"" + attributes.getValue(i) + "\"";
					}
				}
			//summaryXML = summaryXML + ">";
			}
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		String qNameOrig = new String(qName);
		qName = qName.toUpperCase();
		if(currentText == null) { currentText = ""; }

		if(qName.endsWith("SUMMARY") == true) {
			bSummaryMode = false;
			return;
			}

		// If its in summary mode, any further nodes until the next <SUMMARY> are interpreted as summary data
		if(bSummaryMode == true) {
		
			String colName = "";
			for(int i=0; i<summaryColumnStack.size(); i++) {
				colName += summaryColumnStack.get(i).toString() + "/";
				}
			
			//summaryXML = summaryXML + currentText + "</" + qNameOrig + ">";
			// Only add it if there is data available
			if(currentText.trim().length() > 0) {
				colName = colName.trim();
				if(colName.endsWith("/") == true) { colName = colName.substring(0, colName.length()-1); }
				currentItem.summaryMap.put(colName, currentText);
				currentText = "";
				}
				
			summaryColumnStack.remove(summaryColumnStack.size()-1);
			return;
			}

		if(currentItem != null) {
	  		if(qName.endsWith("ORGURI") == true) { currentItem.orgURI = currentText; return; }
			if(qName.endsWith("SERVICEURI") == true) { currentItem.serviceURI = currentText; return; }
			if(qName.endsWith("INSTANCEURL") == true) { currentItem.instanceURL = currentText; return; }
			if(qName.endsWith("INSTANCEURI") == true) { currentItem.instanceURI = currentText; return; }			
			}

		if(qName.endsWith("CHECKBACKURL") == true) { theDocument.checkBackURL = currentText; return; }
		if(qName.endsWith("SERVICEURI") == true) { theDocument.serviceURI = currentText; return; }
		if(qName.endsWith("ORGURI") == true) { theDocument.orgURI = currentText; return; }
		if(qName.endsWith("URL") == true) { theDocument.resultListURL = currentText; return; }
//		if(qName.endsWith("LASTMODIFIED") == true) { currentItem.lastModified = }

		if(qName.endsWith("RESULT") == true) {
			//currentItem.summaryXML = summaryXML;
			theDocument.resultItems.add(currentItem);
			currentItem = null;
			return;
			}

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