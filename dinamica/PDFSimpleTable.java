package dinamica;

import java.io.ByteArrayOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * Reporte PDF generico y basico con logo, titulo, tabla de data 
 * y pie de pagina con estilo "pagina X de Y". 
 * <br>
 * Esta clase lee metadata del config.xml y construye
 * la tabla con sus columnas, valores y formatos. El recordset
 * que suple la data debe ser provisto a esta clase por la clase
 * Transaction correspondiente al Action donde corren.
 * <br><br>
 * Para parametrizar esta clase se definen elementos 
 * en el config.xml como en este ejemplo:
 * <br><br>
 * <xmp>
 * <pdf-table recordset="query.sql" width="100" pageSize="letter" rotate="false" spacingBefore="70">
 *		<logo url="/images/logo.gif" scale="80" />
 *		
 *		<!--opcional-->
 *		<record recordset="viewchart.filter" title="Filtro de Búsqueda" noFilterMsg="-- Sin filtro de búsqueda --" width="40" spacingBefore="20">
 *			<col name="fdesde" title="Fecha desde" format="dd-MM-yyyy" align="center" />
 *			<col name="fhasta" title="Fecha hasta" format="dd-MM-yyyy" align="center" />
 *		</record>
 *
 *		<col name="tramite" title="Trámite" width="80" align="left" />
 *		<col name="fecha" title="Fecha" format="dd-MM-yyyy" width="20" align="center" />
 *		
 *		<!--opcional-->
 *		<after-table-row recordset="total.sql">
 *			<col value="TOTAL" align="right" colspan="1"/>
 *			<col name="total" format="#,###,##0.00" align="right"/>
 *		</after-table-row>
 *
 *		<!--opcional-->
 *		<after-table-image scale="100" url="${def:actionroot}/chart"/>
 * </pdf-table>
 * </xmp>
 * <br><br>
 * Si los campos son de tipo fecha o numéricos, debe indicar el formato. Los anchos
 * son especificados en porcentajes.
 * <br><br>
 * Actualizado: 2010-04-07<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author Francisco Galizia (galiziafrancisco@gmail.com)
 */
public class PDFSimpleTable extends AbstractPDFOutput
{

	//documento config.xml
	dinamica.xml.Document docXML = null;
	
	//elemento raiz de la configuracion del reporte "<pdf-table>"
	dinamica.xml.Element _table = null;
	
	//recordset que contendra la metadata
	Recordset rows = null;
	
	//parametros requeridos para el footer
    PdfTemplate tpl = null;
    BaseFont bf = null;
    PdfContentByte cb = null;
    Image img = null;
    Font tblHeaderFont = null;
    Font tblBodyFont = null;
    
    //parametros generales del reporte	
    String reportTitle = ""; //lo lee de config.xml por defecto
    String footerText = ""; //lo lee de web.xml o config.xml por defecto
    String logoPath = ""; //ubicacion del logotipo
    float scale = 100; //escala del grafico
    String pageXofY = " de ";  //texto por defecto para Pagina X de Y
    
    protected void createPDF(GenericTransaction data, ByteArrayOutputStream buf)
            throws Throwable
    {
    	//permite acceder a los elementos definidos en el config.xml
    	docXML = getConfig().getDocument();
        _table = docXML.getElement("pdf-table");
       
        //recordset para almacenar definicion de columnas
	    rows = new Recordset ();
	    rows.append("name", java.sql.Types.VARCHAR);
	    rows.append("title", java.sql.Types.VARCHAR);
	    rows.append("format", java.sql.Types.VARCHAR);
	    rows.append("width", java.sql.Types.INTEGER);
	    rows.append("align", java.sql.Types.VARCHAR);
	    
	    //obtener definicion de columnas
	    dinamica.xml.Element col[] = docXML.getElements("//pdf-table/col");
	    for (int i = 0; i < col.length; i++) 
	    {
	    	//obtener y validar atributos del elemento
	    	dinamica.xml.Element elem = col[i];
	    	
	    	String name  = elem.getAttribute("name");
	    	String title = elem.getAttribute("title");
	    	String width = elem.getAttribute("width");
	    	String align = elem.getAttribute("align");
	    	
	    	if(name == null)
	    		throw new Throwable("No se encontro el valor del atributo [name] del elemento <col> #" + (i+1));
	    	if(title == null)
	    		throw new Throwable("No se encontro el valor del atributo [title] del elemento <col> #" + (i+1));
	    	if(width == null)
	    		throw new Throwable("No se encontro el valor del atributo [width] del elemento <col> #" + (i+1));
	    	if(align == null)
	    		throw new Throwable("No se encontro el valor del atributo [align] del elemento <col> #" + (i+1));
	    	
	    	//añadir un nuevo registro al recordset
	    	rows.addNew();
	    	rows.setValue("name", name);
	    	rows.setValue("title", title);
	    	rows.setValue("format", elem.getAttribute("format"));
	    	rows.setValue("width", Integer.valueOf(width));
	    	rows.setValue("align", align);
	    }
	   
	    //obtener elemento del logo
	    dinamica.xml.Element logo = docXML.getElement("//logo");
	    if (logo == null)
	    	throw new Throwable("No se encontro el elemento <logo> en el archivo config.xml - forma parte obligatoria de la definición de un reporte genérico.");
	    	
	    logoPath = logo.getAttribute("url");
    	scale = Float.parseFloat(logo.getAttribute("scale"));
	    
    	//patch 20091119 - soportar configuracion del tamano de papel (letter|legal)
    	Rectangle ps = null;
    	String pageSize = _table.getAttribute("pageSize");
    	if (pageSize==null || pageSize.equals("letter")) {
    		ps = PageSize.LETTER;
    	} else if (pageSize.equals("legal")) {
    		ps = PageSize.LEGAL;
    	} else {
    		throw new Throwable("Invalid page size, letter|legal are the only valid options.");
    	}
    	
    	/*patch para poder rotar la pagina colocando un atributo rotate="true" en el config.xml*/
    	String r = _table.getAttribute("rotate");
    	if (r!=null && r.equals("true"))
    		ps = ps.rotate();
    	
		//inicializar documento: tamano de pagina, orientacion, margenes
		Document doc = new Document();
		PdfWriter docWriter = PdfWriter.getInstance(doc, buf);
		doc.setPageSize(ps);
		doc.setMargins(30,30,30,40);
		
		doc.open();
		
		//crear fonts por defecto
		tblHeaderFont = getTableHeaderFont();
		tblBodyFont = getTableBodyFont();
		
		//definir pie de pagina del lado izquierdo
		String footerText = this.getFooter(); //read it from config.xml or web.xml
		String dateFormat = getContext().getInitParameter("pdf-dateformat");
		if (dateFormat==null || dateFormat.equals(""))
				dateFormat = "dd/MM/yyyy HH:mm";
		String reportDate = StringUtil.formatDate(new java.util.Date(), dateFormat);
		
		//crear template (objeto interno de IText) y manejador de evento 
		//para imprimir el pie de pagina
		bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		cb = docWriter.getDirectContent();
		tpl = cb.createTemplate(20, 14);
		docWriter.setPageEvent(new PDFPageEvents(footerText, pageXofY, tpl, bf, cb, reportDate));

		//titulo - lo lee de config.xml por defecto
		reportTitle = getReportTitle();
		Paragraph t = new Paragraph(reportTitle, getTitleFont());
		t.setAlignment(Rectangle.ALIGN_RIGHT);
		doc.add(t);

		//logo
		img = Image.getInstance(this.callLocalAction(logoPath));
		img.scalePercent(scale);
		float imgY = doc.top() - img.getHeight();
		float imgX = doc.left();
		img.setAbsolutePosition(imgX, imgY);
		doc.add(img);	
		
		//imprimir antes de la tabla si es necesario
		beforePrintTable(data, doc, docWriter);
		
		//imprimir tabla principal
		PdfPTable dataTable = getDataTable(data);
		dataTable.setSpacingBefore(getDefaultSpacing());
		//agregar un row de total si es necesario
		addTotalRow(dataTable, data, doc, docWriter);
		doc.add(dataTable);

		//imprimir despues de la tabla si es necesario
		afterPrintTable(data, doc, docWriter);
		
		doc.close();
		docWriter.close();
		
    }

	/**
	 * Retorna una tabla conteniendo la data del recordset
	 * @param data Objeto de negocios que suple los recordsets
	 * @return
	 */
	protected PdfPTable getDataTable(GenericTransaction data) throws Throwable
	{

		//obtener recordset de data
		//obtiene el nombre del recordset mediante el atributo definido en el config.xml
		Recordset rs = data.getRecordset(_table.getAttribute("recordset"));
		rs.top();
		
		//definir array que contendra los tamaños de la columnas
		int colWidth[] = new int[rows.getRecordCount()]; 
		
		rows.top();
		//recorrer el recordset y añadir los valores al array
		while(rows.next()){
			colWidth[rows.getRecordNumber()] = rows.getInt("width");
		}
		
		//definir estructura de la tabla
		//cuenta el numero de registros que contiene el recordset con la metadadata
		//del config.xml, para asi saber cuantas columnas tendra la tabla
		PdfPTable datatable = new PdfPTable(rows.getRecordCount());
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = colWidth;
		datatable.setWidths(headerwidths);
		
		//mediante un atributo definido en el config.xml se sabe de que tamaño sera la tabla
		datatable.setWidthPercentage(new Float(_table.getAttribute("width")));
		datatable.setHeaderRows(1);

		PdfPCell c = null;
		String v = "";

		rows.top();
		while(rows.next()){
			//encabezados de columnas
			c = new PdfPCell( new Phrase(rows.getString("title"), tblHeaderFont) );
			c.setGrayFill(0.95f);
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
		}
	
		//imprimir cuerpo de la tabla
		while (rs.next())
		{
			rows.top();
			while(rows.next()){

				int align = 0;
				//asignar a cada celda la alineacion
				if(rows.getString("align").equals("center"))
					align = Element.ALIGN_CENTER;
				
				if(rows.getString("align").equals("left"))
					align = Element.ALIGN_LEFT;
				
				if(rows.getString("align").equals("right"))
					align = Element.ALIGN_RIGHT;
			
				//asignar a la celda los valores con su respectivo formatos, segun el
				//tipo de dato que contenga
				RecordsetField rf = rs.getField(rows.getString("name"));
				switch (rf.getType()) {
				
					case java.sql.Types.DATE:
					case java.sql.Types.TIMESTAMP:
						if(!rs.isNull(rows.getString("name"))){
							if (rows.getString("format")!=null)
								v = StringUtil.formatDate(rs.getDate(rows.getString("name")), rows.getString("format"));
							else
								v = StringUtil.formatDate(rs.getDate(rows.getString("name")), "dd-MM-yyyy");
						} else
							v = "";
						break;
	
					case java.sql.Types.INTEGER:						
					case java.sql.Types.DOUBLE:
						if(!rs.isNull(rows.getString("name"))){
							if (rows.getString("format")!=null)
								v = StringUtil.formatNumber(rs.getValue(rows.getString("name")), rows.getString("format"));
							else
								v = StringUtil.formatNumber(rs.getValue(rows.getString("name")), "#,###,##0.00");
						} else
							v = "";
						break;
						
					default:
						v = rs.getString(rows.getString("name"));
						break;
				}

				c = new PdfPCell( new Phrase( v, tblBodyFont ) );
				c.setHorizontalAlignment(align);
				datatable.addCell(c);
				
			}
			
		}
	
		return datatable;
		
	}    
    
	/**
	 * Define el font a ser utilizado para los encabezados de la tabla,
	 * puede sobreescribir este metodo si desea cambiar el font en una
	 * subclase de este reporte
	 * @return Font
	 */
	protected Font getTableHeaderFont() {
		return new Font(Font.HELVETICA, 10f, Font.BOLD);
	}
	
	/**
	 * Define el font a ser utilizado para el cuerpo de la tabla,
	 * puede sobreescribir este metodo si desea cambiar el font en una
	 * subclase de este reporte
	 * @return Font
	 */	
	protected Font getTableBodyFont() {
		return new Font(Font.HELVETICA, 10f, Font.NORMAL);
	}
	
	/**
	 * Definir el font para el título del reporte
	 * @return
	 */
	protected Font getTitleFont() {
		return new Font(Font.HELVETICA, 14f, Font.BOLD);
	}
	
	
	/**
	 * Por si desea imprimir algo antes de la tabla de datos
	 * @param data Transaction object pasado a esta clase Output, de aqui puede extraer los recordsets
	 * @param doc
	 * @param docWriter
	 * @throws Throwable
	 */
	protected void beforePrintTable(GenericTransaction data, Document doc, PdfWriter docWriter) throws Throwable
	{
		
		//imprimir filtro del reporte si existe la configuracion
		dinamica.xml.Element e = docXML.getElement("//pdf-table/record");
		if (e!=null) {

			//obtener recordset de filtro
			Recordset rs = data.getRecordset(e.getAttribute("recordset"));
			rs.first();
			
			//titulo sin filtro de búsqueda
			String titleWP = e.getAttribute("noFilterMsg");
			
			//definir estructura de la tabla
			PdfPTable datatable = new PdfPTable(2);
			datatable.getDefaultCell().setPadding(3);
			int headerwidths[] = {50,50};
			datatable.setWidths(headerwidths);
			datatable.setWidthPercentage(Integer.parseInt(e.getAttribute("width")));
			datatable.setSpacingBefore(Integer.parseInt(e.getAttribute("spacingBefore")));
			
			PdfPCell c = null;
			
			//encabezado para toda la tabla
			c = new PdfPCell(new Phrase(e.getAttribute("title"), tblHeaderFont ));
			c.setGrayFill(0.95f);
			c.setColspan(2);
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			dinamica.xml.Element cols[] = docXML.getElements("//pdf-table/record/col");
			for (int i = 0; i < cols.length; i++) 
			{
		    	dinamica.xml.Element elem = cols[i];
		    	
		    	String name  = elem.getAttribute("name");
		    	String title = elem.getAttribute("title");
		    	String format = elem.getAttribute("format");
		    	String align = elem.getAttribute("align");
		    	
		    	if(name == null)
		    		throw new Throwable("No se encontro el valor del atributo [name] del elemento <col> #" + (i+1));
		    	if(title == null)
		    		throw new Throwable("No se encontro el valor del atributo [title] del elemento <col> #" + (i+1));
		    	if(align == null)
		    		throw new Throwable("No se encontro el valor del atributo [align] del elemento <col> #" + (i+1));
		    	
		    	//verificar si el campo es nulo y tiene titulo sin filtro de busqueda, de ser asi no imprimir celda
		    	if (!rs.isNull(name) || titleWP==null) {
			    	c = new PdfPCell(new Phrase(title, tblHeaderFont));
					c.setGrayFill(0.95f);
					c.setHorizontalAlignment(Element.ALIGN_LEFT);
					datatable.addCell(c);
			    	
					String v = "";
					RecordsetField f = rs.getField(name);
					switch (f.getType()) {
		
						case java.sql.Types.DATE:
						case java.sql.Types.TIMESTAMP:
							if(!rs.isNull(name)){
								if (format!=null)
									v = StringUtil.formatDate(rs.getDate(name), format);
								else
									v = StringUtil.formatDate(rs.getDate(name), "dd-MM-yyyy");
							} else
								v = "";
							break;
		
						case java.sql.Types.INTEGER:						
						case java.sql.Types.DOUBLE:
							if(!rs.isNull(name)){
								if (format!=null)
									v = StringUtil.formatNumber(rs.getValue(name), format);
								else
									v = StringUtil.formatNumber(rs.getValue(name), "#,###,##0.00");
							} else
								v = "";
							break;
							
						default:
							v = rs.getString(name);
							break;
					}
					
					c = new PdfPCell( new Phrase( v , tblBodyFont ) );
					
					if (align.equals("left"))
						c.setHorizontalAlignment(Element.ALIGN_LEFT);
					else if (align.equals("center"))
						c.setHorizontalAlignment(Element.ALIGN_CENTER);
					else
						c.setHorizontalAlignment(Element.ALIGN_RIGHT);
					datatable.addCell(c);
		    	}
			}	
			
			//verificar si la tabla mas de una celda de no ser asi imprimir celda de que no coloco
			//filtro de búsqueda
			if (datatable.getRows().size()==1)
			{
				c = new PdfPCell(new Phrase(titleWP, tblHeaderFont));
				c.setHorizontalAlignment(Element.ALIGN_CENTER);
				c.setColspan(2);
				datatable.addCell(c);
			}
			
			doc.add(datatable);
			
		}
		
	}

	/**
	 * Por si desea imprimir algo luego de la tabla de datos
	 * @param data Transaction object pasado a esta clase Output, de aqui puede extraer los recordsets
	 * @param doc
	 * @param docWriter
	 * @throws Throwable
	 */
	protected void afterPrintTable(GenericTransaction data, Document doc, PdfWriter docWriter) throws Throwable
	{
		
		//imprimir grafico si existe la configuracion
		dinamica.xml.Element e = docXML.getElement("//after-table-image");
		if (e!=null) {
			Image img = Image.getInstance(getImage(this.getServerBaseURL() + e.getAttribute("url"), false));
			//patch 20100514 permite colocar en el request el tipo de grafico que sera mostrado
			String plugin = getRequest().getParameter("plugin");
			if (plugin!=null)
				img = Image.getInstance(getImage(this.getServerBaseURL() + e.getAttribute("url") + "?plugin=" + plugin, false));
			img.scalePercent(Integer.parseInt(e.getAttribute("scale")));
			img.setAlignment(Element.ALIGN_CENTER);
			doc.add(img);			
		}
		
	}

	/**
	 * Por si desea añadir una celda de totales a la tabla principal
	 * @param tbl Tabla a la cual añadir la celda
	 * @param data Transaction object pasado a esta clase Output, de aqui puede extraer los recordsets
	 * @param doc
	 * @param docWriter
	 * @throws Throwable
	 */
	protected void addTotalRow(PdfPTable tbl, GenericTransaction data, Document doc, PdfWriter docWriter) throws Throwable
	{

		//imprimir filtro del reporte si existe la configuracion
		dinamica.xml.Element e = docXML.getElement("//pdf-table/after-table-row");
		if (e!=null) {

			//obtener recordset de filtro
			Recordset rs = data.getRecordset(e.getAttribute("recordset"));
			rs.first();
			
			PdfPCell c = null;
			
			dinamica.xml.Element cols[] = docXML.getElements("//pdf-table/after-table-row/col");
			for (int i = 0; i < cols.length; i++) 
			{
		    	dinamica.xml.Element elem = cols[i];
		    	
		    	String name  = elem.getAttribute("name");
		    	String value  = elem.getAttribute("value");
		    	String colspan = elem.getAttribute("colspan");
		    	String format = elem.getAttribute("format");
		    	String align = elem.getAttribute("align");
		    	
		    	String v = "";
		    	
		    	if (name==null) {
		    		v = value;
		    		if (v==null)
		    			v = "";
		    	}
		    	else {
		    		RecordsetField f = rs.getField(name);
					switch (f.getType()) {
		
						case java.sql.Types.DATE:
						case java.sql.Types.TIMESTAMP:
							if(!rs.isNull(name)){
								if (format!=null)
									v = StringUtil.formatDate(rs.getDate(name), format);
								else
									v = StringUtil.formatDate(rs.getDate(name), "dd-MM-yyyy");
							} else
								v = "";
							break;
		
						case java.sql.Types.INTEGER:						
						case java.sql.Types.DOUBLE:
							if(!rs.isNull(name)){
								if (format!=null)
									v = StringUtil.formatNumber(rs.getValue(name), format);
								else
									v = StringUtil.formatNumber(rs.getValue(name), "#,###,##0.00");
							} else
								v = "";
							break;
							
						default:
							v = rs.getString(name);
							break;
					}		    		
		    	}
		    	
				c = new PdfPCell( new Phrase( v , tblBodyFont ) );
				
				if (align.equals("left"))
					c.setHorizontalAlignment(Element.ALIGN_LEFT);
				else if (align.equals("center"))
					c.setHorizontalAlignment(Element.ALIGN_CENTER);
				else
					c.setHorizontalAlignment(Element.ALIGN_RIGHT);

				if (colspan!=null)
					c.setColspan(Integer.parseInt(colspan));
				
				tbl.addCell(c);
				
			}			
			
		}
		
	}
	
	/**
	 * Retorna el espacio a imprimir antes de la tabla de datos,
	 * una subclase puede redefinir este metodo para variar el espacio.
	 * @return El espacio en puntos antes de la tabla de datos
	 */
	protected float getDefaultSpacing() {
		String sBefore = _table.getAttribute("spacingBefore");
		if (sBefore!=null)
			return Integer.parseInt(sBefore);
		else
			return 70;
	}

}
