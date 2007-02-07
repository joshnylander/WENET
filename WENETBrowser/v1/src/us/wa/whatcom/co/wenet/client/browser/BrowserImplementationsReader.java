package us.wa.whatcom.co.wenet.client.browser;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;
import java.util.*;

public class BrowserImplementationsReader extends DefaultHandler
{
public static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
public static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
public static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
public String currentText = null;
public String currentXSLTType = null;
public String currentPrefix = null;
public String currentServiceURI = null;
public String currentColumnName = null;
public String currentColumnXPath = null;
public String currentDisplaySchemeName = null;
public String currentDisplaySchemeEntry = null;
public String currentDisplaySchemeValue = null;
public BrowserImplementation currentImpl = null;
public BrowserRoot currentRoot = null;
public WENETBrowserServlet theManager;
public Boolean bDisplaySummaryCol = null;

	public BrowserImplementationsReader(WENETBrowserServlet browserServlet) {
		theManager = browserServlet;
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
		
		if(qName.endsWith("IMPLEMENTATION") == true) { currentImpl = new BrowserImplementation(); return; }
		if(qName.endsWith("ROOT") == true) { currentRoot = new BrowserRoot(); return; }
		if(qName.endsWith("SUMMARYCOLUMN") == true) { bDisplaySummaryCol = (new Boolean((String)attributes.getValue("showColumn"))); }
		if(qName.endsWith("COLUMNMAPPING") == true) { currentColumnName = null; currentColumnXPath = null; }
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase().trim();
		if(currentText == null || currentText.length() == 0) { return; }

		if(qName.endsWith("SUMMARYCOLUMN") == true) { 
			currentRoot.summaryColumnsMap.put(currentText, bDisplaySummaryCol);
			bDisplaySummaryCol = null;
			}

		if(qName.endsWith("IMPLEMENTATIONNAME") == true) { currentImpl.implName = currentText; return; }		
		if(qName.endsWith("DESCRIPTION") == true) { currentImpl.implDesc = currentText; return; }
		if(qName.endsWith("APPDIRECTORY") == true) { currentImpl.appDirectory = currentText; return; }
		if(qName.endsWith("LARGEAPPLICATIONICON") == true) { currentImpl.largeApplicationIcon = currentText; return; }				
		if(qName.endsWith("SMALLAPPLICATIONICON") == true) { currentImpl.smallApplicationIcon = currentText; return; }
		if(qName.endsWith("SEEDURL") == true) { currentImpl.seedServicePoints.add(currentText); return; }

		if(qName.endsWith("ROOTNAME") == true) { currentRoot.rootName = currentText; return; }
		if(qName.endsWith("LARGEROOTICON") == true) { currentRoot.largeRootIcon = currentText; return; }
		if(qName.endsWith("SMALLROOTICON") == true) { currentRoot.smallRootIcon = currentText; return; }
		if(qName.endsWith("TYPE") == true) { currentXSLTType = currentText; return; }
		if(qName.endsWith("FILE") == true) { currentRoot.presentationXSLTs.put(currentXSLTType, currentText); currentXSLTType=null; return; }				
		if(qName.endsWith("ROOT") == true) { currentImpl.supportedRoots.add(currentRoot); currentRoot=null; return; }
		
		if(qName.endsWith("PREFIX") == true) { currentPrefix = currentText; return; }
		if(qName.endsWith("LOCATION") == true) { theManager.namespaceMap.put(currentPrefix, currentText); currentPrefix=null; return; }		
		if(qName.endsWith("IMPLEMENTATION") == true) { theManager.supportedImplementations.add(currentImpl); currentImpl=null; return; }
		
		if(qName.endsWith("SERVICEURI") == true) { currentServiceURI = currentText; return; }
		if(qName.endsWith("FRIENDLYNAME") == true) {
			if(currentServiceURI != null) { currentImpl.aliasMap.put(currentServiceURI, currentText); }
			currentServiceURI = null;
			return;
			}
			
		if(qName.endsWith("COLUMNNAME") == true) { currentColumnName = currentText; return; }
		if(qName.endsWith("XPATH") == true) { currentColumnXPath = currentText; return; }
		if(qName.endsWith("ALIASMAPPING") == true) { 
		
			if(currentColumnName != null && currentColumnXPath != null) { 
				currentImpl.columnAliasMappings.put(currentColumnXPath, currentColumnName); 
				currentColumnName = null;
				currentColumnXPath = null;
				}
			return;
			}
		
		if(qName.endsWith("SCHEMENAME") == true) { currentDisplaySchemeName = currentText; return; }
		if(qName.endsWith("ENTRYNAME") == true) { currentDisplaySchemeEntry = currentText; return; }
		if(qName.endsWith("ENTRYVALUE") == true) { currentDisplaySchemeValue = currentText; return; }

		if(qName.endsWith("DISPLAYENTRY") == true) { 
			if(currentDisplaySchemeName == null) { System.out.println("NULL Display Schema"); return; } 
			
			HashMap schemeMap = (HashMap)currentImpl.displaySchemeMap.get(currentDisplaySchemeName.toUpperCase());
			if(schemeMap == null) { schemeMap = new HashMap(); }
			schemeMap.put(currentDisplaySchemeEntry, currentDisplaySchemeValue);
			currentImpl.displaySchemeMap.put(currentDisplaySchemeName.toUpperCase(), schemeMap);

			currentDisplaySchemeEntry = null;
			currentDisplaySchemeValue = null;
			return;
			}

		if(qName.endsWith("DISPLAYSCHEME") == true) { currentDisplaySchemeName = null; return; }
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
}