package us.wenet.jawjs.adams;

import java.security.Principal;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class WENETRequest extends HttpServletRequestWrapper {
	private Date when = new Date();
	private String requestIPAddress;
	private String methodName = new String("");
	private String queryXPath = new String("");
	private String queryRoot = new String("");
	private WENETPrincipal userPrincipal;
	
	public WENETRequest(HttpServletRequest request) {
		super(request);
		requestIPAddress = request.getRemoteAddr();
	}

	public String getUserRFC822name() {
		if (userPrincipal != null && userPrincipal.getClientType() == WENETPrincipal.CLIENT_USER) {
			return userPrincipal.getName();
		} else {
			return "";
		}
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public String getQueryXPath() {
		return queryXPath;
	}

	public void setQueryXPath(String queryXPath) {
		this.queryXPath = queryXPath;
	}

	public String getQueryRoot() {
		return queryRoot;
	}

	public void setQueryRoot(String queryRoot) {
		this.queryRoot = queryRoot;
	}

	public Date getWhen() {
		return when;
	}

	public String getRequestIPAddress() {
		return requestIPAddress;
	}
	
	public void setUserPrincipal(WENETPrincipal userPrincipal) {
		this.userPrincipal = userPrincipal;
	}

	public Principal getUserPrincipal() {
		return userPrincipal;
	}
}
