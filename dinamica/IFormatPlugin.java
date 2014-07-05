package dinamica;

import java.util.Locale;

/**
 * Interface that MUST be implemented
 * by custom formatters. These plugins
 * provide extended functionality for format masks
 * that start with "class:". Example:
 * <br><br>
 * ${fld:MyColumn@class:com.formatters.MyFormatter}
 * <br><br>
 * The framework will load this class and then
 * invoke the only method defined by this Interface.
 * <br>
 * The marker can also include "parameters" passed to the
 * formatter class:<br>
 * ${fld:myColName@class:com.Myformatter(AnyParameter1,AnyParameter2)}<br>
 * <br><br>
 * Creation date: 23/05/2005
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public interface IFormatPlugin
{

    /**
     * Format value according to custom-made rules
     * @param colName Column name of the recordset that contains the value to be formatted
     * @param rs Recordset that contains the record with the value
     * to be formatted; current position is the record in question
     * @param locale Current locale if defined in the user session - may be null
     * @param args String that contains the list of "parameters", meaning
     * whatever text that was included after the classname surrounded by (...).
     * Example: ${fld:myColName@class:com.Myformatter(AnyParameter1,AnyParameter2)}<br>
     * It's the responsibility of the programmer to interpret this String and
     * convert it to an array of Strings if necessary.
     * @return String representation of the formatted value
     * @throws Throwable
     */
    public String format(String colName, Recordset rs, Locale locale, String args) throws Throwable;
    
}
