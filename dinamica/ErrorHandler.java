package dinamica;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Transaction module for the general error manager
 * 
 * <br>
 * Creation date: jan/13/2004<br>
 * Last Update: 2009-09-01<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class ErrorHandler extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{

		//capture referer page
		String referer = getRequest().getHeader("Referer");
		if (referer==null)
			referer = "[No disponible]";
		
		getRequest().setAttribute("dinamica.error.referer", referer);
		getRequest().setAttribute("dinamica.error.context", getRequest().getContextPath());
		
	    String key = "dinamica.error.exception";
		Throwable err = (Throwable)getRequest().getAttribute(key);
	    String errUri = (String)getRequest().getAttribute("dinamica.error.url");

	    //error triggered by servlet distinct from Controller or maybe a filter?
	    if (err==null) {
			err = (Throwable)getRequest().getAttribute("javax.servlet.error.exception");
			getRequest().setAttribute("dinamica.error.description", err.getMessage());
	    }
		
		//default log to container
		if (err!=null)
		
			getContext().log("[Dinamica_Exception] " + err.getMessage() 
		    		+ " context: " + getRequest().getContextPath() 
		    		+ " uri:" + errUri 
		    		+ " referer:" + referer);

		    		
		try
		{

			super.service(inputParams);
			
			// capture stack trace for exceptions
			// raised by non-Dinamica Actions
			String trace = (String)getRequest().getAttribute("dinamica.error.stacktrace");
			if (trace==null)
			{
				//this exception was not raised by dinamica.Controller
				//use standard J2EE request attributes
				if (err!=null)
				{			
					//get stack trace
					StringWriter s = new StringWriter();
					err.printStackTrace(new PrintWriter(s));
					getRequest().setAttribute("dinamica.error.stacktrace", s.toString());
				}
					
			}

		}
		catch (Throwable e)
		{
			//log to container if the error handler fails
			getContext().log("[Dinamica_Warning] ErrorHandler failed: " + e.getMessage());
			throw e;
		}

		return 0;
		
	}

}
