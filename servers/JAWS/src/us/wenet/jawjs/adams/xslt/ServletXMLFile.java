package us.wenet.jawjs.adams.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class ServletXMLFile
 */
public class ServletXMLFile extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private String filename;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ServletXMLFile() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Get the absolute path of the image
        ServletContext sc = getServletContext();
    
        // Get the MIME type of the image
        String mimeType = sc.getMimeType(filename);
        if (mimeType == null) {
            sc.log("Could not get MIME type of "+filename);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    
        // Set content type
        response.setContentType(mimeType);
    
        // Set content size
        File file = new File(filename);
        response.setContentLength((int)file.length());
    
        // Open the file and output streams
        FileInputStream in = new FileInputStream(file);
        OutputStream out = response.getOutputStream();
    
        // Copy the contents of the file to the output stream
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            out.write(buf, 0, count);
        }
        in.close();
        out.close();
	}
	
	public void init(ServletConfig sConfig) {
		try {
			super.init(sConfig);
			filename = sConfig.getInitParameter("path-xml");
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}

}
