package us.wa.whatcom.co.wenet.client.toolkit;

import java.util.*;
import java.security.cert.*;

/**
 * The ServiceDocument class is a java encapsulation of the wenet:service document specified in the
 * schema found at: <a href="http://www.co.whatcom.wa.us/apps/wenet/currentschema.jsp">http://www.co.whatcom.wa.us/apps/wenet/currentschema.jsp</a>.
 * There is an associated ServiceDocumentReader class that uses the SAX api to convert the xml 
 * into this object.
 */

public class ServiceDocument
{
/** If there was an error processing or retrieveing the ServiceDocument this object will not be null */
public ToolkitError error = null;

public String serviceDocumentURL;
public String servicePointOwner;
public String serviceDescription;
public String organizationURI;
public String serviceURI;
public long lastModifiedTime;

/** A list of Cluster objects displaying the world as this service point sees it */
public ArrayList clusterList;

/** A list of ServiceMethod objects this service point implements */
public ArrayList supportedMethods;

public String certSubjectDN;
public String certIssuerDN;
public String certSerialNumber;

/** A list of SupportedNamespace objects that this service point outputs data with */
public ArrayList supportedNamespaces;

/** A map of GJXDM XPaths that this service point provides data for */
public HashMap supportedXPathMap;

/** The original XML data that this object was parsed from */
public String xmlText;

	public ServiceDocument() {
		supportedNamespaces = new ArrayList();
		supportedXPathMap = new HashMap();
		supportedMethods = new ArrayList();
		clusterList = new ArrayList();
		xmlText = null;
		}

	/**
	* This is a utility method that finds a ServiceMethod object from the supportedMethods list using either
	* the non-case-sensitive method name or the method url.
	*/
		
	public ServiceMethod findMethod(String name, String url) {
		for(int i=0; i<supportedMethods.size(); i++) {
			ServiceMethod se = (ServiceMethod)supportedMethods.get(i);
			if(name != null) { if(se.methodName.equalsIgnoreCase(name) == true) { return se; } }
			if(url != null) { if(se.methodURL.equalsIgnoreCase(url) == true) { return se; } }
			}
			
		return null;
		}		
	
	/**
	* An embedded utility class that binds each namespaces information into a single object.
	*/
	
	public class SupportedNamespace {
		public String namespace;
		public String prefix;
		public String location;
		public SupportedNamespace() { }
		}

	/**
	* An embedded utility class that binds the information for a single supportedXPath entry
	* into an object. The supported xpath entries come from a  <a href="http://www.co.whatcom.wa.us/apps/wenet/currentschema.jsp">wenet:service</a> document. Objects of this type created 
	* in the ServiceDocumentReader class when a wenet:service document is parsed. These objects are placed into
	* the service documents supportedXPathMap hash map.
	*/
	
	public class SupportedXPath {
		public boolean showInSummary;
		public String fieldName;
		public String fieldDescription;
		public String xPath;
		
		public SupportedXPath() { 
			showInSummary = false;
			}
		}
	/**
	* A utility class that encapsulates the service method information. These classes are created during a parse
	* of a wenet service document through the ServiceDocumentReader class.
	*/

	public class ServiceMethod {
		public ArrayList methodParameters;
		public String methodDescription;
		public String methodType;
		public String methodName;
		public String methodURL;
		public ServiceMethod() { 
			methodParameters = new ArrayList();		
			}
		}

	public class User {
		public String userName;
		public Certificate userCertificate;
		public User() { }
		}
}