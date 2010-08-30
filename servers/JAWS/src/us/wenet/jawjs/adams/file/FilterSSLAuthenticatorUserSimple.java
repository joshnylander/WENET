package us.wenet.jawjs.adams.file;

import java.security.cert.CertPath;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import us.wenet.jawjs.adams.FilterSSLAuthenticatorBase;
import us.wenet.jawjs.adams.WENETPrincipal;

/**
 * Servlet Filter implementation class FilterSSLAuthenticatorUserSimple
 */
public class FilterSSLAuthenticatorUserSimple extends FilterSSLAuthenticatorBase implements Filter {
    
	private String validUsers[];
	private String defaultRoles[];
	
	
    /**
     * @see FilterSSLAuthenticatorBase#FilterSSLAuthenticatorBase()
     */
    public FilterSSLAuthenticatorUserSimple() {
        super();
    }

    private WENETPrincipal buildPrincipal(CertPath certsAsPath) throws ServletException{
    	String name = null;
    	String roles[] = new String[0];
    	String certSubjectDN = new String("");
    	int clientType;
    	
    	// This is creating a user principal
    	clientType = WENETPrincipal.CLIENT_USER;
    	
    	// Extract info from certificate
		X509Certificate userCert;
		userCert = (X509Certificate) certsAsPath.getCertificates().get(0);
		certSubjectDN = userCert.getSubjectDN().getName();
		try {
			name = extractRFC822Name(userCert);
		} catch (CertificateParsingException e) {
			e.printStackTrace();
			throw new ServletException("Authentication failure, problem finding RFC822Name.");
		}
		
		// Check user against valid list
		if (!Arrays.asList(validUsers).contains(name)) {
			// Invalid user
			throw new ServletException("Authentication failure, not a valid user.");
		}
		
		// Set roles
		roles = defaultRoles;
		
		//Return new principal
		return new WENETPrincipal(name, roles, certSubjectDN, clientType);		
	}
    
    private String extractRFC822Name(X509Certificate cert) throws CertificateParsingException {
    	Collection names = null;
		names = cert.getSubjectAlternativeNames();
		Iterator iterator = names.iterator();
		// Loop until we find RFC822Name
		while (iterator.hasNext()){
			List namesList = (List)iterator.next();
			if (namesList != null && namesList.size() > 0){
				if (((Integer)namesList.get(0)).intValue() == 1){	
					return (String)namesList.get(1); 
				}
			} 
		}
		return null;
    }

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		super.init(fConfig);
		this.setRevocationValidation(false);
		validUsers = fConfig.getInitParameter("validUsers").split(";");
		defaultRoles = fConfig.getInitParameter("userRoles").split(";");
	}

}
