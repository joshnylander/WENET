package us.wa.whatcom.co.wenet.client.toolkit;

/**
* This represents the toolkits generic interface between the low-level execution engine and client applications.
* Client applications that wish access to the low-level toolkit functionality can implement this WENETListener
* interface, and call the appropriate add***Listener() method found inside the ExecutionManager class.
* When a client application performs any opertions with the toolkit, the data/objects that were retrieved with
* the ExecutionThreads gets passed to an associated WENETListener implementations process() method. Client applications
* can fill in the process() method with custom functionality.<p>
*
* The WENETJavaToolkit class also implements the WENETListener interface to return data to the high-level query methods.
*/

public interface WENETListener 
{
	/** This method is the end-point for toolkit communications. The value object must be casted into the 
	 * appropriate WENETJavaToolkit type (ResultDocument, InstanceDocument, ServiceDocument) depending on
	 * what operation this call of process() is in response to. The listenerType variable will inform the   
	 * interface user of what type the value object should be. 
	 *
	 * @param value The uncasted return value sent from the execution engine
	 * @param returnID A user set ID, allowing users to track which value objects went with which operations
	 * @param listenerType What type of value is the listener returning. 
	 *
	 * @see us.wa.whatcom.co.wenet.client.toolkit.ExecutionManager#SERVICE_DOCUMENT_LISTENERS ExecutionManager.SERVICE_DOCUMENT_LISTENERS
 	 * @see us.wa.whatcom.co.wenet.client.toolkit.ExecutionManager#QUERY_LISTENERS ExecutionManager.QUERY_LISTENERS
 	 * @see us.wa.whatcom.co.wenet.client.toolkit.ExecutionManager#RESULTS_LISTENERS ExecutionManager.RESULTS_LISTENERS
 	 * @see us.wa.whatcom.co.wenet.client.toolkit.ExecutionManager#AUTHLIST_LISTENERS ExecutionManager.AUTHLIST_LISTENERS
 	 * @see us.wa.whatcom.co.wenet.client.toolkit.ExecutionManager#AUDITLOG_LISTENERS ExecutionManager.AUDITLOG_LISTENERS 	  	 
	 */
	public void process(Object value, String returnID, int listenerType);
}