package us.wa.whatcom.co.wenet.client.toolkit;

import java.nio.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;

public class ResultsDocumentTransformer extends DefaultHandler
{
static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

public boolean bSummaryMode = false;
public String currentText = null;
public WENETJavaToolkit theManager;
public ResultsDocument theDocument;
public ResultsDocument.ResultItem currentItem;
public String resultsText;
public StringBuffer resultsBuffer;

	public ResultsDocumentTransformer(WENETJavaToolkit docMan) {
		theManager = docMan;
		theDocument = null;
		resultsText = "";
		resultsBuffer = new StringBuffer(1024*8);
		}
		
	public String transformResults() {
		
		String txt = "<resultList>\n";
		txt += "<serviceURI> " + theDocument.serviceURI + "</serviceURI>\n";
		txt += resultsBuffer.toString();
		txt += "</resultList>\n";
		
		theDocument = null;
		currentItem = null;		
		resultsText = "";
		resultsBuffer = resultsBuffer.delete(0, resultsBuffer.length());
		return txt;
		}

	public void readXMLString(String theString) throws Exception {
		theDocument = new ResultsDocument();
		currentItem = theDocument.new ResultItem();		
		currentItem.summaryMap = new HashMap();

		StringReader sr = new StringReader(theString);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(false);
		SAXParser sp = spf.newSAXParser();
		sp.parse(new InputSource(sr), this); 
		}
		
	public void readXMLFile(String fileName) throws Exception {
		theDocument = new ResultsDocument();
		currentItem = theDocument.new ResultItem();		
		currentItem.summaryMap = new HashMap();
		
		FileInputStream fis = new FileInputStream(fileName);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(false);
		SAXParser sp = spf.newSAXParser();
		sp.parse(fis, this); 
		fis.close();
		}
			
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
		throws SAXException {
		currentText = new String();
		qName = qName.toUpperCase();
		
		if(qName.endsWith("RESULT") == true) {  
			currentItem.orgURI = "";
			currentItem.serviceURI = "";
			currentItem.instanceURI = "";
			currentItem.instanceURL = "";
			currentItem.summaryMap.clear();
			return; 
			}

		if(qName.endsWith("SUMMARY") == true) { 
			bSummaryMode = true;
			return;
			}
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase();
		if(currentText == null) { currentText = ""; }

		if(qName.endsWith("SUMMARY") == true) {
			bSummaryMode = false;
			return;
			}

		// If its in summary mode, any further nodes until the next <SUMMARY> are interpreted as summary data
		if(bSummaryMode == true) {
			currentItem.summaryMap.put(qName, currentText);
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

		if(qName.endsWith("RESULT") == true) {
			System.out.print("*");

			resultsBuffer.append("<result>\n");
			resultsBuffer.append("<instanceURL><![CDATA[" + currentItem.instanceURL + "]]></instanceURL> \n");
	
			if(currentItem.summaryMap != null) {
				Object column[] = currentItem.summaryMap.keySet().toArray();
				for(int u=0; u<column.length; u++) {
				System.out.print("-");
					resultsBuffer.append("<summary>\n");
					resultsBuffer.append("<name>" + column[u] + "</name>\n");
					resultsBuffer.append("<value>" + currentItem.summaryMap.get(column[u]) + "</value>\n");
					resultsBuffer.append("</summary>\n");
					}
				}		
				
			resultsBuffer.append("</result>\n");
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