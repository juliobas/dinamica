package dinamica.security;

import dinamica.*;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Change current user password using Dinamica 
 * security model database structure (table s_user).
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class ChangePassword extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		
		int rc = 0;
		
		//reuse superclass code
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
			
			conn.setAutoCommit(false);
			
			//get db channel
			Db db = getDb();
			
			//get user primary key - required for password history
			Recordset user = db.get(getSQL(getResource("getuserkey.sql"), inputParams));
			user.next();
			Integer userID = user.getInteger("user_id");
			inputParams.setValue("userid", userID);
			
			//execute update query
			db.exec(getSQL(getResource("update.sql"), inputParams));

			//execute update query
			db.exec(getSQL(getResource("insert-passlog.sql"), inputParams));
			
			conn.commit();
			
		}
		catch (Throwable e)
		{
			if (conn!=null)
				conn.rollback();
			throw e;
		}
		finally
		{
			if (conn!=null)
				conn.close();
		}
		
		return rc;
		
	}

}
