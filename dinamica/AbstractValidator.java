package dinamica;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;

/**
 * Base class to create reusable Validator services.
 * All Validators must subclass this class.
 * <br>
 * Creation date: 29/10/2003<br>
 * Last Update: 29/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public abstract class AbstractValidator extends GenericTransaction 
{

	/**
	 * Executes business specific validation rule for a request
	 * @param req Servlet Request
	 * @param inputParams Recordset with pre-validated request parameters in its native data types
	 * @param attribs Attributes defined in validator.xml for the custom-validator element - these are validator-specific, so the same
	 * valitador can receive a different set of attributes in different Actions, making it more reusable.
	 * @return TRUE if the validation passed OK.
	 * @throws Throwable
	 */
	public abstract boolean isValid(HttpServletRequest req, Recordset inputParams, HashMap<String, String> attribs) throws Throwable;
	
	/**
	 * 
	 * @return Error Message to be displayed by the
	 * "invalid form" action. If this method returns null then
	 * the Controller will use the on-error-label attribute defined
	 * for the validator in the validator.xml file. 
	 */
	public String getErrorMessage()
	{
		return null;
	}

}
