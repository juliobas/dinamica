package dinamica;

import javax.servlet.*;
import java.io.ByteArrayOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

/**
 * Base class to produce PDF output modules (hand made reports)
 * based on the powerful IText-PDF open source component.<br>
 * This super class provides several common utility methods, including
 * the abiity to auto-read the report header, footer and title from web.xml
 * or config.xml, and to retrieve images from URLs (local or remotes), which is
 * used to insert charts into the PDF document by reusing a server-side chart Action, 
 * or to insert another dinamically generated image, like a BarCode. The method to retrieve
 * via HTTP is session sensitive, meaning that it can reuse the same session ID, which is
 * a requirement when accessing local resources that are session-sensitive, like chart Actions.
 * <br><br>
 * In order to reuse this class, you must override the method createPDF(), you may use
 * the default implementation as a code template. For more information look into
 * the /source and /templates resources included with Dinamica distribution.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class AbstractPDFOutput extends GenericOutput
{

	/* (non-Javadoc)
	 * @see dinamica.GenericOutput#print(dinamica.GenericTransaction)
	 */
	public void print(GenericTransaction data) throws Throwable
	{
		ByteArrayOutputStream buf = new ByteArrayOutputStream(32768);
		createPDF(data, buf);
		getResponse().setContentType("application/pdf");
		getResponse().setContentLength(buf.size());
		ServletOutputStream out = getResponse().getOutputStream();
		buf.writeTo(out);
		out.close();
	}

	/**
	 * Receives a byte buffer that should be filled with resulting PDF.
	 * @param data Data module that provides recordsets to this output module
	 * @param buf Buffer to print PDF, then used to send to browser
	 * @throws Throwable
	 */
	protected void createPDF(GenericTransaction data, ByteArrayOutputStream buf) throws Throwable
	{

		//pdf objects
		Document doc = new Document();
		PdfWriter docWriter = PdfWriter.getInstance(doc, buf);

		//header
		HeaderFooter header = new HeaderFooter(new Phrase(getHeader()), false);
		header.setBorder(Rectangle.BOTTOM);
		header.setAlignment(Rectangle.ALIGN_CENTER);
		doc.setHeader(header);
			
		//footer
		HeaderFooter footer = new HeaderFooter(new Phrase(getFooter()), true);
		footer.setBorder(Rectangle.TOP);
		footer.setAlignment(Rectangle.ALIGN_RIGHT);
		doc.setFooter(footer);
			
		//pagesize
		doc.setPageSize(PageSize.LETTER);

		doc.open();
			
			//title
			Paragraph t = new Paragraph(getReportTitle(),new Font(Font.HELVETICA, 18f));
			t.setAlignment(Rectangle.ALIGN_CENTER);
			doc.add(t);
			
			//paragraph
			Paragraph p = new Paragraph("Hello World");
			p.setAlignment(Rectangle.ALIGN_CENTER);
			doc.add(p);
			
		doc.close();
		docWriter.close();
		
	}
	
	/**
	 * Calcula si una tabla (mas un espacio extra) cabe en lo que queda de pagina.
	 * Este metodo compensa la desaparicion del metodo fitsPage que IText tenia
	 * hasta la version 2.0.6, y es necesario en reportes tipo master/detail o parent/child,
	 * fue necesario crear este metodo para poder usar el ultimo IText (v2.1.2).
	 * @param doc Document ya inicializado
	 * @param docWriter PdfWriter ya inicializado
	 * @param tbl La tabla, su ancho debe haber sido definido con setTotalWidth, es decir usando
	 * un ancho absoluto, no porcentajes, de otro modo este metodo no hara un calculo correcto, por
	 * una limitacion de IText.
	 * @param extra Espacio extra para el calculo si se desea, sino pase cero.
	 * @return
	 */
	protected boolean fitsPage(Document doc, PdfWriter docWriter, PdfPTable tbl, int extra){
		
		float a = tbl.getTotalHeight() + extra;
		float b = doc.bottom(doc.bottomMargin());
		float c = docWriter.getVerticalPosition(true);
		
		return ((c - b) > a);
		
	}

	/**
	 * Get default report header (parameter pdf-header) from web.xml (context-param) or current config.xml (custom element).
	 * Any subclass may override this method.
	 * @return Report header text or NULL
	 */
	protected String getHeader()
	{
		
		return getPDFConfigValue("pdf-header");

	}
	
	/**
	 * Get default report footer (parameter pdf-footer) from web.xml (context-param) or current config.xml (custom element).
	 * Any subclass may override this method.
	 * @return Report footer text or NULL
	 */
	protected String getFooter()
	{
		return getPDFConfigValue("pdf-footer") + " ";
	}

	/**
	 * Get default report title (parameter pdf-title) from web.xml (context-param) or current config.xml (custom element).
	 * Most of the time the report title should be defined in config.xml because it is
	 * specific to the report.
	 * Any subclass may override this method.
	 * @return Report title text or NULL
	 */
	protected String getReportTitle()
	{
		return getPDFConfigValue("pdf-title");
	}

	/**
	 * Read a PDF-related config parameter. Will search first
	 * in the Action config.xml, then in web.xml file.
	 * @param param
	 * @return The corresponding value or NULL if not found.
	 */
	protected String getPDFConfigValue(String param)
	{
		String value = null;
		try
		{
			value = getConfig().getConfigValue(param);
			if (value==null || value.trim().equals(""))
			{
				value = getContext().getInitParameter(param);
			}
		}
		catch (Throwable e)
		{
			value = getContext().getInitParameter(param);
			if (value==null)
				value = "";
		}
		return value;
	}


	
}
