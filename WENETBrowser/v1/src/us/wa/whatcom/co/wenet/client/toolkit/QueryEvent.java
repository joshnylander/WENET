package us.wa.whatcom.co.wenet.client.toolkit;

/** This class encapsulates a users request for an ResultsDocument object. These objects get generated when the 
* ExecutionManager.query() method is called. These objects then get stored in a buffer in ExecutionManager until
* they are picked up by a worker ExecutionThread and processed.
*/

public class QueryEvent
{
/** The service document representing the service that is to be queried */
public ServiceDocument queryService;
public String xPathQuery;
public String xPathNamespaces;
public String returnID;

	public QueryEvent() {
		}
}