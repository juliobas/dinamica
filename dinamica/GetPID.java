package dinamica;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Obtiene vía JMX el PID (process ID) del proceso Tomcat
 * que está ejecutándose, y lo almacena en un atributo del request.
 * Esta clase es utilizada por el Action /action/test/getpid para
 * proveer el PID a un cliente Java local que vía JMX usando el PID
 * podrá mandar a recargar una webapp, aunque Tomcat tenga la recarga
 * automática desactivada. 
 * @author Martín Córdova y Asociados C.A.
 *
 */
public class GetPID extends GenericTransaction 
{

	@Override
	public int service(Recordset inputParams) throws Throwable {
		
		super.service(inputParams);
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		String pid = (String)server.getAttribute(new ObjectName("java.lang:type=Runtime"), "Name");
		String pidInfo[] = StringUtil.split(pid, "@");
		getRequest().setAttribute("pid", pidInfo[0]);
		getSession().invalidate();
		return 0;
		
	}

}
