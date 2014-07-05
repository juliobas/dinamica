package dinamica;

import java.io.*;

/**
 * Generic transaction class that saves uploaded file 
 * into blob column in database table via JDBC prepared statements.
 * The SQL will be preprocessed to set all 
 * the required static values, the BLOB data will
 * be sent to the server using a prepared statement.<br>
 * Plase consult the Dinamica BLOB How-to (cat-blob.pdf)
 * to learn about required table structure and available
 * blob management facilities. This class assumes that you are
 * using the generic Upload component provided with Dinamica
 * since v2.0.9 (look into /dinamica/extra folder).<br>
 * NOTE: the file control in the HTML form must be named "file",
 * future versions of this class may use config.xml to read the
 * name of the parameter. 
 * 
 * @author Martin Cordova
 * */
public class SaveBlob extends GenericTableManager
{

	/* (non-Javadoc)
	 * @see dinamica.GenericTransaction#service(dinamica.Recordset)
	 */
	public int service(Recordset inputParams) throws Throwable
	{
		//reuse superclass code
		int rc = super.service(inputParams);
		
		//patch 2007-10-09 - archivo nulo sera considerado un error
		if (inputParams.isNull("file.filename"))
			throw new Throwable("¡Por favor indique una ruta válida de archivo!");
		
		//get temp file
		String path = (String)inputParams.getValue("file");
		File f = new File(path);
		
		//get file size
		Integer size = new Integer((int)f.length()); 
		inputParams.setValue("image_size", size);

		if (size.intValue()==0)
			throw new Throwable("¡No se puede cargar un archivo vacío!");
		
		//prepare sql template (replace static values)
		String sql = getResource("query.sql");
		sql = getSQL(sql, inputParams);
		
		//get db object and save blob using prepared statement
		Db db = getDb();		
		db.saveBlob(sql, path);
		
		//delete temp file
		f.delete();
		
		return rc;
		
	}

}
