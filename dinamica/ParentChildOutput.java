package dinamica;

/**
 * Clase que consume el recordset principal el cual contiene un conjunto de
 * recordset hijos que a su vez contiene recordset nietos... Para ello lee
 * del archivo config.xml los elementos de configuracion necesarios para obtener
 * los archivos HTML donde seran impresa la data de cada recordset. 
 * La estructura de los elementos es la siguiente:
 * <br><br>
 * <group-master recordset="master.sql" template="group-master.htm">
 *		<group-detail recordset="detail1.sql" template="group-detail.htm">
 *			<group-detail recordset="detail2.sql" template="group-detail2.htm">
 *				<group-detail recordset="detail3.sql" template="group-detail3.htm" tag="rows">
 *				</group-detail>
 *			</group-detail>
 *		</group-detail>
 * </group-master>
 * <br><br>
 * Note que el ultimo elemento <group-detail></group-detail> contiene un atributo tag="rows" el cual le
 * permite a este modulo imprimir en ese template una tabla con la data, los demas elementos <group-detail></group-detail>
 * no lo requiere ya que por si mismos ellos se estan repetiendo N veces segun registros tengan en el recordset.
 * Esto quiere decir que el ultimo elemento <group-detail></group-detail> debe contener el atributo tag="rows"
 * <br><br>
 * Por otra parte solo son obligatorios los dos primeros elementos o niveles:
 * <br><br>
 * <xmp>
 * <group-master recordset="master.sql" template="group-master.htm">
 *		<group-detail recordset="detail1.sql" template="group-detail.htm" tag="rows">
 *		</group-detail>
 * </group-master>
 * </xmp>
 * Esto quiere decir que los elemento de tipo <group-detail></group-detail> añadidos dentro
 * del <group-detail></group-detail> principal seran creados dinamicamente por esta transaccion.
 * <br><br>
 * Esta clase no tiene limite en niveles de anidacion se pueden añadir tanto elementos <group-detail></group-detail>
 * como se requieran para la regla de negocio.
 * <br><br>
 * Cada template debe contener un marcador que contiene el nombre del siguiente template algo asi: "${group-detail1.htm}"
 * ya que donde se encuentre ese marcador sera remplazado con los demas template anidados, tambien permite tener mayor
 * control de la plantilla permitiendo realizar operaciones antes o despues del marcador muy parecedido a cuando se trabaja
 * una clase con el framework y se extiende de la misma para hacer una regla de negocio antes o despues de la clase padre.
 * <br><br> 
 * Fecha de actualizacion: 2010-03-23<br>
 * Fecha de creacion: 2010-03-22<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author Francisco Galizia (Martin Cordova y Asociados C.A)
 * */
public class ParentChildOutput extends GenericOutput
{
	//documento config.xml
	dinamica.xml.Document docXML = null;

 	public void print(TemplateEngine te, GenericTransaction data) throws Throwable
	{
		
		//reuse superclass code
		super.print(te, data);
		
		//permite acceder a los elementos definidos en el config.xml
		docXML = getConfig().getDocument();
		dinamica.xml.Element e = docXML.getElement("//group-master");
		
		if (e==null)
			throw new Throwable("No se encontro el elemento <group-master></group-master>");
		
		if (e.getAttribute("recordset")==null)
			throw new Throwable("No se encontro el atributo 'recordset' en el elemento <group-master></group-master>");
		
		//obtener recordset maestro
		Recordset rsMaster = data.getRecordset(e.getAttribute("recordset"));
		
		//buffer
		StringBuilder buf = new StringBuilder();

		/* use custom locale if available */
		java.util.Locale l = (java.util.Locale)getSession().getAttribute("dinamica.user.locale");
		
		//elementos
		String tagName = "//group-master/";
		
		//recorrer recordset maestro
		rsMaster.top();
		while (rsMaster.next())
		{
			if (e.getAttribute("template")==null)
				throw new Throwable("No se encontro el atributo 'template' del elemento <group-master></group-master>");
			
			//construir sub-pagina
			this.resetRowColor();
			TemplateEngine t = new TemplateEngine(getContext(),getRequest(), getResource(e.getAttribute("template")));
			t.setLocale(l);
			
			//valores nulos
			String nullExpr = e.getAttribute("null-value");
			if (nullExpr==null)
				nullExpr = "&nbsp;";
			
			//remplazar conlumnas de recordset maestro
			t.replace(rsMaster,nullExpr);
			
			dinamica.xml.Element e2 = docXML.getElement(tagName + "/group-detail");
			
			if (e2==null)
				throw new Throwable("No se encontro el elemento <group-detail></group-detail>");
			
			if (e2.getAttribute("template")==null)
				throw new Throwable("No se encontro el atributo 'template' del elemento <group-detail></group-detail>");
			
			//apped al template
			t.replace("${" + e2.getAttribute("template") + "}", empaquetaSalida(rsMaster, tagName, l).toString());
			
			//append
			buf.append(t.toString());
		}
		
		//remplazar en el template principal
		te.replace("${group}", buf.toString());
		
	}
	
 	/**
 	 * Metodo que arma la vista con una estructura jerarquica
 	 * sin importar cuantos niveles se definan.
 	 * @param rsDetail Recordset que contiene los recordset hijos
 	 * @param tagName Elemento TAG en xml que tiene la estructura para obtener mas recordset detalles
 	 * @param l Objeto que contiene la localidad o ubicacion geografica
 	 * @return StringBuilder con las vista ya armadas
 	 * @throws Throwable
 	 */
	public StringBuilder empaquetaSalida (Recordset rsDetail, String tagName, java.util.Locale l) throws Throwable {
		
		//buffer
		StringBuilder buf = new StringBuilder();
		
		//obtener elemento
		tagName = tagName + "/group-detail";
		dinamica.xml.Element e = docXML.getElement(tagName);
		
		//si no es nulo realizar recursividad para bajar N niveles
		if (e!=null) {
			
			//recordset hijo
			Recordset rs = rsDetail.getChildrenRecordset();
		
			//recorrer recordset
			rs.top();
			while (rs.next()) {
				
				if (e.getAttribute("template")==null)
					throw new Throwable("No se encontro el atributo 'template' del elemento <group-detail></group-detail>");
				
				//alternar colores?
				IRowEvent event = null;
				String altColors = e.getAttribute("alternate-colors");
				if (altColors!=null && altColors.equalsIgnoreCase("true"))
					event = this;
				
				//valores nulos
				String nullExpr = e.getAttribute("null-value");
				if (nullExpr==null)
					nullExpr = "&nbsp;";
				
				//construir sub-pagina
				this.resetRowColor();
				TemplateEngine t = new TemplateEngine(getContext(),getRequest(), getResource(e.getAttribute("template")));
				t.setRowEventObject(event);
				t.setLocale(l);
				
				//imprimit contador de items si se require
				t.replace("${fld:detail.recordcount}", String.valueOf(rs.getRecordCount())); 
				
				//si el elemento contiene el atributo tag
				String tag = e.getAttribute("tag");
				if (tag!=null)
					//remplazar rows N veces
					t.replace(rs, nullExpr, "rows");
				else
					//replazar una sola vez
					t.replace(rs, nullExpr);
		
				//remplazar conlumnas de recordset maestro
				t.replace(rsDetail,nullExpr);
				
				dinamica.xml.Element e2 = docXML.getElement(tagName + "/group-detail");
				if(e2!= null){
					//apped al template
					t.replace("${" + e2.getAttribute("template") + "}", empaquetaSalida(rs, tagName, l).toString());
				}
				
				//append
				buf.append(t.toString());
			}
		}
		
		return buf;
	}
	
}
