package dinamica;

/**
 * Basic Exception class for all exceptions triggered
 * by the Recordset related classes (Recordset, RecordsetField, Record)
 * <br>
 * Creation date: 11/09/2003<br>
 * Last Update: 11/09/2003<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class RecordsetException extends Throwable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	public RecordsetException(String msg)
	{
		super(msg);
	}


    public RecordsetException()
    {
    }

}
