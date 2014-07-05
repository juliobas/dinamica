/*
 * File upload filter
 * Copyright (C) 2004 Rick Knowles
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package dinamica.upload;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Checks the content type, and wraps the request in a MultipartRequestWrapper if
 * it's a multipart request.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: MultipartRequestFilter.java,v 1.1 2005/08/24 06:43:34 rickknowles Exp $
 */
public class MultipartRequestFilter implements Filter {

	boolean uploadProgress = false;
	
    public void init(FilterConfig config) throws ServletException {
    	String v = config.getServletContext().getInitParameter("upload-progress");
    	if (v!=null && v.equals("true"))
    		uploadProgress = true;
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
    {
    	
    	String contentType = request.getContentType(); 
    	if ((contentType != null) && contentType.startsWith("multipart/form-data")) {
    		try {
    			MultipartRequestWrapper req = new MultipartRequestWrapper(request, uploadProgress);
				chain.doFilter(req, response);
			} catch (OutOfMemoryError e) {
        			throw new ServletException("El servidor no tiene suficiente memoria para completar su solicitud.", e);
        	}
    	}
    	else {
            chain.doFilter(request, response);
        }                
    }
    
}
