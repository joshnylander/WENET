package us.wenet.jawjs.adams;

import java.io.IOException;
import java.security.cert.CertPath;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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

    private void buildPrincipal(CertPath certsAsPath, WENETPrincipal retPrincipal) throws ServletException{
    	// This is creating a user principal
    	retPrincipal.clientType = WENETPrincipal.CLIENT_USER;
    	
    	// Extract info from certificate
		X509Certificate userCert;
		userCert = (X509Certificate) certsAsPath.getCertificates().get(0);
		retPrincipal.certSubjectDN = userCert.getSubjectDN().getName();
		try {
			retPrincipal.name = extractRFC822Name(userCert);
		} catch (CertificateParsingException e) {
			e.printStackTrace();
			throw new ServletException("Authentication failure, problem finding RFC822Name.");
		}
		
		// Check user against valid list
		if (!Arrays.asList(validUsers).contains(retPrincipal.getName())) {
			// Invalid user
			throw new ServletException("Authentication failure, not a valid user.");
		}
		
		// Set roles
		retPrincipal.roles = defaultRoles;
		
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
