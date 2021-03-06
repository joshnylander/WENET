package us.wa.whatcom.co.wenet.client.toolkit;

/** This class encapsulates the error information produced after an error has been generated by the execution engine.
* The error could either be a communications failure or an error parsing the returned XML document. If the exc variable
* is not null, then this is the generated Exception object that generated the intial error. 
*/

public class ToolkitError
{
/** If this is not null, then the original error Exception object is attached. */
public Exception exc = null;
public String errorMsg = null;
public int errorCode = -1;

	public ToolkitError() { }
	public ToolkitError(String msg) { errorMsg = msg; }
	public ToolkitError(String msg, int code) { errorMsg = msg; errorCode = code; }
}