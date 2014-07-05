package dinamica;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Utility class to send mail using SMTP (JavaMail).<br>
 * Provides very simple methods for sending text based emails.
 * 
 * <br>
 * Creation date: jan/14/2004<br>
 * Last Update: Oct/10/2004<br>
 * (c) 2004 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 * */
public class SimpleMail
{
	
	/**
	 * Send a simple text message using a background thread
	 * @param host Mail server address
	 * @param from valid email address
	 * @param fromName Descriptive from name
	 * @param to valid email address
	 * @param subject Email subject
	 * @param body Message body (text only)
	 */
	public void send(String host, String from, String fromName, String to, String subject, String body)
	{
		Thread t = new Thread(new BackgroundSender(host, from, fromName, to, subject, body));
		t.start();
	}

	public void send(String host, String from, String fromName, String to, String subject, String body, String cc)
	{
		Thread t = new Thread(new BackgroundSender(host, from, fromName, to, subject, body, cc));
		t.start();
	}

	/**
	 * BackgroundSender<br>
	 * Send email using a separate Thread to avoid blocking
	 * Creation date: 29/07/2004<br>
	 * (c) 2004 Martin Cordova y Asociados<br>
	 * http://www.martincordova.com<br>
	 * @author Martin Cordova dinamica@martincordova.com
	 */
	class BackgroundSender implements Runnable
	{

		String host = null;
		String from = null;
		String fromName = null;
		String to = null;
		String subject = null;
		String body = null;
		String cc = null;
		
		public BackgroundSender(String host, String from, String fromName, String to, String subject, String body)
		{
			this.host = host;
			this.from = from;
			this.fromName = fromName;
			this.to = to;
			this.subject = subject;
			this.body = body;
		}
		
		public BackgroundSender(String host, String from, String fromName, String to, String subject, String body, String cc)
		{
			this.host = host;
			this.from = from;
			this.fromName = fromName;
			this.to = to;
			this.subject = subject;
			this.body = body;
			this.cc = cc;
		}
				
		
		public void run()
		{
			
			try
			{
				//init email system
				Properties props = System.getProperties();
				props.put( "mail.smtp.host", host );
				Session session = Session.getDefaultInstance( props, null );
				session.setDebug( false );
				
				String recipients[] = null;
				if (to.indexOf(",")>0) {
					recipients = StringUtil.split(to, ",");
				} else {
					recipients = new String[1];
					recipients[0] = to;
				}
				
				for (int i = 0; i < recipients.length; i++) {

					//recipients
					InternetAddress[] toAddrs = InternetAddress.parse( recipients[i], false );
					
					//create message
					Message msg = new MimeMessage( session );
					msg.setRecipients( Message.RecipientType.TO, toAddrs );
					
					if (cc!=null) {
						InternetAddress[] toCC = InternetAddress.parse( cc, false );;
						msg.setRecipients( Message.RecipientType.CC, toCC );
					}
					
					msg.setSubject( subject );
					msg.setFrom( new InternetAddress( from, fromName ) );
					msg.setText( body );
					
					//send 
					Transport.send( msg );
					
				}
				
			}
			catch (Throwable e)
			{
				try
				{
					String d = StringUtil.formatDate(new java.util.Date(), "yyyy-MM-dd HH:mm:ss");
					System.err.println("ERROR [dinamica.SimpleMail@" + d + "]: " + e.getMessage() + " recipient(s): " + to);
				}
				catch (Throwable e1)
				{
				}
			}
			
		}
		
	}

}
