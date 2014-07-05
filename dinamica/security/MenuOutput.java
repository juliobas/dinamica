package dinamica.security;

import dinamica.*;

/**
 * Class description.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class MenuOutput extends GenericOutput
{

	/* (non-Javadoc)
	 * @see dinamica.GenericOutput#print(dinamica.TemplateEngine, dinamica.GenericTransaction)
	 */
	public void print(TemplateEngine te, GenericTransaction data)
		throws Throwable
	{
		
		//reuse superclass code
		super.print(te, data);
		
		//retrieve main recordset (menu titles)
		Recordset menu = data.getRecordset("menu");

		//load repeatable subtemplate
		String section = getResource("section.txt");		
		
		//buffer
		StringBuilder buf = new StringBuilder();
		
		while (menu.next())
		{

			//getmenu items
			GetMenu m = (GetMenu)data;
			Recordset items = m.getMenuItems(menu);

			//build subpage
			TemplateEngine t = new TemplateEngine(getContext(),getRequest(), section);
			t.replace(menu,"");
			t.replace(items,"","rows");
			
			//append subpage section
			buf.append(t.toString());
			
		}
		
		//replace into main template
		te.replace("${menu}", buf.toString());
		
	}

}
