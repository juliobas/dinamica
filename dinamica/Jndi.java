package dinamica;

import java.text.MessageFormat;
import javax.sql.DataSource;
import javax.naming.*;

/**
 * Provides easy API to obtain a datasource via JNDI
 * <br>
 * Creation date: 10/09/2003<br>
 * Last Update: 10/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br> 
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class Jndi
{

	/**
	 * Obtain a DataSource object using its JNDI name
	 * @param name Name of the DataSource like "jdbc/demo" or "java:comp/env/jdbc/demo"
	 * depending on your Server/JNDI provider<br>
	 * <br>
	 * <b>Sample code:</b><br>
	 * <pre>
	 * javax.sql.DataSource ds = Jndi.getDataSource("java:comp/env/jdbc/demo");
	 * java.sql.Connection conn = ds.getConnection();
	 * </pre>
	 * <br>
	 * Please keep in mind that some servers only accept "jdbc/demo" excluding
	 * the prefix "java:comp/env/", and may throw errors if you try to use the prefix,
	 * so it is a good idea to keep this String in a configuration file, like a context
	 * parameter in WEB.XML and load it at runtime, to make your WebApp more flexible and portable.
	 * @return DataSource to provide access to a connection pool
	 * @throws Throwable
	 */
	public static DataSource getDataSource(String name) throws Throwable
	{

		DataSource source = null;

		try
		{
			Context ctx = new InitialContext();
			source = (DataSource) ctx.lookup( name );
			if (source==null)
			{
				String args[] = { name };
				String msg = Errors.DATASOURCE_NOT_FOUND;
				msg = MessageFormat.format(msg, (Object[])args);
				throw new Throwable(msg);			
			}
			else
			{
				return source;
			}
		}
		catch ( Throwable e )
		{
		   throw e;
		}
				
	}

}
