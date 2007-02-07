package us.wa.whatcom.co.wenet.client.toolkit;

/** This class encapsulates a users request for an InstanceDocument object. These objects get generated when the 
* ExecutionManager.result() method is called. These objects then get stored in a buffer in ExecutionManager until
* they are picked up by a worker ExecutionThread and processed.
*/

public class ResultsEvent
{
public String instanceURL;
public ResultsDocument.ResultItem theItem;
public String returnID;

	public ResultsEvent() {
		theItem = null;
		instanceURL = null;
		}
}