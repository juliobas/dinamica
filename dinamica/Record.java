package dinamica;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility class for Recordset: represents a record inside a recordset
 * <br>
 * Creation date: 10/09/2003<br>
 * Last Update: 29/april/2004<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class Record  implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** container for field|value pairs */
	HashMap<String, Object> _record = new HashMap<String, Object>();

	/** every record in a recordset can contain a children recordset */
	Recordset _children = null;

	/**
	 * Quick constructor to build a record given a HashMap with field|value pairs
	 * @param values Record values
	 */
	public Record(HashMap<String, Object> values)
	{
		_record = values;
	}

	/**
	 * Returns the field value (may be null). Throws exception if
	 * the field is not defined in the record 
	 * @param fieldName Field name to retrieve value
	 * @return
	 * @throws Throwable
	 */
	public Object getFieldValue(String fieldName) throws RecordsetException
	{

		if (!_record.containsKey(fieldName))
		{
			//patch 2010-03-10 - print current record field names to help detecting errors
			StringBuilder colNames = new StringBuilder();
			Set<String> s = _record.keySet();
			for (Iterator<String> iterator = s.iterator(); iterator.hasNext();) {
				colNames.append(iterator.next() + ";") ;
			}
			
			String args[] = {fieldName, colNames.toString()};
			String msg = Errors.FIELD_NOT_FOUND;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new RecordsetException(msg);
		}
		return _record.get(fieldName);
			
	}

	/**
	 * Set a field's value - throws exception if field does not exist
	 * @param fieldName Field Name
	 * @param value Value (Date, String, double, int, null)
	 * @throws Throwable
	 */
	public void setValue(String fieldName, Object value) throws RecordsetException
	{
	
		if (!_record.containsKey(fieldName))
		{
			
			//patch 2010-03-10 - print current record field names to help detecting errors
			StringBuilder colNames = new StringBuilder();
			Set<String> s = _record.keySet();
			for (Iterator<String> iterator = s.iterator(); iterator.hasNext();) {
				colNames.append(iterator.next() + ";") ;
			}
			
			String args[] = {fieldName, colNames.toString()};
			String msg = Errors.FIELD_NOT_FOUND;
			msg = MessageFormat.format(msg, (Object[])args);
			throw new RecordsetException(msg);
		}
		
		_record.put(fieldName, value );
		
	}
	
	/**
	 * Set the children recordset of this record
	 * @param rs Children recordset
	 */
	public void setChildren(Recordset rs)
	{
		_children = rs;
	}
	
	/**
	 * Retrieve this record's children recordset
	 * @return A reference to the recordset or null if no children recordset exists
	 */
	public Recordset getChildren()
	{
		return _children;
	}


}
