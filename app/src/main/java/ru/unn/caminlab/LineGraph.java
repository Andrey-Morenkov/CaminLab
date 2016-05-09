package ru.unn.caminlab;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;


public class LineGraph
{
    public Intent getIntent(Context context)
    {
        double x[] = {8.30, 9.00, 9.30, 10.00,10.30,11.00,11.30};
        double y[] = {20.5, 20.8, 21.5, 22.2, 23.1, 22.8, 22.1 };

        TimeSeries series = new TimeSeries("Temp");
        for(int i=0; i < x.length; i++)
        {
            series.add(x[i],y[i]);
        }

        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(series);

        XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        mRenderer.addSeriesRenderer(renderer);


        renderer.setColor(Color.MAGENTA);
        renderer.setAnnotationsTextSize(10);
        renderer.setPointStyle(PointStyle.CIRCLE);
        renderer.setFillPoints(true);
        renderer.setLineWidth(3);

        mRenderer.setXAxisMin(x[0]-1);
        mRenderer.setYAxisMin(y[0]-1);

        mRenderer.setPointSize(5f);
        mRenderer.setShowGrid(true);
        mRenderer.setGridColor(Color.BLACK);
        mRenderer.setXTitle("Hours");
        mRenderer.setYTitle("Temp");
        mRenderer.setAxisTitleTextSize(50f);
        mRenderer.setGridLineWidth(0.2f);
        mRenderer.setLabelsTextSize(50f);
        mRenderer.setBackgroundColor(Color.DKGRAY);
        mRenderer.setApplyBackgroundColor(true);
        mRenderer.setShowLegend(false);

        Intent intent = ChartFactory.getLineChartIntent(context,dataset,mRenderer,"Temp Graph");

        return intent;
    }

}
