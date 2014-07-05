package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * Generic validator to test if a recordset stored in session
 * is empty. Returns false if empty or if the session attribute is null. 
 * Requires a custom attribute called "recordsetId". Throws an exception if no recordset is
 * found for the given session attribute ID (recordsetId).
 * <br><br>
 * Creation date: 2008-04-16<br>
 * Last Update: 2008-04-16<br>
 * (c) 2009 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class RecordsetNotEmpty extends AbstractValidator
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

		boolean flag = true;

		//validate plugin configuration
		boolean b = attribs.containsKey("recordsetId");
		if (!b)
			throw new Throwable("Bad configuration - 'recordsetId' attribute not found.");

		//get name of the parameter representing the array of values
		String paramName = (String)attribs.get("recordsetId");
		
		//read values
		Recordset rs = (Recordset)getSession().getAttribute(paramName);
		
		if (rs==null || rs.getRecordCount()==0)
			flag = false;
		
		return flag;
		
	}

}
