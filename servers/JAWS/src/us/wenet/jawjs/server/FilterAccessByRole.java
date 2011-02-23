package us.wenet.jawjs.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Filter implementation class FilterAccessByRole
 */
public class FilterAccessByRole implements Filter {

	private List roles;

    /**
     * Default constructor. 
     */
    public FilterAccessByRole() {
        // nothing to do here
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// nothing to do here
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		boolean allAccess = false;
		int i = 0;
		
		while (i < roles.size() && allAccess == false) {
			allAccess = req.isUserInRole((String) roles.get(i));
			i++;
		}

		if (allAccess) {
			chain.doFilter(request, response);
		} else {
			resp.sendError(401);
		}
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		roles = Arrays.asList(fConfig.getInitParameter("role-names").split(";"));
	}

}
