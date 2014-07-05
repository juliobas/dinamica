package dinamica;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Esta version de SysInfo no publica el recordset con el ThreadDump,
 * es por lo tanto mas liviana y se utiliza para servir la data
 * al monitor de Tomcat en tiempo real.
 * @author Martin Cordova y Asociados C.A.
 *
 */
public class SysInfo2 extends SysInfo 
{

	@Override
	public int service(Recordset inputParams) throws Throwable {
		super.service(inputParams);
		this.getData().remove("threaddump");
		return 0;
	}

	/**
	 * Esta es una version mas light del metodo de la clase dinamica.SysInfo,
	 * esta especialmente diseñado para ser invocado por el Monitor real time de Tomcat
	 */
	Recordset[] dumpThreadsInfo(MBeanServer server) throws Throwable {
		
		Recordset data[] = new Recordset[2];
		
		int blocked = 0;
		int waiting = 0;
		int runnable = 0;
		int timedWaiting = 0;
		
		Recordset rs = new Recordset();
		rs.append("name", java.sql.Types.VARCHAR);
		rs.append("state", java.sql.Types.VARCHAR);
		rs.append("stacktrace", java.sql.Types.VARCHAR);

		Recordset rs1 = new Recordset();
		rs1.append("runnable", java.sql.Types.INTEGER);
		rs1.append("waiting", java.sql.Types.INTEGER);
		rs1.append("blocked", java.sql.Types.INTEGER);
		rs1.append("timedwaiting", java.sql.Types.INTEGER);
		rs1.append("total", java.sql.Types.INTEGER);
		
		ObjectName objName = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
		Set<ObjectName> mbeans = server.queryNames(objName, null);
		for (ObjectName name: mbeans) {
			ThreadMXBean threadBean;
			threadBean = ManagementFactory.newPlatformMXBeanProxy(server, name.toString(), ThreadMXBean.class);
			long threadIds[] = threadBean.getAllThreadIds();
			for (long threadId: threadIds) {
				ThreadInfo threadInfo = threadBean.getThreadInfo(threadId);
			    String tName = threadInfo.getThreadName();
			    if (tName.startsWith("http")) {
			    	
			    	Thread.State s = threadInfo.getThreadState();
			    	if (s.toString().equals("BLOCKED"))
			    			blocked++;
			    	if (s.toString().equals("WAITING"))
		    			waiting++;
			    	if (s.toString().equals("RUNNABLE"))
		    			runnable++;
			    	if (s.toString().equals("TIMED_WAITING"))
		    			timedWaiting++;
			    }
			}
		
			rs1.addNew();
			rs1.setValue("runnable", runnable);
			rs1.setValue("waiting", waiting);
			rs1.setValue("blocked", blocked);
			rs1.setValue("timedwaiting", timedWaiting);
			rs1.setValue("total", rs.getRecordCount());
		
		}
		
		data[0] = rs1;
		data[1] = rs;
		return data;

	}
	
}
