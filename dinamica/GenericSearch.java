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
 * en config.xml, separados por ";", a su vez debe ser especificado el ID
 * del recordset, por defecto se toma del atributo del 
 * request "paging.recordset" pero cuando no se requiere que la salida este paginada
 * el elemento "searchCols" debe contener un atributo "id" en donde se especifique el ID
 * del recordset. El Action también debe contener una plantilla del query base llamado 
 * query-base.sql, y una plantilla SQL para cada parámetro de búsqueda, 
 * llamado "clause-XXXX-sql", donde "XXXX" sería el ID del parámetro tal como viene en el request 
 * y está definido en el validator.xml.
 * Ejemplo: <br>
 * <xmp>
 * 		<searchCols id="query.sql">fdesde;fhasta</searchCols>
 * </xmp>
 * <br>
 * Actualizado: 2010-04-08<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author mcordova (martin.cordova@gmail.com)
 * */
public class GenericSearch extends GenericTransaction
{

	@Override
	public int service(Recordset inputs) throws Throwable
	{
		//documento config.xml
		dinamica.xml.Document docXML = getConfig().getDocument();
		dinamica.xml.Element searchCols = docXML.getElement("searchCols");
		
		if ( searchCols == null )
			throw new Throwable("El elemento <searchCols> DEBE estar definido.");
		
		//define el ID del recordset a publicar
		String _rsName = searchCols.getAttribute("id");
		//si no existe un atributo ID entonces buscar en el atributo paging.recordset 
		if ( _rsName == null )
			_rsName = (String)getRequest().getAttribute("paging.recordset");
		if ( _rsName == null )
			throw new Throwable("El atributo [id] del elemento <searchCols> no ha sido definido, este atributo DEBE contener el ID del Recordset.");
				
		/* ensamblar query */
		
		//carga el template base del query
		String qBase = getResource("query-base.sql");

		//aqui se almacenaran las condiciones del WHERE
		StringBuffer qFilter = new StringBuffer();
		
		//valores del elemento searchCols
		String cols = searchCols.getValue();
		
		//verificar que el elemento searchCols contiene una lista separada por ";"
		if ( cols == null )
			throw new Throwable("El elemento <searchCols> DEBE contener una lista de valores separados por \";\"");
		
		//obtener array de los valores
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
		
		int rc = 0;
		//retorno data?
		if (rs.getRecordCount()>0) {
			//publicar recordset
			getSession().setAttribute(_rsName, rs);
			publish(_rsName, rs);
			//ejecutar logica del padre
			super.service(inputs);
		} else
			rc = 1;
		
		return rc;
		
	}
	
}
