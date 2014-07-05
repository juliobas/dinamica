package dinamica.security;

import dinamica.*;
import javax.sql.DataSource;
import java.sql.*;

/**
 * Loads menu titles and menu items. This transaction will
 * be used by the output module dinamica.security.MenuOutput.
 * It does provide the menu recordset and a method to retrieve
 * a menu's items recordset.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class GetMenu extends GenericTransaction
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
			
			//get db channel
			Db db = getDb();
			
			//build sql
			String sql = getSQL(getResource("query-master.sql"), inputParams);
			
			//get menus
			Recordset menu = db.get(sql);
			
			publish("menu", menu);
			
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
		
		return rc;
		
	}

	public Recordset getMenuItems(Recordset menu) throws Throwable
	{
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
			
			//build sql
			String sql = getSQL(getResource("query-detail.sql"), menu);
			
			//get menu items
			Recordset items = db.get(sql);
			
			return items;
			
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
		
	}

}
