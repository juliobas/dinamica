package dinamica.audit;

import dinamica.IServiceWrapper;
import dinamica.Recordset;
import dinamica.xml.*;

/**
 * Version de GenericTableManager con soporte para auditoria
 * implementando la interfaz IServiceWrapper. Utiliza elementos
 * de configuracion de config.xml para parametrizar la grabacion
 * de la traza. Ver documento sobre Trazas de Auditoria en el website
 * de Dinamica para mayor informacion sobre como parametrizar esta clase<br>
 * Version: 2007-03-15<br>
 * (c) 2006 Martin Cordova<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova & Asociados (martin.cordova@gmail.com)
 *
 */
public class AuditableTransaction extends dinamica.GenericTableManager implements IServiceWrapper
{
	
	public int service(Recordset inputParams) throws Throwable
	{
		return super.service(inputParams);
	}

	/**
	 * Dejar traza de auditoria
	 */
	public void afterService(Recordset inputParams) throws Throwable
	{
		
		Recordset rsInputs = inputParams;

		Document doc = getConfig().getDocument();
		
		//define source recordset
		String rsName = getConfig().getConfigValue("//audit/recordset");
		if (rsName!=null && !rsName.equals(""))
			rsInputs = this.getRecordset(rsName);
		    rsInputs.next();
		
		//define audit event description
		String info = "";
		Element x[] = doc.getElements("//audit/cols/colname");
		
		for (int i = 0; i < x.length; i++) {
			String colName = x[i].getString();
			info  = info + rsInputs.getString(colName) + "; ";
		}
		
		//remover el ultimo ";"
		if (!info.equals(""))
			info = info.substring(0, info.length()-2);
	
		//create recordset to hold values
		Recordset rs = new Recordset();
		rs.append("area", java.sql.Types.VARCHAR);
		rs.append("operation", java.sql.Types.VARCHAR);
		rs.append("target_table", java.sql.Types.VARCHAR);
		rs.append("extra_info", java.sql.Types.VARCHAR);
		rs.append("pkey", java.sql.Types.INTEGER);

		//set values
		rs.addNew();
		rs.setValue("area", getConfig().getConfigValue("//audit/area"));
		rs.setValue("operation", getConfig().getConfigValue("//audit/oper"));
		rs.setValue("target_table", getConfig().getConfigValue("//audit/table"));
		rs.setValue("extra_info", info);
		
		//obtener el valor del PK del registro en cuestion
		int generatedID = 0;
		String pkey = getConfig().getConfigValue("//audit/pkey").toLowerCase();
		if (pkey.startsWith("select "))
		{
			generatedID = getDb().getIntColValue(getSQL(pkey, rsInputs), "id");
		}
		else
		{
			generatedID = rsInputs.getInt(pkey);
		}
		rs.setValue("pkey", new Integer(generatedID));
				
		//format sql
		String sql = getSQL(getLocalResource("/dinamica/audit/audit_insert.sql"), rs);
		
		//usa formato para SQLServer?
		String sqlServer = getContext().getInitParameter("audit-sqlserver");
		if (sqlServer!=null && sqlServer.equalsIgnoreCase("true"))
			sql = getSQL(getLocalResource("/dinamica/audit/audit_insert_mssql.sql"), rs);
		
		//save audit log into security db
		String dsName = (String)getContext().getAttribute("dinamica.security.datasource");
		dbExec(dsName, sql);
		
	}

	public void beforeService(Recordset inputParams) throws Throwable
	{
	}

}
