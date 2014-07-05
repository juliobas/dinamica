package dinamica;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.sql.Types;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import java.util.Locale;

/**
 * Core-level framework class: Text Generator.
 * <br><br>
 * Generates text output (html, xml, sql, etc) given
 * a template with some special markers that will be replaced
 * by data (supplied from recordsets). This class is used by the MVC mechanism of this framework
 * and for general purpose text management.
 * <br><br>
 * This is a very powerfull class, it can generate html forms
 * with its values ready to be edited, whole tables with one line
 * of code, complete well written, portable SQL statements with one line of code,
 * no matter how many fields or the type of each field, the statement will
 * be formatted appropiately, without programmer effort.
 * <br><br>
 * It helps to COMPLETELY separate content from presentation, the
 * templates are dynamically loaded and some labels are replaced with
 * data provided by recordsets using very simple and effective logic. 
 * <br><br>
 * Markers are represented by:
 * <li> Field Markers: ${fld:FieldName}
 * <li> Default Values Markers: ${def:DefaultValueName}
 * <li> Sequence Markers: ${seq:nextval@SequenceName} - ${seq:currval@SequenceName}. This markers allows cross-database sequence expressions, the generation
 * of the actual SEQUENCE SQL expression is determined by context parameters in WEB.XML (sequence-nextval and sequence-currval). 
 * 
 * <br><br>
 * As you can see, the markers distingish themselves by a prefix which
 * denotes its type of marker (fld, def or seq).
 * <br><br>
 * Field Markers can have an additional attribute (not valid for SQL generation)
 * representing the output format, using any valid Java mask for dates and numbers. 
 * Example: {fld:FieldName@mask} - where "mask" would be the output format mask.<br>
 * <br><br>
 * Repeating blocks can also be generated and are conmmonly used for
 * printing tables or filling HTML SELECT controls (ComboBox/ListBox). Example:
 * <pre>
 * &ltrepeat&gt
 * &lttr&gt
 * 		&lttd&gt${fld:col_1}&lt/td&gt
 * 		&lttd>${fld:col_2}&lt/td&gt
 * &lt/tr&gt
 * &lt/repeat&gt
 * </pre>
 * 
 * This can be replaced using one line of code with the data
 * values of each record in a recordset, default formats are applied
 * unless you specify an output format for a specific field. Default formats
 * are configured in WEB.XML, as context parameters (def-format-date and def-format-double).
 * <br><br>
 * This class also supports two advanced features: Special markers for <b>dynamic labes</b> ${lbl:KeyName} which are replaced
 * according to a pre-selected language (ES, EN, IT, etc), this allows multi-language templates, and
 * also supports <b>includes</b>, the ability to dynamically include the output of another MVC transaction, which allows
 * composition of pages from smaller parts, using a special marker: ${inc:ModuleName}
 * <br><br>
 * Please consult the How-to documentation to learn all about this class and its
 * role inside the framework. 
 * <br><br>
 * Context parameters in WEB.XML used by this class:
 * <xmp>
 *	<context-param>
 *		<param-name>def-format-date</param-name>
 *		<param-value>dd-MM-yyyy</param-value>
 *		<description>default format for dates</description>
 *	</context-param>
 *
 *	<context-param>
 *		<param-name>sequence-nextval</param-name>
 *		<param-value>${seq}.NEXTVAL</param-value>
 *		<description>SQL expression to obtain the next sequence value - sequence name will be ${seq}</description>
 *	</context-param>
 *	
 *	<context-param>
 *		<param-name>sequence-currval</param-name>
 *		<param-value>${seq}.CURRVAL</param-value>
 *		<description>SQL expression to obtain the current sequence value - sequence name will be ${seq}</description>
 *	</context-param>
 * </xmp>
 * 
 * Creation date: 18/09/2003<br>
 * Last Update: 18/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class TemplateEngine
{

	/** template text */
	private String _template = "";
	

	/** servlet request - used to generate many default values related to the request */
	private HttpServletRequest _req = null;
	
	/** servlet context - used to read configuration from the context */
	private ServletContext _ctx = null;
	
	/** custom defined Locale used for format masks in dates and numbers */
	private Locale _locale = null;

	/** callback object that implements the interface IRowEvent */
	private IRowEvent _rowEvent = null;

	/** template encoding -if available- */
	private String _encoding = null;

	private Labels _labels = null;
	
	/**
	 * Set template encoding - for information purporses only
	 * @param encoding Canonical name of character encoding (ISO-8859-1, etc)
	 */
	public void setEncoding(String encoding)
	{
		_encoding = encoding;
	}

	/**
	 * Set reference to callback object that
	 * implements the IRowEvent interface
	 * @param obj Object that implements the IRowEvent interface
	 */
	public void setRowEventObject(IRowEvent obj)
	{
		_rowEvent = obj;
	}

	/**
	 * Set custom LOCALE for formatting
	 * @param l Locale object
	 */
	public void setLocale(Locale l) throws Throwable
	{
		_locale = l;
		replaceLabels(); 
	}

	/**
	 * Set servlet request reference
	 * @param req HTTP Servlet Request object
	 */
	public void setRequest(HttpServletRequest req)
	{
		_req = req;
	}

	/**
	 * Servlet oriented constructor
	 * @param ctx Servlet Context - will be used to read context parameters.
	 * @param req Servlet Request - will be used to produce default values, like userid or remote_addr.
	 * @param template Text template with markers
	 * @throws Throwable
	 */
	public TemplateEngine(ServletContext ctx, HttpServletRequest req, String template)
	{
		_template = template;
		_ctx = ctx;
		_req = req;
		
		//patch june-28-2004 -avoid unnecessary replace calls for repeat sections
		try {
			replaceDefaultValues();
			if (_req!=null && _ctx!=null) {
				
				//patch 2009-08-03 preload user locale - if any
				HttpSession s = req.getSession(false);
				if (s!=null) {
					this._locale = (java.util.Locale)s.getAttribute("dinamica.user.locale");
					replaceSessionAttributes();
				}
				replaceRequestAttributes();
			}
		} catch (Throwable e) {}
		
	}

	/**
	 * Generates SQL using the recordset values from the current record,
	 * substitutes the field markers with properly formatted values. Saves a lot
	 * of work with only one line of code. This method is smart enough as to generate
	 * well formatted values for Strings and portable formats for dates and timestamps,
	 * using the date/timestamp canonical syntax. Also special characters like single quote (') in
	 * Strings will be correctly escaped to avoid SQL syntax errors or SQL injection attacks.
	 * <br><br>
	 * This object must be created using a SQL template containing markers like ${fld:FieldName}, ${def:date} or ${seq:nextval@SeqName}
	 * @param rs Recordset containing at least one record, the record position must be valid
	 * @return Well formatted SQL with all the markers replaced by the corresponding values (only fld, def and seq markers are considered, no format masks allowed) 
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	public String getSql(Recordset rs) throws Throwable
	{
		
		try
		{
		
			//patch 2007-07-17 replace special marker for security schema ${schema}
			//in any SQL template that may contain it
			if (_ctx!=null)
			{
				if (_template.indexOf("${schema}") > 0 ) {
					String schema = _ctx.getInitParameter("security-schema");
					if (schema==null)
						schema = "";
					else
						if (!schema.endsWith(".") && !schema.equals(""))
							schema = schema + ".";
					_template = StringUtil.replace(_template, "${schema}", schema);
				}
			}
			//end patch
			
			if (rs!=null)
			{
				/* get recordset metadata */
				HashMap<String, RecordsetField> flds = rs.getFields();
			
				/* for each field try to replace value */
				Iterator i = flds.values().iterator();
				while (i.hasNext())
				{
				
					RecordsetField f = (RecordsetField)i.next();
					String fname = f.getName();
					Object value = rs.getValue(fname);
					String marker = "${fld:" + fname + "}";
				
					if (value==null)
					{
						_template = StringUtil.replace(_template, marker, "NULL");
					}
					else
					{
						switch (f.getType())
						{
							case Types.VARCHAR:
							case Types.CHAR:
							case Types.LONGVARCHAR:
								String v = (String)value;
								v = StringUtil.replace(v,"'","''");
								_template = StringUtil.replace(_template, marker, "'" + v + "'");
								break;
							
							case Types.DATE:
								java.util.Date d = (java.util.Date)value;
								_template = StringUtil.replace(_template, marker, "{d '" + StringUtil.formatDate(d, "yyyy-MM-dd") + "'}");
								break;
							
							case Types.TIMESTAMP:
								java.util.Date d1 = (java.util.Date)value;
								_template = StringUtil.replace(_template, marker, "{ts '" + StringUtil.formatDate(d1, "yyyy-MM-dd HH:mm:ss.SSS") + "'}");
								break;
							
							default:
								String n = dinamica.StringUtil.formatNumber(value, "#.######");
								n = dinamica.StringUtil.replace(n, ",", ".");									
								_template = StringUtil.replace(_template, marker, n);
								break;
								
						}
					}
				
				}
			}
					
			/* replace default values */
			if (_req!=null)
				replaceDefaultValues();
		
			/* replace SEQUENCE and request/session markers */
			if (_ctx!=null && _req!=null)
			{
				replaceRequestAttributes();
				replaceSessionAttributes();
				replaceLabels();
				
				ArrayList<Marker> seqs = getMarkers("seq");
		
				/* for each field marker set value */
				Iterator<Marker> is = seqs.iterator();
				while (is.hasNext())
				{
					/* get next marker */
					Marker m = (Marker)is.next();
					String seqType = m.getName(); //sequence mode (nextval|currval)
					String seqName = m.getExtraInfo(); //sequence object name
					String marker = "${seq:" + seqType + "@" + seqName + "}";
			
					/* get sequence configuration from context */
					String seqConfigParam = "sequence-" + seqType; 
					String seqExpr = _ctx.getInitParameter(seqConfigParam);
				
					/* throw error if config not found */
					if (seqExpr==null || seqExpr.equals(""))
					{
						String args[] = {marker};
						String msg = Errors.SEQUENCE_BAD_CONFIGURATION;
						msg = MessageFormat.format(msg, (Object[])args);
						throw new Throwable(msg);
					}
							
					/* replace sequence expression */
					String value = "";
					
					//patch for Resin 3.0.6 - Feb26-2004
					if (seqExpr.indexOf("${seq}")<0)
						value = StringUtil.replace(seqExpr, "$[seq]", seqName);
					else
						value = StringUtil.replace(seqExpr, "${seq}", seqName);
					//end patch
					
					_template = StringUtil.replace(_template, marker, value);
			
				}
			}
				
			return _template;
		}
		catch (Throwable e)
		{
			String msg = "[TemplateEngine].\n Template:" + _template + "\n";
			String data = "";
			if (rs!=null)
			{
				data = rs.toString();
				System.err.println(msg + data);
			}
			throw e;
		}
		 
	}
	
	/**
	 * Replace default values present in the template, default values
	 * are special markers expressed in the form: ${def:valueName}<br>
	 * Implemented defaults are:<br>
	 * <li>${def:user} - HttpServletRequest.getPrincipal().getUserName(). If the user is not authenticated then returns an empty string ""
	 * <li>${def:date} - yyyy-MM-dd
	 * <li>${def:time} - HH:mm:ss
	 * <li>${def:timestamp} - yyyy-MM-dd HH:mm:ss.SSS
	 * <li>${def:host} - HttpServletRequest.getServerName()
	 * <li>${def:context} - HttpServletRequest.getContextPath()
	 * <li>${def:remoteaddr} - HttpServletRequest.getRemoteAddr()
	 * <li>${def:uri} - HttpServletRequest.getRequestURI()
	 * <li>${def:dateDMY} - dd-MM-yyyy
	 * <li>${def:dateMDY} - MM-dd-yyyy
	 * <li>${def:actionroot} - action parent path
	 * <li>${def:httpserver} - protocol://hostname:port
	 * <li>${def:session} - session ID
	 * <li>${def:alias} - security alias de la aplicacion 
	 * <br>
	 * All values extracted from HttpServletRequest will be replaced
	 * by an empty string "" if the request object is null. Use the method
	 * setRequest() to set the request object or the special constructor
	 * TemplateEngine(ServletContext ctx, HttpServletRequest req, String t) if you want to set
	 * the request object.
	 * <br>
	 * Default values can be used in every kind of template, SQL, HTML, XML, etc.
	 */
	public void replaceDefaultValues() throws Throwable
	{
		
		//patch june-22-2004 don't waste time
		if (_template.indexOf("${def:")< 0 )
			return;
		
		String markers[] = {
							"${def:user}",
							"${def:date}",
							"${def:time}",
							"${def:timestamp}",
							"${def:host}",
							"${def:context}",
							"${def:remoteaddr}",
							"${def:uri}",
							"${def:dateDMY}",
							"${def:dateMDY}",
							"${def:actionroot}",
							"${def:httpserver}",
							"${def:session}",
							"${def:alias}"
							};
							
		String values[] = new String[markers.length];
		
		String userid = null;
		if (_req != null) userid = _req.getRemoteUser();
		if (userid == null) userid = "";

		java.util.Date d = new java.util.Date();
		values[0] = userid;
		values[1] = StringUtil.formatDate(d, "yyyy-MM-dd");
		values[2] = StringUtil.formatDate(d, "HH:mm:ss");
		values[3] = StringUtil.formatDate(d, "yyyy-MM-dd HH:mm:ss.SSS");
		
		if (_req!=null)
			values[4] = _req.getServerName(); else values[4] = "";

		if (_req!=null)
			values[5] = _req.getContextPath(); else values[5] = "";

		if (_req!=null)
			values[6] = _req.getRemoteAddr(); else values[6] = "";

		if (_req!=null)
			values[7] = _req.getRequestURI(); else values[7] = "";

		values[8] = StringUtil.formatDate(d, "dd-MM-yyyy");
		values[9] = StringUtil.formatDate(d, "MM-dd-yyyy");

		if (_req!=null)
		{
			String path = (String)_req.getAttribute("dinamica.action.path");
			path = path.substring(0, path.lastIndexOf("/"));
			values[10] = path;
		}
		else values[10] = "";
		
		if (_req!=null)
		{
			String http = "http://";
			if (_req.isSecure())
					http = "https://";
			http = http + _req.getServerName() + ":" + _req.getServerPort();
			values[11] = http;
		}
		else values[11] = "";

		if (_req!=null)
		{
			//patch 2009-08-31 - no crear sesion si no existe una
			HttpSession s =  _req.getSession(false);
			if (s!=null)
				values[12] = s.getId();
			else
				values[12] ="N/D";
		}
		else values[12] = "";
		
		if (_req!=null)
		{
			values[13] = (String)_req.getAttribute("dinamica.security.application");
		}
		else values[13] = "";

		
		for (int i=0;i<markers.length;i++)
		{
			_template = StringUtil.replace(_template,markers[i],values[i]);
		}
		
	}
	
	/**
	 * Return current state of the internal template
	 */
	public String toString()
	{
		return _template;
	}

	/**
	 * Returns a list of markers of a given type
	 * @param prefix The type of marker (fld, lbl, inc, seq)
	 * @return ArrayList containing Marker objects
	 * @throws Throwable
	 */
	public ArrayList<Marker> getMarkers(String prefix) throws Throwable
	{	
		
		/* test precondition */
		if (prefix.length()!=3)
		{
			String args[] = {prefix};
			String msg = Errors.INVALID_PREFIX;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new Throwable(msg);
		}
		
		int pos = 0;
		ArrayList<Marker> l = new ArrayList<Marker>();
		
		/* search markers */
		while ( pos >= 0 )
		{
			int pos1 = 0;
			int pos2 = 0;
			int newPos = 0;
			
			/* find start of marker */
			pos1 = _template.indexOf("${" + prefix + ":", pos);
			if (pos1>=0)
			{
				
				/* find end of marker */
				newPos = pos1 + 6;
				pos2 = _template.indexOf("}", newPos);
				
				if (pos2>0)
				{
					
					/* get marker string */
					String fld = _template.substring(newPos, pos2);
					Marker m = new Marker(fld,null,pos1,pos2);
					
					/* search for etra attribute (format or sequence name) */
					int pos3 = fld.indexOf("@");
					if (pos3>0)
					{
						
						String name = fld.substring(0, pos3);
						String extraInfo = fld.substring(pos3 + 1, fld.length());
						
						if ( (name.indexOf(" ")>=0) || (name.indexOf("\r")>=0) || (name.indexOf("\n")>=0) || (name.indexOf('\t')>=0) )
						{
							String args[] = {name};
							String msg = Errors.INVALID_MARKER;
							msg = MessageFormat.format(msg, (Object[])args);
							throw new Throwable(msg);
						}


						m.setName(name);
						m.setExtraInfo(extraInfo);
					}
					
					l.add(m);
				}
				else
				{
					throw new Throwable( Errors.MARKER_UNCLOSED );
				}
				pos = pos2 + 1;
			}
			else
			{
				pos = -1;
			}
		}
		
		return l;
		
	}

	/**
	 * Replace all possible field markers with corresponding
	 * recordset field values from current record. This method
	 * is mainly used to populate forms
	 * @param rs Recordset with a valid record position
	 * @param nullValueExpr The string to represent null values ("" or &ampnbsp;)
	 * @throws Throwable
	 */
	public void replace(Recordset rs, String nullValueExpr) throws Throwable
	{
		
		/* parse the template and create list of field markers */
		ArrayList<Marker> flds = getMarkers("fld");

		/* call internal replace method */
		replace(rs, nullValueExpr, flds);
		
	}

	/**
	 * Navigate all the recordset and replace the
	 * values of each record. This method is used to produce
	 * tables or fill controls like ComboBoxes or ListBoxes. It is
	 * suitable for any section of the template that must be repeated
	 * as many times as records are in the recordset 
	 *
	 * @param rs Recordset
	 * @param nullValueExpr The string to represent null values ("" or &ampnbsp;) - when generating html tables it should be &ampnbsp;
	 * @param RepeatSectionTag A custom tag that encloses the repeatable section
	 * @throws Throwable
	 */
	public void replace(Recordset rs, String nullValueExpr, String repeatSectionTag) throws Throwable
	{
		
		dinamica.parser.FastTemplateEngine fte = new dinamica.parser.FastTemplateEngine();
		
		String section = null;
		String repeatTemplate = null;
		int pos1 = 0;
		int pos2 = 0;		
		
		String tagStart = "<" + repeatSectionTag + ">";
		String tagEnd = "</" + repeatSectionTag + ">";
		
		/* find start of repeat section */
		pos1 = _template.indexOf(tagStart);
		if (pos1>=0)
		{
				
			/* find end of repeat section */
			int newPos = pos1 + tagStart.length();
			pos2 = _template.indexOf(tagEnd, newPos);
				
			if (pos2>0)
			{
				/* get section string */
				section = _template.substring(pos1, pos2 + tagEnd.length());
				repeatTemplate = _template.substring(newPos, pos2);

				/* buffer to contain generated text */
				StringBuilder buf = new StringBuilder();
				
				/* navigate all recordset */
				if (rs.getRecordCount()>0)
				{
					
					// fast template engine
					fte.setTemplate(repeatTemplate);
					ArrayList<dinamica.parser.Marker> markers = fte.getMarkers();
					
					// rewind recordset
					rs.top();
					
					/* for each record */
					while (rs.next())
					{
						
						setValues(fte, markers, rs, nullValueExpr);
						
						// get row
						String row = fte.toString();
						
						/* row event available? */
						if (_rowEvent!=null)
						{
							row = _rowEvent.onNewRow( rs, row );
						}
							
						
						// append text
						buf.append(row);
						
					}
					_template = StringUtil.replace(_template, section, buf.toString());
				}
				else
				{
					_template = StringUtil.replace(_template, section, "");
				}
				
			}
			else
			{
				String args[] = {repeatSectionTag};
				String msg = Errors.REPEAT_TAG_NOT_CLOSED;
				msg = MessageFormat.format(msg, (Object[])args);
				throw new Throwable(msg);
			}

		}
		else
		{
			String args[] = {repeatSectionTag};
			String msg = Errors.REPEAT_TAG_NOT_FOUND;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new Throwable(msg);
		}
		
	}

	
	/**
	 * Replace dynamic labels ${lbl:Name} using the
	 * session locale, which must be indicated using the method
	 * setLocale, otherwise the language code
	 * specified in the context-attribute <b>def-language</b> in WEB.XML will be used
	 * to select the default locale. If none has been properly configured
	 * or the label does not exist for the selected Locale, then
	 * an exception will be triggered. This mechanism uses the label
	 * configuration file stored in /WEB-INF/labels.xml. Please check
	 * the documentation to learn the structure of this file.
	 * Place the appropiate markers and make sure the labels.xml file is properly configured.
	 * In order to use this method, the TemplateEngine must have access to
	 * the ServletContext, so use the appropiate constructor. If there is no
	 * valid reference to the ServletContext, then this method will trigger
	 * an error
	 * @throws Throwable
	 */
	public void replaceLabels() throws Throwable
	{

		if (_ctx==null)
			throw new Throwable("Servlet Context is null - this method can't work without a ServletContext.");

		//patch june-22-2004 don't waste time
		if (_template.indexOf("${lbl:")< 0 )
			return;

		if (_labels==null)
			_labels = new Labels(_ctx);
		
		/* parse the template and create list of label markers */
		ArrayList<Marker> flds = getMarkers("lbl");
		
		/* identify locale to be used */
		String language = null;
		if (_locale==null)
			language = _ctx.getInitParameter("def-language");
		else
			language = _locale.getLanguage();
		 
		if (language==null || language.equals(""))
			throw new Throwable("Language not defined (User Locale or default language may be null)");
		
		for (int i=0; i<flds.size();i++)
		{
			Marker m = (Marker) flds.get(i);
			String name = m.getName();
			String label = "${" + "lbl:" + name + "}";
			_template = StringUtil.replace(_template, label, _labels.getLabel(name, language));
		}
		
	}


	/**
	 * Split template into segments stored into an array.
	 * A segment may be a printable text or an INCLUDE directive
	 * to include the content of another resource from the same context
	 * @return ArrayList containing TemplateSegment objects
	 * @throws Throwable
	 */
	ArrayList<TemplateSegment> getSegments() throws Throwable
	{
		
		ArrayList<TemplateSegment> s = new ArrayList<TemplateSegment>();
		
		/* get include markers */
		ArrayList<Marker> l = getMarkers("inc");

		if (l.size()>0)
		{
			int lastPos = 0;
			for (int i=0; i<l.size();i++)
			{
				/* create segment */
				Marker m = (Marker) l.get(i);
				TemplateSegment seg1 = new TemplateSegment();
				seg1.segmentType = "data";
				seg1.segmentData = _template.substring(lastPos, m.getPos1());
						
				TemplateSegment seg2 = new TemplateSegment();
				seg2.segmentType = "inc";
				seg2.segmentData = m.getName();

				lastPos = m.getPos2() + 1;
			
				s.add(seg1);
				s.add(seg2);
			
			}
			TemplateSegment seg1 = new TemplateSegment();
			seg1.segmentType = "data";
			seg1.segmentData = _template.substring(lastPos);
			s.add(seg1);
			
		}
		else
		{
			
			TemplateSegment seg = new TemplateSegment();
			seg.segmentType = "data";
			seg.segmentData = _template;
			s.add(seg);		
		}
		
		return s; 
	}
	
	/**
	 * Print template and process any INCLUDE directive
	 * present into the template; in order to do this the
	 * class required a reference to the ServletContext, Request
	 * and Response, otherwise it can't dispatch to include another servlets.
	 * No writer must be obtained from the Servlet response object
	 * prior to calling this method, because this method will call getWriter() from
	 * the passed Response object.<br>
	 * <br>
	 * <b>NOTE:</b> default values and dynamic labels will be automatically
	 * replaced by this method. This is the preferred way to print a template
	 * to make sure everything is being replaced, the caller is only responsable
	 * for setting the appropiate response headers.
	 * @param res HttpServletResponse
	 * @throws Throwable if the ServletContext reference is null
	 */
	public void print(HttpServletResponse res) throws Throwable
	{
		
		if (_ctx==null)
			throw new Throwable("ServletContext is null - can't print template because the request dispatcher must be obtained from the ServletContext.");

		replaceDefaultValues();
		replaceLabels();
		replaceRequestAttributes();
		replaceSessionAttributes();
		
		PrintWriter pw = res.getWriter();
		
		// patch 28-june-2004 - set content length if no includes are used in this template
		if (_template.indexOf("${inc:")>=0)
		{
			ArrayList<TemplateSegment> s = getSegments();
			for (int i=0; i<s.size();i++)
			{
				TemplateSegment t = (TemplateSegment)s.get(i);
				if (t.segmentType.equals("inc"))
				{
					try {
						RequestDispatcher rd = _ctx.getRequestDispatcher(t.segmentData);
						rd.include(_req, res);
					} catch (Throwable e)
					{
						String msg = "INCLUDE Error (" + t.segmentData + ") - " + e.getMessage(); 
						throw new Throwable(msg);
					}
				}
				else
				{
					pw.print(t.segmentData);
				}
			}
		}
		else
		{
			//PATCH 2005-02-23 - encoding support
			byte body[] = null;
			if (_encoding!=null)
				body = _template.getBytes(_encoding);
			else
				body = _template.getBytes();
				
			res.setContentLength(body.length);
			pw.print(_template);
		}
		
	}

	/**
	 * HTML Control utility method.<br>
	 * Select combobox item for single select combobox. This method
	 * will insert the word "selected" in the appropiate option element
	 * from the corresponding select html control.<br>
	 * <b>NOTE:</b> All html keywords related to the control must be
	 * in lower case, including attributes and tag names. Name and Value lookup
	 * is case sensitive!
	 * @param controlName HTML control name attribute
	 * @param value Option value to search for
	 * @throws Throwable if can't find control or its closing tag
	 */
	public void setComboValue(String controlName, String value) throws Throwable
	{
		int pos1 = 0;
		int pos2 = 0;
		String combo = "";

		/* define control to find */
		String find = "<select name=\"" + controlName + "\"";
		
		/* find it */
		pos1 = _template.indexOf(find);
		
		/* found? */
		if (pos1>=0)
		{
			/* extract segment  from template */
			pos2 = _template.indexOf("</select>", pos1);
			if (pos2>0)
			{

				/* extract */
				int newpos2 = pos2 + "</select>".length();
				combo = _template.substring(pos1, newpos2);
				
				/* set item=selected if found */
				find = "<option value=\"" + value + "\"";
				String newItem = find + " selected ";
				String temp = StringUtil.replace(combo, find, newItem);

				/* replace into template */
				_template = StringUtil.replace(_template, combo, temp);
				
			}
			else
			{
				throw new Throwable("Can't find closing tag for this HTML control: " + controlName);
			}
		}
		else
		{
			throw new Throwable("HTML control not found: " + controlName);
		}
		
	}
	
	/**
	 * HTML Control utility method.<br>
	 * Set combobox values for multiple items using a recordset
	 * to lookup the values from a field and set the corresponding option items
	 * in the select control. All records from the recordset are used.
	 * @param controlName Name of the html select control which is also the name
	 * of the field name to use from the Recordset 
	 * @param rs
	 * @throws Throwable
	 */
	public void setComboValue(String controlName, Recordset rs) throws Throwable
	{
		if (rs.getRecordCount()==0)
			return;
		
		rs.top();
		while (rs.next())
		{
			/* reuse setComboValue method */
			String value = String.valueOf(rs.getValue(controlName));
			setComboValue(controlName, value);
		}
	}


	/**
	 * Internal method that factorizes most of the code
	 * that is common to all the overloaders
	 * @param rs Recordset
	 * @param nullValueExpr The string to represent null values ("" or &ampnbsp;) - when generating html tables it should be &ampnbsp;
	 * @param markers ArrayList containing the field markers
	 * @throws Throwable
	 */
	@SuppressWarnings("unchecked")
	void replace(Recordset rs, String nullValueExpr, ArrayList markers) throws Throwable
	{
	
		String strValue = null;
		Object value = null;
		String toReplace = null;
		
		/* get recordset fields */
		HashMap rsFlds = rs.getFields();
		
		/* parse the template and create list of field markers */
		ArrayList flds = markers;
		
		/* for each field marker set value */
		Iterator i = flds.iterator();
		while (i.hasNext())
		{
	
			String formatPluginName = null;
			IFormatPlugin fmtObj = null;
			FormatPluginParser fpp = null;
	
			/* get next marker */
			Marker m = (Marker)i.next();
			String fName = m.getName();
			String fFormat = m.getExtraInfo();
			
			/* determine if it is an special field (rowNumber/rowIndex) otherwise if the field exists */
			boolean found = true;
			boolean fakeField = false;
			if (!fName.equals("_rowNumber") && !fName.equals("_rowIndex"))
			{
				if (!rsFlds.containsKey(fName)) 
					found = false;
			}
			else
			{
				fakeField = true;
			}
							
			
			/* recordset contains this field? */
			if (found)
			{
				
				String defDateFmt = null;
				
				/* read default date format */
				if (_ctx!=null)
					defDateFmt = _ctx.getInitParameter("def-format-date");
				
				if (defDateFmt==null || defDateFmt.equals(""))
					defDateFmt = "dd-MM-yyyy";				
				
				/* rebuild marker to replace by searched and replaced by corresponding value */
				if (fFormat!=null)
				{
					toReplace = "${fld:" + fName + "@" + fFormat + "}";
					
					//PATCH 2005-05-23 - get plugin name if available
					if (fFormat.startsWith("class:"))
					{
					    formatPluginName = fFormat.substring(6);
					    fpp = new FormatPluginParser(formatPluginName); 
					    fmtObj = (IFormatPlugin)Thread.currentThread().getContextClassLoader().loadClass(fpp.getName()).newInstance();
					}
				}
				else
					toReplace = "${fld:" + fName + "}";
				
				/* get field value */
				value = rs.getValue(fName);
				
				//custom format??
				if (fmtObj!=null)
				{
				    strValue = fmtObj.format(fName, rs, _locale, fpp.getArgs());
				}
				else
				{
				
					/* apply appropiate null representation */
					if (value==null) 
					{
						strValue=nullValueExpr;
					}
					else
					{
						
						/* get field info */
						RecordsetField f = null;
						if (!fakeField)
							f = (RecordsetField)rsFlds.get(fName);
						else
							f = new RecordsetField(fName,"INTEGER", Types.INTEGER);
							
						
						/* format value according to data type*/
						if (f.getType()!=Types.DATE && f.getType()!=Types.TIMESTAMP )
						{
							/* format defined? */
							if (fFormat==null)
							{
								strValue = String.valueOf(value);
							}
							else
							{
								//is a string data type?
								if (f.getType()==Types.VARCHAR || f.getType()==Types.CHAR || f.getType()==Types.CLOB || f.getType()==Types.LONGVARCHAR)
								{
									if (fFormat.equals("xml"))
									{
										//encode special characters for xml/html output
										strValue = encodeXML((String)value);
									}
									else if (fFormat.equals("html"))
									{
										//encode special characters for xml/html output
										strValue = encodeHTML((String)value);
									}
									else if (fFormat.equals("url"))
									{
										//encode special characters for xml/html output
										strValue = URLEncoder.encode((String)value, "UTF-8");
									}
									else if (fFormat.equals("js"))
									{
										//encode special characters for xml/html output
										strValue = encodeJS((String)value);
									}									
									else
									{
										throw new Throwable("Invalid format mask for the field:" + fName);
									}
									
								}
								// it is a numeric data type
								else
								{
										
									if (_locale==null)
										strValue = StringUtil.formatNumber(value, fFormat);
									else
										strValue = StringUtil.formatNumber(value, fFormat, _locale);
	
								}
							}
						}
						else
						{
							/* apply default or custom date format? */
							if (fFormat==null)
								fFormat = defDateFmt;
							if (_locale==null)
								strValue = StringUtil.formatDate((java.util.Date)value, fFormat);
							else
								strValue = StringUtil.formatDate((java.util.Date)value, fFormat, _locale);
						}
						
					}
	
				}	
					
				/* replace marker with value */
				_template = StringUtil.replace(_template,toReplace,strValue);
						
			}
			
		}
	
	}

	/**
	 * HTML Control utility method.<br>
	 * Select RadioButton control from a group of controls
	 * using a value to match the appropiate control
	 * <b>NOTE:</b> All html keywords related to the control must be
	 * in lower case, including attributes and tag names. Name and Value lookup
	 * is case sensitive!
	 * @param controlName HTML control name attribute
	 * @param value Value to search for
	 * @throws Throwable if can't find control
	 */
	public void setRadioButton(String controlName, String value) throws Throwable
	{

		int pos1 = 0;
		int pos2 = 0;
		String ctrl = "";
		int flag = 0;
		int pos = 0;
	
		while (flag >= 0)
		{

			/* define control to find */
			String find = "<input";
		
			/* find it */
			pos1 = _template.indexOf(find,pos);
		
			/* found? */
			if (pos1>=0)
			{
				flag = 1;
				
				/* extract segment  from template */
				pos2 = _template.indexOf(">", pos1);
				if (pos2>0)
				{

					/* extract */
					int newpos2 = pos2 + ">".length();
					ctrl = _template.substring(pos1, newpos2);
				
					/* check to see if this is the requested control */
					find = "name=\"" + controlName + "\"";
					int newpos1 = ctrl.indexOf(find);
				
					/* found? this is one of the requested controls! */
					if (newpos1>=0)
					{
						
						/* mark this control as "selected" if its value match */
						find = "value=\"" + value + "\"";
						String newItem = find + " checked ";
						String temp = StringUtil.replace(ctrl, find, newItem);

						/* replace into template */
						if (!temp.equals(ctrl))
						{
							_template = StringUtil.replace(_template, ctrl, temp);
							return;
						}
						else
						{
							ctrl = temp;
						}

					}
					pos = pos1 + ctrl.length();
				}
				else
				{
					throw new Throwable("'input' Tag is not properly closed for this HTML control: " + controlName);
				}
			}
			else
			{
				flag = -1;
			}

		}

	}

	/**
	 * HTML Control utility method.<br>
	 * Set checkbox values for multiple items using a recordset
	 * to lookup the values from a field and set the corresponding checkbox items
	 * in the checkbox control group. All records from the recordset are used.
	 * @param controlName Name of the html select control which is also the name
	 * of the field name to use from the Recordset 
	 * @param rs
	 * @throws Throwable
	 */
	public void setCheckbox(String controlName, Recordset rs) throws Throwable
	{
		if (rs.getRecordCount()==0)
			return;
		
		rs.top();
		while (rs.next())
		{
			/* reuse setRadioButton method */
			String value = String.valueOf(rs.getValue(controlName));
			setRadioButton(controlName, value);
		}
	}


	/**
	 * Change template body - if necessary, should be called before replacing any data
	 * because all previous changes will be lost
	 * @param string New template body
	 */
	public void setTemplate(String string)
	{
		_template = string;
	}

	/**
	 * Returns a Tag body, including the tag markers.
	 * This is a utility method that may be used by special
	 * Output modules, like Master/Detail reports that need
	 * to extract and later replace subsections of a template.
	 * @param tagName
	 * @return
	 * @throws Throwable if tagName is not present in the template
	 */
	public String getTagBody(String tagName) throws Throwable
	{

		int pos1 = 0;
		int pos2 = 0;		
		
		String tagStart = "<" + tagName + ">";
		String tagEnd = "</" + tagName + ">";
		
		/* find start of repeat section */
		pos1 = _template.indexOf(tagStart);
		if (pos1>=0)
		{
				
			/* find end of repeat section */
			int newPos = pos1 + tagStart.length();
			pos2 = _template.indexOf(tagEnd, newPos);
				
			if (pos2>0)
			{
				/* extract tag body */
				return _template.substring(pos1, pos2 + tagEnd.length());
			}
			else
			{
				String args[] = {tagName};
				String msg = Errors.REPEAT_TAG_NOT_CLOSED;
				msg = MessageFormat.format(msg, (Object[])args);
				throw new Throwable(msg);
			}

		}
		else
		{
			String args[] = {tagName};
			String msg = Errors.REPEAT_TAG_NOT_FOUND;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new Throwable(msg);
		}


	}

	/**
	 * Replace field markers representing request attribute values
	 * like ${req:attributeID}
	 * @throws Throwable
	 */
	public void replaceRequestAttributes() throws Throwable
	{
		if (_ctx==null)
			throw new Throwable("Servlet Context is null - this method can't work without a ServletContext.");

		//patch june-22-2004 don't waste time
		if (_template.indexOf("${req:")< 0 )
			return;

		/* parse the template and create list of label markers */
		ArrayList<Marker> flds = getMarkers("req");
		
		for (int i=0; i<flds.size();i++)
		{
			Marker m = (Marker) flds.get(i);
			
			String name = m.getName();
			
			//PATCH 2005-04-15  - support for XML/URL encoding
			String fmt = m.getExtraInfo(); 
			if (fmt==null)
				fmt = "";
			else
				fmt = "@" + fmt;
			String label = "${" + "req:" + name + fmt + "}";
			
			/* PATCH 2004-12-06 - request markers were
			 * being eliminated if request attribute was null, creating
			 * problems for custom output modules that set request attributes
			 */
			String value = (String)_req.getAttribute(name);
			if (value!=null)
			{
				if (!fmt.equals(""))
				{
					if (fmt.equals("@xml"))
						value = encodeXML(value);
					else if (fmt.equals("@html"))
						value = encodeHTML(value);
					else if (fmt.equals("@js"))
						value = encodeJS(value);
					else if (fmt.equals("@url"))
						value = URLEncoder.encode(value, "UTF-8");
					else
						throw new Throwable("Invalid encoding directive for request attribute: " + name);
				}
				_template = StringUtil.replace(_template, label, value);
			}
		}		
	}

	/**
	 * Replace field markers representing session attribute values
	 * like ${ses:attributeID}
	 * @throws Throwable
	 */
	public void replaceSessionAttributes() throws Throwable
	{

		if (_req==null)
			throw new Throwable("Request is null - this method can't work without a Request object.");

		//patch june-22-2004 don't waste time
		if (_template.indexOf("${ses:")< 0 )
			return;

		//get session object
		HttpSession session = _req.getSession(false);
		if (session==null)
			return;

		/* parse the template and create list of label markers */
		ArrayList<Marker> flds = getMarkers("ses");
		
		for (int i=0; i<flds.size();i++)
		{
			Marker m = (Marker) flds.get(i);
			String name = m.getName();
			
			// PATCH 2005-05-25 - test existence of session attribute
			Object obj = session.getAttribute(name);
			
			if (obj==null)
			    throw new Throwable("Cannot find Session attribute [" + name + "]; UserID: " + _req.getRemoteUser() + "; Session isNew: " + session.isNew() + "; ");

			//patch 2005-06-09 - avoid errors if attribute type is not String
			String value = String.valueOf(obj);
			String label = "${" + "ses:" + name + "}";
			_template = StringUtil.replace(_template, label, value);
			
		}		

	}

	/**
	 * Encode reserved xml characters (&amp;,&lt;,&gt;,',").<br>
	 * This characters will be replaced by the pre-defined entities.
	 * @param input String that will be processed
	 * @return String with all reserved characters replaced
	 */
	public String encodeXML(String input)
	{
		input = StringUtil.replace(input, "&", "&amp;");
		input = StringUtil.replace(input, "<", "&lt;");
		input = StringUtil.replace(input, ">", "&gt;");
		input = StringUtil.replace(input, "'", "&apos;");
		input = StringUtil.replace(input, "\"", "&quot;");

		return input;
	}

	/**
	 * Encode reserved html characters (&amp;,&lt;,&gt;,',").<br>
	 * This characters will be replaced by the pre-defined entities.
	 * @param input String that will be processed
	 * @return String with all reserved characters replaced
	 */
	public String encodeHTML(String input)
	{
		input = StringUtil.replace(input, "&", "&amp;");
		input = StringUtil.replace(input, "<", "&lt;");
		input = StringUtil.replace(input, ">", "&gt;");
		input = StringUtil.replace(input, "\"", "&quot;");

		return input;
	}	

	/**
	 * Encode reserved javascript characters (\,").<br>
	 * This characters will be replaced by the pre-defined entities.
	 * @param input String that will be processed
	 * @return String with all reserved characters replaced
	 */
	public String encodeJS(String input)
	{
		input = StringUtil.replace(input, "\\", "\\\\");
		input = StringUtil.replace(input, "\"", "\\\"");
		input = StringUtil.replace(input, "'", "\\\'");
		input = StringUtil.replace(input, "\r\n", "\\r\\n");
		input = StringUtil.replace(input, "\n", "\\n");
		
		return input;
	}	
	
	/**
	 * Replace any text into the template
	 * @param toReplace Text to be replaced
	 * @param newValue New value
	 */
	public void replace(String toReplace, String newValue)
	{
		_template = StringUtil.replace(_template, toReplace, newValue);
	}

	/**
	 * Set corresponding column values for template markers<br>
	 * Patch june-28-2004 - Fast parser technique
	 * @param fte Fast template parser (new fck package)
	 * @param rs Recordset positioned on a valid record
	 * @param nullExpression String used to represent a null value
	 * @throws Throwable
	 */
	public void setValues(dinamica.parser.FastTemplateEngine fte, ArrayList<dinamica.parser.Marker> markers, Recordset rs, String nullExpression) throws Throwable
	{

		String strValue = null;
		Object value = null;
		
		/* read default date format */
		String defDateFmt = null;
		if (_ctx!=null)
			defDateFmt = _ctx.getInitParameter("def-format-date");
		if (defDateFmt==null || defDateFmt.equals(""))
			defDateFmt = "dd-MM-yyyy";				
		
		
		/* get recordset fields */
		HashMap<String, RecordsetField> rsFlds = rs.getFields();
		
		/* parse the template and create list of field markers */
		ArrayList<dinamica.parser.Marker> flds = markers;
		
		/* for each field marker set value */
		Iterator<dinamica.parser.Marker> i = flds.iterator();
		while (i.hasNext())
		{

			String formatPluginName = null;
			IFormatPlugin fmtObj = null;
			FormatPluginParser fpp = null;

		    /* get next marker */
			dinamica.parser.Marker m = (dinamica.parser.Marker)i.next();
			String fName = m.getColumnName();
			String fFormat = m.getFormat();

			/* determine if it is an special field (rowNumber/rowIndex) otherwise if the field exists */
			boolean found = true;
			boolean fakeField = false;
			if (!fName.equals("_rowNumber") && !fName.equals("_rowIndex"))
			{
				if (!rsFlds.containsKey(fName)) 
					found = false;
			}
			else
			{
				fakeField = true;
			}
							
			
			/* recordset contains this field? */
			if (found)
			{

			    if (fFormat!=null)
			    {
					//PATCH 2005-05-23 - get plugin name if available
					if (fFormat.startsWith("class:"))
					{
					    formatPluginName = fFormat.substring(6);
					    fpp = new FormatPluginParser(formatPluginName); 
					    fmtObj = (IFormatPlugin)Thread.currentThread().getContextClassLoader().loadClass(fpp.getName()).newInstance();
					}			        
			    }
			    
				/* get field value */
				value = rs.getValue(fName);

				if (fmtObj!=null)
				{
				    strValue = fmtObj.format(fName, rs, _locale, fpp.getArgs());
				}
				else
				{
					/* apply appropiate null representation */
					if (value==null) 
					{
						strValue=nullExpression;
					}
					else
					{
						

					    /* get field info */
						RecordsetField f = null;
						if (!fakeField)
							f = (RecordsetField)rsFlds.get(fName);
						else
							f = new RecordsetField(fName,"INTEGER", Types.INTEGER);
							
						
						/* format value according to data type*/
						if (f.getType()!=Types.DATE && f.getType()!=Types.TIMESTAMP )
						{
							/* format defined? */
							if (fFormat==null)
							{
								strValue = String.valueOf(value);
							}
							else
							{
								//is a string data type?
								if (f.getType()==Types.VARCHAR || f.getType()==Types.CHAR || f.getType()==Types.CLOB )
								{
									if (fFormat.equals("xml"))
									{
										//encode special characters for xml/html output
										strValue = encodeXML((String)value);
									}
									else if (fFormat.equals("html"))
									{
										//encode special characters for xml/html output
										strValue = encodeHTML((String)value);
									}
									else if (fFormat.equals("url"))
									{
										//encode special characters for xml/html output
										strValue = URLEncoder.encode((String)value, "UTF-8");
									}
									else if (fFormat.equals("js"))
									{
										//encode special characters for xml/html output
										strValue = encodeJS((String)value);
									}										
									else
									{
										throw new Throwable("Invalid format mask for the field:" + fName);
									}
									
								}
								// it is a numeric data type
								else
								{
										
									if (_locale==null)
										strValue = StringUtil.formatNumber(value, fFormat);
									else
										strValue = StringUtil.formatNumber(value, fFormat, _locale);
	
								}
							}
						}
						else
						{
							/* apply default or custom date format? */
							if (fFormat==null)
								fFormat = defDateFmt;
							if (_locale==null)
								strValue = StringUtil.formatDate((java.util.Date)value, fFormat);
							else
								strValue = StringUtil.formatDate((java.util.Date)value, fFormat, _locale);
						}
						
					}
				
				}
					
				// set value
				fte.setValue(m.getKey(), strValue);
						
			}
			
		}
		
	}

	/**
	 * Replace all markers of type ${fld:xxx} with an empty string "".<br>
	 * Added on Aug-30-2005 to support a new print mode="clear" in config.xml.
	 * @throws Throwable
	 */
	public void clearFieldMarkers() throws Throwable
	{

		String toReplace = null;
		
		/* parse the template and create list of field markers */
		ArrayList<Marker> flds = getMarkers("fld");
		
		/* for each field marker set value */
		Iterator<Marker> i = flds.iterator();
		while (i.hasNext())
		{

			/* get next marker */
			Marker m = (Marker)i.next();
			String fName = m.getName();
			String fFormat = m.getExtraInfo();

			if (fFormat!=null)
				toReplace = "${fld:" + fName + "@" + fFormat + "}";
			else
				toReplace = "${fld:" + fName + "}";
				
			/* replace marker with value */
			_template = StringUtil.replace(_template,toReplace,"");
						
		}
		
	}
	
}
