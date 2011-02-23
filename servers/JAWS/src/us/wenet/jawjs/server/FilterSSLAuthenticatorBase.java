package us.wenet.jawjs.server;

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Servlet Filter implementation class FilterSSLAuthenticatorBase
 */
public class FilterSSLAuthenticatorBase implements Filter {

    private KeyStore trustStore;
    private boolean revocationValidation = false;

	public void setRevocationValidation(boolean revocationValidation) {
		this.revocationValidation = revocationValidation;
	}

	/**
     * Default constructor. 
     */
    public FilterSSLAuthenticatorBase() {
        // do nothing
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// do nothing
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// Establish WENETRequest object
		WENETRequest req = (WENETRequest) request;
		
		// Does the principal already exist?
		if (req.getUserPrincipal() == null) {
			// No, need to build a new principal

			// Get certificates from request
			X509Certificate[] certs;
			certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

			// Parse and validate certificates
			CertPath cp = buildCertChain(certs, revocationValidation);
			
			req.setUserPrincipal(this.buildPrincipal(cp));
		}


		// pass the request along the filter chain
		chain.doFilter(request, response);
	}
	
	private WENETPrincipal buildPrincipal(CertPath certsAsPath) throws ServletException {
		// This method must be overridden and implemented by subclasses
		return new WENETPrincipal();
	}
	
	private CertPath buildCertChain(X509Certificate[] certs, boolean checkRevocation) throws ServletException {
		try {
			// Do we even have certificates?
			if (certs == null){
				throw new ServletException("Authentication failure, no certificate chain found.");	
			}
			
			// Are there enough of them?
			if(certs.length<2) throw new ServletException("Certificate chain length invalid (length=" + certs.length + ").");

			// Build path builder parameters
			PKIXParameters cpv = new PKIXParameters(trustStore);
			cpv.setDate(new Date());
			cpv.setRevocationEnabled(checkRevocation);

			// Build certificate factory
			CertificateFactory factory = CertificateFactory.getInstance("X509");

			// Build path
			CertPath cp = factory.generateCertPath(Arrays.asList(certs));
			
			// Validate path
			CertPathValidator pathValidator = CertPathValidator.getInstance("PKIX");
			pathValidator.validate(cp, cpv);

			return cp;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException("Authentication failure, error parsing certificates, see stack trace.");
		}
	}
	
	protected String extractRFC822Name(X509Certificate cert) throws CertificateParsingException {
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
		try {
			trustStore = KeyStore.getInstance("JKS");
			// get user password and file input stream
		    char[] password = fConfig.getInitParameter("clientTruststorePassword").toCharArray();
		    
		    //Load from file system
		    java.io.FileInputStream fis = null;
	        fis = new java.io.FileInputStream(fConfig.getInitParameter("clientTruststore"));
	        trustStore.load(fis, password);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException("Unable to open trust keystore.");
		}	
	}

}
