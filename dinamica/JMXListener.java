package dinamica;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Timer;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import java.util.TimerTask;

/**
 * Monitor JMX del framework Dinamica que permite detectar threads en estado de deadlock
 * y tambien alerta en caso de que haya muchos threads (carga) en
 * estado runnable. Deja traza en el log del contexto, y opcionalmente
 * envia emails si se cumplen las condiciones de alerta. Se configura
 * con parametros del contexto cuyos nombres comienzan todos por "jmx-".<br>
 * Todas las trazas que deja este Listener comienzan con el prefijo [Dinamica-JMX]
 * para facilizar su ubicacion en el log de Tomcat.
 * <br><br>
 * 2009-08-18<br>
 * @author Martin Cordova y Asociados C.A.
 *
 */

public class JMXListener extends TimerTask implements ServletContextListener 
{
    private Timer timer = null;
    ServletContext ctx = null;
    int maxRunnable = 0;
    String logPrefix = null;
    int timerInterval = 10000;
    int sendMailInterval = 10;
    int minFreeMem = 0;
    int maxDbConnections = 0;
    String JdbcPoolName = null;
    int maxSessions = 0;
    String sessionManagerName = null;
    String webModuleName = null;
    int maxAvgResponseTime = 0;
    boolean emailOnAlert = false;
    long lastEmailSend = 0;
    StringBuilder msgBody = null;
    String mailRecipient = null;
    String mailSubject = null;
    
    String mailServer = null;
    String mailFrom = null;
    String mailFromDisplayName = null;

	boolean avgTimeEnabled = true;

    public void contextInitialized(ServletContextEvent evt) {
    	
    	// leer la configuracion
    	ctx = evt.getServletContext();
    	
    	timerInterval = Integer.parseInt(ctx.getInitParameter("jmx-timer"));
    	logPrefix = ctx.getInitParameter("jmx-log-prefix");
    	emailOnAlert = ctx.getInitParameter("jmx-send-mail").equalsIgnoreCase("true");
    	sendMailInterval = Integer.parseInt(ctx.getInitParameter("jmx-send-mail-interval"));
    	maxRunnable = Integer.parseInt(ctx.getInitParameter("jmx-max-runnable"));
    	minFreeMem = Integer.parseInt(ctx.getInitParameter("jmx-min-freemem"));
    	maxDbConnections = Integer.parseInt(ctx.getInitParameter("jmx-max-db-connections"));
    	JdbcPoolName = ctx.getInitParameter("jmx-db-pool");
    	maxSessions = Integer.parseInt(ctx.getInitParameter("jmx-max-sessions"));
    	sessionManagerName = ctx.getInitParameter("jmx-session-manager");
    	webModuleName = ctx.getInitParameter("jmx-webapp");
    	maxAvgResponseTime = Integer.parseInt(ctx.getInitParameter("jmx-average"));
    	mailSubject = ctx.getInitParameter("jmx-mail-subject");
    	mailRecipient = ctx.getInitParameter("jmx-mail-recipient");
    	
    	// configuracion general para envio de correo
    	mailServer = ctx.getInitParameter("mail-server");
    	mailFrom = ctx.getInitParameter("mail-from");
    	mailFromDisplayName = ctx.getInitParameter("mail-from-displayname");
    	
        // iniciar el timer
        timer = new Timer();
        timer.schedule(this, 1000, timerInterval);

    	ctx.log("Diagnostico JMX activado para el contexto " + ctx.getContextPath());
        
    }
 
    public void contextDestroyed(ServletContextEvent evt) {
        timer.cancel();
    }
    
    public void run() {
    	
    	boolean alert = false;
    	msgBody = new StringBuilder();
    	
    	try {
			
			//prueba de threads en estado blocked
			if ( deadlockDetected() ) {
				alert = true;
				String msg = "Deadlock de threads detectado!";
				log(msg);
			}			

    		//prueba de threads en estado blocked
			if ( blockedThreadsDetected() ) {
				alert = true;
				String msg = "Bloqueo de threads. Ver traza de Tomcat para el detalle de cada thread.";
				log(msg);
			}			

    		// prueba de threads en estado runnable
			int runnable = getRunnableThreads();
			if ( runnable > maxRunnable ) {
				alert = true;
				String msg = "Sobrecarga de threads en estado RUNNABLE: " + runnable;
				log(msg);
			}
			
			// prueba de minimo de memoria libre
			int freeRam = (int)(Runtime.getRuntime().freeMemory() / 1024) / 1024;
			if ( freeRam < minFreeMem ) {
				alert = true;
				String msg = "Memoria libre por debajo del minimo establecido: " + freeRam;
				log(msg);
			}
			
			// prueba de conexiones a BD para un pool especifico
			MBeanServer jmx = ManagementFactory.getPlatformMBeanServer();
			int numActive = ((Integer)jmx.getAttribute(new ObjectName(JdbcPoolName), "numActive")).intValue();
			if ( numActive > maxDbConnections ) {
				alert = true;
				String msg = "Conexiones a la BD por encima de lo establecido: " + numActive + " Pool: " + JdbcPoolName;
				log(msg);				 
			}
			
			// prueba de conexiones a BD para un pool especifico
			int activeSessions = ((Integer)jmx.getAttribute(new ObjectName(sessionManagerName), "activeSessions")).intValue();
			if ( activeSessions > maxSessions ) {
				alert = true;
				String msg = "Sesiones de usuario por encima de lo establecido: " + activeSessions + " Manager: " + sessionManagerName;
				log(msg);				 
			}		
			
			// prueba de tiempo promedio por request
			int avgRequestTime = getRequestAverage();
			if ( avgRequestTime > maxAvgResponseTime ) {
				if (avgTimeEnabled) {
					alert = true;
					String msg = "Tiempo promedio por request por encima del establecido: " + avgRequestTime;
					log(msg);
					avgTimeEnabled = false;
				}
			} else
				avgTimeEnabled = true;
			
			// evitar enviar mas de 1 email en menos de N minutos, sin importar si se disparan las alertas
			if (lastEmailSend>0 && (System.currentTimeMillis()-lastEmailSend) < 60000 * sendMailInterval)
				alert = false;
			
			//enviar email de ser encesario
			if (alert && emailOnAlert) {
				lastEmailSend = System.currentTimeMillis();
				SimpleMail s = new SimpleMail();
				s.send(mailServer, mailFrom, mailFromDisplayName, mailRecipient, mailSubject, msgBody.toString());
			}
			
			
		} catch (Throwable e) {
			ctx.log(logPrefix + "Error en el monitor JMX de Dinamica.", e);
		}
    }    
    
    /**
     * Retorna el tiempo de respuesta promedio de todos los servlets
     * de una aplicacion web (contexto).  
     * @return Tiempo promedio en milisegundos
     * @throws NullPointerException 
     * @throws ReflectionException 
     * @throws MBeanException 
     * @throws MalformedObjectNameException 
     * @throws InstanceNotFoundException 
     * @throws AttributeNotFoundException 
     */
    int getRequestAverage() throws Throwable {
    	
    	int average = 0;

		MBeanServer jmx = ManagementFactory.getPlatformMBeanServer();
		String servlets[] = (String[]) jmx.getAttribute(new ObjectName(webModuleName), "servlets");
		long requestCount = 0;
		long processingTime = 0;
		for (int i = 0; i < servlets.length; i++) {
			requestCount = requestCount + ((Integer)jmx.getAttribute(new ObjectName(servlets[i]), "requestCount"));
			processingTime = processingTime + ((Long)jmx.getAttribute(new ObjectName(servlets[i]), "processingTime"));
		}
    	if (requestCount>0)
    		average = (int)(processingTime / requestCount);
		
    	return average;
    	
    }
    
    /**
     * Detectar si hay threads en estado bloqueado, dejar traza de
     * cada thread en ese estado, incluyendo el nombre del thread
     * que los bloquea y el stacktrace de cada thread bloqueado.
     * @return TRUE si hay threads bloqueados o false si no los hay
     */
    boolean blockedThreadsDetected() {
		
    	boolean isBlocked = false;
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		long threadIds[] = threadBean.getAllThreadIds();
		if (threadIds!=null) {
			for (long threadId: threadIds) {
				ThreadInfo threadInfo = threadBean.getThreadInfo(threadId, 20);
				String name = threadInfo.getThreadName();
				//es un thread de tomcat?
			    if (name.startsWith("http") && threadInfo.getLockOwnerId()>=0) {
			    	isBlocked = true;
			    	StringBuilder sb = new StringBuilder(2000);
				    sb.append("BLOCKED - " + name);
			    	sb.append(" -> Bloqueado por: " + threadBean.getThreadInfo(threadInfo.getLockOwnerId()).getThreadName());
				    sb.append("\n");
				    StackTraceElement e[] = threadInfo.getStackTrace();
				    for (int i = 0; i < e.length; i++) {
				    	sb.append("\t" + e[i].getClassName() + ":" + e[i].getMethodName() + " at line: " + e[i].getLineNumber() + "\n");
					}
				    ctx.log(sb.toString());
			    }
			}
		}

		return isBlocked;
		
	}

    boolean deadlockDetected() {
    	boolean isDeadlock = false;
    	ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		long threadIds[] = threadBean.findMonitorDeadlockedThreads();
		if (threadIds!=null)
			isDeadlock = true;
		return isDeadlock;
    }
    
    /**
     * Retorna la cantidad de threads de Tomcat que procesa requests http/ssl
     * en estado RUNNABLE.
     * @return Cantidad de threads en estado RUNNABLE
     */
    int getRunnableThreads() {
    	
    	int runnable = 0;
    	
    	ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		long threadIds[] = threadBean.getAllThreadIds();
		for (long threadId: threadIds) {
			ThreadInfo threadInfo = threadBean.getThreadInfo(threadId);
		    String tName = threadInfo.getThreadName();
		    // es un thread de tomcat?
		    if (tName.startsWith("http")) {
		    	if (threadInfo.getThreadState().toString().equals("RUNNABLE"))
	    			runnable++;
		    }
		}    	
    	
    	return runnable;
    	
    }
    
    /**
     * Va construyendo el body del email de alerta a enviar
     * y también deja una traza en el log de tomcat. Va a ser
     * invocado por cada alerta que se dispare.
     * @param msg
     */
    void log(String msg) {
    	msgBody.append(msg + "\n");
    	ctx.log(logPrefix + msg);
    }
    
}