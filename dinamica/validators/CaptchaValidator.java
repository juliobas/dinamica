package dinamica.validators;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.AbstractValidator;
import dinamica.Recordset;

/**
 * Modulo que valida que el texto colocado en un parámetro dado del request
 * sea el mismo generado por la imagen CAPTCHA. Los parametros que usa
 * este validator son:<br>
 * <ul>
 * <li>param: nombre del parámetro del request que contiene el valor a comparar con el captcha generado
 * <li>ignoreCase: si es "true" entonces la comparación no será sensible al uso de mayúsculas o minúsculas
 * </ul>
 * El String que representa los caracteres alfanuméricos generados por el captcha debe estar
 * almacenados en el atributo de sesión con el siguiente id: "dinamica.security.captcha"<br><br>
 * Fecha de actualizacion: 2009-05-25<br>
 * Fecha de creacion: 2008-11-19<br>
 * @author Martin Cordova y Asociados C.A
 */
public class CaptchaValidator extends AbstractValidator {

	String errorMsg = null;
	
	@Override
	public boolean isValid(HttpServletRequest req, Recordset inputParams,
			HashMap<String, String> attribs) throws Throwable 
	{
		boolean flag = false;
		
		String param = (String)attribs.get("param");
		String ignoreCase = (String)attribs.get("ignoreCase");
		
		if (param==null)
			throw new Throwable("Invalid attribute 'param' - cannot be null.");		
		
		String text = inputParams.getString(param);
		String generatedWord = (String)getSession().getAttribute("dinamica.security.captcha");
		
		if (generatedWord==null || generatedWord.equals("") ) {
			errorMsg = "La imagen expiró y ya no es válida. Por favor refresque la imagen de validación.";
			
		} else {
			if (ignoreCase!=null && ignoreCase.equalsIgnoreCase("true")) {
				if (generatedWord.equalsIgnoreCase(text))
					flag = true;			
			} else {
				if (generatedWord.equals(text))
					flag = true;
			}
		}
		return flag;
	}

	@Override
	public String getErrorMessage() {
		return errorMsg;
	}

}
