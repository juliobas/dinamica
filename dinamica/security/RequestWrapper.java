package dinamica.security;

import javax.servlet.http.*;
import java.security.*;

/**
 * Wraps the original http request to provide
 * the same J2EE security APIs under Dinamica
 * security model.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 *
 */

public class RequestWrapper extends HttpServletRequestWrapper
{

	private Principal user = null;

	public RequestWrapper(HttpServletRequest request)
	{
		super(request);
	}

	public Principal getUserPrincipal()
	{
		return user;
	}

	public void setUserPrincipal(Principal user)
	{
		this.user = user;
	}

	public boolean isUserInRole(String roleName)
	{
		
		boolean flag = false;

		DinamicaUser u = (DinamicaUser) user;
		String roles[] = u.getRoles();

		if (roles!=null)
		{
			for (int i = 0; i < roles.length; i++)
			{
				if (roleName.equals(roles[i]))
				{
					flag = true;
					break;
				}
			}
		}

		return flag;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	public String getRemoteUser()
	{
		if (user!=null)
			return user.getName();
		else
			return super.getRemoteUser();
	}

}
