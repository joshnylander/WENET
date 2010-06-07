package us.wenet.jawjs.adams;

import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;

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
		// TODO clean up cache
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
			// TODO Add cache support
			
			// No, then start to build principal
			WENETPrincipal regPrincipal = new WENETPrincipal();

			// Get certificates from request
			X509Certificate[] certs;
			certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

			// Parse and validate certificates
			CertPath cp = buildCertChain(certs, revocationValidation);
			
			this.buildPrincipal(cp, regPrincipal);
			
			// TODO Add cache support
		}


		// pass the request along the filter chain
		chain.doFilter(request, response);
	}
	
	private void buildPrincipal(CertPath certsAsPath, WENETPrincipal retPrincipal) throws ServletException {
		// This method must be overridden and implemented by subclasses
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
