package dinamica.security;

import dinamica.*;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.Calendar;
import java.util.Hashtable;

/**
 * Execute the login and return an exit code:<br><br>
 * 0 - OK<br>
 * 1 - LOGIN FAILED<br>
 * 3 - FORCE NEW PASSWORD<br>
 * 4 - ACCOUNT LOCKED<br>
 * <br><br> 
 * This Transaction provides the default login mechanism
 * against a database based realm, according to the Dinamica
 * security model database (s_user table).
 * <br><br>
 * The request must contain the parameters userlogin and passwd
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class Login extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		
		//default return code (login OK)
		int rc = 0;
		
		//reuse superclass code
		super.service(inputParams);
		
		//set request attributes in case of forward to another Action
		getRequest().setAttribute("userlogin", inputParams.getString("userlogin"));
		
		//get security datasource
		String jndiName = (String)getContext().getAttribute("dinamica.security.datasource");
		if (jndiName==null)
			throw new Throwable("Context attribute [dinamica.security.datasource] is null, check your security filter configuration.");
		
		//get datasource and DB connection
		DataSource ds = Jndi.getDataSource(jndiName); 
		Connection conn = ds.getConnection();
		this.setConnection(conn);

		//recordset con los datos de la cuenta del usuario
		Recordset rs1 = null;
		
		try 
		{
			//get db channel
			Db db = getDb();
			
			//verificar si el usuario es de LDAP o de DB
			String sqlCheckLdap = getSQL(getResource("login-ldap.sql"), inputParams);
			
			//crea un recordset con la data obtenida
			Recordset rsLDAP = db.get(sqlCheckLdap);
			
			//usa login LDAP?
			if(rsLDAP.getRecordCount() > 0) {
				rsLDAP.first();
				rs1 = loginLDAP(inputParams, rsLDAP);
			} else {
				rs1 = loginDB(inputParams);
			}
			
			//check result?
			if (rs1.getRecordCount()==0)
			{

				//get invalid password policy
				String maxRetries = getConfig().getConfigValue("login-max-retries");
				String sCounter = (String)getSession().getAttribute("dinamica.security.invalidlogins");
				if (sCounter==null)	
				{
					sCounter = "1";
				}
				else
				{
					int i = Integer.parseInt(sCounter);
					i++; sCounter = String.valueOf(i);
					int j = Integer.parseInt(maxRetries);
	
					//disable account?
					if (i > j)
					{
						String sql = getResource("disable.sql");
						sql = getSQL(sql,inputParams);
						db.exec(sql);
						rc = 4;
					}
				}

				getSession().setAttribute("dinamica.security.invalidlogins", sCounter);

				//failed
				if (rc==0)
					rc = 1;
			}
			else
			{
				//login ready - check password expiration
				rs1.next();
				
				//save login history record
				String sqlLog = getResource("insert-loginlog.sql");
				sqlLog = getSQL(sqlLog, rs1);
				db.exec(sqlLog);

				if (rs1.getInt("enabled")==1)
				{
					
					int newpass = 0;
					if (!rs1.isNull("force_newpass"))
						newpass = rs1.getInt("force_newpass");
					
					//force password change?
					if (newpass==1)
					{
						rc = 3;
					}
					else
					{
						//check if password has expired
						String gpolicy = (String)getRequest().getAttribute("dinamica.security.passpolicy"); //get default policy
						String sql1 = getSQL(getResource("check-passdate.sql"), rs1);
						Recordset rsPass = db.get(sql1,1);
						if (rsPass.getRecordCount()>0)
						{
							//use default or specific password expiration policy?
							int policy = rs1.getInt("pwd_policy");
							if (policy==-2)
								policy = Integer.parseInt(gpolicy);
							
							//password never expires?
							if (policy!=-1)
							{
								rsPass.next();
								Date d = rsPass.getDate("last_change");
								if (expired(d, policy))
									rc = 3;				
							}
								
						}
						
						//login OK
						if (rc==0)
						{
							
							//registra como sesion activa
							String sessionTrace = getConfig().getConfigValue("session-trace");
							if (sessionTrace!=null && sessionTrace.equalsIgnoreCase("true")) {
								sqlLog = getSQL(getResource("insert-session.sql"), rs1);
								db.exec(sqlLog);
							}
							
							//get user preferences
							getUserPrefs(db, rs1);
							
							//get user roles
							String sqlRoles = getSQL(getResource("roles.sql"), rs1);
							Recordset rs2 = db.get(sqlRoles);

							String roles[] = new String [rs2.getRecordCount()];
							int i=0;
							while (rs2.next())
							{
								roles[i] = rs2.getString("rolename");
								i++;
							}
				
							//create user object
							DinamicaUser user = new DinamicaUser(inputParams.getString("userlogin"), roles);
				
							//store user object into session attribute
							getSession().setAttribute("dinamica.security.login", user);
				
							//set redirect URL
							getRequest().setAttribute("dinamica.security.uri", inputParams.getString("uri"));
						}
					}
				}
				else
				{
					//account locked or disabled
					rc = 4; 
				}

				if (rc==3)
				{
					getSession().setAttribute("dinamica.userlogin", inputParams.getString("userlogin"));
				}
				
			}
			
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (conn!=null)
				conn.close();
		}

		return rc;
		
	}

	/**
	 * Determines if a password has expired
	 * @param d Date of password creation
	 * @param days Days after which the password expires
	 * @return
	 */
	protected boolean expired(java.util.Date d, int days)
	{
		
		boolean b = false;
		
		//set password creation date
		Calendar c1 = Calendar.getInstance();
		c1.setTime(d);
		
		//get today
		Calendar c2 = Calendar.getInstance();
		
		//add N days to password
		c1.add(Calendar.DATE, days);
		
		//check if today is after expiration date
		if( c2.getTime().after( c1.getTime()) )
			b = true;
			
		return b;

	}

	/**
	 * Set session attributes containing user preferences
	 * like Locale and Stylesheet
	 * @param db Db channel
	 * @param user Recordset with user info after successful login
	 * @throws Throwable
	 */
	public void getUserPrefs(Db db, Recordset user) throws Throwable
	{
		java.util.Locale l = new java.util.Locale(user.getString("locale"));
		getSession().setAttribute("dinamica.user.locale", l);
	}
	
	/**
	 * Metodo que ejecuta un query para autenticar el usuario
	 * en base de datos con su login y password, asumiendo que el usuario
	 * no es del LDAP.
	 * @param inputParams Recordset con los parametros del request
	 * @return Recordset con los datos del usuario
	 * @throws Throwable
	 */
	protected Recordset loginDB(Recordset inputParams) throws Throwable 
	{

		String userid = inputParams.getString("userlogin");
		String password = inputParams.getString("passwd");

		//create MD5 hash using the string: userlogin:passwd
		java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
		byte[] b = (userid + ":" + password).getBytes();
		byte[] hash = md.digest(b);
		String pwd = Base64.encodeToString( hash, true );

		//set the "passwd" parameter value to the MD5 hash 
		inputParams.setValue("passwd", pwd);
		
		//get sql for login DB
		String sqlLogin = getSQL(getResource("login.sql"), inputParams);
		
		//get user
		Recordset rs = getDb().get(sqlLogin);
		
		return rs;
		
	}

	/**
	 * Metodo que autentica el usuario en el LDAP, obtiene los
	 * parametros de conexion del web.xml, retorna el recordset con los datos
	 * del usuario si fue autenticado exitosamente, en caso contrario retorna
	 * el recordset sin ningun record.
	 * @param inputParams Recordset con los parametros del request
	 * @param rsLDAP Recordset con los datos del usuario LDAP
	 * @return Recordset con los datos del usuario LDAP, o recordset con la estructura pero sin ningun record
	 * @throws Throwable
	 */
	protected Recordset loginLDAP(Recordset inputParams, Recordset rsLDAP) throws Throwable
	{
		
		//obtener parametros del web.xml con la configuracion para la conexion con LDAP
		String url = getContext().getInitParameter("ldap-url");
		String authentication = getContext().getInitParameter("ldap-authentication");
		
		DirContext ctx = null;
		
		//parametros para hacer login LDAP
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("com.sun.jndi.ldap.connect.pool", "true"); 
	    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	    env.put(Context.PROVIDER_URL, url);
	    env.put(Context.SECURITY_AUTHENTICATION, authentication);
	    env.put(Context.SECURITY_PRINCIPAL, rsLDAP.getString("dn"));
	    env.put(Context.SECURITY_CREDENTIALS, inputParams.getString("passwd"));

	    try {
	    	//hacer login LDAP
	    	ctx = new InitialDirContext(env);
	    } catch (NamingException e) {
	    	//si ocurre excepcion asumimos que fallo el login
	    	rsLDAP.delete(0);
		} finally {
			if (ctx!=null)
				try {
					//si la conexion sigue abierta la cerramos
					ctx.close();
				} catch (NamingException e) {
					e.printStackTrace();
				}
		}
		return rsLDAP;
	}
	
	
}
