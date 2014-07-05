package dinamica.validators;

import java.util.Calendar;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import dinamica.AbstractValidator;
import dinamica.Recordset;
import dinamica.StringUtil;

/**
 * Validator que permite establecer un rango máximo de selección entre dos fechas.<br>
 * Se puede definir que el rango a seleccionar entre dos fechas no sea mayor a cierta cantidad en años, meses o días.<br>
 * Si el intervalo entre las dos fechas supera el intervalo especificado para este validator,
 * entonces retornará FALSE.
 * <br><br>
 * Requiere los siguientes atributos:<br>
 * <ul>
 * <li> date1: nombre del campo a considerar como fecha desde.
 * <li> date2: nombre del campo a considerar como fecha hasta.
 * <li> intervalType: tipo de intervalo, puede ser DAY, MONTH o YEAR
 * <li> interval: numero que representa la cantidad a verificar en el intervalo,
 * se interpretará como DAY, MONTH o YEAR dependiendo del valor del atributo "intervalType".
 * </ul>
 * <br>
 * Fecha de creación: 2009-04-16<br>
 * Fecha de actualización: 2009-04-16<br>
 * @author Martin Cordova y Asociados C.A
 */
public class DateIntervalValidator extends AbstractValidator {

	@Override
	public boolean isValid(HttpServletRequest req, Recordset inputParams,
			HashMap<String, String> attribs) throws Throwable {
		
		//obtener atributo del validator.xml
		String date1 = (String)attribs.get("date1");
		String date2 = (String)attribs.get("date2");
		String intervalType = (String)attribs.get("intervalType");
		String interval = (String)attribs.get("interval");
		//verificar que no este vacios
		if (date1==null || date2==null)
			throw new Throwable("Atributo invalido 'date1' or 'date2' - no puede estar vacio.");
		
		if (intervalType==null)
			throw new Throwable("Atributo invalido 'intervalType' - no puede estar vacio.");
		
		if (interval==null)
			throw new Throwable("Atributo invalido 'interval' - no puede estar vacio.");
		
		//obtener fechas del recordset del request
		String y1 = StringUtil.formatDate(inputParams.getDate(date1), "yyyy");
		String m1 = StringUtil.formatDate(inputParams.getDate(date1), "MM");
		String d1 = StringUtil.formatDate(inputParams.getDate(date1), "dd");
		String y2 = StringUtil.formatDate(inputParams.getDate(date2), "yyyy");
		String m2 = StringUtil.formatDate(inputParams.getDate(date2), "MM");
		String d2 = StringUtil.formatDate(inputParams.getDate(date2), "dd");
		
		//aplicar intervalos
		Calendar c = Calendar.getInstance();
		c.set(Integer.valueOf(y1), Integer.valueOf(m1) - 1, Integer.valueOf(d1));
				
		if(intervalType.equalsIgnoreCase("DAY"))
			c.add(Calendar.DAY_OF_WEEK, Integer.valueOf(interval));
		
		if(intervalType.equalsIgnoreCase("MONTH"))
			c.add(Calendar.MONTH, Integer.valueOf(interval));
		
		if(intervalType.equalsIgnoreCase("YEAR"))
			c.add(Calendar.YEAR, Integer.valueOf(interval));
				
		Calendar c2 = Calendar.getInstance();
		c2.set(Integer.valueOf(y2), Integer.valueOf(m2) - 1, Integer.valueOf(d2));
		
		//comparar que la fecha hasta no se excedio en el intervalo
		if (c2.after(c) == true)
			return false;
		else
			return true;

	}

}
