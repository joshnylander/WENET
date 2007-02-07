package us.wa.whatcom.co.wenet.client.toolkit;

import java.util.*;

public class AuditLog
{
public ToolkitError error;
public String orgURI;
public String serviceURI;
public String theURL;
public ArrayList logEntryList;
public String xmlText;

	public AuditLog() {
		logEntryList = new ArrayList();
		}
		
	public class LogEntry {
		public String userRFC822name;
		public String userCertSubject;
		public java.util.Date when;
		public String requestIPAddress;
		public String requestURL;
		public String methodName;
		public String postData;		
		public LogEntry() { }
		}
}