package dinamica;

import dinamica.xml.*;

/**
 * DESCONTINUADO - VER LA NUEVA PLANTILLA MasterDetail Y SU DOCUMENTO RESPECTIVO.<BR>
 * Generic output module to print master/detail html reports.<br>
 * It will require a published recordset called "group-master.sql" 
 * and will consume a Transaction that is a subclass of MasterDetailReader. 
 * It can be an instance of this class or an instance of a subclass. If your
 * master-detail scenario is rather simple, then this generic Transaction class
 * will be enough to create your report.
 * <br><br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com
 * @author Martin Cordova (dinamica@martincordova.com)
 * */
public class MasterDetailOutput extends GenericOutput
{

    /** reference to the master recordset */
    private Recordset _master = null;
    
    /**
     * Makes available the master recordset to subclasses
     * of this class, because it may be useful to have it when
     * overriding the onNewRow(...) method, which does not
     * receive a reference to the master recordset.
     * @return
     */
    protected Recordset getMaster()
    {
        return _master;
    }
    
	/** 
	 * Subclass GenericOutput to provide the same features
	 * (all the PRINT commands, etc) and generates the
	 * master/detail section of the template with one level 
	 * of grouping. 
	 */
	public void print(TemplateEngine te, GenericTransaction data)
		throws Throwable
	{
		
		//reuse superclass code
		super.print(te, data);
		
		//retrieve master recordset
		Recordset master = data.getRecordset("master");
		_master = master;
		
		//load repeatable subtemplate
		Element gt = getConfig().getDocument().getElement("group-template");
		if (gt==null)
		    throw new Throwable("Element <group-template> not found in config.xml. This element is required by the class dinamica.MasterDetailOutput!");
		
		//alternate colors?
		IRowEvent event = null;
		String altColors = gt.getAttribute("alternate-colors");
		if (altColors!=null && altColors.equalsIgnoreCase("true"))
			event = this;
		
		String nullExpr = gt.getAttribute("null-value");
		if (nullExpr==null)
			nullExpr = "&nbsp;";
		
		//set group template
		String section = getGroupTemplate(gt);		
			
		//buffer
		StringBuilder buf = new StringBuilder();

		/* use custom locale if available */
		java.util.Locale l = (java.util.Locale)getSession().getAttribute("dinamica.user.locale");
		
		//for every master record build a master/detail section
		master.top();
		while (master.next())
		{

			//get detail rows
			MasterDetailReader m = (MasterDetailReader)data;
			Recordset items = m.getDetail(master);
			
			//build subpage
			this.resetRowColor();
			TemplateEngine t = new TemplateEngine(getContext(),getRequest(), section);
			t.setRowEventObject(event);
			t.setLocale(l);
			
			//print items count if required
			t.replace("${fld:detail.recordcount}", String.valueOf(items.getRecordCount())); 
			
			//replace detail rows
			t.replace(items, nullExpr, "rows"); 

			//replace master record columns
			t.replace(master,"&nbsp;"); 
			
			//append subpage section
			buf.append(t.toString());
			
		}
		
		//replace into main template
		te.replace("${group}", buf.toString());
		
	}

	/**
	 * Load repeatable subtemplate
	 * @return repeatable subtemplate body
	 * @throws Throwable
	 */
	protected String getGroupTemplate(Element gt) throws Throwable
	{
	
		return getResource(gt.getString());
		
	}
	
}
