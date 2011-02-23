/**
 * 
 */
package us.wenet.jawjs.server;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * @author jnylander
 *
 */
public class WENETResponse extends HttpServletResponseWrapper {

	/**
	 * @param response
	 */
	public WENETResponse(HttpServletResponse response) {
		super(response);
	}

}
