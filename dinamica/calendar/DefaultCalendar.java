package dinamica.calendar;

import dinamica.*;
import java.sql.Types;
import java.util.*;

/**
 * Custom Calendar Transaction for Dinamica's built-in
 * calendar services. All calendar transactions must
 * store a recordset as a request attribute with ID "calendar"
 * that should contain 3 columns:<br>
 * <ul>
 * <li> day - integer
 * <li> onclick - varchar, value of the onclick attribute
 * <li> html - content of the calendar cell for this day
 * <li> class - class name defining style for this cell (see calendar template)
 * </ul>
 * <br>
 * This transaction expects 2 optional request parameters:
 * <ul>
 * <li> id - ID of the element (textbox) attached to the calendar
 * <li> date - selected date if format dd-mm-yyyy
 * </ul>
 * <br>
 * This calendar transaction in particular was designed to be used
 * by popup JavaScript calendars attached to textboxes, but can also
 * be used for clickable agendas, etc.
 * <br>
 * Creation date: 2006-06-04
 * @author mcordova (dinamica@martincordova.com)
 *
 */

public class DefaultCalendar extends GenericTransaction
{


	public int service(Recordset inputs) throws Throwable
	{
		super.service(inputs);

		//retrieve parameters
		String id = getRequest().getParameter("id");
		String d = getRequest().getParameter("date");
		String d2 = getRequest().getParameter("date.lbound");
		String d3 = getRequest().getParameter("date.ubound");

		if (d2==null)
			d2 = "";

		if (d3==null)
			d3 = "";

		//set date
		Date calDate = ValidatorUtil.testDate(d, "dd-MM-yy");
		if (calDate==null)
			calDate = new Date();

		//lower bound date
		Date minDate = ValidatorUtil.testDate(d2, "dd-MM-yy");

		//upper bound date
		Date maxDate = ValidatorUtil.testDate(d3, "dd-MM-yy");

		// save attached textbox id - used for callbacks
		getRequest().setAttribute("parent.ElementID", id);
		getRequest().setAttribute("date.lbound", d2);
		getRequest().setAttribute("date.ubound", d3);

		// calendar object
		Calendar c = Calendar.getInstance();
		c.setLenient(true);
        c.setTime(calDate);
        //patch 2009-04-13 gdottori
        c.set( Calendar.HOUR, 0 );
        c.set( Calendar.HOUR_OF_DAY, 0 );
        c.set( Calendar.MINUTE, 0 );
        c.set( Calendar.SECOND, 0 );
        c.set( Calendar.MILLISECOND, 0 );
        //end patch

		//recordset with selected year and month
		Recordset rsDate = new Recordset();
		rsDate.append("year", Types.INTEGER);
		rsDate.append("month", Types.INTEGER);
		rsDate.addNew();
		rsDate.setValue("year", new Integer(c.get(Calendar.YEAR)));
		rsDate.setValue("month", new Integer(c.get(Calendar.MONTH)+1));

		//recordset with years
		Recordset rsYears = new Recordset();
		rsYears.append("year", Types.INTEGER);
		for (int i=c.get(Calendar.YEAR)-15; i<c.get(Calendar.YEAR)+15;i++)
		{
			rsYears.addNew();
			rsYears.setValue("year", new Integer(i));
		}

		//selected day (if date==null -> day = today)
		int day = c.get(Calendar.DAY_OF_MONTH);

		//set first day of month
		c.set(Calendar.DATE,1);

		//get partial date mask - used as an href parameter
		String partialDate = StringUtil.formatDate(calDate, "M-yyyy");

		// define recordset
		Recordset rs = createCalendarRecordset();

		// fill empty slots until 1st day of month
		for (int i=Calendar.SUNDAY; i<c.get(Calendar.DAY_OF_WEEK); i++)
		{
			rs.addNew();
			rs.setValue("onclick", "" );
			rs.setValue("html", null);
			rs.setValue("class", "calEmptyCell");
		}

		// feed recordset with selected month
		for (int i=1; i<=c.getActualMaximum(Calendar.DAY_OF_MONTH);i++)
		{
			c.set(Calendar.DATE, i);
			String date = i  + "-" + partialDate;
			rs.addNew();
			rs.setValue("day", new Integer(i));
			rs.setValue("onclick", this.getCellOnClickValue(id, c, day, minDate, maxDate, date));
			rs.setValue("html",  this.getCellHTML(c, day));
			rs.setValue("class",  this.getCellStyle(c, day, minDate, maxDate));
		}

		// fill empty slots from last day of month until next saturday
		if (c.get(Calendar.DAY_OF_WEEK)< Calendar.SATURDAY)
		{
			for (int i = c.get(Calendar.DAY_OF_WEEK); i < Calendar.SATURDAY; i++)
			{
				rs.addNew();
				rs.setValue("onclick", "" );
				rs.setValue("html", null);
				rs.setValue("class", "calEmptyCell");
			}
		}

		//publish recordset as a request attribute for HGrid output class
		getRequest().setAttribute("calendar", rs);

		//publish recordset with current month and year (used by combos)
		publish("calconfig", rsDate);
		publish("years", rsYears);

		return 0;

	}

	/**
	 * Creates the recordset that contains the basic data
	 * to print a Calendar.
	 * @return Recordset with this structure: day (int), onclick (varchar), html (varchar), class (varchar)
	 * @throws Throwable
	 */
	Recordset createCalendarRecordset() throws Throwable
	{
		Recordset rs = new Recordset();
		rs.append("day", Types.INTEGER);
		rs.append("onclick", Types.VARCHAR);
		rs.append("html", Types.VARCHAR);
		rs.append("class", Types.VARCHAR);
		return rs;
	}

	/**
	 * Returns the HTML content for a given calendar cell,
	 * only for cells that contain a day- not called for empty cells.
	 * @param c Calendar set for the day to be printed
	 * @param currentDay Current day
	 * @return A String that contains the HTML to be printed into the cell
	 * @throws Throwable
	 */
	protected String getCellHTML(Calendar c, int currentDay) throws Throwable
	{
		return String.valueOf(c.get(Calendar.DATE));
	}

	/**
	 * Returns the value of the CLASS attribute for a given calendar cell,
	 * only for cells that contain a day- not called for empty cells.
	 * @param c Calendar set for the day to be printed
	 * @param currentDay Selected day
	 * @param minDate Minimal date that can be selected (may be null)
	 * @return A String that contains the name of the class attribute for the table cell,
	 * should match those styles defined in the Calendar main template.
	 * @throws Throwable
	 */
	protected String getCellStyle(Calendar c, int currentDay, Date minDate, Date maxDate) throws Throwable
	{

		//default
		String styleName = "calDay";

		//is current day?
		if (c.get(Calendar.DATE)==currentDay)
			styleName = "calCurDay";

		//should be disabled?
		if (minDate!=null)
		{
			if (c.getTime().compareTo(minDate)<0)
				styleName = "calDisabled";
		}
		if (maxDate!=null)
		{
			if (c.getTime().compareTo(maxDate)>0)
				styleName = "calDisabled";
		}

		return styleName;

	}

	/**
	 * Returns the value of the ONCLICK attribute for a given calendar cell,
	 * only for cells that contain a day- not called for empty cells.
	 * @param elementID ID of the element (textbox) attached to the Calendar
	 * @param c Calendar set for the day to be printed
	 * @param currentDay Selected day
	 * @param minDate Minimal date that can be selected (may be null)
	 * @param date Calendar date preformated as dd-mm-yyyy
	 * @return A String that contains the value of the onclick attribute for the table cell (calls some javascript function)
	 * @throws Throwable
	 */
	protected String getCellOnClickValue(String elementID, Calendar c, int currentDay, Date minDate, Date maxDate, String date) throws Throwable
	{

		//default
		String onclick = "parent.calendarReturnValue('" + elementID + "', '" + date + "')";

		//should be disabled?
		if (minDate!=null)
		{
			if (c.getTime().compareTo(minDate)<0)
				onclick = "";
		}

		if (maxDate!=null)
		{
			if (c.getTime().compareTo(maxDate)>0)
				onclick = "";
		}

		return onclick;

	}

}
