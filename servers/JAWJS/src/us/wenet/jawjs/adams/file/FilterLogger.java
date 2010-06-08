package us.wenet.jawjs.adams.file;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import us.wenet.jawjs.adams.WENETPrincipal;
import us.wenet.jawjs.adams.WENETRequest;
import us.wenet.jawjs.adams.WENETResponse;

/**
 * Servlet Filter implementation class FilterLogger
 */
public class FilterLogger implements Filter {
	private SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-ddTHH:mm:ss");
	private SimpleDateFormat dff = new SimpleDateFormat("YYYYMMddHHmmssSSS");
	
	private String filePath;
	private boolean includeXMLHeader = true;

    /**
     * Default constructor. 
     */
    public FilterLogger() {
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
		
		// pass the request along the filter chain
		chain.doFilter(request, response);
		
		// Now that we have the response...
		WENETRequest req = (WENETRequest) request;
		//WENETResponse resp = (WENETResponse) response;
		WENETPrincipal user = (WENETPrincipal) req.getUserPrincipal();
		
		// Open file and writer
		String fileName = new String(filePath + File.separator + dff.format(req.getWhen()));
		int i = 0;
		// determine file name
		File fl = new File(fileName + "-" + String.valueOf(i));
		while (!fl.createNewFile()) {
			//Need a new name
			fl = new File(fileName +  "-" + String.valueOf(i++));
		}
		
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fl.getPath())));
		
		// Generate XML log entry using data from Request and Response	
		if (includeXMLHeader) {
			out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		}
		
		out.println("<logentry xmlns=\"http://www.co.whatcom.wa.us/apps/wenet/schema/wenet/1\">");
		out.println("<userRFC822name>" + req.getUserRFC822name() + "</userRFC822name>");
		out.println("<userCertSubject>" + user.getCertSubjectDN() + "</userCertSubject>");
		out.println("<when>" + df.format(req.getWhen()) + "</when>");
		out.println("<requestIPAddress>" + req.getRequestIPAddress() + "</requestIPAddress>");
		out.println("<requestURL>" + req.getRequestURL() + "</requestURL>");
		out.println("<methodName>" + req.getMethodName() + "</methodName>");
		out.println("<POSTdata>" + "</POSTdata>");
		out.println("</logentry>");
		out.flush();
		out.close();
		
		/*
	<xsd:element name="logentry">
		<xsd:complexType>
			<xsd:sequence>
				<xsd:element name="userRFC822name" type="xsd:string" minOccurs="1" maxOccurs="1"/>
				<xsd:element name="userCertSubject" type="xsd:string" minOccurs="1" maxOccurs="1"/>
				<xsd:element name="when" type="xsd:dateTime" minOccurs="1" maxOccurs="1"/>
				<xsd:element name="requestIPAddress" type="xsd:string" minOccurs="1" maxOccurs="1"/>
				<xsd:element name="requestURL" type="xsd:string" minOccurs="1" maxOccurs="1"/>
				<xsd:element name="methodName" type="xsd:string" minOccurs="1" maxOccurs="1"/>
				<xsd:element name="POSTdata" type="xsd:string" minOccurs="1" maxOccurs="1"/>
			</xsd:sequence>
		</xsd:complexType>
	</xsd:element>
		 */
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		filePath = fConfig.getInitParameter("file-path");
		if (fConfig.getInitParameter("includeXMLHeader").equalsIgnoreCase("false")) {
			includeXMLHeader = false;
		}
	}

}
