package dinamica;

/**
 * Deletes record from recordset stored in session
 * and then executes whatever queries as defined in
 * config.xml. This class may be used to create the visual effect
 * of record deletion when deleting records from paged views. It requires two
 * special parameters in config.xml: recordset-id (attribute ID used to store recordset
 * in session) and pkey (name of the field that is the primary key of the table, also contained
 * in the recordset). It is also assumed that the request contains a parameter "id", which contains
 * the value of the PKey used to delete a specific record. It will trigger an exception if can't find
 * a record with pkey = id, if config parameters are not present, or if it cannot find the recordset in session.
 * @author Martín Cordova
 */
public class DeleteRecordPaged extends GenericTableManager 
{

	public int service(Recordset inputParams) throws Throwable 
	{
		String rsID  = getConfig().getConfigValue("recordset-id");
		String pkey  = getConfig().getConfigValue("pkey");
		Recordset rs = (Recordset)getSession().getAttribute(rsID);
		
		if (rs==null)
			throw new Throwable("Can't find recordset in session with attribute ID = " + rsID);
		
		int id = inputParams.getInt("id");
		int pos = rs.findRecord(pkey, id);
		if (pos < 0)
			throw new Throwable("Can't find record with column " + pkey + "=" + id);
		else
			rs.delete(pos);

		super.service(inputParams);
		
		return 0;
	}
	
}
