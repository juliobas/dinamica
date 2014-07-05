package dinamica.security;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;


/**
 * Este filtro bloquea solicitudes a los Actions que 
 * se definan en sus filter-mappings, usando una lista
 * de direcciones IP que son las únicas autorizadas
 * para ejecutar estos Actions. Si se bloque un request
 * el mismo es rechazado con un código HTTP 403.
 * <br>
 * 2009-08-31<br>
 * (c) 2009 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova - dinamica@martincordova.com
 *
 */

public class AllowHosts implements Filter
{

    //filter configuration object
    private FilterConfig _config   = null;

	//filter parameters
	String _allowHosts = null;
	boolean _debug = false;
	
    /**
    * Init filter
    **/
    public void init(FilterConfig config) throws ServletException
    {

    	_config = config;

        //get filter config parameters
		_allowHosts = _config.getInitParameter("allow-hosts");

		String debug = _config.getInitParameter("debug");
		
		if (debug!=null && debug.equals("true"))
			_debug = true;

    }


    /**
    * Intercept request
    */
    public void doFilter (
                            ServletRequest request,
                            ServletResponse response,
                            FilterChain next
                         )
    throws IOException, ServletException

    {


		//get http request/response
        HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse)response;

		//get path
		String ip = req.getRemoteAddr();

		if ( _debug )
		{
			debug(req.getRequestURI(), "Intercepting request from: " + ip);
		}

		//requires SSL?
		if (_allowHosts.indexOf(ip)<0)
		{
			if ( _debug )
				debug(req.getRequestURI(), "Request rejected! host blocked -> " + ip);
			
			res.sendError(403,"Host no autorizado.");
			return;
		}

        try
        {
			if ( _debug )
				debug(req.getRequestURI(), "Authorized request from -> " + ip);
        	next.doFilter(request, response);
        }
        catch (Throwable e)
        {
            throw new ServletException(e);
        }

    }


	/* (non-Javadoc)
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy()
	{
		
		_config = null;

	}

	/**
	 * Print debug information to stderr about resource access
	 * @param uri Action being checked for authorization
	 * @param msg Custom debug message
	 */
	void debug(String uri, String msg)
	{
	    try
        {
            StringBuilder buf = new StringBuilder();
            buf.append("[Dinamica_DEBUG_AllowHostFilter] ");
            buf.append("URI (" + uri + ") ");
            buf.append(msg);
            _config.getServletContext().log(buf.toString());
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
	}		
	
}