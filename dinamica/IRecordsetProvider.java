package dinamica;

/**
 * Defines the interface to return a Recordset
 * given a Recordset representing the input parameters
 * <br><br>
 * Creation date: 27/04/2005
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public interface IRecordsetProvider
{

    public Recordset getRecordset(Recordset inputParams) throws Throwable;
    
}
