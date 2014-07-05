package dinamica;

/**
 * Sort recordset in memory, request should include two mandatory
 * parameters: rs and colname. rs is the ID of the session attribute
 * that contains the recordset to be sorted, and colname is the column
 * to use for the sorting.<br>
 * 2007-06-19 <br>
 * @author martin.cordova@gmail.com
 */
public class SortRS extends GenericTransaction
{

	@Override
	public int service(Recordset inputParams) throws Throwable
	{
		super.service(inputParams);
		
		String rsName = getRequest().getParameter("rs");
		
		Recordset rs = (Recordset)getSession().getAttribute(rsName);
		if (rs==null)
			throw new Throwable("Recordset not found in session attribute: " + rsName);
		
		rs.sort(getRequest().getParameter("colname"));
		
		return 0;
	}

}
