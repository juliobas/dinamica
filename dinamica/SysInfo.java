package dinamica;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import javax.management.*;

/**
 * Provee informacion sobre el estatus del proceso Java y de Tomcat. Extrae esta informacion via JMX
 * y la convierte en Recordsets que pueden ser utilizados para generar html, pdf o texto para enviar por email, por ejemplo.
 * Esta clase es utilizada por el action base /action/test.<br><br>
 * 
 * Para que esta clase funcione tomcat debe ser levantado con el siguiente System property en catalina.bat o catalina.sh:<br>
 * -Dcom.sun.management.jmxremote
 * <br><br>
 * 
 * Publica los siguientes recordsets:<br><br>
 * 1) serverinfo:<br>
 * <xmp>
 * 		x.append("java-version", java.sql.Types.VARCHAR);
 *		x.append("java-home", java.sql.Types.VARCHAR);
 *		x.append("os-version", java.sql.Types.VARCHAR);
 *		x.append("server-engine", java.sql.Types.VARCHAR);
 *		x.append("jvmmaxram", java.sql.Types.DOUBLE);
 *		x.append("jvmtotalram", java.sql.Types.DOUBLE);
 *		x.append("jvmfreeram", java.sql.Types.DOUBLE);
 *		x.append("encoding", java.sql.Types.VARCHAR);
 *		x.append("country", java.sql.Types.VARCHAR);
 *		x.append("language", java.sql.Types.VARCHAR);
 *		x.append("starttime", java.sql.Types.DATE);
 *		x.append("uptime", java.sql.Types.VARCHAR);
 * </xmp>
 * <br><br>
 * 2) dbpool:<br>
 * <xmp>
 * 		rs.append("context", java.sql.Types.VARCHAR)
 * 		rs.append("datasource", java.sql.Types.VARCHAR);
 *		rs.append("maxactive", java.sql.Types.INTEGER);
 *		rs.append("numactive", java.sql.Types.INTEGER);
 *		rs.append("numidle", java.sql.Types.INTEGER);
 * </xmp>
 * <br><br>
 * 3) webappsinfo:<br>
 * <xmp>
 * 		rs.append("context", java.sql.Types.VARCHAR);
 *		rs.append("sessions", java.sql.Types.INTEGER);
 *		rs.append("maxactive", java.sql.Types.INTEGER);
 *		rs.append("starttime", java.sql.Types.DATE);
 *		rs.append("processingtime", java.sql.Types.INTEGER);
 *		rs.append("reload", java.sql.Types.VARCHAR);
 *		rs.append("requests", java.sql.Types.INTEGER);
 * </xmp> 
 * <br><br>
 * 4) threadpool:<br>
 * <xmp>
 *		rs.append("name", java.sql.Types.VARCHAR);
 *		rs.append("max", java.sql.Types.INTEGER);
 *		rs.append("total", java.sql.Types.INTEGER);
 *		rs.append("active", java.sql.Types.INTEGER); 
 * </xmp> 
 * <br><br>
 * Creado: 2008-07-31<br>
 * Actualizado: 2008-07-31<br>
 * Framework Dinámica - (c) 2008 Martín Córdova y Asociados C.A.<br>
 * Este código se distribuye bajo licencia LGPL<br>
 * @author martin.cordova@gmail.com
 */
public class SysInfo extends GenericTransaction
{
	
	@SuppressWarnings("unchecked")
	@Override
	public int service(Recordset inputParams) throws Throwable {

		MBeanServer server = (MBeanServer)MBeanServerFactory.findMBeanServer(null).get(0);
	    Set MBeans = server.queryMBeans(null, null);
	    publish("dbpool", getDBPoolInfo(server, MBeans));
	    publish("serverinfo", getBasicServerInfo(server, MBeans));
	    publish("webappsinfo", getWebappsInfo(server, MBeans));

	    Recordset threadInfo[] = dumpThreadsInfo(server);
	    publish("threadpool", threadInfo[0]);
	    publish("threaddump", threadInfo[1]);

	    getSession().invalidate();
	    
	    return 0;

	}
	
	@SuppressWarnings("unchecked")
	Recordset getWebappsInfo(MBeanServer server, Set mbeans) throws Throwable 
	{
		
		Recordset rs = new Recordset();
		rs.append("context", java.sql.Types.VARCHAR);
		rs.append("sessions", java.sql.Types.INTEGER);
		rs.append("maxactive", java.sql.Types.INTEGER);
		rs.append("starttime", java.sql.Types.DATE);
		rs.append("processingtime", java.sql.Types.INTEGER);
		rs.append("requests", java.sql.Types.INTEGER);
		rs.append("performance", java.sql.Types.DOUBLE);
	
		Iterator mBeanSetIterator = mbeans.iterator();
		while (mBeanSetIterator.hasNext()) {
			ObjectInstance objectInstance = (ObjectInstance)mBeanSetIterator.next();
		    ObjectName objectName = objectInstance.getObjectName();
		    String name = objectName.toString();
		    if (name.startsWith("Catalina:j2eeType=WebModule")) {
		    	
		    	//server.invoke(objectName, "reload", null, null);
		    	
		    	rs.addNew();
		    	
		    	String path = (String)server.getAttribute(objectName, "path");
		    	if (path.equals(""))
		    		path="/";

		    	rs.setValue("context", path);
		    	rs.setValue("sessions", server.getAttribute(new ObjectName("Catalina:type=Manager,path=" + path + ",host=localhost"), "activeSessions"));
		    	rs.setValue("maxactive", server.getAttribute(new ObjectName("Catalina:type=Manager,path=" + path + ",host=localhost"), "maxActive"));
		    	
		    	String servlets[] = (String[]) server.getAttribute(objectName, "servlets");
		    	long dur = 0;
		    	for (int i = 0; i < servlets.length; i++) {
					dur = dur + ((Integer)server.getAttribute(new ObjectName(servlets[i]), "requestCount"));
				}
		    	rs.setValue("requests", Long.valueOf(dur));
		    	
		    	Long startTime =  (Long) server.getAttribute(objectName, "startTime");
				Date d = new Date(startTime.longValue());
				rs.setValue("starttime", d);		    	

				rs.setValue("processingtime", server.getAttribute(objectName, "processingTime"));
				
				double performance = 0;
				if (dur>0)
					performance = rs.getDouble("processingtime") / rs.getDouble("requests");
				rs.setValue("performance", Double.valueOf(performance));
				
		    }
		}
		
		rs.sort("context");
		return rs;
		
	}	
	
	@SuppressWarnings("unchecked")
	Recordset getDBPoolInfo(MBeanServer server, Set mbeans) throws Throwable 
	{
		
		Recordset rs = new Recordset();
		rs.append("context", java.sql.Types.VARCHAR);
		rs.append("datasource", java.sql.Types.VARCHAR);
		rs.append("maxactive", java.sql.Types.INTEGER);
		rs.append("numactive", java.sql.Types.INTEGER);
		rs.append("numidle", java.sql.Types.INTEGER);
	
		Iterator mBeanSetIterator = mbeans.iterator();
		while (mBeanSetIterator.hasNext()) {
			ObjectInstance objectInstance = (ObjectInstance)mBeanSetIterator.next();
		    ObjectName objectName = objectInstance.getObjectName();
		    String name = objectName.toString();
		    if (name.indexOf("DataSource")>0) {
		    	
		    	try {
					String items[] = StringUtil.split(name, ",");
					rs.addNew();
					rs.setValue("context", getItem("path", items));
					rs.setValue("datasource", StringUtil.replace(getItem("name", items),"\"", ""));
					rs.setValue("maxactive", server.getAttribute(objectName, "maxActive"));
					rs.setValue("numactive", server.getAttribute(objectName, "numActive"));
					rs.setValue("numidle", server.getAttribute(objectName, "numIdle"));
				} catch (java.lang.Exception e) {
					e.printStackTrace();
				}
		    	
		    }
		}
		
		if (rs.getRecordCount()>0)
			rs.sort("context");
		return rs;
		
	}
	
	String getItem(String name, String values[]) {
		for (int i = 0; i < values.length; i++) {
			if (values[i].startsWith(name)){
				String x[] = StringUtil.split(values[i], "=");
				return x[1];
			}
		}
		return "";
	}
	
	@SuppressWarnings("unchecked")
	Recordset getBasicServerInfo(MBeanServer server, Set mbeans) throws Throwable
	{

		String arch = (String) server.getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "Arch");
		
		/* define recordset structure */
		Recordset x = new Recordset();
		x.append("java-version", java.sql.Types.VARCHAR);
		x.append("java-home", java.sql.Types.VARCHAR);
		x.append("os-version", java.sql.Types.VARCHAR);
		x.append("server-engine", java.sql.Types.VARCHAR);
		x.append("jvmmaxram", java.sql.Types.DOUBLE);
		x.append("jvmtotalram", java.sql.Types.DOUBLE);
		x.append("jvmfreeram", java.sql.Types.DOUBLE);
		x.append("encoding", java.sql.Types.VARCHAR);
		x.append("country", java.sql.Types.VARCHAR);
		x.append("language", java.sql.Types.VARCHAR);
		x.append("starttime", java.sql.Types.DATE);
		x.append("uptime", java.sql.Types.VARCHAR);
		x.append("servertime", java.sql.Types.DATE);
		x.append("dinamica", java.sql.Types.VARCHAR);
		x.append("pid", java.sql.Types.VARCHAR);
		x.append("hostname", java.sql.Types.VARCHAR);
		x.addNew();

		/* get system info */
		x.setValue("dinamica", dinamica.GetVersion.getVersion());
		x.setValue("os-version", 	System.getProperty("os.name") + " " + System.getProperty("os.version") + " - " + Runtime.getRuntime().availableProcessors() + " CPU(s) on " + arch);
		x.setValue("java-version", 	System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
		x.setValue("java-home", 	System.getProperty("java.home"));
		x.setValue("server-engine", getContext().getServerInfo());
		x.setValue("jvmmaxram", 	new Double((Runtime.getRuntime().maxMemory()/1024)/1024));
		x.setValue("jvmtotalram", 	new Double((Runtime.getRuntime().totalMemory()/1024)/1024));
		x.setValue("jvmfreeram", 	new Double((Runtime.getRuntime().freeMemory()/1024)/1024));
		x.setValue("encoding", 		System.getProperty("file.encoding"));
		x.setValue("country", 		System.getProperty("user.country"));
		x.setValue("language", 		System.getProperty("user.language"));		
		
		String pid = (String)server.getAttribute(new ObjectName("java.lang:type=Runtime"), "Name");
		x.setValue("pid", pid);
		
		x.setValue("hostname", getRequest().getLocalName());
		
		Long startTime =  (Long) server.getAttribute(new ObjectName("java.lang:type=Runtime"), "StartTime");
		Date d = new Date(startTime.longValue());
		x.setValue("starttime", d);

		Long uptime =  (Long) server.getAttribute(new ObjectName("java.lang:type=Runtime"), "Uptime");
		long u = uptime.longValue();
		double u1 = ((((double)u / 1000)/60)/60);
		String msg = "";
		if (u1>24) {
			double days = u1 / 24;
			msg = StringUtil.formatNumber(Double.valueOf(days), "#.##") + " días";
		} else {
			msg = StringUtil.formatNumber(Double.valueOf(u1), "#.##") + " horas";
		}
		
		x.setValue("uptime", msg);
		
		x.setValue("servertime", new Date());
		
		return x;
	}
	
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
				ThreadInfo threadInfo = threadBean.getThreadInfo(threadId, 20);
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

			    	rs.addNew();
			    	rs.setValue("name", threadInfo.getThreadName());
			    	rs.setValue("state", threadInfo.getThreadState());
			    	
				    long lockerID = threadInfo.getLockOwnerId();
				    if (lockerID>=0) {
				    	String blockedBy =  " -> Bloqueado por: " + threadBean.getThreadInfo(lockerID).getThreadName();
				    	rs.setValue("state", threadInfo.getThreadState() + blockedBy);
				    }
				    
				    StackTraceElement e[] = threadInfo.getStackTrace();
				    StringBuilder stack = new StringBuilder();
				    for (int i = 0; i < e.length; i++) {
				    	stack.append(" --" + e[i].getClassName() + ":" + e[i].getMethodName() + " at line: " + e[i].getLineNumber());
					}
				    rs.setValue("stacktrace", stack.toString());
			    }
			}
		}
		
		rs1.addNew();
		rs1.setValue("runnable", runnable);
		rs1.setValue("waiting", waiting);
		rs1.setValue("blocked", blocked);
		rs1.setValue("timedwaiting", timedWaiting);
		rs1.setValue("total", rs.getRecordCount());
		
		data[0] = rs1;
		data[1] = rs;
		return data;
		
	}
	
}
