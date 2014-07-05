package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;


/**
 * This validator transforms the value (if not null) of the corresponding
 * parameter, by appending a '%' at the end of the text or by enclosing the
 * the value between '%' characters.<br>
 * It is used to create search patterns for SQL queries like 'value starts with..' that can
 * be used in sql LIKE expressions. Always returns TRUE.<br>
 * As a preventtive measure, all occurrences of the '%' character in the parameter's value 
 * will be erased before applying the transformation.
 * <br><br>
 * Requires the following custom attributes:<br>
 * <ul>
 * <li> parameter: Name of the request parameter to transform. This parameter
 * MUST be defined in validator.xml and must be of type VARCHAR.
 * <li> rule: a string that denotes the possible transformation, accepted
 * values are 'like' and 'contains'.
 * </ul>
 *  
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class SQLPatternTransformer extends AbstractValidator {

	/* (non-Javadoc)
	 * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.HashMap)
	 */
	public boolean isValid(HttpServletRequest req, Recordset inputParams,
			HashMap<String, String> attribs) throws Throwable {

		boolean bParam = attribs.containsKey("parameter");
		if (!bParam)
			throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [parameter] in validator.xml");
		
		boolean bRule = attribs.containsKey("rule");
		if (!bRule)
			throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [rule] in validator.xml");
		
		String rule = (String)attribs.get("rule");
		
		if (!rule.equalsIgnoreCase("like") && !rule.equalsIgnoreCase("contains"))
			throw new Throwable("[" + this.getClass().getName() + "] Invalid attribute value [rule] in validator.xml: " + rule + " - Accepted values are 'like' or 'contains'");
		
		String paramName = (String)attribs.get("parameter");
		if (!inputParams.isNull(paramName))
		{
			String value = inputParams.getString(paramName);
			value = StringUtil.replace(value, "%", "");
			if (rule.equalsIgnoreCase("like"))
				value = value + "%";
			if (rule.equalsIgnoreCase("contains"))
				value = "%" + value + "%";
			inputParams.setValue(paramName, value);
		} else {
			boolean x = attribs.containsKey("ifnull");
			if (x) {
				String value = (String)attribs.get("ifnull");
				inputParams.setValue(paramName, value);
			}
		}
		
		return true;
	}

}
