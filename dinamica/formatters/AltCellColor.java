package dinamica.formatters;

import java.util.Locale;
import dinamica.*;

/**
 * Generic formatter that returns a color name
 * depending on the row position (even or odd).
 * The column name should represent column that
 * would contain the color value, most of the time it
 * may contain a null value. If it does contain a non-null value, then
 * this value will be returned and the even/odd rule will be ignored.<br>
 * The [args] argument should contain the name of two colors, separated by comma. Example:<br>
 * <xmp>
 * {fld:MyColName@class:dinamica.formatters.AltCellColor(cyan, white)}
 * </xmp>
 * This field marker could be placed into a style attribute, in order
 * to change the background color of a cell. It was designed to be used
 * with HGrids (Horizontal grid outputs) to create the alternate cell color effect,
 * much like the alternate row effect already present in regular grid/table presentations.
 * 
 * <br><br>
 * Creation date: 09/07/2005
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class AltCellColor implements IFormatPlugin
{

    /* (non-Javadoc)
     * @see dinamica.IFormatPlugin#format(java.lang.String, dinamica.Recordset, java.util.Locale, java.lang.String)
     */
    public String format(String colName, Recordset rs, Locale locale,
            String args) throws Throwable
    {
    	
    	String value = rs.getString(colName);
    	if (value!=null && !value.equals(""))
    		return value;
    	
        String colors[] = StringUtil.split(args,",");
        String color = colors[1];
        int v = rs.getRecordNumber() % 2;
        if (v==0)
            color = colors[0];
        
        return color;
    }

}
