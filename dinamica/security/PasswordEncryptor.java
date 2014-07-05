package dinamica.security;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * This is not a validator. it is used more like
 * a value transformer. It will transform the parameter
 * with name "passwd" into a MD5 hash using the following
 * combination: userlogin:passwd<br>
 * This validator must be used after all the other validators,
 * because it does assume that the password and userogin parameters
 * have been already validated.
 * <br><br>
 * Creation date: 10/03/2004<br>
 * Last Update: 10/03/2004<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class PasswordEncryptor extends AbstractValidator
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.HashMap)
	 */
	@SuppressWarnings("unchecked")
	public boolean isValid(
		HttpServletRequest req,
		Recordset inputParams,
		HashMap attribs)
		throws Throwable
	{

		if (inputParams.isNull("userlogin"))
			inputParams.setValue("userlogin", getSession().getAttribute("dinamica.userlogin"));

		if (inputParams.isNull("userlogin"))
			inputParams.setValue("userlogin", getUserName());
		
		//retrieve values
		String userid = inputParams.getString("userlogin");
		String password = inputParams.getString("passwd");

		//create MD5 hash using the string: userlogin:passwd
		java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
		byte[] b = (userid + ":" + password).getBytes();
		byte[] hash = md.digest(b);
		String pwd = Base64.encodeToString( hash, true );

		//set the "passwd" parameter value to the MD5 hash 
		inputParams.setValue("passwd", pwd);

		//always return true
		return true;
		
	}

}
