package dinamica.security;

import dinamica.*;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Retrieve user profile record.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class GetProfile extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{

		super.service(inputParams);

		//get security datasource
		String jndiName = (String)getContext().getAttribute("dinamica.security.datasource");
		if (jndiName==null)
			throw new Throwable("Context attribute [dinamica.security.datasource] is null, check your security filter configuration.");
		
		//get datasource and DB connection
		DataSource ds = Jndi.getDataSource(jndiName); 
		Connection conn = ds.getConnection();
		this.setConnection(conn);

		try 
		{
			
			//get db channel
			Db db = getDb();
			
			//get user profile
			Recordset user = db.get(getSQL(getResource("getrecord.sql"), inputParams));
			user.next();

			publish("getrecord.sql", user);
			
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (conn!=null)
				conn.close();
		}
		
		return 0;

	}

}
