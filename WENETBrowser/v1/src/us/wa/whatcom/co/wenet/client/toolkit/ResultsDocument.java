package us.wa.whatcom.co.wenet.client.toolkit;

import java.util.*;
import java.security.cert.*;

/** This class encapsulates the wenet:result document into a java object. There is an associated 
* ResultsDocumentReader to transform the XML into this java object. These objects are created when
* a worker ExecutionThread processes a QueryEvent object and retrieves a wenet:results from a service point.
* The worker thread passes the XML through the ResultsDocumentReader and a new ResultsDocument is born.
*/

public class ResultsDocument
{
/** If this is not null, then attached is the exception that haulted processesing of this ResultsDocument. */
public ToolkitError error = null;

/** This is the list of ResultItem objects that were entries of the wenet:results document */
public String checkBackURL;
public String serviceURI;
public String orgURI;
public String resultListURL;
public ArrayList resultItems;
public String xPathQuery;
public String xmlText;

	public ResultsDocument() {
		resultItems = new ArrayList();
		}

	/** This class encapsulates each individual result item embedded inside a wenet:results doc */
	public class ResultItem {
		public String instanceURL;
		public String instanceURI;
		public java.util.Date lastModified;
		public String orgURI;
		public String serviceURI;
		public HashMap summaryMap = null;
		public String summaryXML = null;
		
		public ResultItem() {  }
	}
}