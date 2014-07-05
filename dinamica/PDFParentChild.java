package dinamica;

import java.io.ByteArrayOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * Clase para la generacion generica del PDF del template
 * ParentChild, esta clase se nutre de una estructura de elementos
 * defenidos en el config.xml la cual es la siguiente:
 * <br><br>
 * <xmp>
 * <pdf-report recordset="" pageSize="" fontFamilyHeader="" fontSizeHeader="" fontStyleHeader="" fontFamilyBody="" fontSizeBody="" fontStyleBody="" rotate="" marginLeft="" marginRight="" marginTop="" marginBottom="">
 *
 *		<logo url="" scale=""/>
 *	
 *		<paragraph name=";" captionleft="" captionRight="" fontFamily="" fontSize="" fontStyle="" format="" align="" spacingBefore="" setIndentationLeft="">
 *	
 *			<paragraph name=";" captionleft="" captionRight="" fontFamily="" fontSize="" fontStyle="" format="" align="" spacingBefore="" setIndentationLeft="">
 *	
 *				<paragraph name=";" captionleft="" captionRight="" fontFamily="" fontSize="" fontStyle="" format="" align="" spacingBefore="" setIndentationLeft="">
 *	
 *					<last-table width="" spacingBefore="" title="" fontFamily="" fontSize="" fontStyle="" >
 *						<cell name="" title="" width="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *						<cell name="" title="" width="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *						<cell name="" title="" width="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *						<cell name="" title="" width="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *					</last-table>
 *			
 *				</paragraph>
 *
 *				<table width="" spacingBefore="" verticalTitle="" setWidths=";" title="" fontFamily="" fontSize="" fontStyle="" setGrayFill="">
 *					<cell name="" title="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *				</table>
 *
 *			</paragraph>
 *
 *			<table width="" spacingBefore="" verticalTitle="" setWidths=";" title="" fontFamily="" fontSize="" fontStyle="" setGrayFill="">
 *				<cell name="" title="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *			</table>
 *
 *		</paragraph>
 *
 *		<table width="" spacingBefore="" verticalTitle="" setWidths=";" title="" fontFamily="" fontSize="" fontStyle="" setGrayFill="">
 *			<cell name="" title="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *		</table>
 *
 *		<table width="" spacingBefore="" verticalTitle="" setWidths=";" title="" fontFamily="" fontSize="" fontStyle="" setGrayFill="">
 *			<cell name="" title="" align="" format="" fontFamily="" fontSize="" fontStyle=""/>
 *		</table>
 *
 * </pdf-report>
 * </xmp>
 * Esta clase no necesita todos los atributos y elemento para la generacion del reporte, 
 * esta realizada especialmente para poder imprimir N niveles de identacion solo necesita 
 * definir el recordset principal el cual contiene los recordset hijo y la clase se encarga
 * del resto.
 * <br><br>
 * Fecha de creacion: 2010-03-24<br>
 * Fecha de actualizacion: 2010-04-04<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author Francisco Galizia (Martin Cordova y Asociados C.A)
 */
public class PDFParentChild extends AbstractPDFOutput
{
	
	//documento config.xml
	dinamica.xml.Document docXML = null;

	//elemento raiz de la configuracion del reporte "<pdf-report>"
	dinamica.xml.Element _pdf = null;
	
	//parametros requeridos para el footer
    PdfTemplate tpl = null;
    BaseFont bf = null;
    PdfContentByte cb = null;
    Image img = null;
    Font tblHeaderFont = null;
    Font tblBodyFont = null;
    Recordset rsLast = null;
    dinamica.xml.Element el = null;
    
    //parametros generales del reporte	
    String reportTitle = ""; //lo lee de config.xml por defecto
    String footerText = ""; //lo lee de web.xml o config.xml por defecto
    String pageXofY = " de ";  //texto por defecto para Pagina X de Y
    
	@Override
    protected void createPDF(GenericTransaction data, ByteArrayOutputStream buf)
            throws Throwable
    {
		
		//permite acceder a los elementos definidos en el config.xml
    	docXML = getConfig().getDocument();
    	_pdf = docXML.getElement("//pdf-report");
    	dinamica.xml.Element title = docXML.getElement("//pdf-title");
    	
    	//soportar configuracion del tamano de papel (letter|legal)
    	Rectangle ps = null;
    	String pageSize = _pdf.getAttribute("pageSize");
    	if (pageSize==null || pageSize.equals("letter")) {
    		ps = PageSize.LETTER;
    	} else if (pageSize.equals("legal")) {
    		ps = PageSize.LEGAL;
    	} else {
    		throw new Throwable("Invalid page size, letter|legal are the only valid options.");
    	}
    	
    	//Poder rotar la pagina colocando un atributo rotate="true" en el config.xml
    	String r = _pdf.getAttribute("rotate");
    	if (r!=null && r.equals("true"))
    		ps = ps.rotate();
    	
		//inicializar documento: tamano de pagina, orientacion, margenes
		Document doc = new Document();
		PdfWriter docWriter = PdfWriter.getInstance(doc, buf);
		doc.setPageSize(ps);
		
		//margenes de las hojas
		String marginLeft = _pdf.getAttribute("marginLeft");
		String marginRight = _pdf.getAttribute("marginRight");
		String marginTop = _pdf.getAttribute("marginTop");
		String marginBottom = _pdf.getAttribute("marginBottom");
		
		if (marginLeft!= null && marginRight!= null && marginTop!= null && marginBottom!= null)
			doc.setMargins(Float.valueOf(marginLeft),Float.valueOf(marginRight),Float.valueOf(marginTop),Float.valueOf(marginBottom));
		else		
			doc.setMargins(30,30,30,40);
		
		doc.open();
		
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
		Paragraph t = new Paragraph(reportTitle, getTitleFont(title));
		t.setAlignment(Rectangle.ALIGN_RIGHT);
		doc.add(t);
		
		//imprimir logo
		printLogo(data, doc, docWriter);
		
		//imprimir tablas o parrafos
		printPDF(data, doc, docWriter);
		
		doc.close();
		docWriter.close();
		
    }
	
	/**
	 * Contiene la logica para saber el orden en que se imprimiran cada
	 * elemento del PDF sea una o varias tablas o uno o varios parrafos
	 * @param data Transaction object pasado a esta clase Output, de aqui puede extraer los recordsets
	 * @param doc Documento
	 * @param docWriter PdfWriter
	 * @throws Throwable
	 */
	public void printPDF (GenericTransaction data, Document doc, PdfWriter docWriter) throws Throwable {
		
		//ancho de las tablas
		float w = doc.right() - doc.left();
		
		//evaluar donde se deben imprimir los elementos que tienen como atributo un recordset
		dinamica.xml.Element nodes[] = docXML.getElements(_pdf);
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].getNodeName().equals("paragraph") || nodes[i].getNodeName().equals("table")) {
				if (nodes[i].getAttribute("recordset")==null) {
					boolean f1 = hasChild("paragraph", nodes[i]);
					boolean f2 = hasChild("table", nodes[i]);
					if (f1 || f2) {
						Recordset rs = data.getRecordset(_pdf.getAttribute("recordset"));
						rs.top();
						while (rs.next()) {
							String nodeName = nodes[i].getNodeName();
							if (nodeName.equals("paragraph") || nodeName.equals("table")) {
								fitsPagePDF(120, rs, nodes[i], data, doc, docWriter, w);
								
								//si tiene elementos hijos
								if (f1) {
								
									if (nodeName.equals("paragraph")) {
										if(nodes[i].getAttribute("recordset")==null)
											doc.add(printParagraph(data, doc, docWriter, nodes[i], rs));
									}
									
									getElements(nodes[i], data, doc, docWriter, rs);
	
								}
								
								//si tiene elementos hijos
								if (f2) {
									
									if (nodeName.equals("table")) {
										Recordset rsN = rs.copyStructure();
										rsN.addNew();
										rs.copyValues(rsN);
										
										if(nodes[i].getAttribute("recordset")==null) {
											if (nodes[i].getAttribute("verticalTitle")!= null && nodes[i].getAttribute("verticalTitle").equals("true"))
												doc.add(printVerticalTable(data, doc, docWriter, nodes[i], rsN, w));
											else			
												doc.add(printHorizontalTable(data, doc, docWriter, nodes[i], rsN, w));
										}
									}
									
									getElements(nodes[i], data, doc, docWriter, rs);
								}
								
								dinamica.xml.Element n[] = docXML.getElements(_pdf);
								for (int j = 0; j < n.length; j++) {
									boolean f3 = hasChild("paragraph", n[j]);
									boolean f4 = hasChild("table", n[j]);
									if (n[j].getNodeName().equals("paragraph") && !f3) {
										if(n[j].getAttribute("recordset")==null)
											doc.add(printParagraph(data, doc, docWriter, n[j], rs));
									}
									
									if (n[j].getNodeName().equals("table") && !f4) {
										Recordset rsN = rs.copyStructure();
										rsN.addNew();
										rs.copyValues(rsN);
										
										if(n[j].getAttribute("recordset")==null) {
											if (n[j].getAttribute("verticalTitle")!= null && n[j].getAttribute("verticalTitle").equals("true"))
												doc.add(printVerticalTable(data, doc, docWriter, n[j], rsN, w));
											else			
												doc.add(printHorizontalTable(data, doc, docWriter, n[j], rsN, w));
										}
									}
								}
							}
						}
					}
				}

				if (nodes[i].getAttribute("recordset")!=null) {
					String nodeName = nodes[i].getNodeName();
					if (nodeName.equals("table")) {
						if(nodes[i].getAttribute("recordset")!=null) {
							if (nodes[i].getAttribute("verticalTitle")!= null && nodes[i].getAttribute("verticalTitle").equals("true"))
								doc.add(printVerticalTable(data, doc, docWriter, nodes[i], null, w));
							else			
								doc.add(printHorizontalTable(data, doc, docWriter, nodes[i], null, w));
						}
					}
					
					if (nodeName.equals("paragraph")) {
						if(nodes[i].getAttribute("recordset")!=null)
							doc.add(printParagraph(data, doc, docWriter, nodes[i], null));
					}
				}
			}
		}
	}
	
	/**
	 * Metodo recursivo que arma cada bloque ya sea tabla o parrafo
	 * dependiendo de los distinto N niveles de anidacion que esten
	 * definidos en el config.xml
	 * @param tagName Nombre del nodo
	 * @param data Transaction object pasado a esta clase Output, de aqui puede extraer los recordsets
	 * @param doc Documento
	 * @param docWriter PdfWriter
	 * @param rsNew Recordset que contiene los hijos
	 * @throws Throwable
	 */
	public void getElements (dinamica.xml.Element node,GenericTransaction data, Document doc, PdfWriter docWriter, Recordset rsNew) throws Throwable {
		
		//ancho de las tablas
		float w = doc.right() - doc.left();
		
		Recordset rs = rsNew.getChildrenRecordset();
		if(rs!=null) {
			dinamica.xml.Element e[] = docXML.getElements(node);
			if (e!=null) {
				boolean f = true;
				PdfPTable tbl = null;
				for (int i = 0; i < e.length; i++) {
					String nodeName = e[i].getNodeName();
					if (nodeName.equals("last-table")) {
						if (e[i].getAttribute("verticalTitle")!= null && e[i].getAttribute("verticalTitle").equals("true"))
							tbl = printVerticalTable(data, doc, docWriter, e[i], rs, w);
						else			
							tbl = printHorizontalTable(data, doc, docWriter, e[i], rs, w);
						f = false;
						doc.add(tbl);
					}
				}
				
				if (f) {
					rs.top();
					while (rs.next()) {
						for (int i = 0; i < e.length; i++) {
							String nodeName = e[i].getNodeName();
							if (nodeName.equals("paragraph") || nodeName.equals("table")) {
								
								boolean f1 = hasChild("paragraph", e[i]);
								boolean f2 = hasChild("table", e[i]);
								
								fitsPagePDF(90, rs, e[i], data, doc, docWriter, w);
								
								//si tiene elementos hijos
								if (f1) {
								
									if (nodeName.equals("paragraph")) {
										if(e[i].getAttribute("recordset")==null)
											doc.add(printParagraph(data, doc, docWriter, e[i], rs));
									}
									
									getElements(e[i], data, doc, docWriter, rs);

								}
								
								//si tiene elementos hijos
								if (f2) {
									
									if (nodeName.equals("table")) {
										Recordset rs2 = rs.getChildrenRecordset();
										Recordset rsN = null;
										if (rs2==null) {
											rsN = rsNew.copyStructure();
											rsN.addNew();
											rsNew.copyValues(rsN);
										} else {
											rsN = rs.copyStructure();
											rsN.addNew();
											rs.copyValues(rsN);
										}
										
										if(e[i].getAttribute("recordset")==null) {
											if (e[i].getAttribute("verticalTitle")!= null && e[i].getAttribute("verticalTitle").equals("true"))
												doc.add(printVerticalTable(data, doc, docWriter, e[i], rsN, w));
											else			
												doc.add(printHorizontalTable(data, doc, docWriter, e[i], rsN, w));
										}
									}
									
									getElements(e[i], data, doc, docWriter, rs);
								}
								
								//si no tiene elementos hijos
								if(!f2 && !f1) {
									
									if (nodeName.equals("paragraph")) {
										if(e[i].getAttribute("recordset")==null)
											doc.add(printParagraph(data, doc, docWriter, e[i], rs));
									}
									
									if (nodeName.equals("table")) {
										Recordset rs2 = rs.getChildrenRecordset();
										Recordset rsN = null;
										if (rs2==null) {
											rsN = rsNew.copyStructure();
											rsN.addNew();
											rsNew.copyValues(rsN);
										} else {
											rsN = rs.copyStructure();
											rsN.addNew();
											rs.copyValues(rsN);
										}
										
										if(e[i].getAttribute("recordset")==null) {
											if (e[i].getAttribute("verticalTitle")!= null && e[i].getAttribute("verticalTitle").equals("true"))
												doc.add(printVerticalTable(data, doc, docWriter, e[i], rsN, w));
											else			
												doc.add(printHorizontalTable(data, doc, docWriter, e[i], rsN, w));
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Ha un salto de pagina si la tabla no entra en lo que queda de pagina
	 * asi se evitan registro huerfanos
	 * @param v Valor extra
	 * @param rs Rercordset que contiene recordset hijos
	 * @param e Elemento
	 * @param data Objeto que suple los recordset
	 * @param doc Documento
	 * @param docWriter Writer
	 * @param w tamaño del documento
	 * @throws Throwable
	 */
	public void fitsPagePDF(int v, Recordset rs, dinamica.xml.Element e, GenericTransaction data, Document doc, PdfWriter docWriter, float w) throws Throwable {
		el = null;
		searchChild("last-table", e);
		if (el!=null) {
			rsLast = null;
			getLastRecordset(rs);
			if (el.getNodeName().equals("last-table")) {
				PdfPTable tbl2 = null;
				if (el.getAttribute("verticalTitle")!= null && el.getAttribute("verticalTitle").equals("true"))
					tbl2 = printVerticalTable(data, doc, docWriter, el, rsLast, w);
				else			
					tbl2 = printHorizontalTable(data, doc, docWriter, el, rsLast, w);
				
				if (!fitsPage(doc, docWriter, tbl2, v))
					doc.newPage();
			}
		}
	}
	
	/**
	 * Verifica si un elemento tiene elemento hijos
	 * @param nodeName Nombre del elemento
	 * @param e Elemento
	 * @return f variable boolean TRUE si tiene elementos hijos FALSE caso contrario
	 * @throws Throwable
	 */
	public boolean hasChild (String nodeName, dinamica.xml.Element e) throws Throwable {
		
		//variable que determinan si un elemento tiene elementos hijos
		boolean f = false;
		
		//verificar que el elemento contiene elementos hijos
		dinamica.xml.Element n[] = docXML.getElements(e);
		for (int k = 0; k < n.length; k++) {
			if (e.getNodeName().equals(nodeName)) {
				if (n[k]!=null) {
					if(n[k].getNodeName().equals("table") || n[k].getNodeName().equals("paragraph") || n[k].getNodeName().equals("last-table")) {
						f = true;
						break;
					}
				}
			}
		}
		
		return f;
		
	}
	
	/**
	 * Busca un elemento hijo dado el nombre
	 * @param nodeName Nombre del elemento
	 * @param e Elemento
	 * @throws Throwable
	 */
	public void searchChild (String nodeName, dinamica.xml.Element e) throws Throwable {
		//verificar que el elemento contiene elementos hijos
		dinamica.xml.Element n[] = docXML.getElements(e);
		for (int k = 0; k < n.length; k++) {
			if (!n[k].getNodeName().equals(nodeName)) {
				searchChild(nodeName, n[k]);
			} else {
				el = n[k];
				break;
			}
		}
	}
	
	/**
	 * Obtener el ultimo nivel del recordset hijos
	 * @param rs Recordset con recordset hijos
	 * @throws Throwable
	 */
	public void getLastRecordset (Recordset rs) throws Throwable {
		
		Recordset rsC = rs.getChildrenRecordset();
		if (rsC!=null)
			getLastRecordset(rsC);
		else
			rsLast = rs;
	}
	
	/**
	 * Imprimir logo en el PDF
	 * @param data Transaction object pasado a esta clase Output, de aqui puede extraer los recordsets
	 * @param doc Documento
	 * @param docWriter PdfWriter
	 * @throws Throwable
	 */
	public void printLogo(GenericTransaction data, Document doc, PdfWriter docWriter) throws Throwable
	{
		//obtener elemento del logo
	    dinamica.xml.Element logo = docXML.getElement("//pdf-report/logo");
	    if (logo != null) {
	    	String logoPath = logo.getAttribute("url");
	    	float scale = Float.parseFloat(logo.getAttribute("scale"));
	    	
			//logo
			img = Image.getInstance(this.callLocalAction(logoPath));
			img.scalePercent(scale);
			float imgY = doc.top() - img.getHeight();
			float imgX = doc.left();
			img.setAbsolutePosition(imgX, imgY);
			doc.add(img);
		}
	}
	
	/**
	 * Permite definir por el config.xml el
	 * de los encabezados.
	 * @return Font
	 */
	public Font getTableHeaderFont() {
		
		//fonts de las hojas para los encabezados
		String fontFamilyHeader = _pdf.getAttribute("fontFamilyHeader");
		String fontSizeHeader = _pdf.getAttribute("fontSizeHeader");
		String fontStyleHeader = _pdf.getAttribute("fontStyleHeader");
		
		int familyHeader = Font.HELVETICA;
		float sizeHeader = 10f;
		int styleHeader = Font.BOLD;
		
		Font f = new Font(familyHeader, sizeHeader, styleHeader);
		
		if (fontFamilyHeader!= null && fontSizeHeader!= null && fontStyleHeader!= null)
			f = getFont(fontFamilyHeader, fontSizeHeader, fontStyleHeader);
		
		return f;
	}
	
	/**
	 * Permite definir por el config.xml el
	 * de los encabezados.
	 * @return Font
	 */
	public Font getTableBodyFont() {
		
		//fonts de las hojas para el contenido
		String fontFamilyBody = _pdf.getAttribute("fontFamilyBody");
		String fontSizeBody = _pdf.getAttribute("fontSizeBody");
		String fontStyleBody = _pdf.getAttribute("fontStyleBody");
			
		int familyBody = Font.HELVETICA;
		float sizeBody = 10f;
		int styleBody = Font.NORMAL;
		
		Font f = new Font(familyBody, sizeBody, styleBody);
		
		if (fontFamilyBody!= null && fontSizeBody!= null && fontStyleBody!= null)
			f = getFont(fontFamilyBody, fontSizeBody, fontStyleBody);
		
		return f;
	}
	
	/**
	 * Definir el font para el título del reporte
	 * @return Font
	 */
	public Font getTitleFont(dinamica.xml.Element title) {
		
		//fonts de las hojas para el contenido
		String fontFamilyTitle = title.getAttribute("fontFamily");
		String fontSizeTitle = title.getAttribute("fontSize");
		String fontStyleTitle = title.getAttribute("fontStyle");
			
		int familyTitle = Font.HELVETICA;
		float sizeTitle = 14f;
		int styleTitle = Font.BOLD;
		
		Font f = new Font(familyTitle, sizeTitle, styleTitle);
		
		if (fontFamilyTitle!= null && fontSizeTitle!= null && fontStyleTitle!= null)
			f = getFont(fontFamilyTitle, fontSizeTitle, fontStyleTitle);
		
		return f;
	}
	
	/**
	 * Parrafo que puede ser anexado al documento PDF
	 * @param paragraph Elemento parrafo
	 * @param rs Recordset con los datos a imprimir en el parrafo
	 * @return Paragraph
	 * @throws Throwable
	 */
	public Paragraph printParagraph(GenericTransaction data, Document doc, PdfWriter docWriter, dinamica.xml.Element paragraph, Recordset rs) throws Throwable
	{
		Paragraph t = new Paragraph();
			
		if (paragraph.getAttribute("recordset")!=null)
			rs = data.getRecordset(paragraph.getAttribute("recordset"));
		
		//si el recordset contiene registros
    	if (rs.getRecordCount() > 0) {
    		
    		if (paragraph.getAttribute("recordset")!=null)
    			rs.first();
   	
	    	//atributos que soporta el elemento parrafo
	    	String name = paragraph.getAttribute("name");
	    	String format = paragraph.getAttribute("format");
	    	String textLeftFld = paragraph.getAttribute("captionLeft");
	    	String textRightFld = paragraph.getAttribute("captionRight");
	    	String align = paragraph.getAttribute("align");
	    	String fontFamily = paragraph.getAttribute("fontFamily");
	    	String fontSize = paragraph.getAttribute("fontSize");
	    	String fontStyle = paragraph.getAttribute("fontStyle");
	    	String setIndentationLeft = paragraph.getAttribute("setIndentationLeft");
	    	
	    	//string que contendra el texto a imprimir en el parrafo
	    	String text = "";
	    	
	    	if (name==null)
	    		throw new Throwable ("No se encontro el atributo 'name' para el elemento <paragraph/>");
	    	
	    	if (align==null)
	    		throw new Throwable ("No se encontro el atributo 'align' para el elemento <paragraph/>");
	    	
	    	//colocar un texto antes de imprimir el valor para el campo
	    	if (textLeftFld!=null) {
	    		
	    		String value[] = StringUtil.split(textLeftFld, ";");
	    		if (value.length > 1) {
			    	for (int i = 0; i < value.length; i++) {
			    		text = text + value[i] + " ";
			    		
			    		String fm = format;
			    		String f[] = StringUtil.split(format, ";");
			    		if (f!=null && f.length > 1)
			    			fm = f[i];
			    		
			    		String value2[] = StringUtil.split(name, ";");
			    		if (value2.length > 1)
			    			text = text + fieldValue(value2[i], fm, rs) + " ";
			    		else
			    			text = text + fieldValue(name, fm, rs) + " ";
					}
	    		} else {
	    			text = text + textLeftFld + " ";
	    			String v[] = StringUtil.split(name, ";");
			    	for (int i = 0; i < v.length; i++) {
			    		
			    		String fm = format;
			    		String f[] = StringUtil.split(format, ";");
			    		if (f!=null && f.length > 1)
			    			fm = f[i];
			    		
			    		text = text + fieldValue(v[i], fm, rs) + " ";
					}
	    		}
	    	}
	    	
	    	if (textRightFld==null && textLeftFld==null) {
		    	//imprimir valor del campo
		    	//array con nombres de campo
		    	String v[] = StringUtil.split(name, ";");
		    	for (int i = 0; i < v.length; i++) {
		    		String fm = format;
		    		String f[] = StringUtil.split(format, ";");
		    		if (f!=null && f.length > 1)
		    			fm = f[i];
		    		
		    		text = text + fieldValue(v[i], fm, rs) + " ";
				}
	    	}
	    	
	    	//colocar un texto despues de imprimir el campo
	    	if (textRightFld!=null) {
	    		
	    		String value[] = StringUtil.split(textRightFld, ";");
	    		if (value.length > 1) {
			    	for (int i = 0; i < value.length; i++) {
			    		
			    		String fm = format;
			    		String f[] = StringUtil.split(format, ";");
			    		if (f!=null && f.length > 1)
			    			fm = f[i];
			    		
			    		String value2[] = StringUtil.split(name, ";");
			    		if (value2.length > 1)
			    			text = text + fieldValue(value2[i], fm, rs) + " ";
			    		else
			    			text = text + fieldValue(name, fm, rs) + " ";
			    		text = text + value[i] + " ";
					}
	    		} else {
	    			String v[] = StringUtil.split(name, ";");
			    	for (int i = 0; i < v.length; i++) {
			    		String fm = format;
			    		String f[] = StringUtil.split(format, ";");
			    		if (f!=null && f.length > 1)
			    			fm = f[i];
			    		
			    		text = text + fieldValue(v[i], fm, rs) + " ";
					}
	    			text = text + textRightFld + " ";
	    		}
	    	}
	    	
	    	//fonts
	    	Font f = new Font(Font.HELVETICA, 14f, Font.BOLD);
			if (fontFamily!= null && fontSize!= null && fontStyle!= null)
				f = getFont(fontFamily, fontSize, fontStyle);
	    	
			t = new Paragraph(text,f);
			
			//alineacion
			if (align.equals("left"))
				t.setAlignment(Element.ALIGN_LEFT);
			else if (align.equals("center"))
				t.setAlignment(Element.ALIGN_CENTER);
			else
				t.setAlignment(Element.ALIGN_RIGHT);
			
			if (setIndentationLeft!=null)
				t.setIndentationLeft(Float.valueOf(setIndentationLeft));
			
			if (paragraph.getAttribute("spacingBefore")!=null)
				t.setSpacingBefore(Integer.parseInt(paragraph.getAttribute("spacingBefore")));
    	}
    	//imprimir parrafo
		return t;
	}
	
	/**
	 * Tabla que puede ser anexada al documento PDF, esta tabla
	 * tiene una estructura donde los encabezados estan de manera horizontal
	 * comenzando en la primera fila
	 * @param data Objeto de negocios que suple los recordsets
	 * @param doc Documento
	 * @param docWriter PdfWriter
	 * @param table Elemento table
	 * @param rs Recordset que contiene la data a imprimir
	 * @return PdfPTable
	 */
	public PdfPTable printHorizontalTable(GenericTransaction data, Document doc, PdfWriter docWriter, dinamica.xml.Element table, Recordset rs, float width) throws Throwable
	{
		if (table.getAttribute("recordset")!=null)
			rs = data.getRecordset(table.getAttribute("recordset"));

		dinamica.xml.Element col[] = docXML.getElements(table);
		Recordset rows = getTableColHorizontal(col);
		
		//definir array que contendra los tamaños de la columnas
		int colWidth[] = new int[rows.getRecordCount()]; 
		
		rows.top();
		//recorrer el recordset y añadir los valores al array
		while(rows.next())
			colWidth[rows.getRecordNumber()] = rows.getInt("width");
		
		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(rows.getRecordCount());
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = colWidth;
		datatable.setWidths(headerwidths);
		datatable.setTotalWidth(width);
		
		if (table.getAttribute("spacingBefore")!=null)
			datatable.setSpacingBefore(Integer.parseInt(table.getAttribute("spacingBefore")));
		
		//mediante un atributo definido en el config.xml se sabe de que tamaño sera la tabla
		if(table.getAttribute("width") == null)
    		throw new Throwable("No se encontro el atributo 'width' del elemento <table>");
		
		datatable.setWidthPercentage(new Float(table.getAttribute("width")));
		datatable.setHeaderRows(1);

		PdfPCell c = null;
		
		//fonts de las hojas para el contenido
		String fontFamilyHeader = table.getAttribute("fontFamily");
		String fontSizeHeader = table.getAttribute("fontSize");
		String fontStyleHeader = table.getAttribute("fontStyle");
		
		//font
		Font f1 = getTableHeaderFont();
		if (fontFamilyHeader!= null && fontSizeHeader!= null && fontStyleHeader!= null)
			f1 = getFont(fontFamilyHeader, fontSizeHeader, fontStyleHeader);
		
		if (table.getAttribute("title")!= null) {
			//encabezados general
			c = new PdfPCell( new Phrase(table.getAttribute("title"), f1) );
			c.setGrayFill(0.95f);
			c.setColspan(rows.getRecordCount());
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
		}
	
		rows.top();
		while(rows.next()){
			//encabezados de columnas
			c = new PdfPCell( new Phrase(rows.getString("title"), f1) );
			c.setGrayFill(0.95f);
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
		}
			
		//imprimir cuerpo de la tabla
		rs.top();
		while (rs.next())
		{
			rows.top();
			while(rows.next()){

				String v = "";
				//array con nombres de campo
		    	String value[] = StringUtil.split(rows.getString("name"), ";");
		    	for (int i = 0; i < value.length; i++) {
		    		v = v + fieldValue(value[i], rows.getString("format"), rs) + " ";
				}
				//fonts de las hojas para el contenido
				String fontFamily = rows.getString("fontFamily");
				String fontSize = rows.getString("fontSize");
				String fontStyle = rows.getString("fontStyle");
				
				//font
				Font f = getTableBodyFont();
				if (fontFamily!= null && fontSize!= null && fontStyle!= null)
					f = getFont(fontFamily, fontSize, fontStyle);
				
				c = new PdfPCell( new Phrase( v, f ) );
				
				String align = rows.getString("align");
				if (align.equals("left"))
					c.setHorizontalAlignment(Element.ALIGN_LEFT);
				else if (align.equals("center"))
					c.setHorizontalAlignment(Element.ALIGN_CENTER);
				else
					c.setHorizontalAlignment(Element.ALIGN_RIGHT);
				
				datatable.addCell(c);
				
			}
			
		}
		
		return datatable;
		
	}    
	
	/**
	 * Tabla que puede ser anexada al documento PDF, esta tabla
	 * tiene una estructura donde los encabezados estan de manera vertical
	 * comenzando en la primera columna
	 * @param data Objeto de negocios que suple los recordsets
	 * @param doc Documento
	 * @param docWriter PdfWriter
	 * @param table Elemento table
	 * @return PdfPTable
	 */
	public PdfPTable printVerticalTable(GenericTransaction data, Document doc, PdfWriter docWriter, dinamica.xml.Element table, Recordset rs, float width) throws Throwable
	{
		if (table.getAttribute("recordset")!=null)
			rs = data.getRecordset(table.getAttribute("recordset"));

		dinamica.xml.Element col[] = docXML.getElements(table);
		Recordset rows = getTableColVertical(col);
		
		//definir array que contendra los tamaños de la columnas
		String colW[] = StringUtil.split(table.getAttribute("setWidths"), ";");
		int colWidth[] = new int[colW.length]; 
		for (int i = 0; i < colW.length; i++) {
			colWidth[i] = Integer.valueOf(colW[i]);
		}
		
		//definir estructura de la tabla
		PdfPTable datatable = new PdfPTable(colWidth.length);
		datatable.getDefaultCell().setPadding(3);
		int headerwidths[] = colWidth;
		datatable.setWidths(headerwidths);
		datatable.setTotalWidth(width);
		
		if (table.getAttribute("spacingBefore")!=null)
			datatable.setSpacingBefore(Integer.parseInt(table.getAttribute("spacingBefore")));
		
		//mediante un atributo definido en el config.xml se sabe de que tamaño sera la tabla
		if(table.getAttribute("width") == null)
    		throw new Throwable("No se encontro el atributo 'width' del elemento <table>");
		datatable.setWidthPercentage(new Float(table.getAttribute("width")));
		
		PdfPCell c = null;
		
		//fonts de las hojas para el contenido
		String fontFamilyHeader = table.getAttribute("fontFamily");
		String fontSizeHeader = table.getAttribute("fontSize");
		String fontStyleHeader = table.getAttribute("fontStyle");
		
		//font
		Font f1 = getTableHeaderFont();
		if (fontFamilyHeader!= null && fontSizeHeader!= null && fontStyleHeader!= null)
			f1 = getFont(fontFamilyHeader, fontSizeHeader, fontStyleHeader);
		
		if (table.getAttribute("title")!= null) {
			//encabezados general
			c = new PdfPCell( new Phrase(table.getAttribute("title"), f1) );
			c.setGrayFill(0.95f);
			c.setColspan(rows.getRecordCount());
			c.setHorizontalAlignment(Element.ALIGN_CENTER);
			datatable.addCell(c);
		}
	
		//imprimir cuerpo de la tabla
		rs.top();
		while (rs.next())
		{
			rows.top();
			while(rows.next()){

				String align = rows.getString("align");
				
				String v = "";
				//array con nombres de campo
		    	String value[] = StringUtil.split(rows.getString("name"), ";");
		    	for (int i = 0; i < value.length; i++) {
		    		v = v + fieldValue(value[i], rows.getString("format"), rs) + " ";
				}
				//fonts de las hojas para el contenido
				String fontFamily = rows.getString("fontFamily");
				String fontSize = rows.getString("fontSize");
				String fontStyle = rows.getString("fontStyle");
				
				//font
				Font f = getTableBodyFont();
				if (fontFamily!= null && fontSize!= null && fontStyle!= null)
					f = getFont(fontFamily, fontSize, fontStyle);
				
				//encabezados de columnas
				c = new PdfPCell( new Phrase(rows.getString("title"), f1) );
				
				//definir si la celda tendra un sobreado de color gris
				if (table.getAttribute("setGrayFill")!=null && table.getAttribute("setGrayFill").equals("true"))
					c.setGrayFill(0.95f);
				
				if (align.equals("left"))
					c.setHorizontalAlignment(Element.ALIGN_LEFT);
				else if (align.equals("center"))
					c.setHorizontalAlignment(Element.ALIGN_CENTER);
				else
					c.setHorizontalAlignment(Element.ALIGN_RIGHT);
				
				datatable.addCell(c);
				
				c = new PdfPCell( new Phrase( v, f ) );
				
				if (align.equals("left"))
					c.setHorizontalAlignment(Element.ALIGN_LEFT);
				else if (align.equals("center"))
					c.setHorizontalAlignment(Element.ALIGN_CENTER);
				else
					c.setHorizontalAlignment(Element.ALIGN_RIGHT);
				
				datatable.addCell(c);
				
			}
			
		}
		
		return datatable;
		
	}    
	
	
	/**
	 * Retorna el valor del campo representado en el recordset
	 * @param fieldName Nombre del campo
	 * @param format Formato para campos de tipo numerico
	 * @param rs Recordset con la data
	 * @return String con el valor
	 * @throws Throwable
	 */
	public String fieldValue (String fieldName, String format, Recordset rs) throws Throwable {
		
		String v = "";
		RecordsetField f = rs.getField(fieldName);
		switch (f.getType()) {

			case java.sql.Types.DATE:
			case java.sql.Types.TIMESTAMP:
				if(!rs.isNull(fieldName)){
					if (format!=null)
						v = StringUtil.formatDate(rs.getDate(fieldName), format);
					else
						v = rs.getString(fieldName);
				} else
					v = "";
				break;

			case java.sql.Types.INTEGER:						
			case java.sql.Types.DOUBLE:
				if(!rs.isNull(fieldName)){
					if (format!=null)
						v = StringUtil.formatNumber(rs.getValue(fieldName), format);
					else
						v = rs.getString(fieldName);
				} else
					v = "";
				break;
				
			default:
				v = rs.getString(fieldName);
				break;
		}
		
		return v;
	}
	
	/**
	 * Retorna las fuentes dados los campos
	 * @return Font
	 */
	public Font getFont(String fontFamily, String fontSize, String fontStyle) {
		
		int familyTitle = 0;
		float sizeTitle = 0f;
		int styleTitle = 0;
		
		if (fontFamily.equalsIgnoreCase("courier"))
			familyTitle = Font.COURIER;
		if (fontFamily.equalsIgnoreCase("helvetica"))
			familyTitle = Font.HELVETICA;
		if (fontFamily.equalsIgnoreCase("times_roman"))
			familyTitle = Font.TIMES_ROMAN;
			
		sizeTitle = Float.valueOf(fontSize);
			
		if (fontStyle.equalsIgnoreCase("bold"))
			styleTitle = Font.BOLD;
		if (fontStyle.equalsIgnoreCase("normal"))
			styleTitle = Font.NORMAL;
		if (fontStyle.equalsIgnoreCase("italic"))
			styleTitle = Font.ITALIC;
		if (fontStyle.equalsIgnoreCase("underline"))
			styleTitle = Font.UNDERLINE;
		if (fontStyle.equalsIgnoreCase("bolditalic"))
			styleTitle = Font.BOLDITALIC;
		
		return new Font(familyTitle, sizeTitle, styleTitle);
	}
	
	/**
	 * Retorna un recordset que contiene la estructura de atributos
	 * que contiene cada elemento col del elemento principal table.
	 * Este metodo aplica solo para la tabla de tipo horinzontal.
	 * @param col Columnas en un array
	 * @return Recordset
	 * @throws Throwable
	 */
	public Recordset getTableColHorizontal (dinamica.xml.Element col[]) throws Throwable {
		
		 //recordset para almacenar definicion de columnas
	    Recordset rows = new Recordset ();
	    rows.append("name", java.sql.Types.VARCHAR);
	    rows.append("title", java.sql.Types.VARCHAR);
	    rows.append("format", java.sql.Types.VARCHAR);
	    rows.append("width", java.sql.Types.INTEGER);
	    rows.append("align", java.sql.Types.VARCHAR);
	    rows.append("fontFamily", java.sql.Types.VARCHAR);
	    rows.append("fontSize", java.sql.Types.VARCHAR);
	    rows.append("fontStyle", java.sql.Types.VARCHAR);
	    
		for (int i = 0; i < col.length; i++) 
	    {
	    	//obtener y validar atributos del elemento
	    	dinamica.xml.Element elem = col[i];
	    	if (elem!=null && elem.getNodeName().equals("col")) {
		    	String name  = elem.getAttribute("name");
		    	String title = elem.getAttribute("title");
		    	String width = elem.getAttribute("width");
		    	String align = elem.getAttribute("align");
		    	String fontFamily = elem.getAttribute("fontFamily");
		    	String fontSize = elem.getAttribute("fontSize");
		    	String fontStyle = elem.getAttribute("fontStyle");
		    	
		    	if(name == null)
		    		throw new Throwable("No se encontro el atributo 'name' del elemento <col> #" + (i+1));
		    	if(title == null)
		    		throw new Throwable("No se encontro el atributo 'title' del elemento <col> #" + (i+1));
		    	if(width == null)
		    		throw new Throwable("No se encontro el atributo 'width' del elemento <col> #" + (i+1));
		    	if(align == null)
		    		throw new Throwable("No se encontro el atributo 'align' del elemento <col> #" + (i+1));
		    	
		    	//añadir un nuevo registro al recordset
		    	rows.addNew();
		    	rows.setValue("name", name);
		    	rows.setValue("title", title);
		    	rows.setValue("format", elem.getAttribute("format"));
		    	rows.setValue("width", Integer.valueOf(width));
		    	rows.setValue("align", align);
		    	rows.setValue("fontFamily", fontFamily);
		    	rows.setValue("fontSize", fontSize);
		    	rows.setValue("fontStyle", fontStyle);
	    	}
	    }
		
		return rows;
	}
	
	/**
	 * Retorna un recordset que contiene la estructura de atributos
	 * que contiene cada elemento col del elemento principal table.
	 * Este metodo aplica solo para la tabla de tipo vertical.
	 * @param col Columnas en un array
	 * @return Recordset
	 * @throws Throwable
	 */
	public Recordset getTableColVertical (dinamica.xml.Element col[]) throws Throwable {
		
		 //recordset para almacenar definicion de columnas
	    Recordset rows = new Recordset ();
	    rows.append("name", java.sql.Types.VARCHAR);
	    rows.append("title", java.sql.Types.VARCHAR);
	    rows.append("format", java.sql.Types.VARCHAR);
	    rows.append("align", java.sql.Types.VARCHAR);
	    rows.append("fontFamily", java.sql.Types.VARCHAR);
	    rows.append("fontSize", java.sql.Types.VARCHAR);
	    rows.append("fontStyle", java.sql.Types.VARCHAR);
	    
		for (int i = 0; i < col.length; i++) 
	    {
	    	//obtener y validar atributos del elemento
	    	dinamica.xml.Element elem = col[i];
	    	if (elem!=null && elem.getNodeName().equals("col")) {
		    	String name  = elem.getAttribute("name");
		    	String title = elem.getAttribute("title");
		    	String align = elem.getAttribute("align");
		    	String fontFamily = elem.getAttribute("fontFamily");
		    	String fontSize = elem.getAttribute("fontSize");
		    	String fontStyle = elem.getAttribute("fontStyle");
		    	
		    	if(title == null)
		    		throw new Throwable("No se encontro el atributo 'title' del elemento <col> #" + (i+1));
		    	if(align == null)
		    		throw new Throwable("No se encontro el atributo 'align' del elemento <col> #" + (i+1));
		    	
		    	//añadir un nuevo registro al recordset
		    	rows.addNew();
		    	rows.setValue("name", name);
		    	rows.setValue("title", title);
		    	rows.setValue("format", elem.getAttribute("format"));
		    	rows.setValue("align", align);
		    	rows.setValue("fontFamily", fontFamily);
		    	rows.setValue("fontSize", fontSize);
		    	rows.setValue("fontStyle", fontStyle);
	    	}
	    }
		
		return rows;
	}

}
