package dinamica.charts;

import java.awt.Color;

import dinamica.*;
import org.jfree.chart.*;
import org.jfree.data.general.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.labels.*;

/**
 * Chart plugin for Pie3D
 * Last update: 20/july/2005
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class PieChart3D extends AbstractChartPlugin
{

	/* (non-Javadoc)
	 * @see dinamica.AbstractChartPlugin#getChart(dinamica.Recordset, dinamica.Recordset)
	 */
	public JFreeChart getChart(Recordset chartInfo, Recordset data)
		throws Throwable
	{

		/* create a chart dataset using the data contained in the Recordset "data" */
		DefaultPieDataset chartdata = new DefaultPieDataset();
		data.top();
		while (data.next())
		{
			String colx = (String)chartInfo.getValue("column-x");
			String coly = (String)chartInfo.getValue("column-y");
			
			/* get label x */
			String label = data.getString(colx);
			
			/* get value y */
			if (data.isNull(coly)) 
				chartdata.setValue(label, 0);
			else
				chartdata.setValue(label, data.getDouble(coly));

		}
		
		/* get chart params */
		String title = (String)chartInfo.getValue("title");

		/* create a chart */
		JFreeChart chart = ChartFactory.createPieChart3D(title, chartdata, true, false, false);
		
		/* set pie decoration */
		configurePlot( chart.getPlot() );

		/* return chart */
		return chart;

	}

	/**
	 * Configure chart decorations
	 */
	public void configurePlot(Plot p) 
	{
		PiePlot3D plot = (PiePlot3D) p;
		plot.setBackgroundPaint(Color.white);
		plot.setOutlinePaint(Color.lightGray);
		StandardPieSectionLabelGenerator splg = new StandardPieSectionLabelGenerator("{2}");
		splg.getPercentFormat().setMinimumFractionDigits(2);
		plot.setLabelGenerator(splg);
		plot.setDepthFactor(0.1);
        plot.setForegroundAlpha(0.7F);
    }
	
}
