package us.wenet.jawjs;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Filter implementation class FilterWrapAsWENETrequest
 */
public class FilterWrapAsWENETrequest implements Filter {

    /**
     * Default constructor. 
     */
    public FilterWrapAsWENETrequest() {
        // No operation
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// No operation
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest wrappedRequest = new WENETRequest((HttpServletRequest)request);
		HttpServletResponse wrappedResponse = new WENETResponse((HttpServletResponse)response);
		// pass the request along the filter chain
		chain.doFilter(wrappedRequest, wrappedResponse);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		// No operation
	}

}
