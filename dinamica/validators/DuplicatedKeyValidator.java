package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * Generic Validator to test if a record key is duplicated,
 * requires the following custom attributes:<br>
 * <ul>
 * <li> sql: query to find any related record. You may use field makers
 * that will be replaced by the corresponding request parameters passed
 * as a recordset to the isValid method.
 * </ul>
 * <br>If you don't use the SQL query you may use the
 * attributes shown below (not recommended).
 * <ul>
 * <li> table: name of the search table.
 * <li> column: name of the search column, also the
 * name of the parameter whose value will be used.
 * <li> varchar: true|false indicates if the column is of type varchar or alike,
 * this is used to demarcate the column search value with the proper delimiter, if any.
 * </ul>
 * <br>
 * Works only for text or numeric values, dates not allowed.
 * <br><br>
 * Creation date: 10/feb/2004<br>
 * Last Update: 10/feb/2004<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * */

public class DuplicatedKeyValidator extends AbstractValidator
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractValidator#isValid(javax.servlet.http.HttpServletRequest, dinamica.Recordset, java.util.HashMap)
	 */
	public boolean isValid(
		HttpServletRequest req,
		Recordset inputParams,
		HashMap<String, String> attribs)
		throws Throwable
	{
		
		boolean flag = true;
		
		//get db channel
		Db db = getDb();

		//detect if sql parameter was passed to the validator
		boolean bSql = attribs.containsKey("sql");

		String sql = "";

		if (!bSql)
		{
			//validate plugin configuration
			boolean b1 = attribs.containsKey("table");
			if (!b1)
				throw new Throwable("Bad configuration - 'table' attribute not found.");
			boolean b2 = attribs.containsKey("column");
			if (!b2)
				throw new Throwable("Bad configuration - 'column' attribute not found.");
			boolean b3 = attribs.containsKey("varchar");
			if (!b3)
				throw new Throwable("Bad configuration - 'varchar' attribute not found.");
	
			//define value enclosing character (quote or nothing)
			String delim = "";
			String isVarchar = (String)attribs.get("varchar");
			if (isVarchar.equals("true"))
				delim = "'";
	
			//get db parameters
			String table = (String)attribs.get("table");
			String col = (String)attribs.get("column");
	
			//get input value
			String value = String.valueOf(inputParams.getValue(col));
	
			//build sql
			sql = "select " + col + " from " + table + " where " + col + " = " + delim + value + delim; 
		}
		else
		{
			String query = (String)attribs.get("sql");
			sql = getResource(query);
			sql = getSQL(sql, inputParams);
		}
		
		//execute sql
		Recordset rs = db.get(sql);
		if (rs.getRecordCount()>0)
			flag = false;

		return flag;
		
	}

}
