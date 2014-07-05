package dinamica;

import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;

import java.sql.*;
import javax.servlet.ServletOutputStream;
import javax.sql.DataSource;

/**
 * Output module to print blob contents stored in PostgreSQL database (v8.x), 
 * like images, pdfs and other types of documents 
 * saved in columns of type "bytea". This class was required
 * because PostgreSQL does implement the standard JDBC BLOB API for
 * its "bytea" data type, so the default dinamica.BlobOutput cannot be used
 * with PostgreSQL databases in the current stable versions (7.4 and 8.x).
 * <br>
 * Creation date: 2-sept-2005<br>
 * Last Update: 2-sept-2005<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class BlobOutputPGSQL extends GenericOutput
{

	/* (non-Javadoc)
	 * @see dinamica.GenericOutput#print(dinamica.GenericTransaction)
	 */
	public void print(GenericTransaction data) throws Throwable
	{
		
		//get datasource object
		String jndiPrefix = getContext().getInitParameter("jndi-prefix");
		String dataSourceName = getContext().getInitParameter("def-datasource");
				
		/* PATCH 2005-03-10 read datasource name from config.xml if available */
		if (getConfig().transDataSource!=null)
			dataSourceName = getConfig().transDataSource;
		
		if (jndiPrefix==null)
			jndiPrefix="";
		
		DataSource ds = Jndi.getDataSource(jndiPrefix + dataSourceName);
		
		Connection conn = null;
		Statement s = null;
		ResultSet rs = null;
		
		ServletOutputStream out = null;

		//si se envio el parametro 'scale' en el url entonces se
		//asume que es una imagen y se escala, se usa para Thumbnails de imagenes solamente
		double scaleFactor = 0;
		String scale = getRequest().getParameter("scale");
		if (scale!=null && !scale.equals("")) {
			scaleFactor = Double.parseDouble(scale);
		}
		
		
		try
		{
			//connect to database
			conn = ds.getConnection();
			s = conn.createStatement();

	  		//get recordset with blob metadata
	  		Recordset info = data.getRecordset("blobinfo");
			
			//get sql to retrieve blob
			String sql = info.getString("sql");

			//set BLOB content-type
			getResponse().setContentType(info.getString("format"));

			//attach?
			String fileName = info.getString("filename");
			if (fileName!=null)
			{
				getResponse().setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";"); 
			}

			//get servlet output stream
			out = getResponse().getOutputStream();

			//execute query and retrieve blob
			rs = s.executeQuery(sql);
			if (rs.next())
			{
				//read blob
				byte[] blob = rs.getBytes(1);
				
				//scale image if requested
				if (blob.length > 20000 && scaleFactor > 0 && info.getString("format").startsWith("image/")) {
					BufferedImage bufimg = ImageIO.read(new ByteArrayInputStream(blob));
					AffineTransform tx = new AffineTransform();
				    tx.scale(scaleFactor, scaleFactor);
				    AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				    BufferedImage newImg =  op.filter(bufimg, null);
				    ByteArrayOutputStream bout = new ByteArrayOutputStream();
				    if (info.getString("format").endsWith("png"))
				    	ImageIO.write(newImg, "png", bout);
				    else
				    	ImageIO.write(newImg, "jpg", bout);
				    blob = bout.toByteArray();
				}
				
				//set content length
				int size = (int)blob.length;
				getResponse().setContentLength(size);
				out.write(blob);
			}

		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{
				if (rs!=null) rs.close();
				if (s!=null) s.close();
				if (conn!=null) conn.close();
				if (out!=null) out.close();
		}
		
	}

}
