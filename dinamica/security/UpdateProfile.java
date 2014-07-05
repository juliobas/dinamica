package dinamica.security;

import dinamica.*;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Update user profile.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class UpdateProfile extends GenericTransaction
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
			
			//update prefs in DB
			String sql = getSQL(getResource("update.sql"), inputParams);
			Db db = getDb();
			db.exec(sql);
		
			//reload new stylesheet
			//reuse Login code
			Login obj = (Login)getObject("dinamica.security.Login");
			obj.getUserPrefs(db, inputParams);
			
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
