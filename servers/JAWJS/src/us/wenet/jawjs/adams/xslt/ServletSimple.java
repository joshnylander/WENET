package us.wenet.jawjs.adams.xslt;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ServletSimple
 */
public class ServletSimple extends XSLTBaseServlet {
	private static final long serialVersionUID = 1L;
	String pathXML;
	String pathXSLT;
	       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServletSimple() {
        super();
        // Nothing to do here
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Get XSLT processor, process XML with XSLT and return result
		apply(pathXSLT, pathXML, request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void init(ServletConfig sConfig) {
		super.init(sConfig);
		pathXML = sConfig.getInitParameter("path-xml");
		pathXSLT = sConfig.getInitParameter("path-xslt");
	}	
}
