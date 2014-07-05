package dinamica;

/**
 * Prints recordset in tabular format using
 * horizontal direction, meaning that each row
 * from the recordset will fill a cell until
 * the maximum nimber of cells is reached, then
 * a new row will be appended to the table. It may
 * be used to print products catalog with pictures
 * and things like that.<br><br>
 * The Actions using this class are supposed to be INCLUDED
 * by other Actions, using the include marker ${inc:/action/xyz?param=value},
 * it was not designed to produce a complete page, just a part of it,
 * a grid with horizontal data direction, to be more specific. Actions
 * using this Output class must define the following elements in config.xml,
 * under the root element:<br><br>
 * <xmp>
 * <row-template>row.htm</row-template>
 * <col-template>col.htm</col-template>
 * <cols>3</cols>
 * <hgrid-recordset>myRecordset.sql</hgrid-recordset>
 * </xmp>
 * <br><br>
 * Of course you can adjust these elements's values to 
 * meet your requirements.
 * <br><br>
 * Most of the time, the Action using this Output class
 * will require only one recordset, which must be created
 * by the parent Action (the one that includes the others)
 * and saved into a request attribute, which means that this
 * class must retrieve the recordset from the request
 * object.
 * <br><br>
 * This class requires a basic template (defined in the output block in config.xml), 
 * like any other text-based output that inherits from dinamica.GenericOutput, 
 * invokes super.print() before proceeding to create its own output. This template
 * should contain the surrounding HTML code for the table, and a dummy marker
 * named ${hgrid} where the actual generated HGrid will be printed.
 * <br><br>
 * Creation date: 09/07/2005
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class HGridOutput extends GenericOutput
{

    public void print(TemplateEngine te, GenericTransaction data)
            throws Throwable
    {
        
        //read config values
        String rsName = getConfig().getConfigValue("hgrid-recordset");
        int cols = Integer.parseInt(getConfig().getConfigValue("cols"));
        Recordset rs = (Recordset)getRequest().getAttribute(rsName);
        String tRow  = getResource(getConfig().getConfigValue("row-template"));
        String tCol  = getResource(getConfig().getConfigValue("col-template"));
        
        //navigate recordset and create String containing
        //hgrid body
        StringBuffer hgrid = new StringBuffer();
        rs.top();
        for (int k=0;k<rs.getRecordCount();k=k+cols)
        {
            StringBuilder colsBuf = new StringBuilder();
	        for (int i=0;i<cols;i++)
	        {
	            if (k+i==rs.getRecordCount()) {
	            	for (int j=i;j<cols;j++) {
	            		colsBuf.append("<td></td>");
	            	}
	                break;
	            }
	            rs.setRecordNumber(k+i);
                TemplateEngine t = new TemplateEngine(getContext(),getRequest(),tCol);
                t.replace(rs, "&nbsp;");
                colsBuf.append(t.toString());
	        }
            hgrid.append(StringUtil.replace(tRow,"${cols}", colsBuf.toString()));
        }
        
        //replace hgrid body into main template
        te.replace("${hgrid}", hgrid.toString());

        // let parent class execute normal processing of config.xml
        super.print(te, data);        
        
    }
}
