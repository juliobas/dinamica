package dinamica.security;

import dinamica.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.sql.*;
import javax.sql.*;

/**
 * This class listens to session events. It is
 * used to destroy the session record in the s_session table
*/
public final class SessionListener implements HttpSessionListener
{
    public void sessionCreated(javax.servlet.http.HttpSessionEvent sbe)
    {
    }
    /**
     * Get sessionID an delete corresponding record
     * in the security database table "s_session"
     */
    public void sessionDestroyed(javax.servlet.http.HttpSessionEvent sbe) 
    {
        
		//get session ID
		HttpSession s = sbe.getSession();
		String id = s.getId();

        Connection conn = null;
        
		ServletContext ctx = s.getServletContext();        

		try 
        {

			//eliminar el registro de la tabla de sesiones activas para esta sesion
			String sql = "delete from ${schema}s_session where jsessionid = '" + id + "'";
			TemplateEngine t = new TemplateEngine(s.getServletContext(), null, sql);
			sql = t.getSql(null);
        	        	
			//usar el mismo datasource del filtro de seguridad
			String jndiName = (String)ctx.getAttribute("dinamica.security.datasource");
			if (jndiName==null)
				throw new Throwable("Context attribute [dinamica.security.datasource] is null, check your security filter configuration.");

			DataSource ds = Jndi.getDataSource(jndiName); 
			conn = ds.getConnection();
			Db db = new Db(conn);
			db.exec(sql);
			
        } 
        catch (Throwable e)
        {
            ctx.log( "SESSION LISTENER ERROR: " + e.getMessage() );
        }
        finally
        {
        	if (conn!=null)
        	{
        		try { conn.close(); } catch (Throwable e1){}
        	}
        	
        }
    
    }

}

