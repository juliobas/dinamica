package dinamica.validators;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * Generic validator for date ranges (date from - date to).<br>
 * Will return FALSE if date2 &lt; date1, requires two custom attributes named "date1" and "date2".<br>
 * A optional attribute is "datasource" for specific datasource in sql file.<br>
 * The possible values for "date1" and "date2" are:<br>
 * <ul>
 * <li> field name of recordset
 * <li> the word 'today' that represents the current date
 * <li> sql file '*.sql' with a SELECT statement that returns a date type field which has the alias 'validator'
 * </ul>
 * Returns TRUE if any of the parameters is null.<br>
 * <br>
 * Creation date: 2003-10-29<br>
 * Last Update: 2009-04-13<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (martin.cordova@gmail.com)
 * */
public class DateRangeValidator extends AbstractValidator
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.ArrayList)
	 */
	public boolean isValid(
		HttpServletRequest req,
		Recordset inputParams,
		HashMap<String, String> attribs)
		throws Throwable
	{

		String datasource = ( attribs.containsKey("datasource") )? attribs.get("datasource") : null;
	    String date1 = (String)attribs.get("date1");
		String date2 = (String)attribs.get("date2");

		if (date1==null || date2==null)
			throw new Throwable("Invalid attributes 'date1' or 'date2' - cannot be null.");

		//patch 2009-04-13 gdottori
		Date d1 = this.getDateValue( inputParams, date1, datasource );
        Date d2 = this.getDateValue( inputParams, date2, datasource );
        //end patch

		if (d1!=null && d2!=null && d2.compareTo(d1) < 0 )
			return false;
		else
			return true;

	}

    //patch 2009-04-13 gdottori
	private Date getDateValue( Recordset rs, String attrib, String datasource ) throws Throwable {

        Date d = null;

        if( attrib.equalsIgnoreCase( "today" ) ) {
            Calendar c = Calendar.getInstance();
            c.setLenient( true );
            c.setTime( new Date() );
            c.set( Calendar.HOUR, 0 );
            c.set( Calendar.HOUR_OF_DAY, 0 );
            c.set( Calendar.MINUTE, 0 );
            c.set( Calendar.SECOND, 0 );
            c.set( Calendar.MILLISECOND, 0 );
            d = c.getTime();
        } else if( attrib.endsWith( ".sql" ) ) {
            Recordset rs1 = null;
            String sql = getResource( attrib );
            sql = getSQL( sql, rs );
            //execute sql
            if( datasource == null ) {
                rs1 = getDb().get(sql, 1);
            } else {
                rs1 = dbGet( datasource, sql, 1 );
            }
            if( rs1.getRecordCount() == 1 ) {
                rs1.first();
                d = rs1.getDate( "validator" );
            }
        } else {
            d = rs.getDate( attrib );
        }

	    return d;
	}
	//end patch

}
