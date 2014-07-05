package dinamica;

import dinamica.xml.*; 

/**
 * Retrieves an XML document (RSS feed) and parse the elements
 * to produce a Recordset.<br>
 * This class can read RSS feeds given a URL passed to it as a configuration element named "url" in config.xml
 * or as a request parameter named "url", and then publishes a Recordset named "rss_feed"
 * that can be printed into the template body as any other recordset. The request parameter
 * takes precedence over the config.xml element.<br>
 * This module is suitable to build Portals that aggregate news from
 * multiple sources, it can be used by Actions that represent "parts" or portlets
 * to be INCLUDED by a main coordinator Action (the portal page).<br>
 * The published recordset will contain the following columns:<br>
 * <ul>
 * <li>title
 * <li>description
 * <li>link
 * <li>pubDate
 * </ul>
 * <br><br>
 * Creation date: 10/03/2004<br>
 * Last Update: 15/02/2005<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 * @throws Throwable If it can retrieve the RSS document or if the "url" was not defined
 * */
public class RSSConsumer extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		
		//reuse superclass code
		int rc = super.service(inputParams);
		
		//define recordset structure (according to RSS standard)
		Recordset rs = new Recordset();
		rs.append("title", java.sql.Types.VARCHAR); 
		rs.append("description", java.sql.Types.VARCHAR);
		rs.append("link", java.sql.Types.VARCHAR);
		rs.append("pubDate", java.sql.Types.VARCHAR);
		
		//get url to load
		String url = getRequest().getParameter("url");
		if (url==null || url.trim().equals(""))
			url = getConfig().getConfigValue("url");
		
		//retrieve XML document via HTTP
		String data = StringUtil.httpGet(url, false);
		
		//parse and navigate XML document
		Document doc = new Document(data);
		Element items[] = doc.getElements("//item");
		for (int i = 0; i < items.length; i++) 
		{
			//add new entry
			rs.addNew();
			rs.setValue("title", doc.getElement(items[i], "title").getValue());
			rs.setValue("description", doc.getElement(items[i], "description").getValue());
			rs.setValue("link", doc.getElement(items[i], "link").getValue());
			rs.setValue("pubDate", doc.getElement(items[i], "pubDate").getValue());
		}
		
		//publish recordset
		publish("rss_feed", rs);
		
		return rc;
		
	}

}
