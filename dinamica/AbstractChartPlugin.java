package dinamica;

import org.jfree.chart.*;
import org.jfree.chart.plot.*;

/**
 * Super class to build server-side chart plugins,
 * each plugin produces a different type of chart,
 * based on the JFreeChart component
 * Last update: 17/11/2003
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public abstract class AbstractChartPlugin
{
	/**
	 * Generate chart object
	 * @param chartInfo Recordset containing the chart configuration
	 * @param data Recordset containing the data to be plotted
	 * @return Reference to chart object
	 * @throws Throwable
	 */
	public abstract JFreeChart getChart(Recordset chartInfo, Recordset data) throws Throwable;
	
	/**
	 * Configure chart decoration (legends, labels, color intensity, etc)
	 * @param p Plot object - cast this object to the specific plot type you need
	 */
	public void configurePlot(Plot p)
	{
	}
	
}
