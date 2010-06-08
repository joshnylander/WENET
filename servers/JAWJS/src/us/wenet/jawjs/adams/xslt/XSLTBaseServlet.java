package us.wenet.jawjs.adams.xslt;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathException;

import net.sf.saxon.value.StringValue;

public class XSLTBaseServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4677467554791061651L;
	
	/**
	* Clear the cache. Useful if stylesheets have been modified, or simply if space is
	* running low. We let the garbage collector do the work.
	*/
	private int cacheSize = 1;
	private HashMap cache = new HashMap(cacheSize);
	
	public void init(ServletConfig sConfig) {
		System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
	}

	protected void cleaCache() {
		clearCache(cacheSize);
	}
	
	protected void clearCache(int size) {
		cache = new HashMap(size);
	}

	/**
	* Apply stylesheet to source document
	*/
	protected void apply(String style, String source, HttpServletRequest req, HttpServletResponse res)
			throws java.io.IOException {
			
			    ServletOutputStream out = res.getOutputStream();
			
			    if (style==null) {
			        out.println("No style parameter supplied");
			        return;
			    }
			    if (source==null) {
			        out.println("No source parameter supplied");
			        return;
			    }
			    try {
			        Templates pss = tryCache(style);
			        Transformer transformer = pss.newTransformer();
			
			        String mime = pss.getOutputProperties().getProperty(OutputKeys.MEDIA_TYPE);
			        if (mime==null) {
			           // guess
			            res.setContentType("text/html");
			        } else {
			            res.setContentType(mime);
			        }
			
			        Enumeration p = req.getParameterNames();
			        while (p.hasMoreElements()) {
			            String name = (String)p.nextElement();
			            if (!(name.equals("style") || name.equals("source"))) {
			                String value = req.getParameter(name);
			                transformer.setParameter(name, new StringValue(value));
			            }
			        }
			
			        String path = getServletContext().getRealPath(source);
			        if (path==null) {
			            throw new XPathException("Source file " + source + " not found");
			        }
			        File sourceFile = new File(path);
			        transformer.transform(new StreamSource(sourceFile), new StreamResult(out));
			    } catch (Exception err) {
			        out.println(err.getMessage());
			        err.printStackTrace();
			    }
			
			}

	/**
	* Maintain prepared stylesheet(s) in memory for reuse
	*/
	private synchronized Templates tryCache(String url)
			throws TransformerException, XPathException {
			    String path = getServletContext().getRealPath(url);
			    if (path==null) {
			        throw new XPathException("Stylesheet " + url + " not found");
			    }
			
			    Templates x = (Templates)cache.get(path);
			    if (x==null) {
			        TransformerFactory factory = TransformerFactory.newInstance();
			        x = factory.newTemplates(new StreamSource(new File(path)));
			        cache.put(path, x);
			    }
			    return x;
			}

	public XSLTBaseServlet() {
		super();
	}

}