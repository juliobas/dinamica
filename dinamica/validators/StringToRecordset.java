package dinamica.validators;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import dinamica.AbstractValidator;
import dinamica.Recordset;
import dinamica.StringUtil;

/**
 * Transforma un String que contiene una serie de valores separados por ";" o 
 * algo similar -se indica en el validator.xml- en un Recordset de un solo campo
 * con el tipo de dato indicado. Toda la parametrizacion se realiza mediante atributos
 * del custom-validator. El Recordset resultante será almacenado en un atributo del
 * request con el ID indicado.<br>
 * En este caso el Validator funge como un transformador de un valor mas que como un validador,
 * por ese motivo siempre retorna true. Si el campo no contiene valores entonces el recordset
 * estara vacio.
 * <br><br>
 * Los atributos que requiere son:<br>
 * <ul>
 * <li> separatorChar: Caracter de separacion entre los valores.<br>
 * <li> recordsetId: ID del atributo en el objeto HttpRequest.<br>
 * <li> colName: Nombre del unico campo que contendra el Recordset.<br>
 * <li> colType: Tipo de dato, soporta solamente varchar o integer.<br>
 * <li> paramName: Nombre del parámetro del request que contiene los valores. 
 * 
 * Creacion: 2009-04-09<br>
 * @author Martin Cordova y Asociados C.A.
 *
 */
public class StringToRecordset extends AbstractValidator {

	@Override
	public boolean isValid(HttpServletRequest req, Recordset inputParams,
			HashMap<String, String> attribs) throws Throwable 
	{
		//valor que retorna por defecto
		boolean flag = true;
		
		//verificar configuracion
		String configParams[] = {"separatorChar","recordsetId","colName","colType","paramName"};
		for (int i = 0; i < configParams.length; i++) {
			if (!attribs.containsKey(configParams[i]))
				throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [" + configParams[i] + "] in validator.xml");
		}

		//leer configuracion
		String recordsetId = (String)attribs.get("recordsetId");
		String separatorChar = (String)attribs.get("separatorChar");
		String colName = (String)attribs.get("colName");
		String colType = (String)attribs.get("colType");
		String paramName = (String)attribs.get("paramName");
		
		//validar configuracion
		if (!colType.equals("varchar") && !colType.equals("integer"))
			throw new Throwable("[" + this.getClass().getName() + "] Invalid value for parameter [coltype] in validator.xml: " + colType + " . Only varchar or integer are supported.");
		
		//leer valor del campo multivaluado y transformarlo en un array de valores
		String values[] = StringUtil.split(inputParams.getString(paramName), separatorChar);
		
		//crear el recordset
		int type = java.sql.Types.INTEGER;
		if (colType.equals("varchar"))
			type = java.sql.Types.VARCHAR;
		Recordset rs = getRsFromArray(values, colName, type);
		
		//almacenarlo en el request
		getRequest().setAttribute(recordsetId, rs);
		
		return flag;
	}

}
