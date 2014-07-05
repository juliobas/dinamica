package dinamica;

/**
 * Utility class used to represent a segment of a template.<br>
 * This class is used by TemplateEngine (method getSegments) to split a template
 * into data segments (printable) and include-directives, so that
 * servlet includes can be processed very easely 
 * <br>
 * Creation date: 2/10/2003<br>
 * Last Update: 2/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class TemplateSegment
{

	/**
	 * May be "inc" for includes or "data" for data
	 */
	public String segmentType = null;
	
	/**
	 * May be segment data (printable text) or an include path
	 * to invoke a servlet using a request dispatcher
	 */
	public String segmentData = null;
	
}
