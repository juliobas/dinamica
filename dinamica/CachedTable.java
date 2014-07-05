package dinamica;


/**
 * Ejecuta las operaciones de añadir, modificar o eliminar registros
 * en un recordset que debe existir en sesion. Es un manejo tipo CRUD
 * pero realiza toda las operaciones en memoria RAM. Depende de unos 
 * parámetros de configuración que se especifican en config.xml dentro del nodo raíz:<br><br>
 * mode: insert, edit, update o delete - indica la operación a realizar.<br>
 * recordsetId: el ID del atributo en sesión que contiene al recorsdet<br>
 * <br>
 * @author Martin Cordova y Asociados C.A.
 *
 */
public class CachedTable extends GenericTableManager 
{

	@Override
	public int service(Recordset inputParams) throws Throwable {
		
		super.service(inputParams);
		
		//lee configuracion
		String mode = getConfig().getConfigValue("mode");
		String rsId = getConfig().getConfigValue("recordsetId");
		
		//recupera recordset de sesion
		Recordset rs = (Recordset)getSession().getAttribute(rsId);
		
		if (mode.equals("insert")) {
			//primera vez? usa la estructura del validator.xml
			if (rs==null) {
				rs = inputParams.copyStructure();
				getSession().setAttribute(rsId, rs);
			}
			//insertar un registro con los parametros pasados por el request
			rs.addNew();
			inputParams.copyValues(rs);
			return 0;
		}

		if (rs==null)
			throw new Throwable("Invalid session state, recordset not found with ID: " + rsId);
		
		if (mode.equals("edit")) {
			//posicionarse en el registro y obtener los valores de los campos
			rs.setRecordNumber(inputParams.getInt("id"));
			return 0;
		}

		if (mode.equals("update")) {
			//posicionarse en el registro y actualizar los valores
			rs.setRecordNumber(inputParams.getInt("rowindex"));
			inputParams.copyValues(rs);
			return 0;
		}
		
		if (mode.equals("delete")) {
			//posicionarse en el registro y eliminarlo
			rs.delete(inputParams.getInt("id"));
			return 0;
		}
		
		throw new Throwable("Invalid mode: " + mode);
		
	}

}
