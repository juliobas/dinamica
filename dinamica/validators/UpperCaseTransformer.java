package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * This validator does not validate but transforms the value of the target parameter
 * by transforming its contents to upper case characters. It is
 * used to create SQL search search expressions against data that is stored in upper case,
 * without programming effort and without SQL expression tricks like value = UPPER('MyLastName...').  
 * It is configured as a validator but works as a parameter value transformer.
 * <br><br>
 * <br><br>
 * Requires the following custom attributes:<br>
 * <ul>
 * <li> parameter: Name of the request parameter to transform. This parameter
 * MUST be defined in validator.xml and must be of type VARCHAR.
 * </ul>
 * Creation date: 22/04/2005
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class UpperCaseTransformer extends AbstractValidator
{

    /* (non-Javadoc)
     * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.HashMap)
     */
    public boolean isValid(HttpServletRequest req, Recordset inputParams,
            HashMap<String, String> attribs) throws Throwable
    {

		boolean bParam = attribs.containsKey("parameter");
		if (!bParam)
			throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [parameter] in validator.xml");
		
		String paramName = (String)attribs.get("parameter");
		if (!inputParams.isNull(paramName))
		{
			String value = inputParams.getString(paramName);
			value = value.toUpperCase();
			inputParams.setValue(paramName, value);
		}
		
		return true;
		
    }

}
