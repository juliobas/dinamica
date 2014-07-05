package dinamica;

import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.sql.*;
import java.io.PrintWriter;
import dinamica.xml.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Central controller to dispatch all requests
 * recevied by a Web Application built with this framework.
 * This class represents the Coordinator of all activities in
 * the application, it is the base for the advanced MVC mechanism implemented
 * by this framework. This servlet should be configured in WEB.XML to intercept
 * requests targeting /trans/... URLs.<br>
 * <br>
 * The application template provided with the framework includes all
 * the required configuration settings in WEB.XML, use that as a starting
 * point for your own applications.
 * <br>
 * Please read the Howto documents and technical articles included with
 * this framework in order to understand and master the inner working mechanisms
 * of the framework and the role of this servlet in particular.
 * <br>
 * Creation date: 3/10/2003<br>
 * Last Update: 3/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class Controller extends HttpServlet
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * JNDI prefix (java:comp/env/ or "")
	 */
	String _jndiPrefix = null;

	/**
	 * Servlet context
	 */
	ServletContext _ctx = null;

	/**
	 * Default JDBC data source
	 */
	DataSource _ds = null;

	/**
	 * trace log file for requests and JDBC
	 */
	String _logFile = null;

	/**
	 * validation error action URI
	 */
	String _validationErrorAction = null;
	
	/**
	 * default app-level request encoding
	 */
	String _requestEncoding = null;
	
	/**
	 * default app-level file encoding
	 */
	String _fileEncoding = null;

	/**
	 * Central point of control to intercept
	 * all transaction requests (the Controller in the MVC mechanism)
	 */
	@SuppressWarnings("unchecked")
	protected void service(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException
	{
		
		//PATCH 2005-09-14 reject all http methods except GET & POST
		String method = req.getMethod().toUpperCase();
		if (!method.equals("GET") && !method.equals("POST"))
		{
			res.sendError(501, method + " not supported");
			return;
		}
		//END PATCH
		
		/* MVC objects */
		GenericTransaction t = null; //model
		GenericOutput o = null; //view
		Recordset inputValues = null; //request parameters to auto-validate

		/* transaction return code */
		int returnCode = 0;

		/* for performance log */			
		long t1 = 0;
		long t2 = 0;
			
		/* log flags */
		boolean saveMvcLog = false;
		boolean saveJdbcLog = false;

		/* activate log writers */
		StringWriter logWriter = new StringWriter();
		PrintWriter logPrinter = new PrintWriter(logWriter); 
		
		Config config = null;
		
		// patch 2009-09-25 - crear sesion de manera explicita
		// para evitar error en Actions que dependen de la existencia de la sesion
		req.getSession();
		
		try
		{
			
			/* get transaction path */
			String path = getPath(req);
			String configData = null;
			
			/* get config file */
			try {
				configData = StringUtil.getResource(_ctx, path + "config.xml");
			} catch (Throwable notFound) {
				res.sendError(404);
				return;				
			}
			
			configData = replaceMacros(req, configData); //replace macros like ${def:actionroot} and ${lbl:xxx}
			
			/* create config object */
			config = new Config(configData, path);

			/*
			 * set request encoding -if required- patch_20050214 
			 * Action's config.xml is searched first, if no encoding
			 * directive is found at this level, then use app-wide
			 * encoding parameter -if defined-
			 */
			if (config.requestEncoding!=null) {
				req.setCharacterEncoding(config.requestEncoding);
			} else {
				if (_requestEncoding!=null)
					req.setCharacterEncoding(_requestEncoding);
			}
			

			/* set request values */
			setRequestValues(req, config);
			
			/* clear session attributes */
			clearSessionAttributes(req, config);

			/* set logs */
			if (config.jdbcLog!=null && config.jdbcLog.equals("true"))
				saveJdbcLog = true;
			
			if (config.mvcLog.equals("true"))
			{
				saveMvcLog = true;
				
				String userLoginName = "N/D";
				HttpSession s = req.getSession();
				dinamica.security.DinamicaUser user = (dinamica.security.DinamicaUser)req.getUserPrincipal();
				if (user!=null)
					userLoginName = user.getName();				
				
				logPrinter.println("--REQUEST-START");
				logPrinter.println("Request: " + path);
				logPrinter.println("Summary: " + config.summary);
				logPrinter.println("Date: " + new java.util.Date(System.currentTimeMillis()));
				logPrinter.println("Thread: " + Thread.currentThread().getName());;
				logPrinter.println("Session ID: " + s.getId());
				logPrinter.println("UserID: " + userLoginName);
								
			}

			/* validate action if required */
			if (config.transValidator!=null && config.transValidator.equals("true"))
			{
				/* validation may require a database connection */
				Connection con = null;
				try
				{
					/* get database connection from pool (default pool or transaction specific datasource) */
					if (config.transDataSource!=null)
						con = Jndi.getDataSource(_jndiPrefix + config.transDataSource).getConnection();
					else
						con = _ds.getConnection();
						
					/* inputs validation */
					t1 = System.currentTimeMillis();
					inputValues = validateInput(req, config, con, saveJdbcLog, logPrinter);
					t2 = System.currentTimeMillis();
					if (saveMvcLog)
						logPrinter.println("Validation performance (ms): " + (t2-t1));					
					
				} catch (Throwable verror)
				{
					throw verror;
				} finally
				{
					if (con!=null) con.close();
				}
			}

			//PATCH: 2006-06-29
			//save request parameters into session?
			if (config.validatorInSession!=null && config.validatorInSession.equals("true"))
			{
				Iterator i = inputValues.getFields().values().iterator();
				while (i.hasNext())
				{
					RecordsetField f = (RecordsetField)i.next();
					if (!inputValues.isNull(f.getName()))
						req.getSession(true).setAttribute(f.getName(), inputValues.getString(f.getName()));
				}			
			}
			
			t1 = System.currentTimeMillis();

			/* invoke transaction */			
			if (config.transClassName!=null)
			{
				
				Connection con = null;
				
				try
				{
				
					/* get database connection from pool (default pool or transaction specific datasource) */
					if (config.transDataSource!=null)
						con = Jndi.getDataSource(_jndiPrefix + config.transDataSource).getConnection();
					else
						con = _ds.getConnection();
					
					/* load transaction class */
					t = (GenericTransaction) getObject(config.transClassName);
					t.init(_ctx, req, res);
					t.setConfig(config);
					t.setConnection(con);
					
					/* log jdbc performance? */
					if (saveJdbcLog)
						t.setLogWriter(logPrinter);
					
					/* requires transaction? */
					if (config.transTransactions.equals("true")) {
						
						//patch 2008-04-30 set transaction isolation level according to config.xml
						if (config.isolationLevel!=null && !config.isolationLevel.equals("")) {
							if (config.isolationLevel.equalsIgnoreCase("READ_UNCOMMITTED"))
								con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
							else if (config.isolationLevel.equalsIgnoreCase("READ_COMMITTED"))
								con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
							else if (config.isolationLevel.equalsIgnoreCase("REPEATABLE_READ"))
								con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
							else if (config.isolationLevel.equalsIgnoreCase("SERIALIZABLE"))
								con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
							else
								throw new Throwable("Invalid transaction isolation level: " + config.isolationLevel);
						}
						con.setAutoCommit(false);
					}
					else
						con.setAutoCommit(true);

					/* wrapper API implemented? */
					IServiceWrapper w = null;
					if (t instanceof dinamica.IServiceWrapper)
					{
						w = (IServiceWrapper)t;
						w.beforeService(inputValues);
					}
			
					/* execute service */
					returnCode = t.service(inputValues);

					//patch 2009-05-20 - send email if configured
					t.sendEmail();
					
					/* wrapper API? */
					if (w!=null)
						w.afterService(inputValues);

					/* commit if transaction mode is ON */
					if (!con.getAutoCommit())
						con.commit();
					
				}
				catch (Throwable e)
				{
					if (con!=null && !con.getAutoCommit())
						con.rollback();
					throw e;
				}
				finally
				{
					if (con!=null) con.close();
				}
				
			}

			t2 = System.currentTimeMillis();
			if (saveMvcLog)
				logPrinter.println("Transaction performance (ms): " + (t2-t1));
			
			/* check transaction exit code */
			String forwardUri = config.getUriForExitCode(returnCode);
			if (forwardUri!=null)
			{
				forward(forwardUri,req,res);
				return;
			}

			t1 = System.currentTimeMillis();

			/* invoke output */
			if (config.outClassName!=null)
			{
				/* set http headers? */
				if (config.headers.equals("true"))
				{
					//2009-03-18 set http status if defined
					if (config.httpStatusCode!=null)
						res.setStatus(Integer.parseInt(config.httpStatusCode));
					
					/* PATCH 2005-02-23 - encoding support */
					String contentType = config.contentType;
					if (contentType!=null) {
						if (config.contentType.startsWith("text"))
						{
							if (contentType.indexOf("charset")<0 && _fileEncoding!=null)
								contentType = contentType + "; charset=" + _fileEncoding;
						}
						res.setContentType(contentType);
					}
					if (config.expiration!=null)
					{
						if (Integer.parseInt(config.expiration)==0) {
							res.setHeader("Cache-Control","no-cache");
							res.setHeader("Pragma","no-cache");
						}
						else
							res.setHeader("Cache-Control","max-age=" + config.expiration);
					}
				}
				
				/* load output class */
				o = (GenericOutput) getObject(config.outClassName);
				o.init(_ctx, req, res);
				o.setConfig(config);
				
				/* is text based output? */
				if ( config.contentType!=null && config.contentType.startsWith("text") )
				{
					/* load template */
					String template = o.getResource(config.outTemplate);
					TemplateEngine te = new TemplateEngine(_ctx, req, template);
					
					/* PATCH 2005-02-23 - encoding support */
					if (config.templateEncoding!=null)
						te.setEncoding(config.templateEncoding);
					else 
						te.setEncoding(_fileEncoding);
					
					/* generate page using business object "t" */
					o.print(te, t);
					
					/* process any includes and send page to response stream */
					te.print(res);
					
				}
				else
				{
					/* print binary output */
					o.print(t);
				}
				
			}

			t2 = System.currentTimeMillis();
			if (saveMvcLog)
			{
				logPrinter.println("Output performance (ms): " + (t2-t1));
			}
			
		}
		
		/* request/form validation error */
		catch (RequestValidationException vex) {
			String uri = (String)req.getAttribute("dinamica.error.validation");
			if (uri==null)
				uri = _validationErrorAction;
			RequestDispatcher rd = _ctx.getRequestDispatcher(uri);
			req.setAttribute("dinamica.errors", vex.getErrors());
			rd.forward(req,res);
		}
		
		/* general application error */
		catch (Throwable e)	{
			
			//get stack trace
			StringWriter s = new StringWriter();
			PrintWriter err = new PrintWriter(s);
			e.printStackTrace(err);

			/* set request attributes for the 
			generic error handler */
			req.setAttribute("dinamica.error.description", e.getMessage());
			req.setAttribute("dinamica.error.url", req.getRequestURL().toString());
			req.setAttribute("dinamica.error.stacktrace", s.toString());
			req.setAttribute("dinamica.error.user", req.getRemoteUser());
			req.setAttribute("dinamica.error.remote_addr", req.getRemoteAddr());
			req.setAttribute("dinamica.error.exception", e);
			
			//patch 2006-01-03 - use custom error handler if defined
			if (config!=null && config.onErrorAction!=null)
			{
				//remember to log exception info for this case
				log(e.getMessage(), e);
				_ctx.getRequestDispatcher(config.onErrorAction).forward(req,res);
			}
			else
			{
				//let the container's error handler do its job
				throw new ServletException(e);
			}
		}

		finally	{
			/* save logs? */
			if (saveJdbcLog || saveMvcLog) {
				logPrinter.println("--REQUEST-END");
				saveLog(logWriter.toString());
			}
		}

	}

	/**
	 * Extract path begining with /trans/...
	 * @param uri
	 * @return req Servlet request object
	 */
	String getPath(HttpServletRequest req) throws Throwable
	{
		
		String uri = null;
		
		/* is this an include? */
		uri = (String)req.getAttribute("javax.servlet.include.request_uri");
		
		if (uri==null)
			uri = req.getRequestURI();
		
		/* get default mapping */
		String find = getInitParameter("base-dir");
		
		/* find start of path */
		int pos = uri.indexOf(find);
		if (pos >= 0)
		{
			
			//PATCH 2005-08-30 required for ${def:actionroot} marker
			String path = uri.substring(pos);
			req.setAttribute("dinamica.action.path", path);
			//END PATCH
			
			return "/WEB-INF" + path + "/";
		}
		else
		{
			throw new Throwable("Invalid base-dir parameter for this request (" + uri + ") - can't extract Transaction path from URI using this prefix: " + find);
		}
		
	}


	/**
	 * Controller initialization tasks.<br>
	 * Reads parameters jndi-prefix and def-datasource
	 * and creates a default datasource object with
	 * modular scope to be used by all threads from this servlet
	 */
	public void init() throws ServletException
	{
		
		try
		{

			/* init tasks */
			_ctx = getServletContext();
			
			/* get prefix for jndi lookups */
			_jndiPrefix = _ctx.getInitParameter("jndi-prefix");
			if (_jndiPrefix==null)
				_jndiPrefix = "";
			
			/* get default datasource */
			String dsName = _jndiPrefix + _ctx.getInitParameter("def-datasource"); 
			if (dsName == null || dsName.trim().equals(""))
				throw new ServletException("Configuration problem detected in WEB.XML: def-datasource context parameter cannot be undefined!");

			_ds = Jndi.getDataSource(dsName);
			
			/* get logfile name */
			_logFile = _ctx.getInitParameter("log-file");
			if (_logFile == null || _logFile.trim().equals(""))
				throw new ServletException("Configuration problem detected in WEB.XML: log-file context parameter cannot be undefined!");

			/* get custom validation action */
			_validationErrorAction = _ctx.getInitParameter("on-validation-error");
			if (_validationErrorAction == null || _validationErrorAction.trim().equals(""))
				throw new ServletException("Configuration problem detected in WEB.XML: on-validation-error context parameter cannot be undefined!");

			/* get custom request encoding */
			_requestEncoding = _ctx.getInitParameter("request-encoding");
			if (_requestEncoding != null && _requestEncoding.trim().equals(""))
				_requestEncoding = null;

			/* get custom template/response encoding */
			_fileEncoding = _ctx.getInitParameter("file-encoding");
			if (_fileEncoding != null && _fileEncoding.trim().equals(""))
				_fileEncoding = null;

			super.init();
			
		}
		catch (Throwable e)
		{
			log("Controller servlet failed on init!");
			throw new ServletException(e);
		} 

	}

	/**
	 * Save message to filesystem, using the context parameter
	 * log-file defined in web.xml and stored in modular variable _logFile
	 * @param message String to append to file
	 */
	void saveLog(String message) 
	{
		StringUtil.saveMessage(_logFile, message);
	}

	/**
	 * Auto-Validate request parameters for single value
	 * parameters - array parameters must be processed
	 * by the business logic using the Servlet Request object
	 * @param req Servlet Request
	 * @param config Configuration for the current Action
	 * @throws Throwable If any validation rule is violated
	 */
	Recordset validateInput(HttpServletRequest req, Config config, Connection conn, boolean jdbcLog, PrintWriter jdbcLogPrinter) throws Throwable
	{

		/* to store validation error messages*/
		RequestValidationException errors = new RequestValidationException(); 

		/* load default date format used to convert date strings to Date objects */
		String dateFormat = _ctx.getInitParameter("def-input-date");
		
		/* recordset to hold request parameters and optional parameters defined in validator.xml */
		Recordset inputs = new Recordset();
		inputs.setID("_request");
		
		/* load validator xml file */
		String file = config.path + "validator.xml";
		String xmlFile = StringUtil.getResource(_ctx, file);

		//patch 2010-02-13 - support ${def:actionroot} in validator.xml
		xmlFile = replaceMacros(req, xmlFile);
		//end patch

		Document xml = new Document( xmlFile );	
		Element root = xml.getRoot();	

		/* get custom errors display action to override /action/error/validation */
		String onErrorAction = root.getAttribute("onerror");
		if (onErrorAction!=null)
			req.setAttribute("dinamica.error.validation", onErrorAction);
		
		/* read session id -2007-05-23- inputParams recordset can be saved in session */
		String sessionID = root.getAttribute("id");
		if (sessionID==null)
			sessionID = "";
		
		/* read parameters pass 1 - create recordset structure */
		Element elements[] = xml.getElements( "parameter" );
		Element param;
		
		for (int i = 0; i < elements.length; i++) 
		{

			/* validation attributes */
			String type = null;
			String id = null;
			String label = null;
			int sqlType = 0;
			String required = null;

			param = elements[i];
            
			/* read attributes */
			id = param.getAttribute( "id" );
			if (id==null)
				throw new Exception ("Invalid Validator. Attribute [id] not found: " + file);

			type = param.getAttribute( "type" );
			if (type==null)
				throw new Exception ("Invalid Validator. Attribute [type] not found: " + file);

			required = param.getAttribute( "required" );
			if (required==null)
				throw new Exception ("Invalid Validator. Attribute [required] not found: " + file);

			label = param.getAttribute( "label" );
			if (label==null)
				throw new Exception ("Invalid Validator. Attribute [label] not found: " + file);

            
			/* validate type attribute */
			if (type.equals("varchar"))	
				sqlType = Types.VARCHAR;
			else if (type.equals("integer"))
				sqlType = Types.INTEGER;
			else if (type.equals("double"))
				sqlType = Types.DOUBLE;
			else if (type.equals("date"))
				sqlType = Types.DATE;
			else
				throw new Exception("Invalid validator data type (" + type + ") in file: " + file);
			
			/* OK - append the field to the recordset */
			inputs.append(id, sqlType);

		}

		/* add one record to hold the parameters values */
		inputs.addNew();
		
		/* read parameters pass 2 - validate parameters */
		for (int j = 0; j < elements.length; j++) 
		{

			/* validation attributes */
			String type = null;
			String value = null;
			String id = null;
			String label = null;
			int sqlType = 0;
			String required = "";
			int maxLen = 0;
			String maxLenAttr = "";
			String minValueAttr = "";
			String maxValueAttr = "";
			String regexp = null;
			String regexpError = null;

			param = elements[j];
            
			/* read attributes */
			id = param.getAttribute( "id" );
			type = param.getAttribute( "type" );
			required = param.getAttribute( "required" );
			label = param.getAttribute( "label" );
			maxLenAttr = param.getAttribute( "maxlen" );
			minValueAttr = param.getAttribute( "min" );
			maxValueAttr = param.getAttribute( "max" );
			regexp = param.getAttribute( "regexp" );
			regexpError = param.getAttribute( "regexp-error-label" );
						
			//patch 2007-07-16 - label in bold
			label = "<b>" + label + "</b>";

			if (maxLenAttr!=null)
				maxLen = Integer.parseInt(maxLenAttr);
			
			/* validate type attribute */
			if (type.equals("varchar"))	
				sqlType = Types.VARCHAR;
			else if (type.equals("integer"))
				sqlType = Types.INTEGER;
			else if (type.equals("double"))
				sqlType = Types.DOUBLE;
			else if (type.equals("date"))
				sqlType = Types.DATE;
			
			/* get value if present in request */
			String data[] = req.getParameterValues(id);
			
			if (data!=null && !data[0].trim().equals(""))
			{
				/* only accept single value parameters - not arrays */
				if (data.length==1)
				{
					/* OK - let's validate according to data type */
					value = data[0].trim();

					/* check maxlen rule */
					if (maxLen>0)
					{
						if (value.length()>maxLen)
							errors.addMessage( id, label + ": " + "${lbl:data_too_long}" + maxLen);
					}

					/* check regular expression */
					if (sqlType==Types.VARCHAR && regexp!=null)
					{
						boolean isMatch = Pattern.matches(regexp, value);
						if (!isMatch)
							errors.addMessage( id, label + ": " + regexpError);
					}

					
					/* convert to datatype if valid */
					switch (sqlType)
					{
						case Types.DATE:
							java.util.Date d = ValidatorUtil.testDate(value, dateFormat);
							if (d==null)
								errors.addMessage( id, label + ": " + "${lbl:invalid_date}");
							else
								inputs.setValue(id, d);
								
							break;
							
						case Types.DOUBLE:
							Double dbl = ValidatorUtil.testDouble(value);
							if (dbl==null)
								errors.addMessage( id, label + ": " + "${lbl:invalid_double}");
							else
								inputs.setValue(id, dbl);
								
							if (minValueAttr!=null && dbl!=null) {
								double minValue = Double.parseDouble(minValueAttr);
								if (dbl.doubleValue() < minValue)
									errors.addMessage( id, label + ": " + "${lbl:min_value_violation}" + minValue);
							}
							
							if (maxValueAttr!=null && dbl!=null) {
								double maxValue = Double.parseDouble(maxValueAttr);
								if (dbl.doubleValue() > maxValue)
									errors.addMessage( id, label + ": " + "${lbl:max_value_violation}" + maxValue);
							}
								
							break;
						
						case Types.INTEGER:
							Integer i = ValidatorUtil.testInteger(value);
							if (i==null)
								errors.addMessage( id, label + ": " + "${lbl:invalid_integer}");
							else
								inputs.setValue(id, i);

							if (minValueAttr!=null && i!=null) {
								int	minValue = Integer.parseInt(minValueAttr);
								if (i.intValue() < minValue)
									errors.addMessage( id, label + ": " + "${lbl:min_value_violation}" + minValue);
							}
							if (maxValueAttr!=null && i!=null) {
								int maxValue = Integer.parseInt(maxValueAttr);
								if (i.intValue() > maxValue)
									errors.addMessage( id, label + ": " + "${lbl:max_value_violation}" + maxValue);
							}
							
							break;
						
						case Types.VARCHAR:
							inputs.setValue(id, value);
							break;

					}
					
				}
				else
				{
					throw new Throwable("Invalid parameter. Generic validator can't process array parameters. Parameter (" + id + ") in file: " + file);
				}
				
			}
			else if (required.equals("true"))
			{
				errors.addMessage( id, label + ": " + "${lbl:parameter_required}");
			}

		}

		/* 
		 * now check if there are any custom validators defined
		 * this is only necessary if there are no previous errors
		 * because custom-validator require valid request parameters 
		 */
		if (errors.getErrors().getRecordCount()==0)
		{
			Element valds[] = xml.getElements("custom-validator");
			for (int i = 0; i < valds.length; i++) 
			{
			
				/* read validator configuration */
				HashMap<String, String> a = new HashMap<String, String>(5);
				Element validator = valds[i];
				String className = validator.getAttribute( "classname" );
				String onErrorLabel = validator.getAttribute( "on-error-label" );
				
				//patch 2008-01-31 - for ExtJS requested by M.Betti
				String id = validator.getAttribute( "id" );
				if (id==null)
					id = "";
				//end patch
			
				/* custom attributes */
				a = validator.getAttributes();
			
				/* load class and instantiate object */
				AbstractValidator t = null;
				t = (AbstractValidator) getObject(className);
				t.init(_ctx, req, null);
				t.setConfig(config);
				t.setConnection(conn);
					
				/* log jdbc performance? */
				if (jdbcLog)
					t.setLogWriter(jdbcLogPrinter);
			
				/* call isValid method */
				boolean b = t.isValid(req, inputs, a);
			
				/* on error add error message to list of errors */
				if (!b)
				{
					String err = t.getErrorMessage();
					if (err==null)
						err = onErrorLabel;
					errors.addMessage(id, err);
				}
			
			}
		}

		if (errors.getErrors().getRecordCount()>0)
		{
			req.setAttribute("_request", inputs);
			throw errors;
		}
		else
		{
			if (!sessionID.equals(""))
				req.getSession().setAttribute(sessionID, inputs);
			return inputs;
		}
		
	}

	/**
	 * Forward request to another resource in the same context
	 * @param uri Absolute path to resource (a valid servlet mapping)
	 * @throws Throwable
	 */
	void forward(String uri, HttpServletRequest req, HttpServletResponse res) throws Throwable
	{
		RequestDispatcher rd = _ctx.getRequestDispatcher(uri);
		rd.forward(req, res);
	}

	/**
	 * Set request attributes if defined in config.xml
	 * @param req
	 * @param config
	 * @throws Throwable
	 */
	void setRequestValues(HttpServletRequest req, Config config) throws Throwable
	{

		Document doc = config.getDocument();

		Element e[] = doc.getElements("set-request-attribute");
		if (e!=null)
		{
			for (int i = 0; i < e.length; i++) 
			{
				Element r = e[i];
				String id = r.getAttribute("id");
				String value = r.getAttribute("value");
				
				//patch 20081118 - soportar archivos de recursos como valor del atributo
				if (value!=null && value.startsWith("file:")) {
					
					String x[] = StringUtil.split(value, ":");
					String file = x[1];
					if (file.startsWith("/")) {
						value = StringUtil.getResource(this.getServletContext(), file);
					} else {
						value = StringUtil.getResource(this.getServletContext(), getPath(req) + file);
					}
				}
				
				req.setAttribute(id, value);
			}
		}
		
	}
	
	/**
	 * Limpiar atributos de sesion si se definen las directivas
	 * para hacerlo en config.xml
	 * @param req
	 * @param config
	 * @throws Throwable
	 */
	void clearSessionAttributes(HttpServletRequest req, Config config) throws Throwable {
		HttpSession s = req.getSession(false);
		if (s==null)
			return;
		Document doc = config.getDocument();
		Element e[] = doc.getElements("clear-session");
		if (e!=null)
		{
			for (int i = 0; i < e.length; i++) 
			{
				Element r = e[i];
				String id = r.getAttribute("id");
				s.removeAttribute(id);
			}
		}
	}
	
	/**
	 * Loads class and returns new instance of this class.
	 * @param className Name of class to load - include full package name
	 * @return New instance of the class
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	Object getObject(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException 
	{
		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return loader.loadClass(className).newInstance();
		
	}
	
	/**
	 * Replace markers like ${def:actionroot} and ${lbl:xxx} in config.xml
	 * @param req Request object
	 * @param xmlData Body of config.xml
	 * @return Body of config.xml with all markers replaced by the corresponding values
	 * @throws Throwable
	 */
	String replaceMacros(HttpServletRequest req, String xmlData) throws Throwable
	{
		//replace ${def:actionroot}
		if (xmlData.indexOf("${def:actionroot}")>0) {
			String actionPath = (String)req.getAttribute("dinamica.action.path");
			actionPath = actionPath.substring(0, actionPath.lastIndexOf("/"));
			xmlData = StringUtil.replace(xmlData, "${def:actionroot}", actionPath);
		}
			
		//patch 2009-08-03 - replace labels if found
		if (xmlData.indexOf("${lbl:")>0) {
			TemplateEngine te = new TemplateEngine(getServletContext(), req, xmlData);
			te.replaceLabels();
			xmlData = te.toString();
		}
		
		//return xml data
		return xmlData;
	}
	
}
