package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * Returns TRUE if value1.equals(value2).<br>
 * value1 and value2 must be of type String. Requires two custom
 * attributes named "value1" and "value2" representing the names
 * of the fields to compare.
 * <br><br>
 * Creation date: 5/03/2004<br>
 * Last Update: 5/03/2004<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class MatchEqualValidator extends AbstractValidator
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.HashMap)
	 */
	public boolean isValid(
		HttpServletRequest req,
		Recordset inputParams,
		HashMap<String, String> attribs)
		throws Throwable
	{
		
		String v1 = (String)attribs.get("value1");
		String v2 = (String)attribs.get("value2");
		
		if (v1==null || v2==null)
			throw new Throwable("Invalid attributes 'value1' or 'value2' - cannot be null.");
		
		if (inputParams.isNull(v1) || inputParams.isNull(v2))
			return true;
			
		String d1 = inputParams.getString(v1);
		String d2 = inputParams.getString(v2);
		
		if ( !d1.equals(d2) )
			return false;
		else
			return true;
			
	}

}
