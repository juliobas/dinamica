package dinamica.charts;

import java.text.SimpleDateFormat;
import org.jfree.chart.JFreeChart;
import dinamica.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.chart.axis.*;

/**
 * Crea un grafico GANTT simple (un solo set de fechas desde/hasta),
 * el recordset debe contener 3 campos, la tarea, la fecha desde y la fecha hasta.
 * Los campos de fecha se indicaran en column-y separados por ;
 * @author Martin Cordova y Asociados C.A.
 *
 */
public class SimpleGantt extends AbstractChartPlugin {

	Recordset chartInfo = null;
	
	@Override
	public JFreeChart getChart(Recordset chartInfo, Recordset data) throws Throwable {


		this.chartInfo = chartInfo;
		
		TaskSeriesCollection collection = new TaskSeriesCollection();
		
		//nombre de la serie
		String titlex = chartInfo.getString("title-x");
		
		//nombres de las series (ej: Planificado;Real)
		String seriesNames[] = StringUtil.split(chartInfo.getString("title-series"), ";");
		
		//campo con el nombre de la tarea
		String colx = chartInfo.getString("column-x");
		
		//campos para fecha_inicio y fecha_fin, de 2 en 2 para cada serie
		String coly = chartInfo.getString("column-y");
		String dataCols[] = StringUtil.split(coly, ";");
		
		//alimentar la data del grafico
		for (int i = 0; i < seriesNames.length; i++) {
			TaskSeries s = new TaskSeries(seriesNames[i]);
			data.top();
			while (data.next()) {
		        s.add(
		        		new Task(
		        				data.getString(colx),
		        				new SimpleTimePeriod( data.getDate(dataCols[(2*i)]), data.getDate(dataCols[(2*i) + 1]) ) 
		        		) 
		        );
			}
			collection.add(s);
		}
		
		
		JFreeChart chart = ChartFactory.createGanttChart(
				chartInfo.getString("title"),  // chart title
	            titlex,              // domain axis label
	            chartInfo.getString("title-y"), // range axis label
	            collection,             // data
	            true,                // include legend
	            false,                // tooltips
	            false                // urls
	        );
		

			configurePlot(chart.getPlot());
		
	        return chart;		
		
	}

	@Override
	public void configurePlot(Plot p) {
		CategoryPlot plot = (CategoryPlot) p;
		DateAxis rangeAxis = (DateAxis) plot.getRangeAxis();
		try {
			rangeAxis.setDateFormatOverride(new SimpleDateFormat( chartInfo.getString("labelx-format") ));
	        rangeAxis.setUpperMargin(0.20);
	        rangeAxis.setVerticalTickLabels(true);
			DateTickUnit tu = new DateTickUnit(DateTickUnitType.DAY, chartInfo.getInt("tick-unit"));
			rangeAxis.setTickUnit(tu);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}
