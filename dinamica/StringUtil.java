package dinamica;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.net.*;


/**
 * Core-level framework class: String and Date basic utility methods.
 * <br><br>
 * Encapsulates utility methods for everyday programming tasks
 * with Strings, Dates and other common stuff.
 * <br>
 * Creation date: 18/09/2003<br>
 * Last Update: 18/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (some code written by Carlos Pineda)
 */
public class StringUtil
{

	/**
	 * Replace ALL occurrences of [old value] with [new value]<br>
	 * This method was written by Carlos Pineda.
	 * @param source String to manipulate
	 * @param pattern Old value
	 * @param newText New value
	 * @return String with replaced text
	 */
	public static String replace(String source, String pattern, String newText)
	{

		int len = pattern.length();
		if (len == 0 )
			return source;

		StringBuilder buf = new StringBuilder( 2 * source.length() );

		int previndex=0;
		int index=0;
		while (true) {
			index = source.indexOf(pattern, previndex);
			if (index == -1) {
				buf.append(source.substring(previndex));
				break;
			}
			buf.append( source.substring(previndex, index)).append( newText );
			previndex = index + len;
		}
		return buf.toString();
		
	}
	
	/**
	 * Format date using a mask and the default locale
	 * @param d Date object
	 * @param format Date mask, like yyyy-MM-dd or any valid java format
	 * @return String representing the formatted string
	 * @throws Throwable
	 */
	public static String formatDate(java.util.Date d, String format) throws Throwable
	{
		SimpleDateFormat f = new SimpleDateFormat();
		f.applyPattern(format);
		return f.format(d);
	}
	
	/**
	 * Format date using a mask and locale
	 * @param d Date object
	 * @param format Date mask, like yyyy-MM-dd or any valid java format
	 * @param loc Custom Locale
	 * @return String representing the formatted string
	 * @throws Throwable
	 */
	public static String formatDate(java.util.Date d, String format, Locale loc) throws Throwable
	{
		SimpleDateFormat f = new SimpleDateFormat(format, loc);
		return f.format(d);
	}	

	/**
	 * Create a java.util.Date object from a String value and a format mask.<br>
	 * The java date formats are supported, for more information please consult the
	 * reference guide for the class <a href="http://java.sun.com/j2se/1.4.1/docs/api/java/text/SimpleDateFormat.html">SimpleDateFormat</a>.<br>
	 * <br>
	 * Sample code:<br>
	 * <pre>
	 * java.util.Date d = StringUtil.getDateObject("2003-12-07 17:00:00","yyyy-MM-dd HH:mm:ss");
	 * </pre>  
	 * @param dateValue A String containg a valid date corresponding to dateFormat mask
	 * @param dateFormat The date format used to represent the date in dateValue
	 * @return A java.util.Date object representing the dateValue parameter
	 * @throws Throwable if dateValue is not represented in dateFormat
	 */
	public static java.util.Date getDateObject(String dateValue, String dateFormat) throws Throwable
	{
		SimpleDateFormat x = new SimpleDateFormat(dateFormat);
		x.setLenient(false);
		return x.parse(dateValue);
	}

	/**
	 * Format a number using a valid Java format mask and the default Locale
	 * @param value Double, Integer or another numeric value
	 * @param numberFormat Java numeric format mask like #,##0.00
	 * @return String representing a formatted number acording to the numberFormat
	 * @throws Throwable
	 */
	public static String formatNumber(Object value, String numberFormat) throws Throwable
	{
		DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance();
		fmt.applyPattern(numberFormat);	
		return fmt.format(value);
	}

	/**
	 * Format a number using a valid Java format mask and a custom Locale
	 * @param value Double, Integer or another numeric value
	 * @param numberFormat Java numeric format mask like #,##0.00
	 * @param loc Custom Locale to use when formatting the number
	 * @return String representing a formatted number acording to the numberFormat
	 * @throws Throwable
	 */
	public static String formatNumber(Object value, String numberFormat, Locale loc) throws Throwable
	{
		DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance(loc);
		fmt.applyPattern(numberFormat);	
		return fmt.format(value);
	}

	/**
	 * Create an array of items from a string with delimiters to separate the items.
	 * This is a very simple wrapper around the native String.split method. If the passed
	 * string is empty ("") or is null then an empty array of length = 0 will be returned.
	 * Also note that an empty value after the last separator will be ignored.
	 * @param s String to split or separate in its parts
	 * @param separator Delimiter string, like a pipe or a tabulator
	 * @return Array of strings containing the separated items
	 */
	public static String[] split(String s, String separator) 
	{
		//patch 2009-04-09 - retornaba un array con longitud = 1 aunque se le pasara un string vacio
		if (s==null || s.equals("")) {
			String x[] = new String[0];
			return x;
		}
		separator = "\\" + separator;
		return s.split(separator);
	}

	/**
	 * Loads a text resource stored into the Web Application context paths
	 * @param path Path to the resource
	 * @return String containing the resource contents
	 * @throws Exception
	 */
	public static String getResource(javax.servlet.ServletContext ctx, String path) throws Throwable
	{
		return getResource(ctx, path, System.getProperty("file.encoding", "ISO8859_1"));
	}

	/**
	 * Append message to file, this method is usually 
	 * used to save log messages
	 * @param path File name
	 * @param message String to append to file
	 */
	public static synchronized void saveMessage(String path, String message) 
	{

		FileOutputStream fos = null;
		PrintWriter pw = null;

		try {
			fos = new FileOutputStream(new File(path), true);
			pw = new PrintWriter(fos, false);
			pw.println(message);
		}
		catch (IOException e) {
			
			try
			{
				String d = StringUtil.formatDate(new java.util.Date(), "yyyy-MM-dd HH:mm:ss");
				System.err.println("ERROR [dinamica.StringUtil.saveMessage@" + d + "]: " + e.getMessage());
			}
			catch (Throwable e1)
			{
			}
		}
		finally {
			
			try {
				if (pw != null)
					pw.close();
				if (fos != null)
					fos.close();
			}
			catch (IOException e) {
			}
			
		}
	}

	/**
	 * Retrieve a text-based document using HTTP GET method.<br>
	 * May be used to retrieve XML documents, news feeds, etc.
	 * @param url A valid URL
	 * @param logStdout if TRUE then this method will print
	 * a tracelog via STDOUT
	 * @return a String containing the whole document
	 * @throws Throwable
	 */
	public static String httpGet(String url, boolean logStdout) throws Throwable
	{

		final int bufferSize = 4096;
		BufferedReader br = null;
		HttpURLConnection urlc = null;
		StringBuilder buffer = new StringBuilder();
		URL page = new URL(url); 
    		
		try
		{
			
			if (logStdout)
				System.err.println("Waiting for reply...:" + url);
			
			urlc = (HttpURLConnection)page.openConnection();          
			urlc.setUseCaches(false);
			
			if (logStdout)
			{
				System.err.println("Content-type = " + urlc.getContentType()); 
				System.err.println("Content-length = " + urlc.getContentLength()); 
				System.err.println("Response-code = " + urlc.getResponseCode());
				System.err.println("Response-message = " + urlc.getResponseMessage());
			}
			
			int retCode = urlc.getResponseCode();
			String retMsg = urlc.getResponseMessage();
			if (retCode>=400)
				throw new Throwable("HTTP Error: " + retCode + " - " + retMsg + " - URL:" + url);
																								   
			br = new BufferedReader(new InputStreamReader(urlc.getInputStream()), bufferSize);
			char buf[] = new char[bufferSize];
			int bytesRead = 0;
			
			while (bytesRead!=-1) 
			{
				bytesRead = br.read(buf);
				if (bytesRead>0)
					buffer.append(buf,0,bytesRead);
			}
			
			if (logStdout)
			{
				System.err.println("Document received.");
			}
			
			return buffer.toString();
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (br != null)
				br.close();
			
			if (urlc!=null)
				urlc.disconnect();				
		}

	}

	/**
	 * Loads a text resource stored into the Web Application context paths
	 * <br>
	 * PATCH 2005-02-17 (v2.0.3) - encoding support
	 * @param ctx Servlet context 
	 * @param path Path to the resource
	 * @param encoding Canonical name of the encoding to be used to read the resource
	 * @return String containing the resource contents
	 * @throws Exception
	 */
	public static String getResource(javax.servlet.ServletContext ctx, String path, String encoding) throws Throwable
	{
		
		StringBuilder buf = new StringBuilder(5000);
		byte[] data = new byte[5000];

		InputStream in = null;
		
		in = ctx.getResourceAsStream(path);
        
		try
		{
			if (in!=null)
			{
				while (true)
				{
					int len = in.read(data);
					if (len!=-1)
					{
						buf.append( new String(data, 0, len, encoding) );
					}
					else
					{
						break;
					}
				}

				return buf.toString();

			}
			else
			{
				throw new Throwable("Invalid path to resource: " + path);
			}
            
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
			if (in!=null)
			{
				try{
					in.close();
				} catch (Exception e){}
			}
		}
        
	}

	/**
	 * Rounds a double to a given number of decimals.<br>
	 * Example: double x = StringUtil.round(100.0500023, 4);<br>
	 * yields: x = 100.0500<nr>
	 * @param n Number
	 * @param decimals Number of decimals to use in the trim operation
	 * @return A double with trimmed decimals.
	 * @throws Throwable
	 */
	public static double round(double n, int decimals) throws Throwable {
		
		String dec = "";
		for (int i = 0; i < decimals; i++) {
			dec = dec + "0";
		}
		String num = dinamica.StringUtil.formatNumber(new Double(n), "#." + dec);
		num = dinamica.StringUtil.replace(num, ",", ".");
		return Double.parseDouble( num );
	}	
	
	/**
	 * Genera un recordset con una sola columna, que contiene tantos
	 * registros como dias haya entre las fechas d1 y d2, incluyendolas,
	 * se utiliza para resolver queries como... "que dias le faltan por entregar
	 * recaudos a X cliente", etc. Este recordset se puede usar para alimentar
	 * una tabla temporal en la BD y luego usarla en un left/right join con otra tabla de negocios
	 * para saber que dias faltan. Es solo uno de los usos que puede tener.
	 * @param colName Nombre de la columna que contendra la fecha
	 * @param d1 Fecha desde (debe ser menor a d2)
	 * @param d2 Fecha hasta
	 * @return El recordset, con un solo campo cuyo nombre es el contenido de la variable colName
	 * @throws Throwable
	 */
	public static Recordset getDays(String colName, Date d1, Date d2) throws Throwable
	{
		if (d2.compareTo(d1)<0) 
			throw new Throwable("La fecha [D1] debe ser menor que [D2]");
		
		Calendar c = Calendar.getInstance();
		c.setTime(d1);

		Recordset rs = new Recordset();
		rs.append(colName, java.sql.Types.DATE);
		
		//generar todos los registros desde d1 hasta d2
		while (c.getTime().compareTo(d2)<=0) {
			rs.addNew();
			rs.setValue(colName, c.getTime());
			c.add(Calendar.DATE, 1);
		}
	
		return rs;
	}	
	
	/**
	 * Retorna un String conteniendo la fecha y hora en formato ANSI, es decir: yyyy-MM-dd HH:mm:ss
	 * Es util para enriquecer mensajes de error, etc.
	 * @return Fecha formateada en formato yyyy-MM-dd HH:mm:ss
	 */
	public static String getTimeStamp() {
		
		try {
			java.util.Date d = new Date();
			return formatDate(d,"yyyy-MM-dd HH:mm:ss");
		} catch (Throwable e) {
			return "";
		}
		
	}
	
}
