package dinamica;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.sql.*;

import javax.servlet.ServletOutputStream;
import javax.sql.DataSource;

/**
 * Generic output module to print blob contents,
 * like images, pdfs and other types of documents
 * saved in database columns of type BLOB or equivalent. 
 * <br>
 * Creation date: 6-jan-2004<br>
 * Last Update: 20-may-2004<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class BlobOutput extends GenericOutput
{

	/* (non-Javadoc)
	 * @see dinamica.GenericOutput#print(dinamica.GenericTransaction)
	 */
	public void print(GenericTransaction data) throws Throwable
	{
		
		final int BUFFER_SIZE = 8192;
		
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
		
		BufferedInputStream buf = null;
		ServletOutputStream out = null;
		
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
				//get reference to BLOB column - must be the only one retrieved!
				Blob blob = rs.getBlob(1);

				//set content length
				int size = (int)blob.length();
				getResponse().setContentLength(size);

				int bytes = 0;
				byte buffer[] = new byte[BUFFER_SIZE];
				buf = new BufferedInputStream( blob.getBinaryStream() );
				
				while( bytes != -1 )
				{
					bytes = buf.read(buffer);
					if (bytes>0)
						out.write(buffer,0,bytes);
				} 			 
			}
		}
		catch (Throwable e)
		{
			throw e;
		}
		finally
		{

			try
			{
				if (buf!=null) buf.close();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}

			try
			{
				if (rs!=null) rs.close();
			}
			catch (SQLException e2)
			{
				e2.printStackTrace();
			}

			try
			{
				if (s!=null) s.close();
			}
			catch (SQLException e3)
			{
				e3.printStackTrace();
			}

			try
			{
				if (conn!=null) conn.close();
			}
			catch (SQLException e4)
			{
				e4.printStackTrace();
			}

			try
			{
				if (out!=null)
				{
					out.close();
				}
			}
			catch (IOException e5)
			{
				e5.printStackTrace();
			}

		}
		
	}

}
