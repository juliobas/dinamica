package dinamica;

/**
 * Generic transaction to provide a recordset
 * containing the fields required to "paint"
 * navigation controls for paged views.
 * 
 * <br>
 * Creation date: 29/10/2003<br>
 * Last Update: 29/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class PagingControls extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		
		int rc = super.service(inputParams);
		
		//get recordset ID
		String recordsetID = (String)getRequest().getAttribute("paging.recordset");
		String pageSize = (String)getRequest().getAttribute("paging.pagesize");
		
		//retrieve from session using this ID
		Recordset rs = (Recordset)getSession().getAttribute(recordsetID);

		//patch 2007-06-02 - paging view facilities for Ajax - requires default pagesize
		if (pageSize!=null && !pageSize.equals(""))
			rs.setPageSize(Integer.parseInt(pageSize));

		//patch 2007-07-10 - paging view facilities for Ajax
		//may want to navigate specific page after search
		String currentPage = (String)getRequest().getParameter("currentpage");
		if (currentPage!=null && !currentPage.equals("")) {
			int page = Integer.parseInt(currentPage);
			//page must be in valid range
			if (page > rs.getPageCount())
				page = rs.getPageCount();
			rs.getPage(page);
		}
		
		
		//publish recordset
		publish("paging.controls", rs.getRecordsetInfo());
		
		return rc;
		
	}

}
