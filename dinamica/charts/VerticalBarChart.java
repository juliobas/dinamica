package dinamica.charts;

import java.awt.Color;
import dinamica.*;
import org.jfree.chart.*;
import org.jfree.data.category.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.*;



/**
 * Chart plugin for Bar 2D charts with multiseries support
 * Last update: 18/11/2003
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class VerticalBarChart extends AbstractChartPlugin
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractChartPlugin#getChart(dinamica.Recordset, dinamica.Recordset)
	 */
	public JFreeChart getChart(Recordset chartInfo, Recordset data)
		throws Throwable
	{

		/* get date format for x-axis if required */
		String dateFormat = (String)chartInfo.getValue("dateformat");

		/* create a chart dataset using the data contained in the Recordset "data" */
		DefaultCategoryDataset chartdata = new DefaultCategoryDataset();
		
		/* get series labels - if any */
		String series[] = null;
		String seriesLabels = (String)chartInfo.getValue("title-series");
		if (seriesLabels!=null)
			series = StringUtil.split(seriesLabels, ";");
		else
		{
			series = new String[1];
			series[0] = ""; 
		}
			

		/* are there multiple series? */
		String dataCols[] = null;
		String coly = (String)chartInfo.getValue("column-y");
		if (coly.indexOf(";")>0)
			dataCols = StringUtil.split(coly, ";");
		else
		{
			dataCols = new String[1];
			dataCols[0] = coly;
		}
		
		
		/* navigate the recordset and feed the chart dataset */
		data.top();
		while (data.next())
		{
			
			/* get label x */
			String colx = (String)chartInfo.getValue("column-x");
			RecordsetField f = data.getField(colx);
			String label = null;
			if (f.getType()==java.sql.Types.DATE)
				label = StringUtil.formatDate((java.util.Date)data.getValue(colx), dateFormat);
			else
				label = String.valueOf(data.getValue(colx));

			/* get value y for each serie */
			for (int i=0;i<dataCols.length;i++)
			{
				if (!data.isNull(dataCols[i])) {
					Double value = new Double(data.getDouble(dataCols[i]));
					chartdata.addValue(value, series[i], label);
				}
			}
		
		}
		
		/* get chart params */
		String title = (String)chartInfo.getValue("title");
		String titlex = (String)chartInfo.getValue("title-x");
		String titley = (String)chartInfo.getValue("title-y");

		/* if there is more than 1 series then use legends */
		boolean useLegend = (dataCols.length>1);

		/* create a chart */
		JFreeChart chart = ChartFactory.createBarChart(
			title,       				// chart title
			titlex,                    	// domain axis label
			titley,                   	// range axis label
			chartdata,                  // data
			PlotOrientation.VERTICAL,  	// orientation
			useLegend,                 	// include legend
			false,                      // tooltips
			false                      	// urls
		);		
		
		/* set chart decoration */
		configurePlot( chart.getPlot() );
		
		//PATCH 2005-07-19 - support for custom default color
		//for single series charts - line, bar and area only
		String color = chartInfo.getString("color");
		if (!useLegend && color!=null)
		{
			CategoryPlot p = (CategoryPlot)chart.getPlot();
			p.getRenderer().setSeriesPaint(0, Color.decode(color));
		}		
		
		/* return chart */
		return chart;
		

	}

	/**
	 * Configure chart decorations
	 */
	public void configurePlot(Plot p) 
	{

		CategoryPlot plot = (CategoryPlot)p;
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        CategoryAxis axis = (CategoryAxis)plot.getDomainAxis();
        axis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);        
        
    }	

}
