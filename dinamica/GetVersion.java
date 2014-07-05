package dinamica;

/**
 * Print framework version
 * @author Martin Cordova
 */
public class GetVersion {

	static String version = "v2.2.5 build 2010-05-17";
	
	public static void main(String[] args) 
	{
		
		System.out.println("Dinamica framework library " + version);
		System.out.println("Requires Java 1.6");
		System.out.println("Distributed under the LGPL licence");
		System.out.println("by Martin Cordova y Asociados C.A.");
		
	}
	
	static public String getVersion() {
		return version;
	}
	
}
