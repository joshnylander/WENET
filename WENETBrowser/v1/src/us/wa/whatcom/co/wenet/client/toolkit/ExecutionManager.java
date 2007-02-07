package us.wa.whatcom.co.wenet.client.toolkit;

import java.util.*;

public class ExecutionManager
{
public static final int SERVICE_DOCUMENT_LISTENERS = 1;
public static final int QUERY_LISTENERS = 2;
public static final int RESULTS_LISTENERS = 3;
public static final int AUTHLIST_LISTENERS = 4;
public static final int AUDITLOG_LISTENERS = 5;

public List workThreads;
public ArrayList queryBuffer;
public ArrayList resultBuffer;
public ArrayList serviceDocumentBuffer;
public ArrayList authlistBuffer;
public ArrayList auditLogBuffer;

public ArrayList auditLogListeners;
public ArrayList authlistListeners;
public ArrayList queryListeners;
public ArrayList resultListeners;
public ArrayList serviceDocListeners;
public WENETJavaToolkit theManager;

	public ExecutionManager(WENETJavaToolkit wt) {
		theManager = wt;
		authlistBuffer = new ArrayList();
		authlistListeners = new ArrayList();
		queryBuffer = new ArrayList();
		resultBuffer = new ArrayList();
		auditLogBuffer = new ArrayList();
		serviceDocumentBuffer = new ArrayList();
		queryListeners = new ArrayList();
		resultListeners = new ArrayList();
		serviceDocListeners = new ArrayList();
		auditLogListeners = new ArrayList();
		}

	//-----------------------------------------------------------------------------------------

	public void feedListeners(Object value, String returnID, int listenerType) {
		if(value == null || returnID == null) { return; }

		switch(listenerType) {
			
			case AUDITLOG_LISTENERS: {
				synchronized(auditLogListeners) {
					for(int i=0; i<auditLogListeners.size(); i++) {
						WENETListener wl = (WENETListener)auditLogListeners.get(i);
						wl.process(value, returnID, AUDITLOG_LISTENERS);
						}
					}
				}
		
			case AUTHLIST_LISTENERS: {
				synchronized(authlistListeners) {
					for(int i=0; i<authlistListeners.size(); i++) {
						WENETListener wl = (WENETListener)authlistListeners.get(i);
						wl.process(value, returnID, AUTHLIST_LISTENERS);
						}
					}
				}
				
			case SERVICE_DOCUMENT_LISTENERS: {
				synchronized(serviceDocListeners) {
					for(int i=0; i<serviceDocListeners.size(); i++) {
						WENETListener wl = (WENETListener)serviceDocListeners.get(i);
						wl.process(value, returnID, SERVICE_DOCUMENT_LISTENERS);
						}				
					}
				return;
				}
				
			case QUERY_LISTENERS: {
				synchronized(queryListeners) {
					for(int i=0; i<queryListeners.size(); i++) {
						WENETListener wl = (WENETListener)queryListeners.get(i);
						wl.process(value, returnID, QUERY_LISTENERS);
						}
					}
				return;
				}
			
			case RESULTS_LISTENERS: {
				synchronized(resultListeners) {
					for(int i=0; i<resultListeners.size(); i++) {
						WENETListener wl = (WENETListener)resultListeners.get(i);
						wl.process(value, returnID, RESULTS_LISTENERS);
						}
					}
				return;
				}
			}
		}

	//-----------------------------------------------------------------------------------------
	// These methods serve as object producers supplying Event objects to the consuming threads
	
	public Object consumeAuditLog() {
		if(auditLogBuffer.size() == 0) { return null; }
		
		synchronized(auditLogBuffer) {
			Object o = auditLogBuffer.get(0);
			auditLogBuffer.remove(0);
			return o;			
			}
		}
	
	public Object consumeAuthlist() {
		if(authlistBuffer.size() == 0) { return null; }
		
		synchronized(authlistBuffer) {
			Object o = authlistBuffer.get(0); 
			authlistBuffer.remove(0);
			return o;
			}
		}
		
	public Object consumeDiscovery() {
		if(serviceDocumentBuffer.size() == 0) { return null; }
		
		synchronized(serviceDocumentBuffer) {
			Object o = serviceDocumentBuffer.get(0); 
			serviceDocumentBuffer.remove(0);
			return o;
			}
		}
		 		
	public Object consumeQuery() {
		if(queryBuffer.size() == 0) { return null; }
		
		synchronized(queryBuffer) {
			Object o = queryBuffer.get(0);
			queryBuffer.remove(0);
			return o;
			}
		}

	public Object consumeResults() {
		if(resultBuffer.size() == 0) { return null; }
		
		synchronized(resultBuffer) {
			Object o = resultBuffer.get(0);
			resultBuffer.remove(0);
			return o;
			}
		}

	//-----------------------------------------------------------------------------------------
	// These methods push the content through the execution loop, after which the ExecutionThread
	// will distribute the results out to the appropriate listeners 

	public void auditLog(ServiceDocument sd, String xPath, String namespaces, String returnID) {
		if(sd == null || xPath == null || returnID == null) { return; }
		
		AuditLogEvent ale = new AuditLogEvent();
		ale.theService = sd;
		ale.xPath = xPath;
		ale.returnID = returnID;
		ale.xPathNamespaces = namespaces;
		auditLogBuffer.add(ale);
		
		}

	public void authlist(ServiceDocument sd, String returnID) {
		if(sd == null || returnID == null) { return; }
		
		AuthlistEvent ae = new AuthlistEvent();
		ae.theService = sd;
		ae.returnID = returnID;
		authlistBuffer.add(ae);
		
		}
					
	public void query(String xPath, String namespaces, ServiceDocument sd, String returnID) {
		if(xPath == null || returnID == null) { return; }
		
		QueryEvent qe = new QueryEvent();
		qe.xPathQuery = xPath;
		qe.xPathNamespaces = namespaces;
		qe.returnID = returnID;
		qe.queryService = sd;
		queryBuffer.add(qe);
		
		}
	
	public void results(ResultsDocument rd, String returnID) {
		if(rd == null || returnID == null) { return; }
		
		// Go through the result document and pass each instance item into the mix
		for(int i=0; i<rd.resultItems.size(); i++) {
			ResultsDocument.ResultItem ri = (ResultsDocument.ResultItem)rd.resultItems.get(i);
			ResultsEvent re = new ResultsEvent();
			re.theItem = ri;
			re.returnID = returnID;
			resultBuffer.add(re);
			}
					
		}
		
	public void results(ResultsDocument.ResultItem ri, String returnID) {
		if(ri == null || returnID == null) { return; }
		
		ResultsEvent re = new ResultsEvent();
		re.theItem = ri;
		re.returnID = returnID;
		resultBuffer.add(re);
					
		}
		
	public void results(String instanceURL, String returnID) {
		if(instanceURL == null || returnID == null) { return; }
		
		ResultsEvent re = new ResultsEvent();
		re.instanceURL = instanceURL;
		re.returnID = returnID;
		resultBuffer.add(re);
		
		}

	public void discover(String serviceURL, String returnID) {
		if(serviceURL == null || returnID == null) { return; }
		
		DiscoveryEvent de = new DiscoveryEvent();
		de.returnID = returnID;
		de.serverURL = serviceURL;
		serviceDocumentBuffer.add(de);

		}	
		
	//-----------------------------------------------------------------------------------------

	public void addAuditLogListener(WENETListener sdl) {
		if(sdl == null) { return; }
		auditLogListeners.add(sdl);
		}
		
	public void removeAuditLogListener(WENETListener sdl) {
		if(sdl == null) { return; }
		auditLogListeners.remove(sdl);
		}
		
	public void addAuthlistListener(WENETListener sdl) {
		if(sdl == null) { return; }
		authlistListeners.add(sdl);
		}
		
	public void removeAuthlistListener(WENETListener sdl) {
		if(sdl == null) { return; }
		authlistListeners.remove(sdl);
		}

	public void addServiceDocListener(WENETListener sdl) {
		if(sdl == null) { return; }
		serviceDocListeners.add(sdl);
		}
		
	public void removeServiceDocListener(WENETListener sdl) {
		if(sdl == null) { return; }
		serviceDocListeners.remove(sdl);
		}
		
	public void addQueryListener(WENETListener ql) {
		if(ql == null) { return; }
		queryListeners.add(ql); 
		}
		
	public void removeQueryListener(WENETListener ql) {
		if(ql == null) { return; }
		queryListeners.add(ql); 
		}
		
	public void addResultListener(WENETListener rl) {
		if(rl == null) { return; }
		resultListeners.add(rl); 
		}
		
	public void removeResultListener(WENETListener rl) {
		if(rl == null) { return; }
		resultListeners.remove(rl); 
		}
}