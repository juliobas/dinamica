package dinamica;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.*;
import org.smx.captcha.Producer;
import org.smx.captcha.impl.*;

/**
 * Output module for server-side captcha generation, serves the image
 * and saves the corresponding text in session attribute: dinamica.security.captcha
 * <br>
 * <br>
 * Creation date: 2007-12-21<br>
 * Last Update: 2007-12-21<br>
 * (c) 2007 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class CaptchaOutput extends GenericOutput
{

	/* (non-Javadoc)
	 * @see dinamica.GenericOutput#print(dinamica.GenericTransaction)
	 */
	public void print(GenericTransaction t) throws Throwable
	{

		//buffer to render image in memory
		ByteArrayOutputStream b = new ByteArrayOutputStream(16384);
		
		//create captcha according to configuration
		Properties props = new Properties(); 
		props.put("format", "png"); 
		props.put("font", getConfig().getConfigValue("font")); 
		props.put("fontsize", getConfig().getConfigValue("fontsize")); 
		props.put("min-width", getConfig().getConfigValue("min-width")); 
		props.put("padding-x", getConfig().getConfigValue("padding-x")); 
		props.put("padding-y", getConfig().getConfigValue("padding-y"));
		props.put("curve", "false"); 
		FactoryRandomImpl inst=(FactoryRandomImpl)Producer.forName("org.smx.captcha.impl.FactoryRandomImpl");
		inst.setSize(6);
		inst.setUseDigits(true);
		Producer.render(b, inst, props);
		
		//get generated word and save it in session
		String text = inst.getLastWord();
		getSession().setAttribute("dinamica.security.captcha", text);
		
		//send bitmap via servlet output
		byte image[] = b.toByteArray();
		getResponse().setContentType("image/png");
		getResponse().setContentLength(image.length);
		getResponse().setHeader("Cache-Control","no-cache");
		getResponse().setHeader("Pragma","no-cache");		
		OutputStream out = getResponse().getOutputStream();
		out.write(image);
		out.close();
				
	}
	
}
