package dinamica;

/**
 * Base class to create Output classes. This kind of classes
 * represent the "view" part of this framework MVC mechanism.
 * This class consumes recordsets published by transactions, which
 * have been previously executed by the controller.<br>
 * <br>
 * Creation date: 4/10/2003<br>
 * Last Update: 4/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class GenericOutput 
	extends AbstractModule
	implements IRowEvent
{

	/* flag used to alternate colors (method onNewRow) */
	private int rowColor = 0;

	/**
	 * Generate text-based output using automatic data binding.<br>
	 * This class will consume Recordsets from the "data" object
	 * passed to this method, which is a Transaction that has been
	 * executed before calling the method.<br>
	 * This methods performs the data binding between the templates
	 * and the recordsets, according to the parameters defined in the
	 * config.xml file - it is a kind of VB DataControl, if you will!
	 * All its functionality relies on the TemplateEngine class.
     * Since this method performs a lot of work, all descendants of this class that
     * want to reimplement this method, should call super.print(TemplateEngine, Transaction) in the first line
     * of the reimplemented method in order to reuse all this functionality.
	 * @param te TemplateEngine containing the text based template ready for data binding
	 * @param data Transaction object that publishes one or more recordsets
	 * @throws Throwable
	 */
	public void print(TemplateEngine te, GenericTransaction data) throws Throwable
	{
		
		/* for each print command */
		if (data!=null)
		{
			Recordset rs = _config.getPrintCommands();
			
			while (rs.next())
			{
				
				String nullExpr = null;
				if (_config.contentType.equals("text/html"))
					nullExpr = "&nbsp;";
				else
					nullExpr = "";
					
				// patch 2004-08-31 - set pagesize via request parameter (optional)
				String pagesize = getRequest().getParameter("pagesize");
				if (pagesize!=null && pagesize.trim().equals(""))
					pagesize=null;
				
				if (pagesize==null)
					pagesize = rs.getString("pagesize");
				// end patch
					
				String mode = (String)rs.getValue("mode");
				String tag = (String)rs.getValue("tag");
				String control = (String)rs.getValue("control");
				String rsname = (String)rs.getValue("recordset");
				String altColors = (String)rs.getValue("alternate-colors");
				String nullValue = (String)rs.getValue("null-value");
				
				if (nullValue!=null)
					nullExpr = nullValue;
					
				if (mode.equals("table"))
				{
					Recordset x = data.getRecordset(rsname);
					
					//patch 2007-06-19 ajax paging support
					if (x.getPageCount()>0)
						pagesize = String.valueOf(x.getPageSize());
						
					if (pagesize!=null)
					{
						int page = 0;
						
						// patch 2004-08-31 - mantain original pagesize if available 
						if (x.getPageCount()==0)
							x.setPageSize(Integer.parseInt(pagesize));
						// end patch
			
						String pageNumber = getRequest().getParameter("pagenumber");
						if (pageNumber==null || pageNumber.equals(""))
						{
							page = x.getPageNumber();
							if (page==0)
								page = 1;
						}
						else
						{
							page = Integer.parseInt(pageNumber);
						}
						x = x.getPage(page);
					}

					if (altColors!=null && altColors.equals("true"))
						te.setRowEventObject(this);
						
					te.replace(x,nullExpr,tag);
				}
				else if (mode.equals("form"))
				{
					nullExpr = "";
					Recordset x = data.getRecordset(rsname);
					if (x.getRecordCount()>0 && x.getRecordNumber()<0)
						x.first(); 
						
					//PATCH 2005-03-01 - enhance error message if recordset is empty
					if (x.getRecordCount()==0)
						throw new Throwable("Recordset [" + rsname + "] has no records; can't print (mode=form) using an empty Recordset.");
						
					te.replace(x, nullExpr);
				}
				else if (mode.equals("combo"))
				{
					if (control==null)
						throw new Throwable("'control' attribute cannot be null when print-mode='combo'");
					Recordset x = data.getRecordset(rsname);
					if (x.getRecordCount()>1)
						te.setComboValue(control,x);
					else {
						if (x.getRecordCount()>0 && x.getRecordNumber()<0)
							x.first();
						if (x.getRecordCount()>0)
							te.setComboValue(control,x.getString(control));
						else
							throw new Throwable("Recordset [" + rsname + "] has no records; can't print (mode=combo) using an empty Recordset.");
					}
				}
				else if (mode.equals("checkbox"))
				{
					if (control==null)
						throw new Throwable("'control' attribute cannot be null when print-mode='checkbox'");
					Recordset x = data.getRecordset(rsname);
					te.setCheckbox(control,x);
				}
				else if (mode.equals("radio"))
				{
					if (control==null)
						throw new Throwable("'control' attribute cannot be null when print-mode='radio'");
					Recordset x = data.getRecordset(rsname);
					if (x.getRecordCount()>0 && x.getRecordNumber()<0)
						x.first();
					te.setRadioButton(control, String.valueOf(x.getValue(control)));
				}
				else if (mode.equals("clear"))
				{
					te.clearFieldMarkers();
				}
				else
				{
					throw new Throwable("Invalid print mode [" + mode + "] attribute in config.xml: " + _config.path);
				}
				
			}
			
		}
		
	}
	
	/**
	 * This method is called for non text based output (images, binaries, etc.).
	 * Reimplementations of this method MUST write the output
	 * thru the Servlet OutputStream.
	 * @param data Transaction object
	 * @throws Throwable
	 */
	public void print(GenericTransaction data) throws Throwable
	{
	}

	/**
	 * Implementation of the interface dinamica.IRowEvent.<br> 
	 * This code is used to alternate row colors,
	 * the row template must include the special
	 * field marker ${fld:_rowStyle} which will be replaced
	 * by the style parameters set in web.xml. 	 
	 * @see dinamica.IRowEvent#onNewRow(dinamica.Recordset, int, java.lang.String)
	 */
	public String onNewRow(Recordset rs, String rowTemplate) throws Throwable
	{

		/*
		 * This code is used to alternate row colors,
		 * the row template must include the special
		 * field marker ${fld:_rowStyle} which will be replaced
		 * by the style parameters set in web.xml. 
		 */
		
		String style1 = getContext().getInitParameter("def-color1");
		String style2 = getContext().getInitParameter("def-color2");
		String currentStyle="";
		
		if (rowColor==0)
		{
			rowColor=1;
			currentStyle = style1;
		}
		else
		{
			rowColor=0;
			currentStyle = style2;
		}
		
		rowTemplate = StringUtil.replace(rowTemplate, "${fld:_rowStyle}", currentStyle);
		
		return rowTemplate;

	}

	/**
	 * resets value of private variable rowColor. This
	 * variable control alternate printing of colors
	 * for tables (print mode="table")
	 *
	 */
	protected void resetRowColor()
	{
	    this.rowColor = 0;
	}


	
}
