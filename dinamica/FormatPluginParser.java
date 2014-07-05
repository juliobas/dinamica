package dinamica;
/**
 * Parses format plugin name to extract
 * parameter information if available 
 * <br><br>
 * Creation date: 03/jun/2005
 * (c) 2005 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * Dinamica Framework - http://www.martincordova.com<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class FormatPluginParser
{

    private String name = null;
    private String args = null;

    public String getArgs()
    {
        if (args!=null && args.equals(""))
            args = null;
        return args;
    }
    public String getName()
    {
        return name;
    }
    
    public FormatPluginParser(String pluginName)
    {
        name = pluginName;
        int pos1 = pluginName.indexOf("(");
        if (pos1 >0)
        {
	        int pos2 = 0;
	        pos2 = pluginName.indexOf(")",pos1+1);
	        if (pos2>0)
	        {
	            args = pluginName.substring(pos1+1,pos2);
	            name = pluginName.substring(0,pos1);
	        }
        }
    }

}
