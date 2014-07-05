package dinamica.security;

import dinamica.*;

/**
 * Send email about failed login attempt
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class LoginFailedAlert extends GenericTableManager
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{

		super.service(inputParams);

		//log alert
		String res = getResource("log-template.txt");
		TemplateEngine t = new TemplateEngine(getContext(),getRequest(), res);
		t.replaceDefaultValues();
		t.replaceLabels();
		t.replaceRequestAttributes();
		getContext().log(t.toString());

		return 0;		

	}
	
}
