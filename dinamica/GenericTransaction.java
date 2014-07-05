package dinamica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.naming.*;
import javax.sql.DataSource;
import dinamica.xml.*;


/**
 * Base class to program business transaction services (database read/write).
 * All transactions should subclass from this class. This class already provides
 * some useful features, like executing an arbitrary number of SELECT queries
 * declared in config.xml as [recordset] elements. In many cases, your Action
 * will only need this class as its business logic or Transaction. If you
 * subclass from this class (highly recommended when writing your own business logic classes),
 * you should call super.service() at some point inside your own service() implementation, this
 * way you can reuse lots of generic code that executes queries defined in config.xml, etc. 
 * 
 * <br>
 * Creation date: 2003-10-04<br>
 * Last Update: 2007-10-25<br>
 * (c) 2003-2008 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class GenericTransaction extends AbstractModule
{
	
	/** store recordsets published by this service */
	private HashMap<String, Recordset> _publish = new HashMap<String, Recordset>();
	
	/**
	 * Publish recordset to be consumed by Output modules
	 * @param key Recordset ID
	 * @param data Recordset object
	 */
	protected void publish(String key, Recordset data) throws Throwable
	{
		_publish.put(key, data);

		/* get recordset simple metadata (recordcount, pagecount, etc) */
		data.setID(key);
		Recordset info = data.getRecordsetInfo();
			
		/* publish this new recordset */
		String infoID = key + ".metadata";
		_publish.put(infoID, info);
				
	}
	
	/**
	 * Transaction service - this method must be redefined
	 * by descendants of this class, include a super.service(inputParams)
	 * as the first line of your service() method code to reuse base
	 * functionality (auto-creation of recordsets based on recordset elements defined in config.xml).
	 * In this method the business logic will be contained, and results will be represented
	 * as recordsets that will be consumed by Output objects. Recordsets are published using
	 * the method publish(id, rsObject). This class provides a method to retrieve
	 * a recordset using its ID and throws an error if the recordset is not present in the HashMap.<br>
	 * If inputParams is not null then it is published with the id "_request".
	 * @param inputParams Request parameters pre-validated and represented as a Recordset with one record.
	 * Recordset fields are set according to the data types defined in the validator.xml file.
	 * @return 0 if success - any other return values are user defined
	 * @throws Throwable
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		int rc = createRecordsets(inputParams);
		
		if (inputParams!=null)
			_publish.put("_request", inputParams);
		
		return rc;
		
	}
	
	/**
	 * Create recordsets using config.xml parameters.
	 * For recordsets created using SQL templates, all values
	 * from the inputParams recordset will be auto-replaced into
	 * the template. This recordset is only created when using
	 * a validator (validator.xml definition to auto-validate request parameters). 
	 * @throws Throwable in case of invalid config.xml parameters or JDBC exceptions.
	 */
	protected int createRecordsets(Recordset inputParams) throws Throwable
	{
	
		int rc = 0;
	
		/* get database object */
		Db db = getDb();
	
		/* get recordsets config */
		Recordset rs = _config.getRecordsets();
		Recordset rs1 = null;
		
		/* for each defined recordset */
		while (rs.next())
		{
			
			/* get parameters */
			String id = (String)rs.getValue("id");
			String source = (String)rs.getValue("source");
			String scope = (String)rs.getValue("scope");
			String onempty = (String)rs.getValue("onempty");
			String maxRows = (String)rs.getValue("maxrows");
			int limit = 0;
			if (maxRows!=null)
				limit = Integer.parseInt(maxRows);
			String dataSrc = rs.getString("datasource");
			String params = rs.getString("params");
			String totalCols = rs.getString("totalCols");

			String fetchSize = (String)rs.getValue("fetchsize");
			if (fetchSize==null)
				db.setFetchSize(0);
			else
				db.setFetchSize(Integer.parseInt(fetchSize));
			
			/* create recordset using appropiate source */			
			if (source.equals("sql"))
			{
				String sqlFile = getResource(id);
				sqlFile = this.getSQL(sqlFile, inputParams);
				
				if (params!=null)
				{
					// PATCH 2005-04-05 support for alternative input parameters recordset for SQL templates
					Recordset rsParams = getRecordset(params);
					if (rsParams.getRecordCount()>0)
						rsParams.first();
					else
						throw new Throwable("The recordset [" + params + "] used to replace SQL template values is empty.");
					sqlFile = this.getSQL(sqlFile, rsParams);
				}
				
				//PATCH 2005-03-14 support datasource defined at recordset level
				if (dataSrc==null)
				{
					if (limit>0)
						rs1 = db.get(sqlFile, limit);
					else
						rs1 = db.get(sqlFile);
				}
				else
				{
					rs1 = dbGet(dataSrc, sqlFile, limit);
				}
				
				if (onempty!=null)
				{
					//patch 2009-07-08 - do not evaluate anymore recordsets if on-empty rule matches
					if (rs1.getRecordCount()==0) {
						rc = Integer.parseInt(onempty);
						break;
					}
				}
			}
			else if (source.equals("session"))
			{
				rs1 = (Recordset)getSession().getAttribute(id);
				//PATCH 2005-03-01 - enhance error message if session attribute is null
				if (rs1==null)
					throw new Throwable("Recordset [" + id + "] not found in Session attribute, maybe the application was reloaded, destroying the session.");
			}
			else if (source.equals("request"))
			{
				rs1 = (Recordset)_req.getAttribute(id);
				if (rs1==null)
					throw new Throwable("Request attribute [" + id + "] does not contain a recordset.");
			}
			else if (source.equals("textfile"))
			{
				rs1 = this.getRsFromFlatFile(id);
			}
			else if (source.equals("class"))
			{
			    IRecordsetProvider rsProv = (IRecordsetProvider)getObject(id);
				rs1 = rsProv.getRecordset(inputParams);
				if (onempty!=null){
					if (rs1.getRecordCount()==0)
					rc = Integer.parseInt(onempty);	
				}				
			} else if (source.equals("total"))
			{
				//patch 2010-04-08 soporte para sumar o totalizar en memoria un campo
				if ( source.equals("total") && totalCols==null)
					throw new Throwable ("El atributo [totalCols] no ha sido definido para el elemento: recordset.");
				if ( source.equals("total") && params==null)
					throw new Throwable ("El atributo [params] no ha sido definido para el elemento: recordset.");
				
				//obtener array de los valores
				String cols[] = StringUtil.split(totalCols, ";");
				//armar recordset de total
				rs1 = new Recordset();
				for (int i = 0; i < cols.length; i++) {
					rs1.append(cols[i], java.sql.Types.DOUBLE);
				}
				rs1.addNew();
				
				//recordset de donde se sumara cada registro
				Recordset rsParams = getRecordset(params);
				
				//realizar operacion de sumatoria
				computeTotal(rs1, rsParams, cols);
			}
			else
			{
				throw new Throwable("Invalid recordset source in config.xml (" + _config.path + "). Source attribute values can be sql, session, textfile or request only.");
			}

			/* publish this recordset */
			_publish.put(id, rs1);
			
			/* get recordset simple metadata (recordcount, pagecount, etc) */
			rs1.setID(id);
			Recordset info = rs1.getRecordsetInfo();
			
			/* publish this new recordset */
			String infoID = id + ".metadata";
			_publish.put(infoID, info);
			
			/* persist recordset if necessary (in session or request object */
			if (scope.equals("session"))
			{
				getSession().setAttribute(id, rs1);
			}
			else if (scope.equals("request"))
			{
				_req.setAttribute(id, rs1);
			}
			else if (!scope.equals("transaction"))
			{
				throw new Throwable("Invalid recordset scope in config.xml (" + _config.path + "). Scope attribute values can be transaction, session or request only.");
			}
			
		}
		
		return rc;
	}
	
	/**
	 * Realiza la operacion de sumatoria de los valores de todos los registros de un
	 * recordset dado los campos.
	 * @param rsTotal Recordset que contendra los valores a sumar
	 * @param rsMaster Recordset que contiene los campos a sumar
	 * @param cols Array con los campos a sumar
	 * @throws Throwable
	 */
	protected void computeTotal(Recordset rsTotal, Recordset rsMaster, String cols[]) throws Throwable {
		
		rsTotal.first();
		for (int i = 0; i < cols.length; i++) {
			double total = rsMaster.getSUM(cols[i]);
			rsTotal.setValue(cols[i], total);	
		}
		
	}
	
	/**
	 * Returns a recordset published by this transaction
	 * @param id ID or symbolic name which was used to publish the
	 * recordset - either by code or using the config.xml elements.
	 * @return Recordset
	 * @throws Throwable if ID oes not match any of the IDs of the published recordsets
	 */
	public Recordset getRecordset(String id) throws Throwable
	{
		if (_publish.containsKey(id))
		{
			return (Recordset)_publish.get(id);
		}
		else
		{
			throw new Throwable("Invalid recordset ID: " + id);
		}
	}
	
	/**
	 * Generate SQL command. Encapsulates the use of the TemplateEngine
	 * class, to make it easier for developers writing Transaction Modules
	 * @param sql SQL Template
	 * @param rs Recordset with at least one record - there must be
	 * a current record
	 * @return SQL command with replaced values
	 * @throws Throwable
	 */
	protected String getSQL(String sql, Recordset rs) throws Throwable
	{
		
		TemplateEngine t = new TemplateEngine(_ctx,_req, sql);
		
		//patch 2010-03-23 - soportar un nuevo tipo de marker ${lst:colName@rsID} para
		//incluir listas de valores para clausulas IN (a,b,c)
		if (sql.indexOf("${lst:")>0) {
			ArrayList<Marker> markers = t.getMarkers("lst");
			if (markers.size()>0) {
				for (Iterator<Marker> iterator = markers.iterator(); iterator.hasNext();) {
					Marker marker = iterator.next();
					String colName = marker.getName();
					String rsID = marker.getExtraInfo();
					String newMarker = "${lst:" + colName + "@" + rsID + "}";
					
					//recuperar recordset sea del request o del hashmap interno de esta clase
					Recordset rsList = (Recordset)getRequest().getAttribute(rsID);
					if (rsList==null)
						rsList = getRecordset(rsID);
					
					String values = getSqlIN(rsList, colName);
					t.replace(newMarker, values);
				}
			}
		}
		
		return t.getSql(rs);
		
	}
	
	/**
	 * Load the appropiate class and creates an object
	 * that MUST subclass GenericTransaction. This method is
	 * used by Transactions that delegate work on "subtransaction"
	 * objects. All these classes subclass GenericTransaction to inherit all the
	 * code supporting business logic programming. You may define your
	 * own methods in those classes, they are intended to refactor
	 * common business code that may be used by multiple Transactions.<br>
	 * Typically, you will use code like this:<br>
	 * <pre>
	 * MyOwnClass obj = (MyOwnClass)getObject("mypackage.MyOwnClass");
	 * obj.myMethod();
	 * </pre>
	 * <br>
	 * An object created this way inherits all the power of
	 * the GenericTransaction, including the availability of
	 * security information (current user), access to the same
	 * database connection as the caller, etc. Both objects participate
	 * in the same JDBC Transaction if this feature was enabled in
	 * the config.xml file.
	 * 
	 * @param className Name of the class to instantiate
	 * @return An object of class GenericTransaction
	 * @throws Throwable
	 */
	protected GenericTransaction getObject(String className) throws Throwable
	{

		GenericTransaction t = null;

		/* load transaction class */
		t = (GenericTransaction) Thread.currentThread().getContextClassLoader().loadClass(className).newInstance();
		t.init(_ctx, _req, _res);
		t.setConfig(_config);
		t.setConnection(_conn);
					
		/* log jdbc performance? */
		t.setLogWriter(_pw);

		return t;
		
	}
	
	/**
	 * Create a recordset with all the fields
	 * required to produce a chart with ChartOutput. This recordset
	 * will contain no records.
	 * @return Recordset with the column structure required by 
	 * the class ChartOutput
	 * @throws Throwable
	 */
	public Recordset getChartInfoRecordset() throws Throwable
	{
		/* define chart params recordset */
		Recordset rs = new Recordset();
		rs.append("chart-plugin", java.sql.Types.VARCHAR);
		rs.append("title", java.sql.Types.VARCHAR);
		rs.append("title-x", java.sql.Types.VARCHAR);
		rs.append("title-y", java.sql.Types.VARCHAR);
		rs.append("column-x", java.sql.Types.VARCHAR);
		rs.append("column-y", java.sql.Types.VARCHAR);
		rs.append("title-series", java.sql.Types.VARCHAR);
		rs.append("width", java.sql.Types.INTEGER);
		rs.append("height", java.sql.Types.INTEGER);
		rs.append("data", java.sql.Types.VARCHAR);
		rs.append("dateformat", java.sql.Types.VARCHAR);
		
		//added on april-06-2004
		rs.append("session", java.sql.Types.VARCHAR); //true|false: save in session?
		rs.append("image-id", java.sql.Types.VARCHAR);//session attribute id

		//added on july-19-2005
		rs.append("color", java.sql.Types.VARCHAR); //true|false: save in session?

		//added on march-17-2010
		rs.append("labelx-format", java.sql.Types.VARCHAR); //formato para etiquetas en el eje X
		rs.append("tick-unit", java.sql.Types.INTEGER); //escala para eje X
		
		
		return rs;		
	}

	/**
	 * Return DataSource object using JNDI prefix
	 * configured in web.xml context parameter. This is an
	 * utility method to help simplify Transaction code. A DataSource
	 * can be obtained with a single line of code:<br><br>
	 * <pre>
	 * javax.sql.DataSource ds = getDataSource("jdbc/customersDB");
	 * setConnection(ds.getConnection);
	 * ....
	 * </pre>
	 * <br>
	 * Remember that when you use your own datasource, you
	 * must close the connection in your Transaction code! consult
	 * the reference guide ("Sample code" section) for more information.
	 * 
	 * @param name Name of the datasource (Example: jdbc/customersdb)
	 * @return DataSource object
	 * @throws Throwable If DataSource cannot be obtained
	 */
	protected DataSource getDataSource(String name) throws Throwable
	{

	  //get datasource config from web.xml
	  String jndiPrefix = "";
	  if (getContext()!=null)
	  	jndiPrefix = getContext().getInitParameter("jndi-prefix");
	  else
	  	jndiPrefix = "java:comp/env/";
	  
	  if (jndiPrefix==null)
	  	jndiPrefix="";
	  
	  DataSource ds = null;
	  if (!name.startsWith(jndiPrefix))
		  ds = Jndi.getDataSource(jndiPrefix + name);
	  else
		  ds = Jndi.getDataSource(name);
	  
	  if (ds==null)
	  	throw new Throwable("Can't get datasource: " + name);
	  	
	  return ds;
	  		
	}

	/**
	 * Return the default application DataSource object
	 * as configured in web.xml context parameters. This is a
	 * utility method to help simplify Transaction code. A DataSource
	 * can be obtained with a single line of code:<br><br>
	 * <pre>
	 * javax.sql.DataSource ds = getDataSource();
	 * setConnection(ds.getConnection());
	 * ....
	 * </pre>
	 * <br>
	 * Remember that when you use your own datasource, you
	 * must close the connection in your Transaction code! please consult
	 * the reference guide ("Sample code" section) for more information.
	 * 
	 * @return DataSource object
	 * @throws Throwable If DataSource cannot be obtained
	 */
	protected DataSource getDataSource() throws Throwable
	{

	  //get datasource config from web.xml
	  String jndiPrefix = null;
	  String name = null;
	  
	  if (getContext()!=null)
	  {
	  	
	  	if (getConfig().transDataSource!=null)
	  		name = getConfig().transDataSource;
	  	else
			name = getContext().getInitParameter("def-datasource");
			
		jndiPrefix = getContext().getInitParameter("jndi-prefix");
		
	  }
	  else
		throw new Throwable("This method can't return a datasource if servlet the context is null.");
	  
	  if (jndiPrefix==null)
		jndiPrefix="";
	  
	  DataSource ds = Jndi.getDataSource(jndiPrefix + name);
	  if (ds==null)
		throw new Throwable("Can't get datasource: " + name);
	  	
	  return ds;
	  		
	}

	/**
	* Returns an "env-entry" value stored in web.xml.
	* @param name env-entry-name element
	**/
	protected String getEnvEntry(String name) throws Throwable
	{
     	
		Context env = (Context) new InitialContext().lookup("java:comp/env");
		String v = (String) env.lookup(name);
		return v;

	}
	
	/**
	 * Retrieve internal HashMap containing all published Recordsets
	 * in case some output module needs to serialize this object
	 * or anything else
	 * @return HashMap containing all published Recordsets
	 */
	public HashMap<String, Recordset> getData()
	{
		return _publish;
	}
	
	/**
	 * Utility method to retrieve a recordset from a different data source
	 * than the one used by the action
	 * @param DataSourceName Data Source name like "jdbc/xxxx"
	 * @param sql SQL command that returns a result set
	 * @param limit The maximum number of rows to retrieve (0 = no limit) 
	 * @return
	 * @throws Throwable
	 */
	protected Recordset dbGet(String DataSourceName, String sql, int limit) throws Throwable
	{
		java.sql.Connection conn = getDataSource(DataSourceName).getConnection();
		try
		{
			Db db = new Db(conn);
			
			if (this._pw!=null)
				db.setLogWriter(_pw);
			
			if (limit>0)
				return db.get(sql, limit);
			else
				return db.get(sql);
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (conn!=null)
				conn.close();
		}		
	}
	
	/**
	 * Utility method to retrieve a recordset from a different data source
	 * than the one used by the action
	 * @param DataSourceName Data Source name like "jdbc/xxxx"
	 * @param sql SQL command that returns a result set
	 * @return
	 * @throws Throwable
	 */
	protected Recordset dbGet(String DataSourceName, String sql) throws Throwable
	{
		return dbGet(DataSourceName, sql, 0);
	}	
	
	/**
	 * Creates a recordset according to a structure defined in a
	 * flat file. The 1st line defines the column types, the second line
	 * defines the column names. From 3rd line begins data records. Columns
	 * are separated by TAB, rows are separated by CR+NL.
	 * @param path Path to flat file defining recordset structure and data. If path starts with "/..." it is interpreted as a location relative
	 * to the context, otherwise it is assumed to be located in the Action's path.
	 * @return Recordset according to the flat file structure
	 * @throws Throwable
	 */
	protected Recordset getRsFromFlatFile(String path) throws Throwable
    {
            Recordset rs = new Recordset();
            String data = getResource(path);
            String lineSep = "\r\n";
            if (data.indexOf(lineSep)<0)
            	lineSep = "\n";
            	
            String rows[] = StringUtil.split(data, lineSep);
            String listseparator = "\t";

            //adjust which list separator is used, tab or comma or semicolon
            if (rows[0].indexOf(",") != -1)
                    listseparator = ",";
            else
	            if (rows[0].indexOf(";") != -1)
	                    listseparator = ";";

            String fields[] = StringUtil.split(rows[0], listseparator);
            String names[] = StringUtil.split(rows[1], listseparator);
            boolean is_blank_row = false;

            if (fields.length!=names.length)
                    throw new Throwable("Row #2 (column names) does not match the right number of columns.");

            for (int i=0;i<fields.length;i++)
            {
                    if (fields[i].toLowerCase().equals("varchar"))
                            rs.append(names[i], java.sql.Types.VARCHAR);
                    else if (fields[i].toLowerCase().equals("date"))
                            rs.append(names[i], java.sql.Types.DATE);
                    else if (fields[i].toLowerCase().equals("integer"))
                            rs.append(names[i], java.sql.Types.INTEGER);
                    else if (fields[i].toLowerCase().equals("double"))
                            rs.append(names[i], java.sql.Types.DOUBLE);
                    else {
                            throw new Throwable("Invalid column type [" + fields[i] +"]. Valid column types are: varchar, date, integer and double.");
                    }

            }

            for (int i=2;i<rows.length;i++)
            {
                    //here is a empty line
                    if(rows[i].equals(""))
                            continue;

                    //add a record if not all of last row is null,flatfile recordset does not allow all fields is null
                    if(!is_blank_row)
                        rs.addNew();

                    //initial flag
                    is_blank_row = true;

                    String value[] = StringUtil.split(rows[i], listseparator);

                    //if (fields.length!=value.length)
                    //      throw new Throwable("Row #" + i + " does not match the right number of columns.");

                    for (int j=0;j<Math.min(fields.length,value.length);j++)
                    {
                            //replace lables such as ${lbl:},${ses:},%{req:}
                            value[j] = getSQL(value[j],null);

                            //if this field is null,then set field null.if all fields is null,is_blank_row will equal true.
                            if(value[j].equals("")||value[j].toLowerCase().equals("null"))
                            {
                                    rs.setValue(names[j], null);
                            }
                            else
                            {
                                    is_blank_row = false;
                                    if (fields[j].toLowerCase().equals("varchar"))
                                            rs.setValue(names[j], value[j]);
                                    else if (fields[j].toLowerCase().equals("date"))
                                    {
                                            if(value[j].indexOf("@") != -1) //formated date value
                                            {
                                                    String date[] = StringUtil.split(value[j], "@");
                                                    rs.setValue(names[j], StringUtil.getDateObject(date[0],date[1]));
                                            }
                                            else
                                            {
                                                    rs.setValue(names[j], StringUtil.getDateObject(value[j],"yyyy-MM-dd"));
                                            }
                                    }
                                    else if (fields[j].toLowerCase().equals("integer"))
                                            rs.setValue(names[j], new Integer(value[j]));
                                    else if (fields[j].toLowerCase().equals("double"))
                                            rs.setValue(names[j], new Double(value[j]));
                            }
                    }
            }

            //remove the last row if all field is null
            if(is_blank_row)
                    rs.delete(rs.getRecordNumber());

            return rs;
    }
	
	/**
	 * Ejecutar un comando SQL para insert, update o delete usando un DataSource específico
	 * que puede ser distinto al DataSource por defecto del Action que ejecuta esta clase.
	 * Es importante notar que la ejecución de este query no formará parte de la transacción
	 * JDBC que pudiera englobar a la ejecución de este Action.
	 * @param dataSourceName Nombre del pool de conexiones, como "jdbc/dinamica" por ejemplo.
	 * @param sql Comando SQL de modificación de datos.
	 * @return La cantidad de registros afectados por la ejecución del comando SQL
	 * @throws Throwable Si ocurre algún error ejecutando el comando SQL, no deja conexiones abiertas.
	 */
	protected int dbExec(String dataSourceName, String sql) throws Throwable
	{

		java.sql.Connection conn = getDataSource(dataSourceName).getConnection();
		try
		{
			Db db = new Db(conn);
			
			if (this._pw!=null)
				db.setLogWriter(_pw);
			
			return db.exec(sql);
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (conn!=null)
				conn.close();
		}			
		
	}
	
	/**
	 * Retorna un recordset dado un array unidimensional, tendra un solo campo y un registro por cada elemento del array.
	 * Es un metodo utilitario que sirve para recibir datos concatenados en un solo parametro, separados por ";" u otro caracter,
	 * que son convertidos en un array -con StringUtil.split()- y luego en registros individuales para un insert en batch, por ejemplo.<br>
	 * Si la data representa fechas, las mismas deben venir en el formato especificado por def-input-date en web.xml.
	 * @param data Array conteniendo la data
	 * @param colName Nombre que tendra el campo en el recordset
	 * @param dataType Tipo de dato: java.sql.Types.VARCHAR, DOUBLE, DATE, INTEGER.
	 * @return Recordset conteniendo un registro por cada elemento del array
	 * @throws Throwable
	 */
	protected Recordset getRsFromArray(String data[], String colName, int dataType) throws Throwable
	{
		
		/* load default date format used to convert date strings to Date objects */
		String dateFormat = _ctx.getInitParameter("def-input-date");
		
		Recordset rs = new Recordset();
		rs.append(colName, dataType);
		
		for (int i = 0; i < data.length; i++) {
			rs.addNew();
			switch (dataType) {
				case java.sql.Types.INTEGER:
					rs.setValue(colName, new Integer(data[i]));
					break;
				case java.sql.Types.DATE:
					java.util.Date d = ValidatorUtil.testDate(data[i], dateFormat);
					if (d==null)
						throw new Throwable("El valor no representa una fecha: " + data[i] + " en el formato: " + dateFormat);
					rs.setValue(colName, d);
					break;
				case java.sql.Types.DOUBLE:
					rs.setValue(colName, new Double(data[i]));
					break;
				default:
					rs.setValue(colName, data[i]);
					break;
			}
		}
		
		return rs;

	}

	/**
	 * Retorna la lista de valores que se utiliza con el filtro "IN (....)"
	 * de una clausula WHERE de una consulta SQL.<br>
	 * Dado un recordset, retorna un String que contiene una lista
	 * de valores tomados de una columna del recordset, separados por coma
	 * y encerrados entre parentesis. Dependiendo del tipo de dato de la columna,
	 * los valores estaran representados como literales VARCHAR o como numeros enteros.
	 * Solo soporta campos de tipo VARCHAR o INTEGER.<br><br>
	 * @param rs Recordset
	 * @param colName Nombre de la columna a usar
	 * @return Clausula IN o un string vacio si el recordset no tiene registros
	 * @throws Throwable
	 */
	public String getSqlIN(Recordset rs, String colName) throws Throwable 
	{
		String sql = "";
		
		int type = rs.getField(colName).getType();
		StringBuilder b = new StringBuilder();
		b.append( "(" );
		rs.top();
		while (rs.next()) {
			switch (type) {
			case java.sql.Types.VARCHAR:
			case java.sql.Types.CHAR:				
				String v = StringUtil.replace(rs.getString(colName),"'","''");
				b.append("'" + v + "'" + ",");
				break;
			case java.sql.Types.INTEGER:
			case java.sql.Types.BIGINT:
				b.append(rs.getString(colName) + ",");
				break;
			}
		}
		b.deleteCharAt(b.length()-1);
		b.append( ")" );
		
		if (rs.getRecordCount()>0)
			sql = b.toString();
		return sql;
	}
	
	/**
	 * Envía email de manera declarativa usando la configuración de config.xml en caso de que haya
	 * sido definida y de que el atributo enabled=true en el elemento mail, por ejemplo:<br><br>
	 * <xmp>
	 * 	<mail enabled="true">
	 *	<subject>Confirmación de registro en el Sistema Registro de Gastos</subject>
	 *	<mail-to recordset="_request" addressColName="email"/>
	 *	<body recordset="getid.sql">message.txt</body>
	 * </mail>
	 * </xmp>
	 * <br><br>
	 * Esta facilidad requiere que se configuren los siguientes parámetros en web.xml:<br>
	 * mail-server, mail-from y mail-from-displayname.<br><br>
	 * Este método es invocado por el Controller, luego de que el método service() fue ejecutado. Si no hay
	 * configuración para enviar email retornará de inmediato, así que el overhead es mínimo.
	 * Si hay problemas aplicando la configuración, la transacción será abortada con una excepción
	 * y un rollback de ser necesario, pero un envio fallido de email no causará esto, de hecho el
	 * email se envía en background, si da problemas los mismos serán registrados en STDERR, pero
	 * no causan que se aborte la transacción.<br>
	 * @throws Throwable
	 */
	public void sendEmail() throws Throwable {
		
		//busca si hay configuracion activa
		Element e = getConfig().getDocument().getElement("mail");
		if (e==null) 
			return;
		
		String enabled = e.getAttribute("enabled");
		if (enabled==null || !enabled.equalsIgnoreCase("true"))
			return;
		
		//procesar el contenido del email
		Element bodyElem = getConfig().getDocument().getElement(e, "body");
		String body = getResource(bodyElem.getValue());
		String rsBodyID = bodyElem.getAttribute("recordset");
		Recordset rsBody = null;
		if (rsBodyID!=null)
			rsBody = getRecordset(rsBodyID);
		body = getEmailBody(body, rsBody);
		
		//procesar el asunto, si contiene markers seran reemplazados si se indico un recordset
		Element subjectElem = getConfig().getDocument().getElement(e, "subject");
		String rsSubjectID = subjectElem.getAttribute("recordset");
		Recordset rsSubject = null;
		if (rsSubjectID!=null)
			rsSubject = getRecordset(rsSubjectID);
		String subject = subjectElem.getValue();
		subject = getEmailSubject(subject, rsSubject);
		
		//procesar destinatario(s) si no vienen de un recordset, entonces el valor del
		//elemento mail-to en config.xml deberia indicar el destinatario
		String mailTo = null;
		Element mailToElem = getConfig().getDocument().getElement(e, "mail-to");
		String rsMailToID = mailToElem.getAttribute("recordset");
		String colName = mailToElem.getAttribute("colname");
		Recordset rsMailTo = null;
		if (rsMailToID!=null) {
			StringBuilder sb = new StringBuilder();
			rsMailTo = getRecordset(rsMailToID);
			
			//patch 2009-09-10
			if (rsMailTo.getRecordCount()==0)
				throw new Throwable("El recordset " + "[" + rsMailToID + "] usado para los destinatarios del email no puede estar vacío.");
			
			rsMailTo.top();
			while (rsMailTo.next()) {
				sb.append(rsMailTo.getString(colName) + ",");
			}
			sb.deleteCharAt(sb.length()-1); //remover ultima coma
			mailTo = sb.toString();
		}
		else
			mailTo = mailToElem.getValue();
		
		if (mailTo==null)
			throw new Throwable("Parámetro de configuración mail-to inválido.");
		
		//leer configuracion centralizada para envio de email
		String host = getMailServerAddress();
		String mailFrom = getEmailFrom();
		String mailFromDisplayName = getEmailFromDisplayName();
		
		//enviar email usando un thread en background
		SimpleMail s = new SimpleMail();
		s.send(host, mailFrom, mailFromDisplayName, mailTo, subject, body);		
		
	}
	
	/**
	 * Retorna el contenido del email, se lee de config.xml y puede
	 * inyectarse data de un recordset. Este método se puede sobreescribir
	 * si se necesita algo más específico.
	 * @param template Template de contenido, se lee de config.xml y debe ser un archivo de texto
	 * contenido en el directorio del Action en cuestión.
	 * @param rs Recordset que debe contener 1 registro. Puede ser nulo. 
	 */
	public String getEmailBody(String template, Recordset rs) throws Throwable
	{
		return replaceRecordset(template, rs);
	}

	/**
	 * Retorna el asunto del email, se lee de config.xml y puede
	 * inyectarse data de un recordset si el template contiene markers. Este método se puede sobreescribir
	 * si se necesita algo más específico.
	 * @param template Template para el asunto, se lee de config.xml
	 * @param rs Recordset que debe contener 1 registro. Puede ser nulo. 
	 */
	public String getEmailSubject(String template, Recordset rs) throws Throwable
	{
		return replaceRecordset(template, rs);
	}
	
	/**
	 * Reemplaza los datos del 1er registro de un recordset (que usualmente tendrá
	 * solo un registro) dentro de un template, usando mode=form.
	 * @param template String que contiene el template
	 * @param rs Recordset con el registro que contiene los datos a inyectar dentro del template. Puede ser nulo.
	 * @return El template con los valores inyectados
	 * @throws Throwable
	 */
	protected String replaceRecordset(String template, Recordset rs) throws Throwable
	{
		TemplateEngine t = new TemplateEngine(getContext(),getRequest(), template);
		t.replaceDefaultValues();
		t.replaceLabels();
		t.replaceRequestAttributes();
		t.replaceSessionAttributes();
		if (rs!=null) {
			rs.first();
			t.replace(rs, "");
			if (!rs.getID().equals("_request"))
				t.replace(getRecordset("_request"), "");
		}
		return t.toString();
	}
	
	/**
	 * Retorna la direccion de email de remitente, por defecto la
	 * lee de web.xml, parametro "mail-from".<br> Se debe poner
	 * una direccion que pertenezca al dominio del mail server SMTP
	 * que se utiliza para enviar el email, de lo contrario será rechazado
	 * por los servidores de destino. Puede sobreescribir este método
	 * si necesita proveer este campo de manera programática.
	 * @return String con la direccion de email del remitente
	 * @throws Throwable
	 */
	public String getEmailFrom() throws Throwable
	{
		String value = getContext().getInitParameter("mail-from");
		if (value==null || value.equals(""))
			throw new Throwable("Parámetro inválido en web.xml: mail-from");
		return value;
	}
	
	/**
	 * Retorna un nombre descriptivo del remitente, por defecto lo
	 * lee de web.xml, parametro "mail-from-displayname".<br> Puede sobreescribir este método
	 * si necesita proveer este campo de manera programática.
	 * @return String con el nombre descriptivo del remitente
	 * @throws Throwable
	 */
	public String getEmailFromDisplayName() throws Throwable
	{
		String value = getContext().getInitParameter("mail-from-displayname");
		if (value==null || value.equals(""))
			throw new Throwable("Parámetro inválido en web.xml: mail-from-displayname");
		return value;
	}
	
	/**
	 * Retorna la direccion del servidor de correo SMTP a utilizar para enviar email, por defecto la
	 * lee de web.xml, parametro "mail-server".<br> Puede ser una direccion IP
	 * o un nombre de host. Puede sobreescribir este método si necesita proveer este campo de manera programática.
	 * @return String con la direccion del servidor SMTP
	 * @throws Throwable
	 */
	public String getMailServerAddress() throws Throwable
	{
		String value = getContext().getInitParameter("mail-server");
		if (value==null || value.equals(""))
			throw new Throwable("Parámetro inválido en web.xml: mail-server");
		return value;
	}	
	
}
