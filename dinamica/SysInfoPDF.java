package dinamica;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * Reporte PDF (IText) con información y estatus del sistema vía JMX (específico para Tomcat 6).
 * Obtiene la informacion de recordsets publicados por la clase SysInfo.java, y genera un
 * reporte con calidad de imprenta del estatus del servidor.
 * <br><br>
 * Actualizado: 2008-08-03<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author mcordova (martin.cordova@gmail.com)
 */
public class SysInfoPDF extends AbstractPDFOutput
{

	//parametros requeridos para el footer
    PdfTemplate tpl = null;
    BaseFont bf = null;
    PdfContentByte cb = null;
    Image img = null;
    Font tblHeaderFont = null;
    Font tblBodyFont = null;
    
    //cambiar: parametros generales del reporte	
    String reportTitle = ""; //lo lee de config.xml por defecto
    String footerText = ""; //lo lee de web.xml o config.xml por defecto
    String logoPath = ""; //ubicacion del logotipo
    String pageXofY = " de ";  //texto por defecto para Pagina X de Y
    
    protected void createPDF(GenericTransaction data, ByteArrayOutputStream buf)
            throws Throwable
    {

		//inicializar documento: tamano de pagina, orientacion, margenes
		Document doc = new Document();
		PdfWriter docWriter = PdfWriter.getInstance(doc, buf);
		doc.setPageSize(PageSize.LETTER);
		doc.setMargins(30,30,30,40);
		
		doc.open();

		//crear fonts por defecto
		tblHeaderFont = new Font(Font.HELVETICA, 9f, Font.BOLD);
		tblBodyFont = new Font(Font.HELVETICA, 9f, Font.NORMAL);
		
		//definir pie de pagina del lado izquierdo
		String footerText = this.getFooter(); //read it from config.xml or web.xml
		String reportDate = StringUtil.formatDate(new java.util.Date(), "dd-MM-yyyy HH:mm");

		//leer del config.xml la ubicacion del logotipo
		logoPath = getConfig().getConfigValue("pdf-logo");
		
		//crear template (objeto interno de IText) y manejador de evento 
		//para imprimir el pie de pagina
		bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
		cb = docWriter.getDirectContent();
		tpl = cb.createTemplate(20, 14);
		docWriter.setPageEvent(new PDFPageEvents(footerText, pageXofY, tpl, bf, cb, reportDate));

		//titulo - lo lee de config.xml por defecto
		reportTitle = getReportTitle();
		Paragraph t = new Paragraph(reportTitle, new Font(Font.HELVETICA, 14f, Font.BOLD));
		t.setAlignment(Rectangle.ALIGN_RIGHT);
		doc.add(t);

		//logo
		img = Image.getInstance(getImage(this.getServerBaseURL() + logoPath, false));
		img.scalePercent(100);
		float imgY = doc.top() - img.getHeight();
		float imgX = doc.left();
		img.setAbsolutePosition(imgX, imgY);
		doc.add(img);	
		
		//imprimir tabla principal con la informacion general del proceso JAVA
		PdfPTable datatbl1 = getDataTable(data);
		datatbl1.setSpacingBefore(70);
		doc.add(datatbl1);
		
		//imprimir tabla de las aplicaciones del contenedor
		PdfPTable datatbl2 = getDataTable1(data);
		datatbl2.setSpacingBefore(20);
		doc.add(datatbl2);
		
		//imprimir tabla de los database pools
		PdfPTable datatbl4 = getDataTable3(data);
		datatbl4.setSpacingBefore(20);
		doc.add(datatbl4);

		//imprimir tabla de los thread pools
		PdfPTable datatbl3 = getDataTable2(data);
		datatbl3.setSpacingBefore(20);
		doc.add(datatbl3);
		
		doc.newPage();
		
		doc.add(getThreadDumpTable(data));
		
		doc.close();
		docWriter.close();
		
    }

	/**
	 * Retorna una tabla conteniendo la informacion general del proceso JAVA
	 * @param data Objeto de negocios que suple los recordsets
	 * @return
	 */
	PdfPTable getDataTable(GenericTransaction data) throws Throwable
	{

		//obtener recordset de filtro
		Recordset rs = data.getRecordset("serverinfo");
		rs.first();
		
		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(2);
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = {40,60};
		datatable.setWidths(headerwidths);
		datatable.setWidthPercentage(80);
		
		PdfPCell c = null;
		String d = null;

		//encabezado para toda la tabla
		c = new PdfPCell(new Phrase("Información general del proceso Java", tblHeaderFont ));
		c.setGrayFill(0.95f);
		c.setColspan(2);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		//fila 1
		c = new PdfPCell(new Phrase("Host", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 1
		d = rs.getString("hostname") + " PID: " + rs.getString("pid");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	

		
		//fila 1
		c = new PdfPCell(new Phrase("Sistema operativo", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 1
		d = rs.getString("os-version");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	

		//fila 2
		c = new PdfPCell(new Phrase("Java", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 2
		d = rs.getString("java-version");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 3
		c = new PdfPCell(new Phrase("Java Home", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 3
		d = rs.getString("java-home");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
				
		//fila 4
		c = new PdfPCell(new Phrase("Servidor", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 4
		d = rs.getString("server-engine");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
				
		//fila 5
		c = new PdfPCell(new Phrase("Límite de RAM", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 5
		d = StringUtil.formatNumber(rs.getValue("jvmmaxram"), "#,###,##0.00") + " MB";
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 6
		c = new PdfPCell(new Phrase("RAM reservada", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 6
		d = StringUtil.formatNumber(rs.getValue("jvmtotalram"), "#,###,##0.00") + " MB";
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 7
		c = new PdfPCell(new Phrase("RAM Libre", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 7
		d = StringUtil.formatNumber(rs.getValue("jvmfreeram"), "#,###,##0.00") + " MB";
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
				
		//fila 8
		c = new PdfPCell(new Phrase("Codificación de archivos", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 8
		d = rs.getString("encoding");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 9
		c = new PdfPCell(new Phrase("País", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 9
		d = rs.getString("country");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 10
		c = new PdfPCell(new Phrase("Idioma", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 10
		d = rs.getString("language");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 11
		c = new PdfPCell(new Phrase("Fecha del sistema", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 11
		d = StringUtil.formatDate(new Date(), "dd-MM-yyyy HH:mm:ss");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 12
		c = new PdfPCell(new Phrase("Arranque del servidor", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 12
		d = StringUtil.formatDate(rs.getDate("starttime"), "dd-MM-yyyy HH:mm:ss");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	
		
		//fila 13
		c = new PdfPCell(new Phrase("Tiempo corriendo", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 13
		d = rs.getString("uptime");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);	

		//fila 14 col1
		c = new PdfPCell(new Phrase("Framework Dinámica", tblHeaderFont));
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);
		
		//fila 14 col2
		d = rs.getString("dinamica");
		c = new PdfPCell( new Phrase( d , tblBodyFont ) );
		c.setHorizontalAlignment(Element.ALIGN_LEFT);
		datatable.addCell(c);			
		
		return datatable;
		
	}

	
	/**
	 * Retorna una tabla conteniendo las aplicaciones del contenedor
	 * @param data Objeto de negocios que suple los recordsets
	 * @return
	 */
	PdfPTable getDataTable1(GenericTransaction data) throws Throwable
	{

		//obtener recordset de data
		Recordset rs = data.getRecordset("webappsinfo");
		rs.top();

		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(7);
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = {12,18,14,12,20,12,12};
		datatable.setWidths(headerwidths);
		datatable.setWidthPercentage(100);
		datatable.setHeaderRows(2);

		PdfPCell c = null;
		String v = "";
		
		//encabezado principal
		c = new PdfPCell( new Phrase("Aplicaciones corriendo en este servidor", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		c.setColspan(7);
		datatable.addCell(c);
		
		//encabezados de columnas
		c = new PdfPCell( new Phrase("Contexto", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);

		c = new PdfPCell( new Phrase("Arranque", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Sesiones abiertas", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Pico de sesiones", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Tiempo consumido (ms)", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Requests atendidos", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Rendimiento (ms/req)", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		
		//imprimir cuerpo de la tabla
		while (rs.next())
		{
			v = rs.getString("context");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_LEFT);
			datatable.addCell(c);
			
			v = StringUtil.formatDate(rs.getDate("starttime"),"dd-MM-yyyy HH:mm:ss");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = StringUtil.formatNumber(rs.getValue("sessions"),"#,###,###");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = StringUtil.formatNumber(rs.getValue("maxactive"),"#,###,###");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = StringUtil.formatNumber(rs.getValue("processingtime"),"#,###,###");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = StringUtil.formatNumber(rs.getValue("requests"),"#,###,###");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);

			v = StringUtil.formatNumber(rs.getValue("performance"),"#,###,##0.00");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
		
		}
		
		return datatable;
		
	}    
	
	/**
	 * Retorna una tabla conteniendo los thread pools
	 * @param data Objeto de negocios que suple los recordsets
	 * @return
	 */
	PdfPTable getDataTable2(GenericTransaction data) throws Throwable
	{

		//obtener recordset de data
		Recordset rs = data.getRecordset("threadpool");
		rs.top();

		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(5);
		datatable.getDefaultCell().setPadding(3);
		datatable.setWidthPercentage(60);
		datatable.setHeaderRows(2);

		PdfPCell c = null;
		String v = "";
		
		//encabezado principal
		c = new PdfPCell( new Phrase("Threads de Tomcat por estatus ", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		c.setColspan(5);
		datatable.addCell(c);
		
		//encabezados de columnas
		c = new PdfPCell( new Phrase("Runnable", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);

		c = new PdfPCell( new Phrase("Waiting", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Blocked", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Timed Waiting", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);

		c = new PdfPCell( new Phrase("TOTAL", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		//imprimir cuerpo de la tabla
		while (rs.next())
		{
			v = rs.getString("runnable");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = rs.getString("waiting");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = rs.getString("blocked");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = rs.getString("timedwaiting");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
		
			v = rs.getString("total");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);

		}
		
		return datatable;
		
	}    
	
	/**
	 * Retorna una tabla conteniendo los database pools
	 * @param data Objeto de negocios que suple los recordsets
	 * @return
	 */
	PdfPTable getDataTable3(GenericTransaction data) throws Throwable
	{

		//obtener recordset de data
		Recordset rs = data.getRecordset("dbpool");
		rs.top();

		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(5);
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = {20,35,15,15,15};
		datatable.setWidths(headerwidths);
		datatable.setWidthPercentage(60);
		datatable.setHeaderRows(2);

		PdfPCell c = null;
		String v = "";
		
		//encabezados principales
		c = new PdfPCell( new Phrase("Database Pools", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		c.setColspan(2);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Conexiones", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		c.setColspan(3);
		datatable.addCell(c);
		
		//encabezados de columnas
		c = new PdfPCell( new Phrase("Contexto", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);

		c = new PdfPCell( new Phrase("Pool", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Activas", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Inactivas", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		c = new PdfPCell( new Phrase("Máximas", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		datatable.addCell(c);
		
		//imprimir cuerpo de la tabla
		while (rs.next())
		{
			v = rs.getString("context");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_LEFT);
			datatable.addCell(c);
			
			v = rs.getString("datasource");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = rs.getString("numactive");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = rs.getString("numidle");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
			v = rs.getString("maxactive");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
			
		}
		
		return datatable;
		
	}    
    
	PdfPTable getThreadDumpTable(GenericTransaction data) throws Throwable
	{

		//obtener recordset de data
		Recordset rs = data.getRecordset("threaddump");
		rs.top();

		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(1);
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = {100};
		datatable.setWidths(headerwidths);
		datatable.setWidthPercentage(60);
		datatable.setHeaderRows(1);

		PdfPCell c = null;
		String v = "";
		
		//encabezados principales
		c = new PdfPCell( new Phrase("Thread Dump", tblHeaderFont) );
		c.setGrayFill(0.95f);
		c.setHorizontalAlignment(Element.ALIGN_CENTER);
		c.setColspan(2);
		datatable.addCell(c);
		
		//imprimir cuerpo de la tabla
		while (rs.next())
		{

			v = rs.getString("name") + " / " + rs.getString("state")
			+ "\n" + rs.getString("stacktrace");
			c = new PdfPCell( new Phrase( v, tblBodyFont ) );
			c.setHorizontalAlignment(Element.ALIGN_LEFT);
			datatable.addCell(c);
			
		}
		
		return datatable;
		
	} 	
	
}
