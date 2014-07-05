package dinamica.charts;

import java.awt.Color;
import dinamica.*;
import org.jfree.chart.*;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.*;


/**
 * Chart plugin for XYBar charts with multiseries support
 * Last update: 18/11/2003
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class XYAreaChart extends AbstractChartPlugin
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractChartPlugin#getChart(dinamica.Recordset, dinamica.Recordset)
	 */
	public JFreeChart getChart(Recordset chartInfo, Recordset data)
		throws Throwable
	{

		/* get series labels - if any */
		String series[] = null;
		String seriesLabels = chartInfo.getString("title-series");
		if (seriesLabels!=null)
			series = StringUtil.split(seriesLabels, ";");
		else
		{
			series = new String[1];
			series[0] = "";  
		}

		String colx = chartInfo.getString("column-x");
		
		/* are there multiple series? */
		String dataCols[] = null;
		String coly = chartInfo.getString("column-y");
		if (coly.indexOf(";")>0)
			dataCols = StringUtil.split(coly, ";");
		else
		{
			dataCols = new String[1];
			dataCols[0] = coly;
		}
		
	
		/* get value y for each serie */
		XYSeriesCollection dataset = new XYSeriesCollection();
		for (int i=0;i<dataCols.length;i++)
		{

			XYSeries xy = new XYSeries(series[i]);

			/* navigate the recordset and feed the chart dataset */
			data.top();
			while (data.next())
			{
				
					Double value = new Double(data.getDouble(dataCols[i]));
					if (value==null) 
						value = new Double(0);
					xy.add(data.getDouble(colx), value.doubleValue());
			}
			dataset.addSeries(xy);		

		}
		
		
		/* get chart params */
		String title = (String)chartInfo.getValue("title");
		String titlex = (String)chartInfo.getValue("title-x");
		String titley = (String)chartInfo.getValue("title-y");

		/* if there is more than 1 series then use legends */
		boolean useLegend = (dataCols.length>1);

		/* create a chart */
		
		JFreeChart chart = ChartFactory.createXYAreaChart(
				title,       				// chart title
				titlex,                    	// domain axis label
				titley,                   	// range axis label
				dataset,                  	// data
				PlotOrientation.VERTICAL, 	// orientation
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

		XYPlot plot = (XYPlot)p;
       
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinesVisible(true);
        
    }	
	
}
