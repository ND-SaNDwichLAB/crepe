package com.example.crepe.ui.chart;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.crepe.database.Collector;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;


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
        this.context = context;
    }

    // the parameter width is the screen width, used to position the chart properly
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
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);
        dataSet.setColor(Color.parseColor("#223651"));

        // set attributes for x axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setLabelCount(5);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.parseColor("#1C2B34"));
        xAxis.setDrawGridLines(false);
        xAxis.setGridLineWidth(0);
        xAxis.setGranularity(1f); // only intervals of 1 day

        ValueFormatter xAxisFormatter = new DayAxisValueFormatter(lineChart);
        xAxis.setValueFormatter(xAxisFormatter);

        // set attributes for y axis
        YAxis yAxisLeft = lineChart.getAxisLeft();
        YAxis yAxisRight = lineChart.getAxisRight();
        yAxisLeft.setGridLineWidth(1f);
        yAxisLeft.setGridColor(Color.parseColor("#DADADA"));
        yAxisRight.setDrawLabels(false);
        yAxisLeft.setDrawAxisLine(false);
        yAxisRight.setDrawAxisLine(false);
        yAxisLeft.setTextSize(10f);
        yAxisLeft.setTextColor(Color.parseColor("#1C2B34"));


        // set the min height of the chart
        lineChart.setMinimumHeight(500);
        // clear the description, use a textLayout for the title
        lineChart.setDescription(null);
        // disable interaction with the chart
        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);

        lineChart.setNoDataText("There's not data to be displayed currently.");

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.setExtraOffsets(40, 0, 40, 20);

        lineChart.getLegend().setEnabled(false);

        lineChart.invalidate(); // refresh

        return lineChart;

    }

    public Pair<TextView, LinearLayout.LayoutParams> buildChartYAxisLabels() {
        TextView yAxisLabel = new TextView(context);
        yAxisLabel.setText("Daily Data \n (MB)");
        yAxisLabel.setTextSize(10f);
        yAxisLabel.setTextColor(Color.parseColor("#1C2B34"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        params.setMargins(70, 100, 0, 0);

        return new Pair<>(yAxisLabel, params);
    }

    public Pair<TextView, LinearLayout.LayoutParams> buildChartTitle() {
        TextView chartTitle = new TextView(context);
        chartTitle.setText("Daily Volume of Collected Data");
        chartTitle.setTextSize(14f);
        chartTitle.setTypeface(null, Typeface.BOLD);
        chartTitle.setTextColor(Color.parseColor("#1C2B34"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        params.setMargins(0, 120, 0, -50);

        return new Pair<>(chartTitle, params);

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
