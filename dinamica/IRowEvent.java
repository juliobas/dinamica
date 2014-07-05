package dinamica;

/**
 * This interface may be implemented by classes
 * that need to make very custom modifications the the
 * generated rows when producing a table with the TemplateEngine
 * class. This way can be produced special effects like
 * alternate row colors. 
 * 
 * <br>
 * Creation date: 29/10/2003<br>
 * Last Update: 29/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public interface IRowEvent
{

	/**
	 * This method is called before appending the
	 * row template to the table template
	 * @param rs Recordset used to fill the table, the record
	 * position will the the current record
	 * @param rowTemplate Row buffer
	 * @return Modified row template
	 * @throws Throwable
	 */
	public String onNewRow(Recordset rs, String rowTemplate) 
		throws Throwable;

}
