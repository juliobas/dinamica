package dinamica;

/**
 * Generic Transaction used for server-side Chart Actions.
 * This Transaction class will read some specific elements from
 * config.xml in order to set the values of the chart configuration
 * recordset used by the ChartOutput class. This Transaction will save
 * you the effort of writing a class for the sole purpose of configuring
 * a chart, instead you will use config.xml to provide chart properties.
 * <br><br>
 * (c) 2004-2008 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class GenericChart extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		
		//reuse superclass code
		super.service(inputParams);
		
		//get chart config recordset
		Recordset rs = getChartInfoRecordset();
		
		fillChartConfig(rs);

		/* publish recordset to be consumed by output module */
		publish("chartinfo", rs);
		
		//return OK
		return 0;
		
	}

	/**
	 * Lee la configuracion basica del plugin de charts, este metodo
	 * puede ser llamado por subclases para que lea la configuracion y luego
	 * las subclases leen parametros adicionales que ellas mismas definieron 
	 * al hacer un override del metodo getChartInfoRecordset().<br>
	 * <br>PATCH 2008-03-06 mejorar la extensibilidad del framework para soportar
	 * nuevos parametros de configuracion de charts no previstos por el framework.
	 * @param rs Recordset retornado por el metodo getChartInfoRecordset() de la clase GenericTransaction
	 * @throws Throwable
	 */
	public void fillChartConfig(Recordset rs) throws Throwable
	{

		
		String dateFormat = getConfig().getConfigValue("//chart/dateformat", "dd-MM-yyyy");
		if (dateFormat==null || dateFormat.equals("")) {
			dateFormat = getContext().getInitParameter("def-format-date");
		}
		
		//read chart properties from config.xml
	  	String session = getConfig().getConfigValue("//chart/session", null);		
		String imageid = getConfig().getConfigValue("//chart/image-id", null);
		
		//patch 20100514 permite colocar en el request el tipo de grafico que sera mostrado
		String plugin = getRequest().getParameter("plugin");
		if (plugin==null)
			plugin = getConfig().getConfigValue("//chart/plugin");
		
		Integer width = new Integer(getConfig().getConfigValue("//chart/width"));
		Integer height = new Integer(getConfig().getConfigValue("//chart/height"));
		String title = getConfig().getConfigValue("//chart/title");
		String titlex = getConfig().getConfigValue("//chart/title-x");
		String titley = getConfig().getConfigValue("//chart/title-y");
		String series = getConfig().getConfigValue("//chart/title-series");
		String recordset = getConfig().getConfigValue("//chart/recordset");
		String fieldx = getConfig().getConfigValue("//chart/field-x");
		String fieldy = getConfig().getConfigValue("//chart/field-y");
		String color = getConfig().getConfigValue("//chart/color", null);
		String labelXFormat = getConfig().getConfigValue("//chart/labelx-format", null);
		String tickUnit = getConfig().getConfigValue("//chart/tick-unit", null);
		
		//set chart properties using a recordset
		rs.addNew();
		rs.setValue("dateformat", dateFormat); //in pixels
		rs.setValue("title", title);
		rs.setValue("title-x", titlex); //irrelevant for pie charts
		rs.setValue("title-y", titley); //irrelevant for pie charts
		rs.setValue("column-x", fieldx); 
		rs.setValue("column-y", fieldy); //if multiseries then type multiple column names separated by ";"
		rs.setValue("title-series", series); //no series - otherwise set label names separated by ";"
		rs.setValue("width", width); //in pixels
		rs.setValue("height", height); //in pixels
		rs.setValue("data", recordset); //recordset that must be stored in session attribute
		
		/* select the type of chart you want to use */
		rs.setValue("chart-plugin", plugin);
		
		//added on april-6-2004 - persist image bytes in session attribute
		//to be reused by PDF generators
		rs.setValue("session", session);
		rs.setValue("image-id", imageid);

		//added on july-19-2005 - default color for charts
		//with 1 series only (applies to bar, line and area only)
		rs.setValue("color", color);
		
		rs.setValue("labelx-format", labelXFormat);
		if (tickUnit!=null)
			rs.setValue("tick-unit", Integer.valueOf(tickUnit));
		
	}
	
}
