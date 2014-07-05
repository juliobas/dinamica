package dinamica;

import java.io.Serializable;
import java.sql.Types;

/**
 * Utility class for Recordset: represents a Recordset field metadata
 * <br>
 * Creation date: 10/09/2003<br>
 * Last Update: 10/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class RecordsetField  implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** field name */
	private String _name = null;
	
	/** sql native type name */
	private String _sqlTypeName = null;
	
	/** jdbc data type (java.sql.Types) */
	private int _type = 0;
	
	/**
	 * Quick way to build an object of this class
	 * @param name Field Name
	 * @param typeName Native Type name
	 * @param type JDBC Data Type
	 */
	public RecordsetField(String name, String typeName, int type)
	{
		switch (type) {
			case Types.DECIMAL:
			case Types.FLOAT:
			case Types.NUMERIC:
				type = Types.DOUBLE;
				break;
	
			case Types.BIGINT:
			case Types.SMALLINT:
			case Types.TINYINT:
				type = Types.INTEGER;
				break;
		} 
		
		_name = name;
		_sqlTypeName = typeName;
		_type = type;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return _name;
	}

	/**
	 * @return
	 */
	public String getSqlTypeName()
	{
		return _sqlTypeName;
	}

	/**
	 * @return
	 */
	public int getType()
	{
		return _type;
	}

	public RecordsetField() {}
	
}
