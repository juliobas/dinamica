package dinamica;

import dinamica.xml.*;
import java.sql.Types;

/**
 * Encapsulates access to the XML configuration
 * of a framework transaction
 * <br>
 * Creation date: oct/4/2003<br>
 * Last Update: jan/3/2005<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class Config
{

	/** xml parser objects */
	Document _doc = null;
	Element _root = null;
	Element _trans = null;
	Element _output = null;

	/** summary */
	public String summary = null;

	/** performance logs */
	public String mvcLog = null;
	public String jdbcLog = null;

	/** transaction classname */
	public String transClassName = null;

	/** transaction datasource */
	public String transDataSource = null;
	
	/** transaction validator */
	public String transValidator = null;
	
	/** automatic transactions */
	public String transTransactions = null;

	/** auto-create recordset definitions */	
	Recordset _rs = new Recordset();

	/** output classname */
	public String outClassName = null;
	
	/** output template */
	public String outTemplate = null;
	
	/** output content-type */
	public String contentType = null;
	
	/** output expiration */
	public String expiration = null;
	
	/** flag that indicates if http headers shoud be set */
	public String headers = null;	
	
	/** specific request encoding for the current Action */
	public String requestEncoding = null;
	
	/** encoding used to read template files */
	public String templateEncoding = null;
	
	/** output data binding commands */
	Recordset _print = new Recordset();
			
	/** transaction path */
	public String path = null;

	/** customized error handler for this action -if any- */
	public String onErrorAction = null;
	
	/** save request parameters as session attributes using their names as IDs */
	public String validatorInSession = null;
			
	public String isolationLevel = null;
	
	public String httpStatusCode = null;
	
	/**
	 * Default constructor
	 * @param xmlData Transaction configuration data in XML
	 * @param path Action directory relative to the context
	 * @throws Throwable
	 */
	public Config(String xmlData, String path) throws Throwable
	{
		
		/* remember path */
		this.path = path;
		
		/* create basic xml objects */
		_doc = new Document(xmlData);
		_trans = _doc.getElement("transaction");
		_output = _doc.getElement("output");

		Element encoding = _doc.getElement("request-encoding");
		if (encoding!=null)
			requestEncoding = encoding.getString();

		//patch 2006-01-03 - custom error handler for this action
		Element onError = _doc.getElement("on-error");
		if (onError!=null)
			onErrorAction = onError.getString();
		//end of patch
		
		/* summary */
		summary = _doc.getElement("summary").getString();

		/* MVC logs */
		mvcLog = _doc.getElement("log").getString();

		/* structure of the recordset */
		_rs.append("id", Types.VARCHAR);
		_rs.append("source", Types.VARCHAR);
		_rs.append("scope", Types.VARCHAR);
		_rs.append("onempty", Types.VARCHAR);
		_rs.append("maxrows", Types.VARCHAR);
		_rs.append("datasource", Types.VARCHAR);
		_rs.append("params", Types.VARCHAR);
		_rs.append("fetchsize", Types.VARCHAR);
		_rs.append("totalCols", Types.VARCHAR);
	
		/* structure of the recordset */
		_print.append("mode", Types.VARCHAR);
		_print.append("recordset", Types.VARCHAR);
		_print.append("tag", Types.VARCHAR);
		_print.append("control", Types.VARCHAR);
		_print.append("pagesize", Types.VARCHAR);
		_print.append("alternate-colors", Types.VARCHAR);
		_print.append("null-value", Types.VARCHAR);
	
		/* read transaction configuration */
		if (_trans!=null)
		{
			Element e = _doc.getElement("//transaction/datasource");
			if (e!=null)
				transDataSource = e.getString();
				
			transClassName = _doc.getElement("//transaction/classname").getString();
			transValidator = _doc.getElement("//transaction/validator").getString();
			transTransactions = _doc.getElement("//transaction/transaction").getString();
			jdbcLog = _doc.getElement("//transaction/jdbc-log").getString();

			//patch 2008-04-30 read transaction isolation level
			isolationLevel = _doc.getElement("//transaction/transaction").getAttribute("level");
			
			Element val = _doc.getElement("//transaction/validator");
			validatorInSession = val.getAttribute("session");
			
			/* any auto-create recordsets? */
			Element rsElems[] = _doc.getElements("//transaction/recordset");
			for (int i = 0; i < rsElems.length; i++)
			{
				_rs.addNew();
				Element rs = rsElems[i];
				String id = rs.getAttribute("id");
				String mode = rs.getAttribute("source");
				String scope = rs.getAttribute("scope");
				String onempty = rs.getAttribute("on-empty-return");
				String maxrows = rs.getAttribute("max-rows");
				String rsDataSrc = rs.getAttribute("datasource");
				if (rsDataSrc!=null && rsDataSrc.trim().equals(""))
					rsDataSrc = null;
				String rsParams = rs.getAttribute("params");
				if (rsParams!=null && rsParams.trim().equals(""))
					rsParams = null;
				String fetchSize = rs.getAttribute("fetch-size");
				String totalCols = rs.getAttribute("totalCols");
								
				_rs.setValue("id", id);
				_rs.setValue("source", mode);
				_rs.setValue("scope", scope);
				_rs.setValue("onempty", onempty);
				_rs.setValue("maxrows", maxrows);
				_rs.setValue("datasource", rsDataSrc);
				_rs.setValue("params", rsParams);
				_rs.setValue("fetchsize", fetchSize);
				_rs.setValue("totalCols", totalCols);
			}
			if ( _rs.getRecordCount()>0)
				_rs.top();
		}

		/* read transaction configuration */
		if (_output!=null)
		{
			Element x = null;
			outClassName = _doc.getElement("//output/classname").getString();
			
			//PATCH 2005-02-17 encoding support
			x = _doc.getElement("//output/template");
			if (x!=null) {
				templateEncoding = x.getAttribute("file-encoding");
				if (templateEncoding!=null && templateEncoding.trim().equals(""))
					templateEncoding=null;
				outTemplate = x.getString();
			}
				
			x = _doc.getElement("//output/content-type");
			if (x!=null) {
				contentType = x.getString();
				//PATCH 2005-02-17 encoding support
				if (contentType.indexOf("charset")<0 && templateEncoding!=null)
					contentType = contentType + "; charset=" + templateEncoding;
			}
				
				
			x = _doc.getElement("//output/expiration");
			if (x!=null)
				expiration = x.getString();
			x = _doc.getElement("//output/set-http-headers");
			if (x!=null)
				headers = x.getString();
			else
				headers = "false";
			
			//2009-03-18 set specific http status code for this Action
			//user by ajax form validation
			x = _doc.getElement("//output/http-status");
			if (x!=null)
				httpStatusCode = x.getString();
			
			/* any auto-create recordsets? */
			Element rsElems[] = _doc.getElements("//output/print");
			for (int i = 0; i < rsElems.length; i++) 
			{
				_print.addNew();
				Element rs = rsElems[i];
				String mode = rs.getAttribute("mode");
				String rset = rs.getAttribute("recordset");
				String tag = rs.getAttribute("tag");
				String control = rs.getAttribute("control");
				String pagesize = rs.getAttribute("pagesize");
				String altColors = rs.getAttribute("alternate-colors");
				String nullValue = rs.getAttribute("null-value");
				
				_print.setValue("mode", mode);
				_print.setValue("recordset", rset);
				_print.setValue("tag", tag);
				_print.setValue("control", control);
				_print.setValue("pagesize", pagesize);
				_print.setValue("alternate-colors", altColors);
				_print.setValue("null-value", nullValue);
			}
			if ( _print.getRecordCount()>0)
				_print.top();
			
		}
	
	}
	
	/**
	 * Return configuration for auto-create recordsets defined in transaction config.xml file
	 * @return Recordset with fields: id, source and scope.
	 */
	public Recordset getRecordsets()
	{
		return _rs;
	}
	
	/**
	 * Return configuration for print (dtaa-binding) commands defined in transaction config.xml file
	 * @return Recordset with fields: mode, recordset, tag, control
	 */
	public Recordset getPrintCommands()
	{
		return _print;
	}
	
	/**
	 * Provide access to Electric XML document object
	 * @return
	 */
	public Document getDocument()
	{
		return _doc;
	}
	
	/**
	 * Provides easy access to custom elements in config.xml file
	 * @param tagName An element or tag name under the root element.<br>
	 * Supports XPath expressions.
	 * @return Element value
	 * @throws Throwable if the element cannot be found
	 */
	public String getConfigValue(String tagName) throws Throwable
	{
		Element e = _doc.getElement(tagName);
		if (e!=null)
			return e.getString();
		else
			throw new Throwable("Configuration element not found: " + tagName);
		
	}
	
	/**
	 * Provides easy access to custom elements in config.xml file
	 * @param tagName An element or tag name under the root element.<br>
	 * Supports XPath expressions.
	 * @param value Default value - This is returned if the element cannot be found
	 * @return Element's value or default value if Element was not found
	 * @throws Throwable On XML parser exceptions
	 */
	public String getConfigValue(String tagName, String value) throws Throwable
	{
		Element e = _doc.getElement(tagName);
		if (e!=null)
			return e.getString();
		else
			return value;
		
	}	
	
	/**
	 * Return URI to foward on a given exit code
	 * @param exitCode
	 * @return
	 * @throws Throwable
	 */
	public String getUriForExitCode(int exitCode) throws Throwable
	{
		String uri = null;
		
		Element onexit = _doc.getElement("//on-exit[@return-code='" + exitCode + "']");
		if (onexit!=null)
			uri = onexit.getAttribute("forward-to");
		return uri;
	}

}
