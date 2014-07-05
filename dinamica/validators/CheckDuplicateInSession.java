package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.AbstractValidator;
import dinamica.Recordset;
import dinamica.RecordsetField;

/**
 * Clase que verifica la duplicidad de un registro en un
 * recordset que se encuentra en sesion. Verifica
 * que el valor del campo en el request no se encuentre duplicado en el recordset
 * de sesion. Cuando se este actualizando el recordset de session es obligatorio especificar
 * como parametro del request "rowindex" que contiene el _rowindex del record a modificar
 * <br>
 * Los atributos que acepta este Validator son los siguientes:
 * <ul>
 * <li> colname: nombre del parametro del request que sera validado y nombre del campo del recordset en sesion
 * <li> recordset: nombre del recordset que se encuentra en memoria
 * </ul>
 * <br>
 * Fecha de creacion: 2009-07-28<br>
 * Fecha de actualiacion: 2010-04-10<br>
 * @author Martin Cordova y Asociados C.A (Francisco Galizia galiziafrancisco@gmail.com)
 */
public class CheckDuplicateInSession extends AbstractValidator {

	@Override
	public boolean isValid(HttpServletRequest req, Recordset inputParams,
			HashMap<String, String> attribs) throws Throwable {
		
		boolean flag = true;
		
		boolean b1 = attribs.containsKey("recordset");
		if (!b1)
			throw new Throwable("Atributo 'recordset' no fue encontrado.");
		
		boolean b2 = attribs.containsKey("colname");
		if (!b2)
			throw new Throwable("Atributo 'colname' no fue encontrado.");
		
		//obtener recordset de session
		Recordset rs = (Recordset)getSession().getAttribute((String)attribs.get("recordset"));
		if(rs != null) {
			rs.top();
			//obtener el tipo de dato del campo para asi poder realizar la verificacion
			RecordsetField rf = inputParams.getField((String)attribs.get("colname"));
			switch (rf.getType()) {
	
				case java.sql.Types.DATE:
				case java.sql.Types.TIMESTAMP:
					//verifica que ya exista el valor en el recordset
					if (rs.findRecord((String)attribs.get("colname"), inputParams.getDate((String)attribs.get("colname"))) != -1) {
						//si el recordset del request contiene un parametro con el nombre rowindex ignora el recordNumber
						//que corresponde al mismo registro
						if (inputParams.containsField("rowindex")) {
							if(rs.getRecordNumber() != inputParams.getInt("rowindex"))
								flag = false;
						} else
							flag = false;
					}
					break;
	
				case java.sql.Types.INTEGER:	
					//verifica que ya exista el valor en el recordset
					if (rs.findRecord((String)attribs.get("colname"), inputParams.getInteger((String)attribs.get("colname"))) != -1){
						//si el recordset del request contiene un parametro con el nombre rowindex ignora el recordNumber
						//que corresponde al mismo registro
						if (inputParams.containsField("rowindex")) {
							if(rs.getRecordNumber() != inputParams.getInt("rowindex"))
								flag = false;
						} else
							flag = false;
					}
					break;
				default:
					//verifica que ya exista el valor en el recordset
					if (rs.findRecord((String)attribs.get("colname"), inputParams.getString((String)attribs.get("colname"))) != -1) {
						//si el recordset del request contiene un parametro con el nombre rowindex ignora el recordNumber
						//que corresponde al mismo registro
						if (inputParams.containsField("rowindex")) {
							if(rs.getRecordNumber() != inputParams.getInt("rowindex"))
								flag = false;
						} else
							flag = false;
					}
					break;
			}
		}
		return flag;
	}

}
