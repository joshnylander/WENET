package us.wenet.jawjs.adams;

import java.security.Principal;
import java.util.Arrays;

public class WENETPrincipal implements Principal {
	public static int CLIENT_SP = 1;
	public static int CLIENT_USER = 2;

	protected String name = null;
	protected String roles[] = new String[0];
	protected String certSubjectDN = new String("");
	protected int clientType;
	
	public WENETPrincipal() {
		//do nothing
	}
	
	public WENETPrincipal(String name, String[] roles, String certSubjectDN, int clientType) {
		super();
		this.name = name;
		this.roles = roles;
		this.certSubjectDN = certSubjectDN;
		this.clientType = clientType;
	}

	public int getClientType() {
		return clientType;
	}

	public String getCertSubjectDN() {
		return certSubjectDN;
	}

    public String[] getRoles() {
        return (this.roles);
    }
    
	public String getName() {
		return this.name;
	}
	
	/**
     * The authenticated Principal to be exposed to applications.
     */
    protected Principal userPrincipal = null;

    public Principal getUserPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else {
            return this;
        }
    }
	
	/**
     * Does the user represented by this Principal possess the specified role?
     *
     * @param role Role to be tested
     */
    public boolean hasRole(String role) {

        if("*".equals(role)) // Special 2.4 role meaning everyone
            return true;
        if (role == null)
            return (false);
        return (Arrays.binarySearch(roles, role) >= 0);

    }
	
	/**
     * Return a String representation of this object, which exposes only
     * information that should be public.
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("WENETPrincipal[");
        sb.append(this.name);
        sb.append(",");
        sb.append(this.certSubjectDN);
        sb.append("(");
        for( int i=0;i<roles.length; i++ ) {
            sb.append( roles[i]).append(",");
        }
        sb.append(")]");
        return (sb.toString());

    }
    
}
