package dinamica;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import java.sql.Connection;

/**
 * Base class that factorizes reusable behavior
 * for Output and Transaction classes
 * <br>
 * Creation date: 4/10/2003<br>
 * Last Update: 4/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public abstract class AbstractModule
{

	ServletContext 		_ctx = null;
	HttpServletRequest 	_req = null;
	HttpServletResponse	_res = null;
	Connection 			_conn = null;
	Config 				_config = null;
	PrintWriter			_pw = null;
	

	/**
	 * Initialize this object
	 * @param ctx
	 * @param req
	 */
	public void init(ServletContext ctx, HttpServletRequest req, HttpServletResponse res)
	{
		_ctx = ctx;
		_req = req;
		_res = res;
	}

	/**
	 * Set log writer for Db object (to log jdbc operations)
	 * @param pw
	 */
	public void setLogWriter(PrintWriter pw)
	{
		_pw = pw;
	}

	/**
	 * Return framework Db object (wrapper for JDBC operations)
	 * using the connection and the LogWriter passed to this object
	 * @return Db object
	 * @throws Throwable
	 */
	protected Db getDb() throws Throwable
	{
		
		if (_conn==null)
			throw new Throwable("Connection object is null!"); 
		
		Db db = new Db(_conn);
		if (_pw!=null)
			db.setLogWriter(_pw);
		return db;
		
	}

	/**
	 * Set transaction configuration
	 * @param config
	 */
	public void setConfig(Config config)
	{
		_config = config;
	}

	/**
	 * Set database connection
	 * @param connection
	 */
	public void setConnection(Connection connection)
	{
		_conn = connection;
	}

	/**
	 * Return UserID for the current security session, if any,
	 * otherwise returns null
	 * @return
	 */
	protected String getUserName()
	{
		return _req.getRemoteUser();
	}

	/**
	 * Returns true if user belongs to role
	 * @param roleName Name of the role as defined in the security layer
	 * @return
	 */
	protected boolean isUserInRole(String roleName)
	{
		return _req.isUserInRole(roleName);
	}

	/**
	 * Return HTTP Session, force session creation if necessary
	 * @return HttpSession reference 
	 */
	protected HttpSession getSession()
	{
		HttpSession s = _req.getSession(true);
		return s;
	}

	/**
	 * The application uses a default DataSource.
	 * A connection from this pool is made available 
	 * for Transaction modules
	 * @return Default Database Connection
	 */
	protected Connection getConnection()
	{
		return _conn;
	}

	/**
	 * @return Servlet Context
	 */
	protected ServletContext getContext()
	{
		return _ctx;
	}

	/**
	 * @return Servlet Request object
	 */
	protected HttpServletRequest getRequest()
	{
		return _req;
	}

	/**
	 * @return Servlet Response object
	 */
	protected HttpServletResponse getResponse()
	{
		return _res;
	}

	/**
	 * 
	 * @return Config object
	 */
	protected Config getConfig()
	{
		return _config;
	}

	/**
	 * Write message to the log writer
	 * @param message Message to log
	 */
	protected void log(String message)
	{
		_pw.println(message);
	}

	/**
	 * Load a text resource from the Action path
	 * (where config.xml is located) or from a relative
	 * path inside the context. The resource may be
	 * a SQL or HTML template, any text based file.
	 * @param fileName Resource file name; if starts with "/" then
	 * it is interpreted as a path relative to the context, otherwise
	 * the Action's path is used. 
	 * @return A String containing the resource
	 * @throws Throwable
	 */
	public String getResource(String fileName) throws Throwable
	{
		
		//ALL CODE PATCHED 2005-02-18 - encoding support
		
		String path = null;
		
		//relative to the context?
		if (fileName.startsWith("/"))
		{
			path = fileName;
			
			//PATCH 2005-08-31 support for ${def:actionroot} on config.xml template element
			String actionPath = (String)_req.getAttribute("dinamica.action.path");
			actionPath = actionPath.substring(0, actionPath.lastIndexOf("/"));
			path = StringUtil.replace(path, "${def:actionroot}", actionPath);
			//END PATCH
		}
		else
		{
			path = _config.path + fileName;
		}
		
		//global encoding?
		String encoding = getContext().getInitParameter("file-encoding");
		if (encoding!=null && encoding.trim().equals(""))
			encoding = null;
		
		//load resource with appropiate encoding if defined
		if (_config.templateEncoding!=null)
			return StringUtil.getResource(_ctx, path, _config.templateEncoding);
		else if (encoding!=null)
			return StringUtil.getResource(_ctx, path, encoding);
		else
			return StringUtil.getResource(_ctx, path);
		
	}

	/**
	 * Load a text resource relative to the class location
	 * @param path Pathname of the resource, if it is a filename
	 * then the resource location is assumed in the same path as the class
	 * otherwise may be a sybdirectory relative to the class directory.
	 * @return A String containing the resource
	 * @throws Throwable
	 */
	protected String getLocalResource(String path) throws Throwable
	{

		StringBuffer buf = new StringBuffer(5000);
		byte[] data = new byte[5000];

		InputStream in = null;
		
		in = this.getClass().getResourceAsStream(path);
        
		try
		{
			if (in!=null)
			{
				while (true)
				{
					int len = in.read(data);
					if (len!=-1)
					{
						buf.append(new String(data,0,len));
					}
					else
					{
						break;
					}
				}
				return buf.toString();
			}
			else
			{
				throw new Throwable("Invalid path to resource: " + path);
			}
            
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (in!=null)
			{
				try{
					in.close();
				} catch (Exception e){}
			}
		}
				
	}

	/**
	 * Retorna el valor de un cookie en el request
	 * @param req Request del servlet
	 * @param cookieName Nombre del cookie
	 * @return Valor del cookie o NULL si no consigue el cookie en el request
	 */
	protected String getCookieValue(HttpServletRequest req, String cookieName)
	{
		String value = null;
		Cookie c[] = req.getCookies();
		if (c!=null)
		{
			for (int i=0;i<c.length;i++)
			{
				if (c[i].getName().equalsIgnoreCase(cookieName))
				{
					value = c[i].getValue();
					break;
				}
			}
		}
		return value;
	}

	/**
	 * Return byte array with the content of a remote binary resource
	 * accessed via HTTP(S).
	 * @param url URL of the resource
	 * @param sessionID Session ID
	 * @param deprecated. Use instead context-parameter "httpclient-debug"
	 * @return Byte array with the content of the file
	 * @throws Throwable In case of any HTTP error of if the data cannot
	 * be read for any reason.
	 */
	protected byte[] getImage(String url, String sessionID, boolean debug) throws Throwable
	{
		
		//print trace to STDERR?
		debug = false;
		String useLog = getContext().getInitParameter("httpclient-debug");
		if (useLog!=null && useLog.equals("true"))
			debug = true;
		
		HttpURLConnection urlc = null;
		BufferedInputStream bin = null;
		final int bufferSize = 10240;
		byte[] buffer = null;
		URL page = new URL(url); 
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
			
		if (debug)
			System.err.println("[httpclient] Waiting for reply...:" + url);

		try
		{
			urlc = (HttpURLConnection)page.openConnection();  
			urlc.setUseCaches(false);
			urlc.addRequestProperty("Host", getRequest().getServerName() + ":" + getRequest().getServerPort());
			urlc.addRequestProperty("Cookie", "JSESSIONID=" + sessionID);
			urlc.addRequestProperty("Cache-Control", "max-age=0");
			
			if (debug)
			{
				System.err.println("[httpclient] Content-type = " + urlc.getContentType()); 
				System.err.println("[httpclient] Content-length = " + urlc.getContentLength()); 
				System.err.println("[httpclient] Response-code = " + urlc.getResponseCode());
				System.err.println("[httpclient] Response-message = " + urlc.getResponseMessage());
			}
			
			int retCode = urlc.getResponseCode();
			String retMsg = urlc.getResponseMessage();
			if (retCode>=400)
				throw new Throwable("HTTP Client Error: " + retCode + " - " + retMsg + " - URL:" + url);
							
			int size = urlc.getContentLength();
			if (size > 0)				
				buffer = new byte[size];
			else
				buffer = new byte[bufferSize];
			
			bin = new BufferedInputStream(urlc.getInputStream(), buffer.length);
											   
			int bytesRead = 0;
			do
			{
				bytesRead = bin.read(buffer);
				if (bytesRead > 0) {
					bout.write(buffer, 0, bytesRead);
					if (debug)
						System.err.println("[httpclient] Retrieved segment of " + bytesRead + " bytes.");
				}
			} while (bytesRead != -1);
			
			if (debug)
				System.err.println("[httpclient] Document retrieved.");
			
			return bout.toByteArray();
			
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (bin != null)
				bin.close();
				
			if (urlc != null)
				urlc.disconnect();
		}

	}

	/**
	 * Retrieve the session ID from the request headers
	 * looking for a cookie named JSESSIONID. This method was
	 * implemented because some Servers (WepSphere 5.1) won't
	 * return the real cookie value when the HttpSession.getId()
	 * method is invoked, which causes big trouble when retrieving an
	 * image from an Action using HTTP and the session ID. This problem
	 * was discovered while testing PDF reports with charts on WAS 5.1, it
	 * is specific to WAS 5.1, but this method works well with all tested
	 * servlet engines, including Resin 2.x. 
	 * @return The session ID as stored in the cookie header, or NULL if it can find the cookie.
	 * @throws Throwable
	 */
	protected String getSessionID()
	{
		String value = null;
		Cookie c[] = getRequest().getCookies();
		for (int i=0;i<c.length;i++)
		{
			if (c[i].getName().equals("JSESSIONID"))
			{
				value = c[i].getValue();
				break;
			}
		}
		return value;
	}

	/**
	 * Return byte array with the content of a remote binary resource
	 * accessed via HTTP(S) - a Cookie header (JSESSIONID) with the current
	 * session ID will be added to the request headers
	 * @param url URL of the resource
	 * @param logStdout deprecated. Use instead context-parameter "httpclient-debug".
	 * @return Byte array with the content of the file
	 * @throws Throwable In case of any HTTP error of if the data cannot
	 * be read for any reason.
	 */
	protected byte[] getImage(String url, boolean logStdout) throws Throwable
	{
		String sID = getSessionID();
		return getImage(url, sID, logStdout);
	}	

	/**
	 * Invoke local Action (in the same context) via HTTP GET
	 * preserving the same Session ID
	 * @param path Action's path, should start with /action/...
	 * @return Action response as a byte array, can be converted into a String or 
	 * used as is (in case of images or PDFs).
	 * @throws Throwable
	 */
	protected byte[] callLocalAction(String path) throws Throwable
	{
		return getImage(getServerBaseURL() + path, getSessionID(), false);
	}
	
	
	/**
	 * Returns base URL for retrieving images from the same host
	 * where the application is running. The programmer will need to
	 * add the rest of the path, like /action/chart or /images/logo.png, etc.
	 * @return Base URL to retrieve images from current host
	 */
	protected String getServerBaseURL()
	{
    	//URL prefix to retrieve images from same host/context/session
    	String server = "http://";
		if (getRequest().isSecure())
			server = "https://";
		server = server + getRequest().getServerName() + ":" + getRequest().getServerPort() + getRequest().getContextPath();
		return server;
	}		
	
}
