package dinamica;

import java.io.*;
import javax.servlet.ServletOutputStream;
import java.util.*;

/**
 * DataOutput<br>
 * Output module that serializes the internal HashMap
 * object mantained by the Transaction object, which
 * does contain all the published Recordsets. As a result,
 * all Recordsets arre transmitted as byte stream over HTTP
 * to the client who sent the request.
 * <br><br>
 * Creation date: 08/06/2004<br>
 * (c) 2004 Martin Cordova y Asociados<br>
 * http://www.martincordova.com<br>
 * @author Martin Cordova dinamica@martincordova.com
 */
public class DataOutput extends GenericOutput
{

	/**
	 * Serialize a HashMap containing Recordsets 
	 * and transmit them over the servlet OutputStream
	 */
	public void print(GenericTransaction data) throws Throwable
	{
		
		//retrieve hashmap containing all published recordsets
		HashMap<String, Recordset> obj = data.getData();
		
		//serialize obj to byte array
		ByteArrayOutputStream bout = new ByteArrayOutputStream(); 
		ObjectOutputStream out = new ObjectOutputStream(bout);
		out.writeObject(obj);
		out.close();
		byte buffer[] = bout.toByteArray();
		
		//print buffer
		getResponse().setContentType("application/octet-stream");
		getResponse().setContentLength(buffer.length);
		ServletOutputStream sout = getResponse().getOutputStream();
		sout.write(buffer);
		sout.close();
		
	}

}
