package dinamica;

import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Core-level framework class: Database API.
 * <br><br>
 * Encapsulates the JDBC API most common operations
 * with safe and efficient code. Uses the same connection
 * for all its operations. Does not close connections, that is the
 * responsability of the Controller. It has built-in detailed log support
 * for JDBC operations, activated via config.xml.
 * <br>
 * Creation date: 12/09/2003<br>
 * Last Update: 12/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class Db
{

	/** database connection */
	private Connection _conn = null;

	/** log writer for performance data */
	private PrintWriter _log = null;

	/** container for batch commands */
	private ArrayList<String> _batch = new ArrayList<String>();

	/** database version info */
	private String _dbVersion = null;

	/** tamaño del lote de registros a leer de un resultset */
	private int fetchSize = 0;
	
	/** 
	 * Permite entonar el driver JDBC para indicarle el tamaño del lote de lectura de rows
	 * cuando se crea un resultset
	 * @param size Tamaño del lote
	 */
	public void setFetchSize(int size) {
		this.fetchSize = size;
	}
	
	/**
	 * Set the log writer. This class will log
	 * all query executions -as a text report- if this object is set
	 * @param writer
	 */
	public void setLogWriter(PrintWriter writer) 
	{
		_log = writer;
	
	}

	/** Default constructor to initialize the object */
	public Db(Connection conn)
	{
		_conn = conn;
		DatabaseMetaData md;
		try
		{
			
			md = _conn.getMetaData();
			
			_dbVersion = md.getDatabaseProductName() 
			+ " " + md.getDatabaseProductVersion() 
			+ " (" + md.getDriverName() 
			+ " " + md.getDriverVersion() + ")";
			
		}
		catch (SQLException e)
		{
		}
	}

	/**
	 * Execute an action query (inserts, updates, deletes)
	 * @param sql SQL command to execute
	 * @return The number of rows affected
	 * @throws Throwable
	 */
	public int exec(String sql) throws Throwable
	{
		
		long t1 = 0;
		long t2 = 0;
		
		Statement s = null;
		int rows = 0;
		
		try
		{
			
			t1 = System.currentTimeMillis();
			
			s = _conn.createStatement();
			rows = s.executeUpdate(sql);
			
			t2 = System.currentTimeMillis();

			if (_log!=null)
			{
				long time = t2 - t1;
				_log.println("--JDBC-LOG-START");
				_log.println("DBMS: " + _dbVersion);
				_log.println("Date: " + new java.util.Date(t1));
				_log.println("Thread: " + Thread.currentThread().getName());
				_log.println("Sql: " + sql);
				_log.println("Rows affected: " + rows);
				_log.println("Time (ms): " + time);
				_log.println("--JDBC-LOG-END");
			}
			
			return rows;
		}
		
		catch (Throwable e)
		{
			String err = e.getMessage() + " [" + sql  + "]";
			throw new Throwable(err, e);
		}
		
		finally
		{
			if (s!=null) s.close();
		}
		
	}

	/**
	 * Add sql to list of commands to be executed
	 * in batch using the method exec(). These commands will be accumulated
	 * until the exec() methods is called, then the internal buffer will be cleared.
	 * @param sql SQL Command (insert, update or delete)
	 */
	public void addBatchCommand(String sql)
	{
		_batch.add(sql);
	}

	/**
	 * Execute commands in batch - the commands must be previously loaded
	 * into this object using the method addBatchCommand(). This method
	 * will clear the batch command buffer after execution.
	 * @param sql SQL command to execute
	 * @return Array where each element contains the number of rows affected by each statement
	 * @throws Throwable
	 */
	public int[] exec() throws Throwable
	{
		
		long t1 = 0;
		long t2 = 0;
		
		Statement s = null;
		int rows[] = null;
		
		try
		{
			
			t1 = System.currentTimeMillis();
			
			s = _conn.createStatement();
			for (int i=0; i<_batch.size(); i++)
			{
				s.addBatch((String)_batch.get(i));
			}
			rows = s.executeBatch();
			
			t2 = System.currentTimeMillis();

			if (_log!=null)
			{
				long time = t2 - t1;
				_log.println("--JDBC-LOG-START");
				_log.println("DBMS: " + _dbVersion);
				_log.println("Date: " + new java.util.Date(t1));
				_log.println("Thread: " + Thread.currentThread().getName());
				_log.println("Batch size: " + _batch.size());
				_log.println("Time (ms): " + time);
				_log.println("--JDBC-LOG-END");
			}
			
			//PATCH 2005-09-02 clear internal batch array list after execute
			_batch.clear();
			//END PATCH
			
			return rows;
		}
		catch (SQLException sqe)
		{
			if (sqe.getNextException()!=null)
				throw new Throwable(sqe.getNextException().getMessage(), sqe.getCause());
			else
				throw sqe;
		}		
		catch (Throwable e)
		{
			throw e;
		}
		
		finally
		{
			if (s!=null) s.close();
		}
		
	}

	/**
	 * Creates a recordset given a SQL query. 
	 * @param conn Database Connection
	 * @param sql SQL Query that returns a Resultset
	 * @throws Throwable
	 */
	public Recordset get(String sql) throws Throwable
	{
		return get(sql, 0);
	}

	/**
	 * Creates a recordset given a SQL query. 
	 * @param conn Database Connection
	 * @param sql SQL Query that returns a Resultset
	 * @param limit Maximum number of rows to read from the DataBase
	 * @throws Throwable
	 */
	public Recordset get(String sql, int limit) throws Throwable
	{

		long t1 = 0;
		long t2 = 0;
		long l1 = 0;
		long l2 = 0;

		ResultSet rs = null;
		Statement stmt = null;

		try
		{

			t1 = System.currentTimeMillis();

			/* execute query */
			stmt = _conn.createStatement();

			stmt.setFetchSize(fetchSize);
			
			if (limit > 0)
				stmt.setMaxRows(limit);

			rs = stmt.executeQuery(sql);
			
			t2 = System.currentTimeMillis();

			/* create recordset given the resultset */
			l1 = System.currentTimeMillis();
			Recordset recs = new Recordset(rs);
			l2 = System.currentTimeMillis();

			if (_log!=null)
			{
				long time1 = t2 - t1;
				long time2 = l2 - l1;
				_log.println("--JDBC-LOG-START");
				_log.println("DBMS: " + _dbVersion);
				_log.println("Date: " + new java.util.Date(t1));
				_log.println("Thread: " + Thread.currentThread().getName());
				_log.println("sql: " + sql);
				_log.println("Records: " + recs.getRecordCount());
				_log.println("Columns: " + recs.getFieldCount());
				_log.println("Query execution time (ms): " + time1);
				_log.println("Data loading time (ms): " + time2);
				_log.println("--JDBC-LOG-END");
				
			}
			
			return recs;
			
		}
		catch (Throwable e)
		{
			String err = e.getMessage() + " [" + sql  + "]";
			throw new Throwable(err, e);
		}
		finally
		{
			if( rs != null ) rs.close(); 
			if( stmt != null ) stmt.close();
		}
		
	}

	/**
	 * Execute prepared statement using a recordset; only the current record is used
	 * @param sql SQL command to precompile
	 * @param rs Recordset containing the values to be set into the prepared statement
	 * @param params Array containing the names of the fields to use. The order must match the place holders (those ? marks) in the prepared statement
	 * @return The number of records affected
	 * @throws Throwable - if the params array contains no elements or contains more elements than the recordset field count
	 */
	public int exec(String sql, Recordset rs, String[] params) throws Throwable
	{

		long t1 = 0;
		long t2 = 0;
	
		/* check for invalid parameters */
		if (params.length == 0 || params.length > rs.getFieldCount())
		{
			String args[] = {String.valueOf(params.length), String.valueOf(rs.getFieldCount())};
			String msg = Errors.INVALID_STMT_PARAMS;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new Throwable(msg);
		}
		
		PreparedStatement s = null;
		int rows = 0;
		
		try
		{
			
			t1 = System.currentTimeMillis();
			
			s = _conn.prepareStatement(sql);
			
			/* get recordset metadata*/
			HashMap<String, RecordsetField> flds = rs.getFields();
			
			/* set value for each field */
			for (int i=0;i<params.length;i++)
			{
				RecordsetField f = (RecordsetField)flds.get(params[i]);
				Object value = rs.getValue(params[i]);
				if (value==null)
				{
					s.setNull(i+1, f.getType());	
				}
				else
				{
					//PATCH 2005-04-15 - support for SQLDATE instead of DATE
					if (f.getType()==java.sql.Types.DATE)
					{
						java.util.Date d = (java.util.Date)value;
						value = new Date(d.getTime());
					}
					s.setObject(i+1, value, f.getType());
				}
			}
			
			rows = s.executeUpdate();
			
			t2 = System.currentTimeMillis();

			if (_log!=null)
			{
				long time = t2 - t1;
				_log.println("--JDBC-LOG-START");
				_log.println("DBMS: " + _dbVersion);
				_log.println("Date: " + new java.util.Date(t1));
				_log.println("Thread: " + Thread.currentThread().getName());
				_log.println("Sql: " + sql);
				_log.println("Rows affected: " + rows);
				_log.println("Time (ms): " + time);
				_log.println("--JDBC-LOG-END");
			}
			
			return rows;
		}
		
		catch (Throwable e)
		{
			String err = e.getMessage() + " [" + sql  + "]";
			throw new Throwable(err, e);
		}
		
		finally
		{
			if (s!=null) s.close();
		}

	}

	/**
	 * Execute batch prepared statement using a recordset; all the records are used
	 * @param sql SQL command to precompile
	 * @param rs Recordset containing the values to be set into the prepared statement
	 * @param params Array containing the names of the fields to use. The order must match the place holders (those ? marks) in the prepared statement
	 * @return The number of records affected
	 * @throws Throwable - if the params array contains no elements or contains more elements than the recordset field count
	 */
	public int[] execBatch(String sql, Recordset rs, String[] params) throws Throwable
	{

		long t1 = 0;
		long t2 = 0;
	
		/* check for invalid parameters */
		if (params.length == 0 || params.length > rs.getFieldCount())
		{
			String args[] = {String.valueOf(params.length), String.valueOf(rs.getFieldCount())};
			String msg = Errors.INVALID_STMT_PARAMS;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new Throwable(msg);
		}
		
		PreparedStatement s = null;
		int rows[] = null;
		
		try
		{
			
			t1 = System.currentTimeMillis();
			
			s = _conn.prepareStatement(sql);

			/* get recordset metadata*/
			HashMap<String, RecordsetField> flds = rs.getFields();

			/* navigate each record in the recordset */
			rs.top();
			while (rs.next())
			{

				/* set value for each field */
				for (int i=0;i<params.length;i++)
				{
					RecordsetField f = (RecordsetField)flds.get(params[i]);
					Object value = rs.getValue(params[i]);
					if (value==null)
					{
						s.setNull(i+1, f.getType());	
					}
					else
					{
						//PATCH 2005-04-15 - support for SQLDATE instead of DATE
						if (f.getType()==java.sql.Types.DATE)
						{
							java.util.Date d = (java.util.Date)value;
							value = new Date(d.getTime());
						}						
						s.setObject(i+1, value, f.getType());
					}
				}

				/* add batch statement */
				s.addBatch();

			}
			
			/* execute all commands */
			rows = s.executeBatch();
			
			t2 = System.currentTimeMillis();

			if (_log!=null)
			{
				long time = t2 - t1;
				_log.println("--JDBC-LOG-START");
				_log.println("DBMS: " + _dbVersion);
				_log.println("Date: " + new java.util.Date(t1));
				_log.println("Thread: " + Thread.currentThread().getName());
				_log.println("Sql: " + sql);
				_log.println("Batch size: " + rs.getRecordCount());
				_log.println("Time (ms): " + time);
				_log.println("--JDBC-LOG-END");
			}
			
			return rows;
		}
		
		catch (SQLException sqe)
		{
			if (sqe.getNextException()!=null)
				throw new Throwable(sqe.getNextException().getMessage(), sqe.getCause());
			else
				throw sqe;
		}
		
		catch (Exception e)
		{
			String err = e.getMessage() + " [" + sql  + "]";
			throw new Throwable(err, e);
		}
		
		finally
		{
			if (s!=null) s.close();
		}

	}

	/**
	 * Save binary file to blob column using a prepared statement.<br>
	 * The prepared statement must contain only one dynamic parameter (?),
	 * and it must correspond to the BLOB column. Example:<br>
	 * insert into images (id, title, imgsize, data) values (1,'my image', 8112, ?)
	 * <br><br>
	 * This means that the SQL must be pre-processed by your code in order to
	 * set the static values. GenericTransaction superclass provides the method getSql()
	 * to help you achieve easy static SQL generation.
	 * @param sql SQL used to build prepared statement. The blob column will be the only dynamic (?) parameter.
	 * @param path File to be uploaded into the blob column
	 * @throws Throwable
	 */
	public void saveBlob(String sql, String path) throws Throwable
	{

		/* create buffer to read image data */
		File f = new File( path );
		FileInputStream img = new FileInputStream(f);		
		int size = (int)f.length();
		BufferedInputStream buf = new BufferedInputStream(img);

		/* save image using prepared statement */
		PreparedStatement p = null;
            
		try
		{
			p = _conn.prepareStatement(sql);
			p.setBinaryStream(1, buf, size);
			p.execute();
		}
		catch (SQLException sqe)
		{
			Throwable t = null;
			String msg = null;
			String date = StringUtil.formatDate(new java.util.Date(), "dd-MM-yyyy HH:mm:ss");
			
			if (sqe.getNextException()!=null) {
				msg = sqe.getNextException().getMessage();
				t = sqe.getCause();
			}
			else {
				msg = sqe.getMessage();
				t = sqe;
			}
			System.err.println("[WARNING@" + date + "] Db.saveBlob failed: " + msg + " SQL: [" + sql + "]");
			throw new Throwable("Error cargando el archivo en base de datos", t);
				
		}		
		catch (Throwable e)
		{
			String date = StringUtil.formatDate(new java.util.Date(), "dd-MM-yyyy HH:mm:ss");
			System.err.println("[WARNING@" + date + "] Db.saveBlob failed: " + e.getMessage() + " SQL: [" + sql + "]");
			throw new Throwable("Error cargando el archivo en base de datos", e);
		}
		finally
		{
			if (p!=null) p.close();
			if (img!=null) img.close();
			if (buf!=null) buf.close();
		}

	}

	/**
	 * Execute query and return value of a given column
	 * @param sql Query to execute and retrieve a recordset
	 * @param columnName Name of the column whose value will be retrieved
	 * @return Value of column as Object or null if recordset is empty
	 * @throws Throwable
	 */
	public Object getColValue(String sql, String columnName) throws Throwable
	{
		Recordset rs = get(sql);
		if (rs.getRecordCount()>0)
		{
			rs.first();
			return rs.getValue(columnName);
		}
		else
			return null;
	}

	/**
	 * Execute query and return value of a given column
	 * @param sql Query to execute and retrieve a recordset
	 * @param columnName Name of the column whose value will be retrieved
	 * @return Value of column as int
	 * @throws Throwable if query fails or if recordset is empty 
	 */
	public int getIntColValue(String sql, String columnName) throws Throwable
	{
		Recordset rs = get(sql);
		if (rs.getRecordCount()>0)
		{
			rs.first();
			return rs.getInt(columnName);
		}
		else
			throw new Throwable("Recordset is empty!");
	}

	/**
	 * Execute query and return value of a given column
	 * @param sql Query to execute and retrieve a recordset
	 * @param columnName Name of the column whose value will be retrieved
	 * @return Value of column as double
	 * @throws Throwable if query fails or if recordset is empty 
	 */	
	public double getDoubleColValue(String sql, String columnName) throws Throwable
	{
		Recordset rs = get(sql);
		if (rs.getRecordCount()>0)
		{
			rs.first();
			return rs.getDouble(columnName);
		}
		else
			throw new Throwable("Recordset is empty!");
	}
	
	/**
	 * Iniciar transaccion JDBC.<br>
	 * Asume que en caso de error
	 * el Controller del framework deberia hacer el rollback,
	 * y de lo contrario tambien ocuparse del commit. La intencion 
	 * de este metodo es dar un control a las clases Transaction de
	 * en que momento dar inicio a una transaccion, para los casos
	 * donde deben hacerse varias consultas antes de proceder a modificar
	 * la data, de esta manera, la transaccion no engloba a los SELECTs y se
	 * minimizan los bloqueos.
	 * @throws Throwable encapsulando a una SQLException
	 */
	public void beginTrans() throws Throwable
	{
		try {
			_conn.setAutoCommit(false);
		} catch (SQLException e) {
			throw new Throwable(e);
		}
	}
	
	/**
	 * Hacer el commit de una transacción, para los casos
	 * donde se necesita control programático de una transacción
	 * dentro de una clase de lógica de negocios. Son casos aislados,
	 * procesos batch con optimizaciones muy particulares.
	 * @throws Throwable
	 */
	public void commit() throws Throwable
	{
		try {
			_conn.commit();
		} catch (SQLException e) {
			throw new Throwable(e);
		}
	}
	
}
