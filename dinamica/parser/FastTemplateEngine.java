package dinamica.parser;

import java.util.*;


/**
 * FastTemplateEngine<br>
 * New fast parser engine, provides high performance 
 * template management. Instances of this class are serializable.
 * <br><br>
 * Creation date: 20/06/2004<br>
 * (c) 2004 Martin Cordova y Asociados<br>
 * http://www.martincordova.com/fck<br>
 * @author Martin Cordova dinamica@martincordova.com
 */
public class FastTemplateEngine implements java.io.Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** holds markers and its data values */
	private HashMap<String, String> markers = new HashMap<String, String>();
	
	/** holds static sections of the template */
	private ArrayList<String> bodyParts = new ArrayList<String>();

	/** holds dynamic sections of the template */
	private ArrayList<String> dataParts = new ArrayList<String>();

	public FastTemplateEngine()
	{
	}

	public FastTemplateEngine(String templateBody) throws Throwable
	{
		setTemplate(templateBody);
	}

	/**
	 * Set template body
	 * @param body A String that contains the template text
	 * @throws Throwable if body=null
	 */
	public void setTemplate(String body) throws Throwable
	{
		
		if (body==null)
			throw new Throwable("Invalid parameter [body]: cannot be null.");
			
		parse(body);
		
	}

	/**
	 * Set data value to be used to replace a certail marker
	 * defined in the template.
	 * @param key Marker name, must be in the form ${markerName}
	 * @param value Data value
	 * @throws Triggers exception if no template has been set with setTemplate()
	 * or if the key does not match any marker.
	 */
	public void setValue(String key, String value) throws Throwable
	{
		if (markers.containsKey(key))
			markers.put(key, value);
		else
		{
			String logMsg = "Invalid template key: " + key;
			throw new Throwable(logMsg);
		}
	}

	/**
	 * Parse template body to extract static parts
	 * and data markers; the template will be divided
	 * into several sections in order to assemble the page
	 * using a high performance technique.
	 * @param t Template body
	 * @throws Throwable
	 */
	private void parse(String t) throws Throwable
	{

		int pos = 0;
		int pos1 = 0;
		int pos2 = 0;
		int newPos = 0;
		String str = "${fld:";
		
		/* search markers */
		while ( pos >= 0 )
		{
			
			/* find start of marker */
			pos1 = t.indexOf(str, pos);
			if (pos1>=0)
			{
				
				/* find end of marker */
				newPos = pos1 + str.length();
				pos2 = t.indexOf("}", newPos);
				
				if (pos2>0)
				{
					// get marker
					String marker = t.substring(pos1, pos2 + 1);
					markers.put(marker, marker);
					dataParts.add(marker);
					
					// get body part before marker
					String part = t.substring(pos, pos1);
					bodyParts.add(part);
				}
				else
				{
					String logMsg = "Invalid template - marker not closed with '}'.";
					throw new Throwable( logMsg );
				}
				pos = pos2 + 1;
			}
			else
			{
				// get final body part
				String part = t.substring(pos);
				bodyParts.add(part);
				pos = -1;
			}
		}
		
	}

	/**
	 * Clear all markers data values, this is specially
	 * useful if you store Template objects in cache
	 * and need to reuse them with another set of data values.
	 */
	public void resetValues()
	{
		Set<String> keys = markers.keySet();
		Object k[] = keys.toArray();
		for (int i=0; i<k.length;i++)
		{
			markers.put((String)k[i], (String)k[i]);
		}

	}

	/**
	 * Returns the content of the template with all the markers
	 * replaced by the corresponding value of by an empty string of
	 * no value was defined for a certain marker.
	 * @return Template body
	 */
	public String toString()
	{

		StringBuilder sb = new StringBuilder();
		
		int markerCount = dataParts.size();
		for (int i=0; i<bodyParts.size(); i++)
		{
			sb.append(bodyParts.get(i));
			if (i<markerCount)
				sb.append(markers.get(dataParts.get(i)));
		}
		
		return sb.toString();
		
	}
	
	/**
	 * Returns a list of markers contained in a FastTemplate object
	 * @param prefix The type of marker (fld, lbl, def, inc, seq)
	 * @return ArrayList containing Marker objects
	 * @throws Throwable
	 */
	public ArrayList<Marker> getMarkers()
	{
		
		String name = "";
		String format = "";
		
		ArrayList<Marker> l = new ArrayList<Marker>();
		
		Object k[] = markers.keySet().toArray();
		for (int i=0; i<k.length;i++)
		{
			String key = (String)k[i];
			String prefix = key.substring(2,5);
		
			int pos = key.indexOf("@");
			if (pos>0)
			{
				name = key.substring(6, pos);
				format = key.substring(pos + 1, key.length()-1);
			}
			else
			{
				name = key.substring(6, key.length()-1);
				format = null;
			}
				
			Marker m = new Marker(key, prefix, name, format);
			l.add(m);

		}
		return l;
	}
	
}
