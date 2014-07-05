package dinamica.security;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.*;
import javax.servlet.http.*;
import dinamica.*;
import dinamica.security.DinamicaUser;
import dinamica.security.RequestWrapper;
import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;

/**
 * Servlet Filter to enforce Dinamica
 * server-side security. This module
 * checks/enforce authentication and autorization
 * over protected resources, requires special configuration
 * via filter parameters and the existence of a database schema
 * containing the tables defined by Dinamica framework for
 * the security system. It also assumes the existence of
 * a set of generic Actions used for security tasks (login, loginerror, etc).
 * This filter mantains API level compatibity with Servlet J2EE
 * security methods, like getUserName, isUserInRole, etc. 
 * 
 * <br>
 * Creation date: 10/march/2004<br>
 * Last Update: 10/march/2004<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova - dinamica@martincordova.com
 *
 */

public class SecurityFilter implements Filter
{

    //filter configuration object
    private FilterConfig _config   = null;

	//filter parameters
	String _dataSource = null;
	String _ssl = null;
	String _loginForm = null;
	DataSource _ds = null;
	String _appAlias = null;
	String _passwordPolicy = null;
	boolean _debug = false;
	
	//cache to store protected services and its authorized roles
	HashMap<String, String[]> protectedRes = new HashMap<String, String[]>();

    /**
    * Init filter
    **/
    public void init(FilterConfig config) throws ServletException
    {

        _config = config;
        String contextName = _config.getServletContext().getServletContextName();
        
		try
		{
        
	        //get filter config parameters
	        _dataSource = _config.getInitParameter("datasource");
			_loginForm = _config.getInitParameter("loginform");
			_ssl = _config.getInitParameter("ssl");
			_appAlias = _config.getInitParameter("app-alias");
			String debug = _config.getInitParameter("debug");
	
			if (debug!=null && debug.equals("true"))
				_debug = true;
			
			// get prefix for jndi lookups
			String _jndiPrefix = config.getServletContext().getInitParameter("jndi-prefix");
			if (_jndiPrefix==null)
				_jndiPrefix = "";

			String jndiName = _jndiPrefix + _dataSource;
			
			//get filter datasource
			_ds = Jndi.getDataSource(jndiName);
			
			//init security cache (protected resources)	
			loadProtectedResources();
			
			//save datasource JNDI name as a context attribute
			//to be used by other modules
			_config.getServletContext().setAttribute("dinamica.security.datasource", jndiName);
			
			_config.getServletContext().log("[Dinamica] SecurityFilter started for context: " + contextName);

		}
		catch (Throwable e)
		{
			_config.getServletContext().log("[Dinamica] SecurityFilter FAILED for context: " + contextName);
			throw new ServletException(e);
		}	

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

		//flag used to indicate if the request can proceed
		boolean isOK = false;

		//get http request/response
        HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse)response;

		//get path - is this an include?
		String uri = (String)req.getAttribute("javax.servlet.include.request_uri");
		if (uri==null)
			uri = req.getRequestURI();		
		
		String context = req.getContextPath(); //patch 2006-01-12
		
		URI uri2;
		try {
			uri2 = new URI(uri);
		} catch (URISyntaxException e1) {
			throw new ServletException(e1);
		}
		
        uri = uri2.normalize().toString();
		uri = uri.substring(context.length());

		HttpSession s = req.getSession(true);
		
		if ( _debug )
		{
			debug(uri, "Intercepting request...");
			debug(uri, "Session Cookie: " + s.getId());
		}
		
		//get authenticated principal
		DinamicaUser user = (DinamicaUser)s.getAttribute("dinamica.security.login");
		
		//create request wrapper
		RequestWrapper rw = new RequestWrapper(req);
		rw.setUserPrincipal(user);

		//set default attributes related to security settings
		rw.setAttribute("dinamica.security.application", _appAlias);
		rw.setAttribute("dinamica.security.passpolicy", _passwordPolicy);

		//requires SSL?
		if (_ssl.equals("true") && !req.isSecure())
		{
			//PATCH 2006-11-08 - log not authenticated access
			if ( _debug )
				debug(uri, "Request rejected! - SSL required.");
			
			//not an ssl request - reject it
			res.sendError(401,"Requires secure (HTTPS) access.");
			return;
		}

        try
        {

			//is protected?
			if (protectedRes.containsKey(uri))
			{

				//get authorized roles
				String roles[] = (String[])protectedRes.get(uri);

				//authenticated? can't access protected 
				//resource without proper authentication
				if (user==null)
				{

					//PATCH 2006-11-08 - log not authenticated access
					if ( _debug )
						debug(uri, "Request not authenticated! - redirecting to login page.");
					
					
					//redirect to login page
					String loginPage = context + _loginForm;
					res.sendRedirect(loginPage);
					return; //patch 2006-02-17 - failed with Tomcat
				}
				else
				{

					//2005-06-09 - debug to stderr
				    if ( _debug )
					{
					    debug(uri, roles, user);
					}
				    
				    //authorized?
					for (int i=0; i<roles.length; i++)
					{
						//check user roles
						if (rw.isUserInRole(roles[i]))
						{
							//OK - authorized
							isOK = true;
							break;
						}
					}
				}
				
			}
			else
			{

				//2006-02-16 - debug to stderr
			    if ( _debug )
				{
				    debug(uri, user);
				}				
				
				//not a protected resource - let it pass
				isOK = true;
			}
			
			if (isOK)
            	next.doFilter(rw, response);
            else
            	res.sendError(403,"NOT AUTHORIZED");

        }

        catch (Throwable e)
        {
            throw new ServletException(e);
        }

    }

	/**
	 * Load into cache the list of protected
	 * resources and authorized roles for each resource
	 * @throws Throwable
	 */
	void loadProtectedResources() throws Throwable
	{

		TemplateEngine t = new TemplateEngine(_config.getServletContext(), null, "");;
		Connection con = null;
		try
		{
		
			String sql = "";
			String clearSessions = _config.getInitParameter("clear-sessions");
			
			con = _ds.getConnection();
			Db db = new Db(con);
			
			//limpiar tabla s_session (sesiones activas) por si acaso se quedaron registros huerfanos
			if (clearSessions!=null && clearSessions.equalsIgnoreCase("true")) {
				sql = "delete from ${schema}s_session where context_alias = '" + _appAlias + "'";
				t.setTemplate(sql);
				sql = t.getSql(null);
				db.exec(sql);
			}
 
			
			//get default password expiration policy for this webapp
			String sqlpass = "select pwd_policy from ${schema}s_application where app_alias = '" + _appAlias + "'";
			t.setTemplate(sqlpass);
			sqlpass = t.getSql(null);
			
			Recordset rspass = db.get(sqlpass);
			
			if (rspass.getRecordCount()==0)
			    throw new Throwable("Security configuration not found for this application alias: " + _appAlias + " - please check your web.xml configuration regarding the security filter.");
			
			rspass.next();
			_passwordPolicy = rspass.getString("pwd_policy");
			
			//get list of protected resource for this app-alias
			sql = "select service_id, path" 
			+ " from ${schema}s_service s, ${schema}s_application a"
			+ " where s.app_id = a.app_id"
			+ " and a.app_alias = '" + _appAlias + "'";

			t.setTemplate(sql);
			sql = t.getSql(null);
			
			Recordset rs = db.get(sql);
					
			//get authorized roles for each resource
			String query = "select r.rolename from"
							+ " ${schema}s_service_role sr, ${schema}s_role r"
							+ " where sr.role_id = r.role_id"
							+ " and sr.service_id = ";

			t.setTemplate(query);
			query = t.getSql(null);
			
			while (rs.next())
			{

				sql = query + rs.getString("service_id");
				
				Recordset rs2 = db.get(sql);
				
				//store rolenames into array
				String roles[] = new String[rs2.getRecordCount()];
				int i = 0;
				while (rs2.next())
				{
					roles[i] = rs2.getString("rolename");
					i++;
				}
				
				//save resource + roles array into hashmap
				protectedRes.put(rs.getString("path"), roles);
			
			}
		} 
		catch (Throwable error)
		{
			throw error;
		} 
		finally
		{
			if (con!=null) con.close();
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
	 * Print debug information to stderr  about resource access authorization
	 * @param uri Action being checked for authorization
	 * @param uriRoles Autorized roles
	 * @param user Current authenticated user
	 */
	void debug(String uri, String[] uriRoles, DinamicaUser user)
	{
	    try
        {
            StringBuilder buf = new StringBuilder();
            buf.append("[Dinamica_DEBUG_SecurityFilter] ");
            buf.append("URI (" + uri + ") ");
            buf.append("Authorized Roles (");
			for (int i=0; i<uriRoles.length; i++)
			{
			    buf.append(uriRoles[i]+";");
			}
            buf.append(") ");
            buf.append("USER (" + user.getName() + ") ");
            buf.append("User Roles (");
            String userRoles[] = user.getRoles();
			for (int i=0; i<userRoles.length; i++)
			{
			    buf.append(userRoles[i]+";");
			}
            buf.append(") ");
            
            _config.getServletContext().log(buf.toString());
            
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
	}

	/**
	 * Print debug information to stderr about access to public (non-protected) resources
	 * @param uri Action being checked for authorization
	 * @param user Current authenticated user
	 */
	void debug(String uri, DinamicaUser user)
	{
	    try
        {
            StringBuilder buf = new StringBuilder();
            buf.append("[Dinamica_DEBUG_SecurityFilter] ");
            buf.append("PUBLIC URI (" + uri + ") ");

            if (user!=null)
            {
	            buf.append("USER (" + user.getName() + ") ");
	            buf.append("User Roles (");
	            String userRoles[] = user.getRoles();
				for (int i=0; i<userRoles.length; i++)
				{
				    buf.append(userRoles[i]+";");
				}
	            buf.append(") ");
            }
            _config.getServletContext().log(buf.toString());
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
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
            buf.append("[Dinamica_DEBUG_SecurityFilter] ");
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