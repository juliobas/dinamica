package dinamica;

/**
 * Generic Blob reader.<br>
 * This Transaction publishes a Recordset with the
 * information required by the BlobOutput module
 * to print a blob column's content, whether it is an image or
 * a document. It is meant to be a generic mechanism to send
 * blobs as response to requests without requiring special coding.<br>
 * The whole mechanism depends on a certain table structure, that at least 
 * must contain these fields: content_type, image_data and filename (required only if blob will be sent as attachment).  
 * A primary key to retrieve the blob is also required, usually an ID (integer).
 * The Action that uses this Transaction must include two query files: query-info.sql and query-blob.sql. 
 * They must have those names. Also include a custom element in config.xml: &gt;attach&lt;true&gt;/attach&lt; Set it to "true" if you want
 * the browser to open a "Save as" dialog box, otherwise it should be "false".<br>
 * You may include your own validation rules before this Transaction's execute() method
 * is invoked. The BlobOutput module will read the blob column by itself, which means that it will
 * open its own database connection using the default application datasource (context parameter in web.xml).<br>
 * The Recodset must be published with the name "blobinfo". The fields of this recordset are: 
 * format, filename (null if attach=false), size and sql.<br><br>
 * <b>NOTE:</b> A validator.xml file with the field that represents the blob ID is required. Set validator=true in config.xml.
 * 
 * <br>
 * Creation date: 6/jan/2004<br>
 * Last Update: 20/may/2004<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class GetBlob extends GenericTransaction
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		
		//reuse superclass code
		int rc = super.service(inputParams);
		
		//create recordset structure
		Recordset rs = new Recordset();
		rs.append("sql", java.sql.Types.VARCHAR);
		rs.append("filename", java.sql.Types.VARCHAR);
		rs.append("format", java.sql.Types.VARCHAR);
		rs.addNew();
		
		//get image metadata
		String sql1 = getSQL(getResource("query-info.sql"), inputParams);
		Recordset rsInfo = getDb().get(sql1);
		rsInfo.first();
		
		//create sql to retrieve blob
		String sql2 = getSQL(getResource("query-blob.sql"), inputParams); 
		
		//fill recordset fields
		rs.setValue("sql", sql2);
		
		//content-type of the blob
		rs.setValue("format", rsInfo.getValue("content_type"));

		//set filename only if attach=true
		String attach = getConfig().getConfigValue("attach");
		if (attach!=null && attach.equals("true"))
		{
			if (rsInfo.containsField("filename"))
				rs.setValue("filename", rsInfo.getValue("filename"));
			else
				throw new Throwable("Cannot attach BLOB output if [filename] column is not present in the [query-info.sql] template.");
		}

		publish("blobinfo", rs);

		return rc;

	}

}
