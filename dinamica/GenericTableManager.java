package dinamica;

import dinamica.xml.*;

/**
 * Generic Transaction to execute one or more simple
 * action queries like INSERT, UPDATE or DELETE.<br>
 * This module requires one or more extra elements in config.xml
 * called "query", containing the filename of the SQL file
 * to load and execute. Input parameters will be automatically
 * replaced if config.xml contains validator=true.<br>
 * Please remember that if you are going to execute more than one
 * statement you have to activate JDBC transactions in your config.xml
 * file. Also note that this class extends dinamica.GenericTransaction
 * and it will invoke it parent's service() method before proceeding to
 * execute the queries defined via "query" elements in config.xml, it means
 * that if you defined one or more recordsets in config.xml, these will be created
 * BEFORE the DML queries are executed.<br><br>
 * Last update: 2009-06-22
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class GenericTableManager extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{

		//reuse superclass code
		super.service(inputParams);

		//get database channel
		Db db = getDb();
		
		//read xml - search for all <query> elements in config.xml
		Element q[] = getConfig().getDocument().getElements("query");
		
		if (q!=null)
		{
			//execute every query
			for (int i = 0; i < q.length; i++) 
			{
				
				//read template filename
				String queryName = q[i].getString();

				//load sql template
				String sql = getResource(queryName);
				
				//check if the "params" attribute is present
				String rsName = q[i].getAttribute("params");
				
				//patch 2010-03-08
				String dsName = q[i].getAttribute("datasource");
				
				//replace markers using a specific recordset
				if (rsName!=null)
				{
					Recordset rs = getRecordset(rsName);
					if( rs.getRecordCount()>1 ) {
						rs.top();
						while (rs.next()) {
							String t = this.getSQL(sql, rs);
							t = this.getSQL(t, inputParams);
							if (dsName==null)
								db.exec(t);
							else
								dbExec(dsName, t);
							
						}
					} else if( rs.getRecordCount()==1 ) {
						rs.first();
						sql = this.getSQL(sql, rs);
						sql = this.getSQL(sql, inputParams);
						if (dsName==null)
							db.exec(sql);
						else
							dbExec(dsName, sql);
					}
					
				} else {
					sql = this.getSQL(sql, inputParams);
					if (dsName==null)
						db.exec(sql);
					else
						dbExec(dsName, sql);
				}
				
			}
			
		}
		
		return 0; //OK
		
	}

}
