package dinamica;

/**
 * Construye un mensaje con toda la informacion del sistema, para luego
 * ser enviada por email al destinatario configurado en el config.xml
 * Creado: 2008-08-03<br>
 * Actualizado: 2008-08-03<br>
 * Framework Dinámica - (c) 2008 Martín Córdova y Asociados C.A.<br>
 * Este código se distribuye bajo licencia LGPL<br>
 * @author Francisco Galizia galiziafrancisco@gmail.com
 */
public class SysInfoEmail extends SysInfo {

	@Override
	public int service(Recordset inputParams) throws Throwable {
		super.service(inputParams);
		return 0;
	}

	@Override
	public String getEmailBody(String template, Recordset rs) throws Throwable {

		//obtener recordsets
		Recordset rs1 = getRecordset("dbpool");
		Recordset rs2 = getRecordset("threadpool");
		Recordset rs3 = getRecordset("webappsinfo");
		Recordset rs4 = getRecordset("serverinfo");
		Recordset rs5 = getRecordset("threaddump");

		//construir mensaje
		TemplateEngine t = new TemplateEngine(getContext(),getRequest(), template);
		t.replaceDefaultValues();
		t.replaceLabels();
		t.replaceRequestAttributes();
		t.replace(rs1, "", "pool");
		t.replace(rs2, "", "thread");
		t.replace(rs3, "", "webapp");
		t.replace(rs5, "", "threaddump");
		t.replace(rs4, "");
		return t.toString();
	
	}
	
}
