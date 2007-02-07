package us.wa.whatcom.co.wenet.client.toolkit;

import java.util.*;

public class AuthList
{
public String theURL;
public String serviceURI;
public String orgURI;
public String lastModified;
public ArrayList authCertList;
public ArrayList userList;
public String xmlText;
public ToolkitError error;

	public AuthList() {
		authCertList = new ArrayList();
		userList = new ArrayList();
		}
		
	public class CertificateInfo {
		public String issuerDN;
		public String subjectDN;
		public String serialNumber;
		public CertificateInfo() { }
		}
		
	public class User {
		public String userName;
		public String userInfo;
		public ArrayList roleList;
		public User() { roleList = new ArrayList(); }
		}
}
