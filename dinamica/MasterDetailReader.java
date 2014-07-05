package dinamica;

import javax.sql.*;
import java.sql.*;

/**
 * DESCONTINUADO - VER LA NUEVA PLANTILLA MasterDetail Y SU DOCUMENTO RESPECTIVO.<BR>
 * Base class to create your own Transaction classes
 * for master/detail reports. You can use an instance
 * of this class if your report requirements are simple
 * to configure, otherwise you should subclass and redefine
 * service() to publish a  recordset called "master" and
 * redefine the getDetail() method too. 
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class MasterDetailReader extends GenericTransaction
{

	/** 
	 * Publish recordset used for the master and subtotal
	 */
	public int service(Recordset inputParams) throws Throwable
	{

		int rc = 0;
		
		//reuse superclass code
		super.service(inputParams);
		
		//create master recordset and publish it
		//with the ID "master"
		Db db = getDb();
		String sql = getResource(getConfig().getConfigValue("query-master"));
		sql = getSQL(sql, inputParams);
		Recordset rs = db.get(sql);
		publish("master", rs);
		
		return rc;
		
	}

	/**
	 * Return recordset for detail section
	 * @param master Master recordset positioned on the current record
	 * @return
	 * @throws Throwable
	 */
	public Recordset getDetail(Recordset master) throws Throwable
	{

		//get datasource and DB connection
		DataSource ds = getDataSource(); 
		Connection conn = ds.getConnection();
		this.setConnection(conn);

		try 
		{
			
			//get db channel
			Db db = getDb();
			
			//build sql
			String sql = getResource(getConfig().getConfigValue("query-detail"));
			sql = getSQL(sql, master);
			
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
