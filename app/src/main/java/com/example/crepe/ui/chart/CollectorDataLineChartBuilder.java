package com.example.crepe.ui.chart;

import android.content.Context;

import com.example.crepe.database.Collector;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class CollectorDataLineChartBuilder {
    LineChart lineChart;
    Collector collector;
    Context context;



    public CollectorDataLineChartBuilder(Context context, Collector collector) {
        this.lineChart = new LineChart(context);
        this.collector = collector;
    }

    public LineChart build() {

        // fake some data
        Integer dataPointNum = 10;
        List<Entry> entries = new ArrayList<Entry>();

        float max = 5;
        float min = 0.5F;

        for (Integer i = 0; i < dataPointNum; i++) {
            // turn data into Entry objects
            Random rand = new Random();

            float randomValue = rand.nextFloat() * (max - min) + min;

            entries.add(new Entry( (float) i, randomValue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Label"); // add entries to dataset

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setLabelCount(5);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // only intervals of 1 day

        ValueFormatter xAxisFormatter = new DayAxisValueFormatter(lineChart);
        xAxis.setValueFormatter(xAxisFormatter);
        // set the min height of the chart
        lineChart.setMinimumHeight(1000);
        // set the description
        Description chartDescription = lineChart.getDescription();
        chartDescription.setText("The Amount of Data You Have Contributed");
        chartDescription.setTextSize(13);
        chartDescription.setPosition(850, 50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh

        return lineChart;

    }

    public class DayAxisValueFormatter extends ValueFormatter {
        private final LineChart chart;
        public DayAxisValueFormatter(LineChart chart) {
            this.chart = chart;
        }
        @Override
        public String getFormattedValue(float value) {
            return "Jan " + String.format("%.0f", value + 1);
        }
    }
}
