package dinamica;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * 
 * Servlet filter to generate a performance log
 * according to the configured filter map and 
 * the init parameter "limit". A value of 0 will
 * log all requests, a value > 0 will log only those request
 * whose execution time takes more than "limit" milliseconds.
 * 
 * <br>
 * Creation date: 5/jan/2004<br>
 * Last Update: 5/jan/2004<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 *
 */

public class PerformanceFilter implements Filter
{

    //filter configuration object
    private FilterConfig _config   = null;

	//log threshold (log requests that take more than N milliseconds)
	private int _limit = 0;
	
	//ruta completa del archivo para almacenar la traza de performance
	String _file = null;

    /**
    * init filter
    **/
    public void init(FilterConfig config) throws ServletException
    {

        _config = config;
        
        //load configuration
       	_limit = Integer.parseInt(_config.getInitParameter("limit"));
        _file = _config.getInitParameter("path");

    }

    /**
    * clean up
    **/
    public void destroy()
    {
           _config = null;
    }

    /**
    * Filter main method
    */
    public void doFilter (
                            ServletRequest request,
                            ServletResponse response,
                            FilterChain next
                         )

    throws IOException, ServletException

    {

        long t1 = 0;
        long t2 = 0;

        t1 = System.currentTimeMillis();

        HttpServletRequest req = (HttpServletRequest) request;

        //measure performance
        try
        {

            t1 = System.currentTimeMillis();
            next.doFilter(request, response);
            t2 = System.currentTimeMillis();
			long tt = t2-t1;

            //save log if necessary
			if (tt >= _limit)
			{
				saveLog(req, tt);				
			}

        }

        catch (Throwable e)
        {
            throw new ServletException(e);
        }

    }

	/**
	 * Save performance record to log file defined in web.xml
	 * @param req HTTP Servlet Request to extract useful information
	 * @param t Elapsed time in milliseconds
	 * @throws Throwable
	 */
	void saveLog(HttpServletRequest req, long t) throws Throwable
	{
		
		/* is this an include? */
		String uri = (String)req.getAttribute("javax.servlet.include.request_uri");
		if (uri==null)
			uri = req.getRequestURI();
		
		String d = StringUtil.formatDate(new java.util.Date(), "yyyy-MM-dd");
		String time = StringUtil.formatDate(new java.util.Date(), "HH:mm:ss");
		String tt = String.valueOf(t);
		String ip = req.getRemoteAddr();
		
		String msg = d + "\t" + time + "\t" + uri + "\t" + Thread.currentThread().getName() + "\t" + ip + "\t" + tt;
				
		StringUtil.saveMessage(_file, msg);
		
	}

}