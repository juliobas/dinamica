package dinamica;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.sql.*;

/**
 * Core-level framework class: Recordset - General purpose, tabular data structure.
 * <br><br>
 * Represents a disconnected recordset, a kind of multi-purpose ResultSet
 * without connection to the database, much like
 * the famous ADO disconnected recordset from VB6, 
 * can be created from a SQL query or manufactured
 * by code to represent any tabular data. It is a kind of
 * generic data container that may save a lot of work because
 * it can be used to represent several entities avoiding the need
 * to write custom classes to represent each specific entity.
 * It is serializable, so it can be safely used in HTTP Sessions, JNDI, etc. 
 * <br><br>
 * This recordset mantains all data in memory and does not need
 * an open connection to the database, provides several constructors
 * to isolate the programmer from the task of programming with the
 * JDBC API, but also provides low level access if desired.
 * <br><br>
 * Each field in the recordset is mantained in its native data type
 * as represented by the JDBC driver, or a java null value.
 * <br>
 * <br>
 * Creation date: 10/09/2003<br>
 * Last Update: 11/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class Recordset implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** contains list of fields */
	private HashMap<String, RecordsetField> _fields = new HashMap<String, RecordsetField>();

	/** contains list of records */
	private ArrayList<Record> _data = new ArrayList<Record>();

	/** recordset position (0...N-1)*/
	private int _recordNumber = -1;

	/** paging support */
	private int _pageCount = 0;
	private int _pageSize = 0;
	private int _currentPage = 0;	

	/** recordset ID */
	private String _ID = null; 

	
	private String _lastSortColName = null;
	private String _lastSortMode = null;
	
	/**
	 * Return records per page (page size) for recordsets 
	 * used in paged views
	 * @return
	 */
	public int getPageSize()
	{
		return _pageSize;
	}
	
	/**
	 * Create a recordset with one record containing
	 * general recordset parameters like: recordcount, pagecount, currentpage 
	 * @return Recordset Object
	 * @throws Throwable
	 */
	public Recordset getRecordsetInfo() throws Throwable
	{
		
		Recordset rs = new Recordset();
		rs.append("recordcount", Types.INTEGER);
		rs.append("pagecount", Types.INTEGER);
		rs.append("currentpage", Types.INTEGER);
		rs.append(_ID + ".recordcount", Types.INTEGER);
		
		rs.addNew();
		rs.setValue("recordcount", Integer.valueOf(_data.size()));
		rs.setValue("pagecount", Integer.valueOf(_pageCount));
		rs.setValue("currentpage", Integer.valueOf(_currentPage));
		rs.setValue(_ID + ".recordcount", Integer.valueOf(_data.size()));
		
		return rs;
		
	}

	/**
	 * Set recordset ID (an arbitrary String)
	 * @param id
	 */
	public void setID(String id)
	{
		_ID = id;
	}

	/**
	 * Retrieve ID of this Recordset
	 * @return String representing the ID
	 */
	public String getID() {
		return _ID;
	}
	
	/**
	 * Paging support.<br>
	 * Returns the number of record pages determined by setPageSize
	 */
	public int getPageCount()
	{
		return _pageCount;
	}

	/**
	 * Paging support.<br>
	 * Returns the current page number (1...N)
	 */
	public int getPageNumber()
	{
		return _currentPage;
	}
	

	/**
	 * Paging support.<br>
	 * Define page size (records per page)
	 */
	public void setPageSize(int p) throws Throwable
	{
	
		if (p <=0 )
		{
			throw new Throwable("Invalid page size, must be > 0!");
		}

		if (_data.size() == 0)
		{
			throw new Throwable("Invalid page size, recordset is empty!");
		}
	
		_pageSize = p;

		java.math.BigDecimal b1 = new java.math.BigDecimal(_data.size());
		java.math.BigDecimal b2 = new java.math.BigDecimal(_pageSize);
		_pageCount = b1.divide(b2,java.math.BigDecimal.ROUND_UP).intValue();
		
		if (getRecordCount()>0)
			_currentPage = 1;
		
	}


	/**
	 * Paging support.<br>
	 * Get Recordset representing the requeste page of records
	 */
	public Recordset getPage(int p) throws Throwable
	{
	
		if (p < 1 || p > _pageCount)
			throw new Throwable("Invalid page number: " + p + " - the Recordset contains " + _pageCount + " pages.");
	
		_currentPage = p;
	
		//calculate first and last row numbers for this page
		int row1 = (p - 1) * _pageSize;
		int row2 = (p * _pageSize) - 1;
		if (row2 > (_data.size()-1))
			row2 = _data.size() - 1;

		 
		//create an identical recordset containing only the records for the requested page
		ArrayList<Record> newData = new ArrayList<Record>(_pageSize);
		for (int i=row1;i<=row2;i++)
		{
			newData.add(_data.get(i));
		}
		
		//new recordset representing data page
		Recordset x = new Recordset();
		
		//copy metadata
		HashMap<String,RecordsetField> newFields = new HashMap<String,RecordsetField>(_fields.keySet().size());
		newFields.putAll(_fields);
		x.setFields(newFields);
		
		//set recordset data
		x.setData(newData);
		
		//return new recordset containing page/
		return x;			
	
	}

	/**
	 * Feed the recordset metadata (column structure)
	 * @param fields HashMap containing the recordset field objects
	 */
	protected void setFields(HashMap<String, RecordsetField> fields)
	{
		_fields = fields;
	}
	
	/**
	 * Feed the recordset data
	 * @param data ArrayList containing record objects
	 */
	protected void setData(ArrayList<Record> data)
	{
		this._data = data;
	}

	/**
	 * Returns the current record position (0...N-1)
	 * @return
	 */
	public int getRecordNumber()
	{
		return _recordNumber;
	}

	/**
	 * Returns the number of records in recordset
	 * @return
	 */
	public int getRecordCount()
	{
		return _data.size();
	}

	/**
	 * Returns number of fields in recordset
	 * @return
	 */
	public int getFieldCount()
	{
		return _fields.size();
	}

	/**
	 * Returns HashMap containing RecordsetField objects
	 * representing the Recordset fields
	 * @return 
	 */
	public HashMap<String, RecordsetField> getFields()
	{
		return _fields;
	}

	/**
	 * Returns ArrayList containing the recordset data (the records)
	 * @return 
	 */
	public ArrayList<Record> getData()
	{
		return _data;
	}

	/**
	 * Append a field to the recordset structure.<br>
	 * It is used when creating a Recordset from a JDBC query
	 * @param fieldName Field Name
	 * @param nativeSqlType SQL native data type name
	 * @param type JDBC data type (java.sql.Types)
	 */
	private void append(String fieldName, String nativeSqlType, int type)
	{
		RecordsetField f = new RecordsetField(fieldName, nativeSqlType, type);
		_fields.put(fieldName, f);
	}

	/**
	 * Append a field to the recordset structure.<br>
	 * It is used when manufacturing a Recordset from code
	 * @param fieldName Field Name
	 * @param nativeSqlType SQL native data type name
	 * @param type JDBC data type (java.sql.Types) - only INTEGER, LONG, VARCHAR, DATE, TIMESTAMP or DOUBLE are supported
	 */
	public void append(String fieldName, int type) throws RecordsetException
	{
		
		String sqlTypeName = null;
		
		switch (type)
		{
			case Types.INTEGER:
				sqlTypeName = "INTEGER";
				break;
				
			case Types.BIGINT:
				sqlTypeName = "LONG";
				break;

			case Types.VARCHAR: case Types.CHAR:
				sqlTypeName = "VARCHAR";
				break;

			case Types.DATE:
				sqlTypeName = "DATE";
				break;

			case Types.TIMESTAMP:
				sqlTypeName = "TIMESTAMP";
				break;

			case Types.DOUBLE:
				sqlTypeName = "DOUBLE";
				break;
				
		}

		if (sqlTypeName==null)
		{
			String args[] = {String.valueOf(type)};
			String msg = Errors.INVALID_DATATYPE;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new RecordsetException(msg);			
		}
		
		append(fieldName, sqlTypeName, type);
		
	}

	/**
	 * Add a record to the recordset and set record number position
	 * to the new inserted record
	 *
	 */
	public void addNew()
	{
		
		HashMap<String, Object> values = new HashMap<String, Object>();
		
		Iterator<String> i = _fields.keySet().iterator();
		while (i.hasNext())
		{
			String f = i.next();
			values.put(f, null);
		}
		
		_data.add(new Record(values));
		
		/* set record number */ 
		_recordNumber++;
				
	}

	/**
	 * Set record position inside recordset
	 * @param recNum Record Number (0...getRecordCount()-1)
	 * @throws Throwable
	 */
	public void setRecordNumber(int recNum) throws RecordsetException
	{
		
		checkRecordPosition(recNum);		
		_recordNumber = recNum;
		
	}

	/**
	 * Set field value for current record (determined by getRecordNumber())
	 * @param fieldName Field Name
	 * @param value Field Value (Date, String, int, double, null)
	 * @throws Throwable
	 */
	public void setValue(String fieldName, Object value) throws RecordsetException
	{

		checkRecordPosition();		
				
		RecordsetField f = null;
		try {
			f = getField(fieldName);
		} catch (Throwable e) {
			throw new RecordsetException(e.getMessage());
		}
		
		if (value!=null)
		{
			switch (f.getType()) {
			
				case java.sql.Types.DATE:
					if (!(value instanceof java.util.Date))
						throw new RecordsetException("Invalid data type of field: " + fieldName + "; passed value must be a DATE object.");
					break;
						
				case java.sql.Types.INTEGER:
					if (!(value instanceof java.lang.Integer) && !(value instanceof java.lang.Long))
						throw new RecordsetException("Invalid data type of field: " + fieldName + "; passed value must be an INTEGER object.");
					break;
	
				case java.sql.Types.DOUBLE:
					if (!(value instanceof java.lang.Double))
						throw new RecordsetException("Invalid data type of field: " + fieldName + "; passed value must be an DOUBLE object.");
					break;
			}
		}
		
		Record rec = _data.get(_recordNumber );
		rec.setValue(fieldName, value);
		
	}

	/**
	 * Return field value given a field name
	 * @param fieldName Field Name. May be reserved field names: _rowIndex (0...N-1) or _rowNumber (1...N)
	 * @return
	 * @throws Throwable
	 */
	public Object getValue(String fieldName) throws Throwable
	{

		/* check for valid cursor position */
		checkRecordPosition();
				
		/* special treatment for reserved field names */
		if (fieldName.equals("_rowIndex"))
		{
			return Integer.valueOf(_recordNumber);
		}
		else if (fieldName.equals("_rowNumber"))
		{
			return Integer.valueOf(_recordNumber + 1);
		}
		else
		{

			Record rec = (Record)_data.get(_recordNumber );
			Object value = rec.getFieldValue(fieldName);
			
			//patch 2009-08-24 - convertir campos CLOB a String
			if (value!=null) {
				RecordsetField f = _fields.get(fieldName);
				if (f.getType()==java.sql.Types.CLOB) {
					java.sql.Clob clob = (java.sql.Clob)value; 
					value =  clob.getSubString((long)1, (int)clob.length());
				}
			}
			return value;
		}

	}

	/**
	 * Fill recordset with resultset data and metadata. It is the
	 * responsability of the caller of this method to close the resultset
	 * and other jdbc objects involved. The resultset must be positioned before
	 * the first record
	 * @param rs Resultset
	 * @throws Throwable
	 */
	private void loadRecords(java.sql.ResultSet rs) throws Throwable
	{

		/* load field definitions */
		ResultSetMetaData md = rs.getMetaData();
		int cols = md.getColumnCount();
		for (int i=1;i<=cols;i++)
		{
			append(md.getColumnLabel(i).toLowerCase(), md.getColumnTypeName(i), md.getColumnType(i));
		}
		
		/* load data */
		while (rs.next())
		{
			HashMap<String, Object> flds = new HashMap<String, Object>(cols);
			for (int i=1;i<=cols;i++)
			{
				flds.put(md.getColumnLabel(i).toLowerCase(), rs.getObject(i));
			}
			_data.add(new Record(flds));
		}
		
	}

	/**
	 * Create a recordset given a resultset. It is the
	 * responsability of the caller of this method to close the resultset
	 * and other jdbc objects involved.
	 * @param rs ResultSet positiones before the first record
	 * @throws Throwable
	 */
	public Recordset(ResultSet rs) throws Throwable
	{
		loadRecords(rs);
	}

	/**
	 * Creates a recordset given a SQL query
	 * @param conn Database Connection
	 * @param sql SQL Query that returns a Resultset
	 * @throws Throwable
	 */
	public Recordset(Connection conn, String sql) throws Throwable
	{
		this(conn, sql, 0);
	}

	/**
	 * Creates a recordset given a SQL query. 
	 * @param conn Database Connection
	 * @param sql SQL Query that returns a Resultset
	 * @param limit Maximum number of rows to read from the DataBase
	 * @throws Throwable
	 */
	public Recordset(java.sql.Connection conn, String sql, int limit) throws Throwable
	{
		
		ResultSet rs = null;
		Statement stmt = null;

		try
		{

			/* execute query */
			stmt = conn.createStatement();
			
			if (limit > 0)
				stmt.setMaxRows(limit);
			
			rs = stmt.executeQuery(sql);
			loadRecords(rs);
			
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if( rs != null ) rs.close(); 
			if( stmt != null ) stmt.close();
		}
		
	}

	/**
	 * Default constructor used when creating 
	 * the recordset from code (a "manufactured" recordset)
	 *
	 */
	public Recordset()
	{
		/* default fields */
		//append("_rowIndex", "INTEGER", Types.INTEGER);
		//append("_rowNumber", "INTEGER", Types.INTEGER);
	}

	/**
	 * Move pointer to next record. Returns true if not EOF
	 * @return
	 */
	public boolean next() 
	{
		if (_recordNumber < (_data.size()-1))
		{
			_recordNumber++;
			return true;
		}		
		else
		{
			return false;
		}
	}

	/**
	 * Returns a text-formatted report with the
	 * structure of the recordset, very usefull for
	 * debugging purposes
	 */
	public String toString()
	{
		StringWriter sw = new StringWriter(1000);
		PrintWriter pw = new PrintWriter(sw);
		
		pw.println("Recordset Information");
		pw.println("Record Count: " + getRecordCount());
		pw.println("Field Count: " + getFieldCount());
		pw.println("Structure:");
		pw.println("----------------------------------");
		pw.println("NAME|SQL-TYPE-NAME|JDBC-TYPE-ID");
		
		Iterator<RecordsetField> i = _fields.values().iterator();
		while (i.hasNext())
		{
			RecordsetField f = (RecordsetField)i.next();
			
			pw.println(f.getName() + "|" + f.getSqlTypeName() + "|" + f.getType());
		}
		
		return sw.toString();
	}

	/**
	 * Set cursor position before first record,
	 * it is like a "rewind" command
	 *
	 */
	public void top()
	{
		_recordNumber = -1;
	}

	/**
	 * Set cursor position on first record
	 * @throws Throwable
	 */
	public void first() throws Throwable
	{
		setRecordNumber(0);
	}

	/**
	 * Set cursor position on last record
	 * @throws Throwable
	 */
	public void last() throws Throwable
	{
		setRecordNumber(_data.size()-1);
	}

	/**
	 * Delete record (from memory)
	 * @param recNum Record Number (0..N-1)
	 */
	public void delete(int recNum) throws Throwable
	{
		checkRecordPosition(recNum);
		_data.remove(recNum);
		_recordNumber--;
	}

	/**
	 * Return a Recordset field object describing its properties
	 * @param fieldName Field name to locate the field object
	 * @return Reference to Recordset field
	 * @throws Throwable if fieldName does not exist in Recordset metadata
	 */
	public RecordsetField getField(String fieldName) throws Throwable
	{
		if (_fields.containsKey(fieldName))
			return (RecordsetField)_fields.get(fieldName);
		else
			throw new Throwable("Field not found:" + fieldName);
	}

	/**
	 * Copy record values from this recordset to
	 * a destination recordset, using the current
	 * record of both recordsets. Destination recordset
	 * fields that match names with source recordset fields
	 * must be of the same type too. It does not matter if some
	 * fields from the source recordset are not defined in the destination
	 * recordset. Only name-matching fields will be considered
	 * for the operation. 
	 * @param rs Destination recordsets
	 * @throws Throwable
	 */
	public void copyValues(Recordset rs) throws Throwable
	{
		checkRecordPosition();
		HashMap<String, RecordsetField>  flds = rs.getFields();
		Iterator<RecordsetField> i = _fields.values().iterator();
		while (i.hasNext())
		{
			RecordsetField f = (RecordsetField)i.next();
			String name = f.getName();
			if (flds.containsKey(name))
			{
				rs.setValue(name, getValue(name));
			}
		}
		
	}

	/**
	 * Wrapper method for getValue() - avoids casting the data type
	 * @param colName Column name to retrieve its value from the current record
	 * @return The column value in its native data type
	 * @throws Throwable
	 */
	public String getString(String colName) throws Throwable
	{
		Object obj = getValue(colName);
		if (obj!=null)
			return String.valueOf(obj);
		else
			return null;
	}

	/**
	 * Wrapper method for getValue() - avoids casting the data type
	 * @param colName Column name to retrieve its value from the current record
	 * @return The column value in its native data type
	 * @throws Throwable
	 */
	public java.util.Date getDate(String colName) throws Throwable
	{
		java.util.Date d = null;
		d = (java.util.Date)getValue(colName);
		return d;
	}

	/**
	 * Wrapper method for getValue() - avoids casting the data type
	 * @param colName Column name to retrieve its value from the current record
	 * @return The column value in its native data type
	 * @throws Throwable
	 */
	public double getDouble(String colName) throws Throwable
	{
		Double d = new Double( String.valueOf(getValue(colName)) );
		return d.doubleValue();
	}

	/**
	 * Wrapper method for getValue() - avoids casting the data type
	 * @param colName Column name to retrieve its value from the current record
	 * @return The column value in its native data type
	 * @throws Throwable
	 */
	public int getInt(String colName) throws Throwable
	{
		Integer i = new Integer(String.valueOf(getValue(colName)));
		return i.intValue();
	}

	/**
	 * Wrapper method for getValue() - avoids casting the data type
	 * @param colName Column name to retrieve its value from the current record
	 * @return The column value in its native data type
	 * @throws Throwable
	 */
	public Integer getInteger(String colName) throws Throwable
	{
		Integer i = new Integer(String.valueOf(getValue(colName)));
		return i;
	}

	/**
	 * Tests if the give column value is null for the
	 * current record
	 * @param colName Column name
	 * @return TRUE if the value is null
	 * @throws Throwable If record position is not valid or the column does not exist in the recordset
	 */
	public boolean isNull(String colName) throws Throwable
	{
		if (getValue(colName)==null)
			return true;
		else
			return false;
	}

	/**
	 * Check if recordset contains field with a given name
	 * @param name Name of the field to check its existence
	 * @return TRUE if field exists, FALSE if not
	 */
	public boolean containsField(String name)
	{
		if (_fields.containsKey(name))
			return true;
		else
			return false;
	}

	/**
	 * Tests if a given record number represents
	 * a valid record position in the Recordset
	 * @param recNum Record number (between 0...N-1 where N is the number of records)
	 * @throws RecordsetException If the test fails
	 */
	private void checkRecordPosition(int recNum) throws RecordsetException
	{
		if (recNum < 0 || recNum > _data.size()-1)
		{

			StringBuffer errMsg = new StringBuffer();
			
			errMsg.append("Invalid record position: " + recNum + "; ");
			if (recNum==-1)
				errMsg.append("After creating a Recordset you must move to a valid record using next(), first(), last() or setRecordNumber() methods before attempting read/write operations with any record of this Recordset; ");
			errMsg.append("This Recordset contains " + _data.size() + " record(s); Set the record position between 0 and N-1 where N is the number of records.");

			throw new RecordsetException(errMsg.toString());			

		}		
	}

	/**
	 * Tests if the current record number represents
	 * a valid record position in the Recordset. This overload
	 * reuses the checkRecordPosition(int recNum) method.
	 * @throws RecordsetException If the test fails
	 */
	private void checkRecordPosition() throws RecordsetException
	{
		checkRecordPosition(this._recordNumber);		
	}

	/**
	 * Set the children recordset for the current record
	 * @param rs Children recordset
	 * @throws Throwable If the record position is not valid
	 */
	public void setChildrenRecordset(Recordset rs) throws Throwable
	{
		checkRecordPosition();
		Record rec = (Record)_data.get(_recordNumber);
		rec.setChildren(rs);	
	}

	/**
 	 * Retrieve current record's children recordset
	 * @return A reference to the children recordset or null if no children recordset exists
	 * @throws Throwable If the record position is not valid
	 */
	public Recordset getChildrenRecordset() throws Throwable
	{
		checkRecordPosition();
		Record rec = (Record)_data.get(_recordNumber);
		return rec.getChildren();	
	}

	/**
	 * Sort Recordset data in ascending order by the given column
	 * @param col Name of the column to be used for the sort
	 * @throws Throwable
	 */
	public void sort(String col) throws Throwable
	{
    
		//patch 2007-06-19
		//when ordering for 2nd time on the same column, apply descending order
		if (_lastSortColName!=null && _lastSortColName.equals(col))
		{
			if (_lastSortMode==null)
				_lastSortMode = "desc";
			else
				_lastSortMode = null;
				
		} else
		{
			_lastSortMode = null;
			_lastSortColName = col;
		}
		
		Comparator<Record> comp = new Comp(col, _lastSortMode);
		Collections.sort(_data, comp);
    	
	}

	/**
	 * Comparator inner class that provides sorting support
	 */
	class Comp implements Serializable, Comparator<Record>
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String _sortCol = null;
		private String _sortMode = null;
		
		public Comp(String colName, String sortMode) throws Throwable
		{
			_sortMode = sortMode;
			_sortCol = colName;
			
			if (!containsField(colName))
				throw new Throwable("Invalid column name passed to sort() method: " + colName);
		}
		
		
		public int compare(Record r1, Record r2)  
		{
			
			int result = 0;

			try
			{
				if (r1.getFieldValue(_sortCol)==null)
				{
					result = 0; 
				}
				else if (r2.getFieldValue(_sortCol)==null)
				{
					result = 1;
				}
				else
				{
					@SuppressWarnings("unchecked")
					Comparable<Object> x1 = (Comparable<Object>)r1.getFieldValue(_sortCol);
					result = x1.compareTo(r2.getFieldValue(_sortCol));
					
					//descending order?
					if (_sortMode!=null) {
						if (result<0)
							result = 1;
						else if (result > 0)
							result = -1;
					}
				}
			}
			catch (Throwable e)
			{
				System.err.println("SORT ERROR: " + e.getMessage());
			}
			
			return result;
			
		}

	}

	/**
	 * Retrieve recordset metadata. This method returns
	 * a Recordset containing the columns: name, typename, typeid.
	 * "typeid" is the java.sql.Type value<br>
	 * There will be one record for each column.
	 * @return Recordset
	 * @throws Throwable
	 */
	public Recordset getMetaData() throws Throwable
	{

		// prepare the new Recordset structure
		Recordset rs = new Recordset();		
		rs.append("name", java.sql.Types.VARCHAR);
		rs.append("typename", java.sql.Types.VARCHAR);
		rs.append("typeid", java.sql.Types.INTEGER);

		//get recordset structure
		HashMap<String, RecordsetField>  flds = this.getFields();
		Iterator<RecordsetField> i = flds.values().iterator();                

		// fill the structure with the data
		while (i.hasNext()) 
		{
				RecordsetField f = (RecordsetField) i.next();
				rs.addNew();
				rs.setValue("name", f.getName() );
				rs.setValue("typename", f.getSqlTypeName() );
				rs.setValue("typeid", Integer.valueOf(f.getType()) );
		}
				
		return rs;

	}        

	/**
	 * Duplicate the Recordset structure in a new Recordset without any data
 	* @return Empty Recordset with the same structure
 	* @throws Throwable
 	*/
	public Recordset copyStructure() throws Throwable
	{

		//new recordset
		Recordset newRS  = new Recordset();
		
		//get metadata
		Recordset infoRS = getMetaData();
		infoRS.top();
		while (infoRS.next()) 
		{
				// obtain the column's data
				String name = infoRS.getString("name");
				int jdbcTypeId = infoRS.getInt("typeid");

				//add column def
				newRS.append(name, jdbcTypeId);
		}
        
		return newRS;

	}

	/**
	 * Clear current record values, set every field's value to null
	 */
	public void clear() throws Throwable
	{
		checkRecordPosition();
		Iterator<RecordsetField> i = _fields.values().iterator();
		while (i.hasNext())
		{
			RecordsetField f = (RecordsetField)i.next();
			setValue(f.getName(), null);
		}
	}

	/**
	 * Find a record where a column's value matches a given value
	 * @param colName Column to use for the search
	 * @param value Value to search for
	 * @return Record position (0...N-1) or -1 if not found
	 * @throws Throwable
	 */
	public int findRecord(String colName, int value) throws Throwable
	{
		int rc = -1;
		
		top();
		while (next())
		{
			if (value==getInt(colName))
			{
				rc = this.getRecordNumber();
				break;
			}
		}
		
		return rc;
	}

	/** 
	* Find a record where a column's value matches a given value
	* @param colName Column to use for the search
	* @param value Value to search for
	* @return Record position (0...N-1) or -1 if not found
	* @throws Throwable
	*/
	public int findRecord(String colName, String value) throws Throwable
	{
		int rc = -1;
		
		top();
		while (next())
		{
			if ( value.equals(getString(colName)) )
			{
				rc = this.getRecordNumber();
				break;
			}
		}
		
		return rc;
	}

	/** 
	* Find a record where a column's value matches a given Date value
	* @param colName Column to use for the search
	* @param value Date value to search for
	* @return Record position (0...N-1) or -1 if not found
	* @throws Throwable
	*/
	public int findRecord(String colName, java.util.Date value) throws Throwable
	{
		int rc = -1;
		
		top();
		while (next())
		{
			if (value.compareTo(getDate(colName))==0) 
			{
				rc = this.getRecordNumber();
				break;
			}
		}
		
		return rc;
	}	

	/**
	 * Sumar o totalizar el valor de una columna para todos los registros del recordset
	 * @param colName Nombre de la columna a sumar
	 * @return El resultado de la suma, si el recordset esta vacio retorna cero.
	 * @throws Throwable
	 */
	public double getSUM(String colName) throws Throwable
	{
		double total = 0;
		
		top();
		while (next())
		{
			total = total + getDouble(colName);
		}
		
		return total;
	}
	
}
