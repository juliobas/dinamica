package dinamica;

import java.util.Date;

/**
 * Utility class to validate String values against data types
 * in order to test if the value represents de data type (dates, numbers, etc.)
 * <br>
 * Creation date: 25/10/2003<br>
 * Last Update: 25/10/2003<br>
 * (c) 2003 Martin Cordova<br>
 * This code is released under the LGPL license<br>
 * @author Martin Cordova
 */
public class ValidatorUtil
{

	/**
	 * Test a String value to check if it does represent
	 * a valid date according to a specific format
	 * @param dateValue Value to test
	 * @param dateFormat Date format used to represent a date in value
	 * @return NULL if value can't be converted
	 */
	public static Date testDate(String dateValue, String dateFormat)
	{
		
		try
		{
			Date d = StringUtil.getDateObject(dateValue, dateFormat);
			return d;
		}
		catch (Throwable e)
		{
			return null;
		}

	}

	/**
	 * Test a String value to check if it does represent
	 * a valid double number
	 * @param doubleValue Value to test
	 * @return NULL if value can't be converted
	 */
	public static Double testDouble(String doubleValue)
	{

		try
		{
			//patch 2009-10-05
			if (doubleValue.indexOf(",")>0)
				doubleValue = StringUtil.replace(doubleValue, ",", ".");
			double d = Double.parseDouble(doubleValue);
			return new Double(d);
		}
		catch (Throwable e)
		{
			return null;
		}

	}

	/**
	 * Test a String value to check if it does represent
	 * a valid integer number
	 * @param integerValue Value to test
	 * @return NULL if value can't be converted
	 */
	public static Integer testInteger(String integerValue)
	{

		try
		{
			int i = Integer.parseInt(integerValue);
			return Integer.valueOf(i);
		}
		catch (Throwable e)
		{
			return null;
		}

	}

}
