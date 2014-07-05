package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * Generic validator to test if an array based parameter
 * is empty. Returns false if empty. Requires a custom attribute
 * called "parameter-name".
 * <br><br>
 * Creation date: feb/10/2004<br>
 * Last Update: feb/10/2004<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class ArrayNotEmptyValidator extends AbstractValidator
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
		boolean b = attribs.containsKey("parameter-name");
		if (!b)
			throw new Throwable("Bad configuration - 'parameter-name' attribute not found.");

		//get name of the parameter representing the array of values
		String paramName = (String)attribs.get("parameter-name");
		
		//read values
		String value[] = req.getParameterValues(paramName);
		
		//test
		if (value==null)
			flag = false;
		
		return flag;
		
	}

}
