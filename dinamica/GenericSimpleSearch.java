package dinamica;

/**
 * Motor de busqueda genérico, construye un SQL condicionalmente
 * de acuerdo a los parametros recibidos, ejecuta el query
 * y retorna 0 o 1 dependiendo de si el recordset tiene o no
 * registros. Esta clase dejara el recordset en sesion para que
 * pueda mostrarse en una vista paginada. 
 * <br><br>
 * Esta clase soporta un número variable de parámetros de búsqueda,
 * los mismos deben ser especificados en un elemento llamado "searchCols" 
 * en config.xml, separados por ";", y el Action también debe contener
 * una plantilla del query base llamado query-base.sql, y una plantilla SQL para cada
 * parámetro de búsqueda, llamado "clause-XXXX-sql", donde "XXXX" sería el 
 * ID del parámetro tal como viene en el request y está definido en el validator.xml.<br>
 * Es imprescindible que el config.xml tenga definido el atributo de request "paging.recordset".
 * <br>
 * Actualizado: 2009-05-26<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author mcordova (martin.cordova@gmail.com)
 * */
public class GenericSimpleSearch extends GenericTransaction
{

	@Override
	public int service(Recordset inputs) throws Throwable
	{

		//define el ID del recordset a publicar
		String _rsName = (String)getRequest().getAttribute("paging.recordset");
		
		if ( _rsName == null )
			throw new Throwable("El atributo del request [paging.recordset] no ha sido definido, este atributo DEBE contener el ID del Recordset.");
				
		//reutiliza la logica de la clase padre
		int rc = super.service(inputs);
		
		/* ensamblar query */
		
		//carga el template base del query
		String qBase = getResource("query-base.sql");

		//aqui se almacenaran las condiciones del WHERE
		StringBuffer qFilter = new StringBuffer();
		
		String cols = getConfig().getConfigValue("searchCols");
		String params[] = StringUtil.split(cols, ";");
		
		for (int i=0;i<params.length;i++)
		{
			if (inputs.getValue(params[i])!=null)
				qFilter.append(getResource("clause-" + params[i]+ ".sql"));
		}

		//ya tenemos la lista de condiciones
		String where = qFilter.toString();
		
		//ahora reemplaza las condiciones en el query base
		qBase = StringUtil.replace(qBase,"${filter}", where);

		/* listo el query - quedo ensamblado */
		
		//ahora reemplaza los valores de los parametros en el query
		String sql = getSQL(qBase, inputs);
		
		//ejecutar query y crear recordset
		Recordset rs = getDb().get(sql);
		
		//retorno data?
		if (rs.getRecordCount()>0)
		{
			//publicar recordset
			getSession().setAttribute(_rsName, rs);
			rc = 0;
		}
		else
		{
			rc = 1;
		}
		
		return rc;
		
	}

}
