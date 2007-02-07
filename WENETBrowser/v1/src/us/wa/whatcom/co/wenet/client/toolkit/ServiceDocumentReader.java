package us.wa.whatcom.co.wenet.client.toolkit;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*; 
import java.io.*;
import java.util.*;

/**
* This class converts an XML wenet:service document into a WENETJavaToolkit ServiceDocument object.
*/

public class ServiceDocumentReader extends DefaultHandler
{
static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

public String currentText = null;
public WENETJavaToolkit theManager;
public ServiceDocument theDocument;
public ServiceDocument.SupportedNamespace currentNamespace;
public ServiceDocument.SupportedXPath currentXPath;
public ServiceDocument.ServiceMethod currentMethod;
public String currentRootNode;

public Cluster currentCluster;
public Cluster.ClusterMember currentMember;
public boolean bClusterMode = false;

public ArrayList unknownTagsList;

public static final byte OSC_DEFAULT = 1;
public static final byte OSC_CLUSTERMEMBER = 2;

/** OrgURI and ServiceURI are used multiple times in the XML, this specifies which context */
public byte osURIContext = OSC_DEFAULT;

	public ServiceDocumentReader(WENETJavaToolkit docMan) {
		theManager = docMan;
		theDocument = null;
		unknownTagsList = new ArrayList();
		}

	/**
	* This method starts the XML parser reading the xml from a string object
	*/
	public void readXMLString(String theString) throws Exception {
		StringReader sr = new StringReader(theString);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();		
		sp.parse(new InputSource(sr), this); 
		}
		
	/**
	* This method starts the XML parser reading the xml from a file.
	* @param fileName the name of the file containing the xml
	*/	
	public void readXMLFile(String fileName) throws Exception {
		FileInputStream fis = new FileInputStream(fileName);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();		
		sp.parse(fis, this); 
		fis.close();
		}

	/**
	* Certain tags require special initialization when they are first encountered.
	*/
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
		throws SAXException {
		currentText = new String();
		qName = qName.toUpperCase();
		
		if(qName.endsWith("CLUSTER") == true) { currentCluster = new Cluster(theManager); }
		if(qName.endsWith("CLUSTERMEMBER") == true) { 
			currentMember = currentCluster.new ClusterMember(); 
			osURIContext = OSC_CLUSTERMEMBER;
			return;
			}
			
		if(qName.endsWith("METHOD") == true) { currentMethod = theDocument.new ServiceMethod(); }		
		if(qName.endsWith("SUPPORTEDXPATH") == true) { 
			currentXPath = theDocument.new SupportedXPath(); 
			if(attributes.getValue("showInSummary") != null) { currentXPath.showInSummary = (new Boolean(attributes.getValue("showInSummary"))).booleanValue(); }
			if(attributes.getValue("fieldName") != null) { currentXPath.fieldName = attributes.getValue("fieldName"); }
			if(attributes.getValue("fieldDescription") != null) { currentXPath.fieldName = attributes.getValue("fieldDescription"); }
			}			
		
		if(qName.endsWith("NAMESPACE") == true) {
			currentNamespace = theDocument.new SupportedNamespace();
			if(attributes.getValue("prefix") != null) { currentNamespace.prefix = attributes.getValue("prefix"); }
			if(attributes.getValue("location") != null) { currentNamespace.location = attributes.getValue("location"); }
			}
		}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		qName = qName.toUpperCase();
		if(currentText == null) { currentText = ""; }
		

		if(qName.endsWith("SERVICE") == true) { return; }
		
		if(qName.endsWith("SERVICEURI") == true) { 
			switch(osURIContext) {
				case OSC_DEFAULT: { theDocument.serviceURI = currentText; return; }				
				case OSC_CLUSTERMEMBER: { currentMember.serviceURI = currentText; return; }
				}
			}
		
		if(qName.endsWith("ORGURI") == true) { 
			switch(osURIContext) {
				case OSC_DEFAULT: { theDocument.organizationURI = currentText; return; }
				case OSC_CLUSTERMEMBER: { currentMember.orgURI = currentText; return; }
				}
			}
		
		//-----------------------------------------------------------------------
		if(qName.endsWith("DESCRIPTION") == true) { theDocument.serviceDescription = currentText; return; }
		if(qName.endsWith("OWNER") == true) { theDocument.servicePointOwner = currentText; return; }
		if(qName.endsWith("SERVICECERTIFICATE") == true) { return; }
		if(qName.endsWith("ISSUERDN") == true) { theDocument.certIssuerDN = currentText; return; }
		if(qName.endsWith("SUBJECTDN") == true) { theDocument.certSubjectDN = currentText; return; }
		if(qName.endsWith("SERIALNUMBER") == true) { theDocument.certSerialNumber = currentText; return; }

		//-----------------------------------------------------------------------
		if(qName.endsWith("INSTANCEROOTS") == true) { return; }
		if(qName.endsWith("INSTANCEROOT") == true) { return; }
		if(qName.endsWith("ROOTNODE") == true) { currentRootNode = currentText; return; }
		if(qName.endsWith("SUPPORTEDXPATH") == true) { 
			currentXPath.xPath = currentText; 
			
			ArrayList al = (ArrayList)theDocument.supportedXPathMap.get(currentRootNode);
			if(al == null) { al = new ArrayList(); }
			al.add(currentXPath);
			
			theDocument.supportedXPathMap.put(currentRootNode, al);
			currentXPath = null;
			currentRootNode = null;
			return; 
			}
		
		if(qName.endsWith("NAMESPACE") == true) {
			currentNamespace.namespace = currentText;
			theDocument.supportedNamespaces.add(currentNamespace);			
			currentNamespace = null;
			return;			
			}
					
		//-----------------------------------------------------------------------
		if(qName.endsWith("CLUSTERS") == true) { return; }
		if(qName.endsWith("CLUSTERNAME") == true) { currentCluster.clusterName = currentText; return; }
		if(qName.endsWith("CLUSTERURI") == true) { currentCluster.clusterURI = currentText; return; }
		if(qName.endsWith("MYPARENTINCLUSTER") == true) { currentCluster.parentClusterURI = currentText; return; }
		if(qName.endsWith("SERVICEDESCRIPTORURL") == true) { currentMember.serviceDescriptorURL = currentText; return; }
		if(qName.endsWith("CLUSTERMEMBER") == true) { currentCluster.clusterMembers.add(currentMember); currentMember = null; osURIContext = OSC_DEFAULT; return; }
		if(qName.endsWith("CLUSTER") == true) { 
			theDocument.clusterList.add(currentCluster); 
			currentCluster = null; 
			return; 
			}
	
		
		if(qName.endsWith("METHODNAME") == true) { currentMethod.methodName = currentText; return; }
		if(qName.endsWith("METHODURL") == true) { currentMethod.methodURL = currentText; return; }
		if(qName.endsWith("METHODTYPE") == true) { currentMethod.methodType = currentText; return; }
		if(qName.endsWith("METHODDESCRIPTION") == true) { currentMethod.methodDescription = currentText; return; }
		if(qName.endsWith("METHODPARAMETERS") == true) { return; }
		if(qName.endsWith("METHODPARAMETER") == true) { currentMethod.methodParameters.add(currentText); return; }
		if(qName.endsWith("METHOD") == true) { theDocument.supportedMethods.add(currentMethod); }
		if(qName.endsWith("METHODS") == true) { return; }
		
		unknownTagsList.add(qName);		
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