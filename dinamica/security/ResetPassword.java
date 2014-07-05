package dinamica.security;

import java.util.UUID;
import dinamica.*;

/**
 * Generar una contraseña random, se usa para modulos de "Olvidé mi contraseña",
 * que deben regenerar una contraseña temporal y enviarla por email. Esta clase
 * depende de que el Action tenga un validator con estos elementos como mínimo:<br>
 * userlogin, passwd, passwd_clear. El campo passwd será la contraseña regenerada y
 * encriptada como un HASH la cual será actualizada en BD, el campo passwd_clear será
 * la representación en texto libre, para ser enviada por email. Se supone que el Action
 * que usa esta clase tiene lo necesario para validar captcha, el SQL para actualizar
 * la contraseña en BD, etc.<br> El validator DEBE contener esos campos, pero el request
 * solo pasa el campo userlogin, ya que las contraseñas son computadas por esta clase.
 * Claro que el request pasará otros datos para la identificación positiva del usuario 
 * mediante un custom-validator, como su email, pero no es relevante para la regeneración
 * del password.<br><br>
 * Fecha de actualizacion: 2009-05-23<br>
 * @author Martin Cordova y Asociados C.A <br>
 */
public class ResetPassword extends GenericTableManager 
{
	@Override
	public int service(Recordset inputParams) throws Throwable 
	{
		//genera password random
		String guid[] = StringUtil.split(UUID.randomUUID().toString(),"-");
		String password = guid[0];
		inputParams.setValue("passwd", password);
		inputParams.setValue("passwd_clear", password);
		
		//aplicar HASH al password
		AbstractValidator val = (AbstractValidator)getObject("dinamica.security.PasswordEncryptor");
		val.isValid(getRequest(), inputParams, null);
		
		//dejar que la clase genérica se encargue de actualizar la BD y notificar por email
		//el nuevo password
		super.service(inputParams);

		return 0;
	}
}
