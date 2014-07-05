package dinamica.charts;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.*;


/**
 * Chart plugin for Horizontal Bar3D charts with multiseries support
 * Last update: 24/july/2005
 * @author Martin Cordova (dinamica@martincordova.com)
 */
public class HorizontalBarChart3D extends VerticalBarChart3D
{

	public void configurePlot(Plot p) 
	{
		super.configurePlot(p);
		CategoryPlot plot = (CategoryPlot)p;
		plot.setOrientation(PlotOrientation.HORIZONTAL);
		plot.setDomainGridlinesVisible(false);
		CategoryAxis axis = (CategoryAxis)plot.getDomainAxis();
        axis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);  		
	}

}
