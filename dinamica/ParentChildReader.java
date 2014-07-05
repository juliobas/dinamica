package dinamica;

/**
 * Ejecuta un conjunto de queries jerarquicamente relacionados
 * y exporta un solo recordset, que a su vez contiene recordsets
 * hijos, que a su vez contienen recordsets nietos... Utiliza
 * la facilidad del recordset que le permite a cada registro de un
 * recordset contener un recordset de hijos, esto permite representar
 * estructuras de data jerarquica pasando un solo recordset. Necesita
 * que le sea especificado en config.xml la estructura jerarquica para poder
 * procesar los queries. La estructura de los elementos es la siguiente:
 * <br><br>
 * <xmp>
 * <group-master recordset="master.sql">
 *		<group-detail recordset="detail1.sql">
 *			<group-detail recordset="detail2.sql">
 *				<group-detail recordset="detail3.sql">
 *				</group-detail>
 *			</group-detail>
 *		</group-detail>
 * </group-master>
 * </xmp>
 * <br><br>
 * A su vez puede contener un atributo scope="session" el cual es no es obligatorio
 * pero permite mantener en la sesion el recordset final publicado por esta transaccion,
 * si dicho atributo no es colocado se asume que le recordset sera publicado solo en la
 * transaccion.<br><br>
 * Por otra parte solo son obligatorios los dos primeros elementos o niveles:
 * <br><br>
 * <xmp>
 * <group-master recordset="master.sql">
 *		<group-detail recordset="detail1.sql">
 *		</group-detail>
 *	</group-master>
 * </xmp>
 * Esto quiere decir que los elemento de tipo <group-detail></group-detail> añadidos dentro
 * del <group-detail></group-detail> principal seran creados dinamicamente por esta transaccion.
 * <br><br>
 * Esta clase no tiene limite en niveles de anidacion se pueden añadir tanto elementos <group-detail></group-detail>
 * como se requieran para la regla de negocio
 * <br><br> 
 * Fecha de Actualizado: 2010-03-23<br>
 * Fecha de Creacion: 2010-03-22<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author Francisco Galizia (Martin Cordova y Asociados C.A)
 */

public class ParentChildReader extends GenericTransaction
{
	//documento config.xml
	dinamica.xml.Document docXML = null;

	public int service(Recordset inputParams) throws Throwable
	{
		//reutilizar facilidades de la clase padre
		int r = super.service(inputParams);
		
		//permite acceder a los elementos definidos en el config.xml
		docXML = getConfig().getDocument();
		dinamica.xml.Element e = docXML.getElement("//group-master");
		
		if (e==null)
			throw new Throwable("No se encontro el elemento <group-master></group-master>");
		
		if (e.getAttribute("recordset")==null)
			throw new Throwable("No se encontro el atributo 'recordset' en el elemento <group-master></group-master>");
		
		//recordset maestro
		Recordset rsMaster = getRecordset(e.getAttribute("recordset"));
		while (rsMaster.next())  {
			
			//obtener elementos
			String tagName = "//group-master/group-detail";
			dinamica.xml.Element e2 = docXML.getElement(tagName);
			
			if (e2==null)
				throw new Throwable("No se encontro el elemento <group-detail></group-detail>");
			
			if (e2.getAttribute("recordset")==null)
				throw new Throwable("No se encontro el atributo 'recordset' en el elemento <group-detail></group-detail>");
			
			//obtener recordset hijos
			Recordset rsNew = empaquetaRecordset(rsMaster, e2.getAttribute("recordset"), inputParams, tagName);
			//añadir al recordset
			rsMaster.setChildrenRecordset(rsNew);
			
		}
		
		//se debe guardar el recordset en sesion?
		if (e.getAttribute("scope")!=null && e.getAttribute("scope").equalsIgnoreCase("session"))
			getSession().setAttribute(e.getAttribute("recordset"), rsMaster);
		
		return r; 
		
	}
	
	/**
	 * Metodo recursivo que permite anexar recordset hijos
	 * a un recordset padre que a su vez puede ser un recodset
	 * hijo de otro padre y asi sucesivamente.
	 * @param rsDetail Recordset detalle al cual se le anexaran recordset hijos
	 * @param fileName Nombre del archivo que contiene el SQL del recordset hijos
	 * @param rsMaster Recordset Maestro en el cual es hijo rsDetail
	 * @param tagName Elemento TAG en xml que tiene la estructura para obtener mas recordset detalles
	 * @return Recordset que contiene los recordset hijos
	 * @throws Throwable
	 */
	public Recordset empaquetaRecordset (Recordset rsDetail, String fileName, Recordset rsMaster, String tagName) throws Throwable {
		
		//obtener recordset 
		String sql = getSQL(getResource(fileName), rsMaster);
		sql = getSQL(sql, rsDetail);
		Recordset rs = getDb().get(sql);
		
		//obtener elemento
		tagName = tagName + "/group-detail";
		dinamica.xml.Element e = docXML.getElement(tagName);
		
		//si no es nulo realizar recursividad para bajar N niveles
		if (e!=null) {
			//recorrer recordset
			rs.top();
			while (rs.next()) {
				if (e.getAttribute("recordset")==null)
					throw new Throwable("No se encontro el atributo 'recordset' en el elemento <group-detail></group-detail>");
				
				//obtener recordset hijos
				Recordset rsNew = empaquetaRecordset(rs, e.getAttribute("recordset"), rsDetail, tagName);
				//añadir al recordset
				rs.setChildrenRecordset(rsNew);
			}
		}
		
		return rs;
	}
	
}
