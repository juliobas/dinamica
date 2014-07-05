package dinamica;

/**
 * Represents a field marker in a text template. This
 * is a utility class to be used by TemplateEngine. The attribute
 * "extraInfo" may represent the output format or the sequence name, depending on
 * the marker type (field or sequence)
 * <br>
 * Creation date: 23/09/2003<br>
 * Last Update: 23/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class Marker
{

	private String name = null;
	private String extraInfo = null;
	private int pos1 = 0;
	private int pos2 = 0;

	public Marker(String name, String extraInfo, int pos1, int pos2)
	{
		this.name = name;
		this.extraInfo = extraInfo;
		this.pos1 = pos1;
		this.pos2 = pos2;
	}

	/**
	 * @return
	 */
	public String getExtraInfo()
	{
		return extraInfo;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @param string
	 */
	public void setExtraInfo(String string)
	{
		extraInfo = string;
	}

	/**
	 * @param string
	 */
	public void setName(String string)
	{
		name = string;
	}

	/**
	 * @return Start position of marker
	 */
	public int getPos1()
	{
		return pos1;
	}

	/**
	 * @return End position of marker
	 */
	public int getPos2()
	{
		return pos2;
	}

}
