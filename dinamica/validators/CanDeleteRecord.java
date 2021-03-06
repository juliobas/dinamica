package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.*;

/**
 * This validator can be used to detect if there are
 * any related records in another table before trying to
 * delete a certain record. Returns TRUE if there are no
 * related records meaning that the record can be deleted.
 * You will need to call this validator for every referential
 * integrity check before deleting a record.
 * <br><br>
 * Rrequires the following custom attributes:<br>
 * <ul>
 * <li> sql: query to find any related record. You may use field makers
 * that will be replaced by the corresponding request parameters passed
 * as a recordset to the isValid method.
 * </ul>
 *  
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class CanDeleteRecord extends AbstractValidator
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

		if (!bSql)
		{

			throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [sql] in validator.xml");

		}
		else
		{

			//read config
			String query = (String)attribs.get("sql");
			
			//load template and replace parameter values
			String sql = getResource(query);
			sql = getSQL(sql, inputParams);

			//execute sql
			Recordset rs = db.get(sql);
			if (rs.getRecordCount()>0)
				flag = false;

		}
		
		return flag;

	}

}
