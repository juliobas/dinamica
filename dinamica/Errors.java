package dinamica;

/**
 * Contains error message constants for all the framework classes
 * <br>
 * Creation date: 10/09/2003<br>
 * Last Update: 10/09/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova (dinamica@martincordova.com)
 */
class Errors
{

	/* recordset errors */
	final static String INVALID_DATATYPE = "Invalid data type: {0} - please use only (java.sql.Types) INTEGER, BIGINT, VARCHAR, TIMESTAMP, DATE or DOUBLE."; 
	final static String FIELD_NOT_FOUND = "Field not found: {0}. The Recordset does not contain a field with this name. Recordset fields are: {1}"; 
	final static String RECNUM_OUT_OF_RANGE = "Invalid record number: {0}. The Record number must be between 0 and {1}."; 

	/* jndi errors */
	final static String DATASOURCE_NOT_FOUND = "JDBC DataSource not found: {0}";
	
	/* database error */
	final static String DATABASE_ERROR = "JDBC Error processing query: {0}";
	final static String INVALID_STMT_PARAMS = "Invalid prepared statement parameters. Params array size={0}. Recordset field count={1}";

	/* template engine errors */
	final static String INVALID_PREFIX = "Invalid prefix length: {0}";
	final static String INVALID_MARKER = "Invalid Marker ID - must be a contiguous string of letters and numbers, no spaces or special characters: {0}";
	final static String MARKER_UNCLOSED = "Marker is not properly closed with with a brace '}'.";
	final static String SEQUENCE_BAD_CONFIGURATION = "SQL Sequences are not properly in WEB.XML as context parameters: {0}.";
	final static String REPEAT_TAG_NOT_FOUND = "Repeat Section Tag not found in template: {0}.";
	final static String REPEAT_TAG_NOT_CLOSED = "Repeat Section Tag not properly closed: {0}.";

}

