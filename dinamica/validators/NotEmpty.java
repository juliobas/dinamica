package dinamica.validators;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import dinamica.AbstractValidator;
import dinamica.Recordset;
import dinamica.StringUtil;

/**
 * Verificar que un conjunto de campos del request no sean nulos.
 * Este Validator lee el parametro [params] de su elemento custom-validator,
 * este parametro debe contener un valor como params="campo1;campo2;campo3", los
 * campos deben ser separados por ;<br><br>
 * Creacion: 2008-01-03<br>
 * @author Martin Cordova y Asociados C.A.
 *
 */
public class NotEmpty extends AbstractValidator {

	@Override
	public boolean isValid(HttpServletRequest req, Recordset inputParams,
			HashMap<String, String> attribs) throws Throwable 
	{

		boolean flag = false;
		
		//read field names
		boolean bParam = attribs.containsKey("params");

		if (!bParam)
		{
			throw new Throwable("[" + this.getClass().getName() + "] Missing attribute [params] in validator.xml");
		}
		else
		{

			//read config
			String value = (String)attribs.get("params");
			String fields[] = StringUtil.split(value, ";");
						
			//get parameter value if available
			for (int i = 0; i < fields.length; i++) {
				if (!inputParams.isNull(fields[i])) {
					flag = true;
					break;
				}
			}

		}
		
		return flag;
	
	
	}

}
