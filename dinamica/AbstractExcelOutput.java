package dinamica;

import java.io.ByteArrayOutputStream;
import javax.servlet.ServletOutputStream;
import jxl.*;
import jxl.write.WritableWorkbook;


/**
 * Base class used to create Excel document generators, which
 * are a special type of Output modules.<br>
 * It is based on the JExcel API open source component.<br>
 * <br>
 * In order to reuse this class, you must override the method createWorkbook.
 * <br><br>
 * (c) 2007 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class AbstractExcelOutput extends GenericOutput
{

	/**
	 * Create excel document using Java Excel API, after assembling the document
	 * send it to the browser.
	 */
	public void print(GenericTransaction data) throws Throwable
	{
		
		//store document into memory
		ByteArrayOutputStream buf = new ByteArrayOutputStream(32768);
		createWorkbook(data, buf);
		
		//send output to browser
		getResponse().setContentType("application/vnd.ms-excel");
		getResponse().setHeader("Content-Disposition", getAttachmentString());
		getResponse().setContentLength(buf.size());
		ServletOutputStream out = getResponse().getOutputStream();
		buf.writeTo(out);
		buf.close();
		out.close();
		
	}
	
	/**
	 * Create excel document as a workbook. This method
	 * should be overriden by subclasses
	 * @return workbook ready to print via servlet output stream
	 * @throws Throwable
	 */
	public WritableWorkbook createWorkbook(GenericTransaction data, ByteArrayOutputStream buf) throws Throwable
	{
		WritableWorkbook wb = Workbook.createWorkbook(buf);
		
		wb.write();
	    wb.close(); 
	    
	    return wb;
	}
	
	/**
	 * Sets the value of the Content-Disposition response header, by default
	 * it will be [attachment; filename="data.xls";], but can be overriden
	 * @return Content-Disposition response header value
	 */
	protected String getAttachmentString()
	{
		return "attachment; filename=\"data.xls\";";
	}

}
