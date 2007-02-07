package us.wa.whatcom.co.wenet.client.toolkit;

import javax.net.ssl.*; 

/** This class is the WENETJavaToolkit host name verification override. It is necessary to fool our client side
 * SSL into not worrying that the host name attached to the incoming certificate is not the host we are connecting to.
 */

public class Verifier implements HostnameVerifier 
{ 
    public Verifier() { } 

    public boolean verify(String hostname, SSLSession session) { 
        return true; 
    	} 
} 