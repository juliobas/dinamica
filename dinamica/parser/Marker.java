package dinamica.parser;

/**
 * Marker
 * Creation date: 23/06/2004<br>
 * @author Martin Cordova dinamica@martincordova.com
 * (c) 2004 Martin Cordova y Asociados
 * http://www.martincordova.com
 */
public class Marker
{

	private String prefix = null;
	private String key = null;
	private String columnName = null;
	private String format = null;
	private String value = null;
	
	public Marker()
	{
	}
	
	public Marker(String key, String prefix, String columnName, String format)
	{
		this.key = key;
		this.prefix = prefix;
		this.columnName = columnName;
		this.format = format;
	}
	
	/**
	 * @return
	 */
	public String getColumnName()
	{
		return columnName;
	}

	/**
	 * @return
	 */
	public String getFormat()
	{
		return format;
	}

	/**
	 * @return
	 */
	public String getKey()
	{
		return key;
	}

	/**
	 * @return
	 */
	public String getPrefix()
	{
		return prefix;
	}

	/**
	 * @param string
	 */
	public void setColumnName(String string)
	{
		columnName = string;
	}

	/**
	 * @param string
	 */
	public void setFormat(String string)
	{
		format = string;
	}

	/**
	 * @param string
	 */
	public void setKey(String string)
	{
		key = string;
	}

	/**
	 * @param string
	 */
	public void setPrefix(String string)
	{
		prefix = string;
	}

	/**
	 * @return
	 */
	public String getValue()
	{
		if (value==null)
			value = key;
		return value;
	}

	/**
	 * @param string
	 */
	public void setValue(String string)
	{
		value = string;
	}

}
