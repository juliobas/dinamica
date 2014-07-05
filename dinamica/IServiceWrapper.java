package dinamica;

/**
 * IServiceWrapper<br>
 * Interface that defines methods that will be invoked 
 * before and after the invocation of the service() method
 * in any descendant of the GenericTransaction class. This
 * interface should be implemented only by descendants of
 * GenericTransaction, it has no effect on other types.<br><br>
 * The purpose of this interface is to provide a simple way
 * to add "wrapper" code to specific services, this may be used
 * to implement very specific audit logs. It is a general purpose
 * solution thay intends to meet different audit requirements.
 * The Controller will invoke these methods if the current Transaction
 * object implements this interface. These methods will form part of
 * a JDBC transaction if JDBC transactions have been enabled for
 * the current Action. These methods will use by default the same 
 * connection of the service() method. If any of these methods fail,
 * an exception will be thrown and if any JDBC transaction is active, it
 * will be rolled back.
 * <br><br>
 * Creation date: 26/01/2005<br>
 * http://www.martincordova.com<br>
 * @author mcordova - dinamica@martincordova.com
 */
public interface IServiceWrapper
{

	public void beforeService(Recordset inputParams) throws Throwable;
	public void afterService(Recordset inputParams) throws Throwable;

}
