package dinamica.security;

import java.security.*;

/**
* Authenticated web user. This class is used by the RequestWrapper
* to provide the security principal. This way the SecurityFilter will
* provide support for the same J2EE security APIs.
*/

public class DinamicaUser implements Principal, java.io.Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String name = null;
	String roles[] = null;

	public DinamicaUser(String name, String roles[])
	{
		this.name = name;
		this.roles = roles;
	}

	public String getName()
	{
		return name;
	}

	public String[] getRoles()
	{
		return this.roles;
	}

	public boolean equals(Object b)
	{
		if (!(b instanceof DinamicaUser))
			return false;

		return name.equals(((DinamicaUser) b).getName());
	}

	public int hashCode()
	{
		return name.hashCode();
	}

	public String toString()
	{
		return name;
	}

}
