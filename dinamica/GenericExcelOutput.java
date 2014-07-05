package dinamica;

import java.io.ByteArrayOutputStream;
import javax.xml.xpath.XPathExpressionException;
import jxl.*;
import jxl.write.*; 
import jxl.write.Number;
import dinamica.xml.*;

/**
 * Clase generica para exportar un archivo excel con el contenido de un recordset,
 * Esta clase lee del config.xml un conjunto de elementos para imprimir las columnas y filas.
 * Se definen elementos en el config.xml como en el ejemplo:<br>
 * <xmp>
 * <excel recordset="query.sql" sheetname="Reporte" filename="data.xls" date-format="dd-MM-yyyy">
 *		<col id="col1" label="Columna 1"/>
 *		<col id="col2" label="Columna 2"/>
 *		<col id="col3" label="Columna 3"/>
 *		<col id="col4" label="Columna 4"/>
 *		<col id="col5" label="Columna 5"/>
 *		<col id="col6" label="Columna 6"/>
 *	</excel>
 *</xmp>
 * <br><br>
 * El atributo date-format es opcional, la mascara por defecto se lee de web.xml, del parametro
 * de contexto def-format-date.
 * <br><br>
 * Actualizado: 2008-07-28<br>
 * Framework Dinamica - Distribuido bajo licencia LGPL<br>
 * @author Martin Cordova y Asociados C.A.
 */

public class GenericExcelOutput extends AbstractExcelOutput
{
	
	/**
	 * Crear un workbook para exportar un recordset como un archivo excel
	 * @param data Data object passed by the Transaction object to this Output object 
	 * @return Workbook
	 * @throws Throwable
	 */
	public WritableWorkbook createWorkbook(GenericTransaction data, ByteArrayOutputStream buf) throws Throwable
	{

		String dateFmt = getContext().getInitParameter("def-format-date");
		
        //obtener referencia a config.xml
		Document doc = getConfig().getDocument();
		String customDateFmt = doc.getElement("excel").getAttribute("date-format"); 
		if (customDateFmt==null)
			customDateFmt = dateFmt;
		
		//crear el workbook
		WritableWorkbook wb = Workbook.createWorkbook(buf);
		WritableSheet sheet = wb.createSheet(doc.getElement("excel").getAttribute("sheetname"), 0);
		WritableCellFormat dateFormat = new WritableCellFormat (new DateFormat(customDateFmt));
		
	    //codigo que lee los nombres de las columnas, campos y recordset del config.xml
        Element cols[] = doc.getElements("//col");
     
        beforeData(sheet, data, 0);
        
        //obtener recordset de data
	    Recordset rs = data.getRecordset(doc.getElement("excel").getAttribute("recordset"));
	    rs.top();
	    
	    int count = sheet.getRows();
	    
	    //obtener todos los label del config.xml
	    for (int i = 0; i < cols.length; i++) {
        	Label label = new Label(i, count, cols[i].getAttribute("label")); 
        	sheet.addCell(label);
        }
    	
	    
	    //añadir la data a partir del row 1
        while (rs.next())
	    {
        	count = sheet.getRows(); 
        	
    	    for (int i = 0; i < cols.length; i++) {
    	    	
    	    	String colName = cols[i].getAttribute("id");

    	    	if(rs.isNull(colName)){
    	    		Label label = new Label( i, count, "" ); 
        			sheet.addCell(label);
    	    	}else{
					//asignar a la celda el valor segun el
					//tipo de dato que contenga
					RecordsetField rf = rs.getField(colName);
					switch (rf.getType()) {
	
						case java.sql.Types.DATE:
							DateTime date = new DateTime( i, count, rs.getDate(colName), dateFormat );
		        			sheet.addCell(date);
							break;
	
						case java.sql.Types.INTEGER:						
						case java.sql.Types.DOUBLE:
							Number number = new Number( i, count, rs.getDouble(colName) ); 
							sheet.addCell(number);
							break;
							
						default:
							Label label = new Label( i, count, rs.getString(colName) ); 
		        			sheet.addCell(label);						
							break;
					}    	    	
    	    }
    	    }
        	
	    }
        
        afterData(sheet, data, count);
        
        wb.write();
        wb.close(); 
        
        //retornar documento para su impresion hacia el browser
        return wb;
		
	}

	@Override
	protected String getAttachmentString() {
		
		String fileName = "data.xls";
		try {
			fileName = getConfig().getDocument().getElement("excel").getAttribute("filename");
		} catch (XPathExpressionException e) {
		}
		return "attachment; filename=\"" + fileName + "\";";
	}
	
	/**
	 * Metodo que permite añadir data a la hoja de calculo antes
	 * de la imprimir la data del detalle, es especial para los casos
	 * cuando se desea imprimir titulos antes de la data. 
	 * @param sheet Hoja de calculo
	 * @param data Objeto Transaction que contiene los recordsets del Action
	 * @param countReg Row donde esta posicionada la hoja de calculo
	 * @return sheet Hoja de calculo
	 * @throws Throwable
	 */
	protected WritableSheet beforeData (WritableSheet sheet, GenericTransaction data, int countReg) throws Throwable {
		return sheet;
	}
	
	/**
	 * Metodo que permite añadir data a la hoja de calculo despues
	 * de la imprimir la data del detalle, es especial para los casos
	 * cuando se desea imprimir un total. 
	 * @param sheet Hoja de calculo
	 * @param data Objeto Transaction que contiene los recordsets del Action
	 * @param countReg Row donde esta posicionada la hoja de calculo
	 * @return sheet Hoja de calculo
	 * @throws Throwable
	 */
	protected WritableSheet afterData (WritableSheet sheet, GenericTransaction data, int countReg) throws Throwable {
		return sheet;
	}
}
