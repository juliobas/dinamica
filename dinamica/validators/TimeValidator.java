package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * This validator returns FALSE if the corresponding field
 * cannot be parsed as a valid time with format HH:mm<br>
 * If parameter's value is NULL then the validator returns TRUE
 * <br><br>
 * Requires the following custom attributes:<br>
 * <ul>
 * <li> parameter: Name of the request parameter to validate. This parameter
 * MUST be defined in validator.xml.
 * </ul>
 *  
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class TimeValidator extends AbstractValidator 
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.HashMap)
	 */
	public boolean isValid(HttpServletRequest req, Recordset inputParams, HashMap<String, String> attribs) throws Throwable 
	{

		boolean flag = true;
		
		//detect if sql parameter was passed to the validator
		boolean bParam = attribs.containsKey("parameter");

		if (!bParam)
		{

			throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [parameter] in validator.xml");

		}
		else
		{

			//read config
			String paramName = (String)attribs.get("parameter");
						
			//get parameter value if available
			if (!inputParams.isNull(paramName))
			{
				String time = inputParams.getString(paramName);
				
				//validate as TIME with 24hrs format
				try	{
					StringUtil.getDateObject(time,"HH:mm");
				} catch (Throwable e)	{
					flag  = false; 
				}
			}

		}
		
		return flag;

	}

}
