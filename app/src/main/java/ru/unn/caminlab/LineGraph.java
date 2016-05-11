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
import java.util.Random;
import java.util.Calendar;


public class LineGraph {
    public static final int StatCount = 95;

    public Intent getIntent(Context context)
    {
        double x[] = new double[StatCount];
        double y[] = new double[StatCount];

        Calendar c = Calendar.getInstance();
        int currH = c.getTime().getHours();
        int currM = c.getTime().getMinutes();

        Random rnd = new Random(System.currentTimeMillis());


        for (int i = 0; i < 95; i++)
        {
            x[i] = 15 + (30 - 15) * rnd.nextDouble();
        }

        currM = currM / 15; // Чекпоинт

        int m = 0;

        for(int i = currH; i > (currH-23)-1; i--)
        {
            if (m <= 94)
            {
                if(i==currH)
                {
                    for (int j = currM; j >= 0; j--)
                    {
                        y[m] = TimeBuilder(i,j);
                        m++;
                    }
                }
                else
                {
                    for (int j = 3; j >= 0; j--)
                    {
                        y[m] = TimeBuilder(i,j);
                        m++;
                    }
                }
            }
        }


        //double x[] = {8.30, 9.00, 9.30, 10.00,10.30,11.00,11.30};
        //double y[] = {20.5, 20.8, 21.5, 22.2, 23.1, 22.8, 22.1 };

        TimeSeries series = new TimeSeries("Temp");              // разобраться с графиком
        for (int i = 0; i < x.length; i++)
        {
            series.add(x[i], y[i]);
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

        mRenderer.setXAxisMin(x[0] - 1);
        mRenderer.setYAxisMin(y[0] - 1);

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

        Intent intent = ChartFactory.getLineChartIntent(context, dataset, mRenderer, "Temp Graph");

        return intent;
    }

    double TimeBuilder(int hours, int minmnoj)
    {
        double tmp=0.0;
        double hours1 = (double)hours;
        double minmnoj1 = (double) minmnoj;

        tmp = hours1 + (0.01*(minmnoj1*15));
        if(tmp < 0)
        {
            tmp = (24+hours) + (0.1*(minmnoj*15));
        }
        return tmp;
    }
}
