package us.wenet.jawjs.adams;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStore.LoadStoreParameter;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Servlet Filter implementation class FilterSSLSecurity
 */
public class FilterSSLSecurity implements Filter {
	
	private KeyStore trustStore;

    /**
     * Default constructor. 
     */
    public FilterSSLSecurity() {
        // No operation
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// TODO close keystore
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		// TODO Auto-generated method stub
		// place your code here
		
		try {
			X509Certificate[] certs;
			certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
			if (certs == null){
				throw new ServletException("Authentication failure, no certificate chain found.");	
			}
			if(certs.length<2) throw new ServletException("Certificate chain length invalid (length=" + certs.length + ").");
			//CertStore cs = CertStore.getInstance("Collection", new CollectionCertStoreParameters(Arrays.asList(certs)));
			X509CertSelector cs = new X509CertSelector();
			PKIXBuilderParameters cpp = new PKIXBuilderParameters(trustStore, cs);
			CertPath cp = CertPathBuilder.getInstance(CertPathBuilder.getDefaultType()).build(cpp).getCertPath();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertPathBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		
		/*
		 * certs = (Object[]) req.getAttribute("javax.servlet.request.X509Certificate");
		if (certs == null){
			throw new AuthenticationException("Authentication failure, no certificate chain found.");	
		}
		if(certs.length<2) throw new AuthenticationException("Certificate chain length " +
				"invalid (length=" + certs.length + ").");
		//We have a chain, so let's make sure we are using the proper class, i.e.
		//java.security.cert.X509Certificate 
		X509Certificate[] x509Certs = new X509Certificate[certs.length];
		for(int i = 0; i < certs.length; i++){
			if (certs[i] instanceof java.security.cert.X509Certificate){
				x509Certs[i] = (java.security.cert.X509Certificate) certs[i];
			}else if(certs[i] instanceof javax.security.cert.X509Certificate){	
				x509Certs[i] = convertCert((javax.security.cert.X509Certificate) certs[i]);
			}else{	
				throw new AuthenticationException("Authentication failure, unknown certificate type.");
			}
		}
		//store the subject DN for both SP and user authentication
		user.put("dn", x509Certs[0].getSubjectDN().toString());
		// Get the RFC822Name from the first cert in the chain.  
		if (certificateType == USER_CERTIFICATE){
			Collection names = null;
			try {
				names = x509Certs[0].getSubjectAlternativeNames();
			} catch (CertificateParsingException cpe){
				throw new AuthenticationException("Authentication failure, couldn't " +
													"get subject alternative names.", cpe);
			}
			System.out.print("<))  Alternative Names:");
			Iterator iterator = names.iterator();
			// Loop until we find RFC822Name
			while (iterator.hasNext()){
				List namesList = (List)iterator.next();
				if (namesList != null && namesList.size() > 0){
					if (((Integer)namesList.get(0)).intValue() == 1){	
						rfc822Name = (String)namesList.get(1); 
						user.put("name", rfc822Name);
						if (debug) System.out.println("\tRFC822Name = '" + rfc822Name + "'");
						break;
					}
				} 
			}
		}
		 */

		// pass the request along the filter chain
		chain.doFilter(request, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		// Open keystore
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
