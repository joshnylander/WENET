package us.wenet.jawjs.adams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Servlet Filter implementation class FilterForwardByRole
 */
public class FilterForwardByRole implements Filter {
	
	private List roles;
	private String path;

    /**
     * Default constructor. 
     */
    public FilterForwardByRole() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
		// TODO Auto-generated method stub
	}

	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		boolean doForward = false;
		int i = 0;
		
		while (i < roles.size() && doForward == false) {
			doForward = req.isUserInRole((String) roles.get(i));
			i++;
		}

		if (doForward) {
			req.getRequestDispatcher(path).forward(request, response);
		} else {
			chain.doFilter(request, response);
		}
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
		roles = Arrays.asList(fConfig.getInitParameter("role-names").split(";"));
		path = fConfig.getInitParameter("resource");
	}

}
