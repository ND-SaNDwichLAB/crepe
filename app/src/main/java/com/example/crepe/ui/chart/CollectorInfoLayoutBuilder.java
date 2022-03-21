package com.example.crepe.ui.chart;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CollectorInfoLayoutBuilder {
    Context context;
    Map<String, Drawable> apps;

    public static final String DELETED = "deleted";
    public static final String DISABLED = "disabled";
    public static final String ACTIVE = "active";
    public static final String NOTYETSTARTED = "notYetStarted";
    public static final String EXPIRED = "expired";


    // we will use the following constructor more often, because we initialize
    public CollectorInfoLayoutBuilder(Context context, Map<String, Drawable> apps) {
        this.context = context;
        this.apps = apps;
    }

    public ConstraintLayout buildCollectorInfoView(Collector collector, ViewGroup containerLayout) {
        ConstraintLayout collectorInfoLayout = (ConstraintLayout) LayoutInflater.from(context).inflate(R.layout.collector_info_for_data_fragment, containerLayout, false);
        // get the app name text field from the card and populate it with app name
        TextView appNameTextView = (TextView) collectorInfoLayout.findViewById(R.id.collectorTitle);
        appNameTextView.setText(collector.getAppName());

        TextView collectorDescriptionTextView = (TextView) collectorInfoLayout.findViewById(R.id.collectorDataDescription);
        collectorDescriptionTextView.setText(collector.getDescription());

        TextView scheduleStartTextView = (TextView) collectorInfoLayout.findViewById(R.id.startTime);
        scheduleStartTextView.setText("Started: "+collector.getCollectorStartTimeString());
        TextView scheduleEndTextView = (TextView) collectorInfoLayout.findViewById(R.id.endTime);
        scheduleEndTextView.setText("Scheduled end: "+collector.getCollectorEndTimeString());

        // get the app status and display it
        ImageView collectorStatusImg = (ImageView) collectorInfoLayout.findViewById(R.id.statusImageView);
        TextView collectorStatusTxt = (TextView) collectorInfoLayout.findViewById(R.id.statusText);
        if (collector.getCollectorStatus().equals(ACTIVE)){
            collectorStatusTxt.setText("Running");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_24_green);
        } else if (collector.getCollectorStatus().equals(DISABLED)){
            collectorStatusTxt.setText("Disabled");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_grey);
        } else if (collector.getCollectorStatus().equals(EXPIRED)){
            collectorStatusTxt.setText("Expired");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_grey);
        } else {
            collectorStatusTxt.setText("Not yet started");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_yellow);
        }

        // get App logo
        ImageView appImg = (ImageView) collectorInfoLayout.findViewById(R.id.collectorImg);
        Drawable appImage = apps.get(collector.getAppName());
        if (appImage == null){
            appImg.setImageResource(R.drawable.nd_logo);
        } else {
            appImg.setImageDrawable(appImage);
        }

        return collectorInfoLayout;
    }

    // the parameter width is the screen width, used to position the chart properly
    public LinearLayout buildChart(Collector collector) {
        LineChart lineChart;
        lineChart = new LineChart(context);

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


        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        if(lineChart.getParent() != null) {
            ((ViewGroup) lineChart.getParent()).removeView(lineChart);
        }
        linearLayout.addView(lineChart);

        return linearLayout;
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

    public Pair<TextView, LinearLayout.LayoutParams> buildSampleDataPieceTitle() {
        TextView sampleDataTitle = new TextView(context);
        sampleDataTitle.setText("Sample of Collected Data");
        sampleDataTitle.setTextColor(Color.parseColor("#1C2B34"));
        sampleDataTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams sampleDataTitleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sampleDataTitleParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        sampleDataTitleParams.setMargins(0, 120, 0, 0);
        return new Pair<>(sampleDataTitle, sampleDataTitleParams);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Pair<TextView, LinearLayout.LayoutParams> buildSampleDataPiece(Collector collector) {
        String sampleData = "{\n" +
                "\t\tu_id: 0038291,\n" +
                "\t\ttime: 01012021, \n" +
                "\t\tprice: 2.09, \n" +
                "\t\tdestination: \"2098 Murray Ave\",\n" +
                "\t\tstart: \"220 Fifth Ave\", \n" +
                "\t\tduration: 10 min \n}";
        TextView sampleDataText = new TextView(context);
        sampleDataText.setText((CharSequence) sampleData);
        sampleDataText.setTypeface(context.getResources().getFont(R.font.courier_prime_regular));
        LinearLayout.LayoutParams sampleDataContentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sampleDataContentParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        sampleDataContentParams.setMargins(70, 20, 0, 0);

        return new Pair<>(sampleDataText, sampleDataContentParams);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public LinearLayout build(Collector collector) {

        // if the collector is in deleted status, don't display anything
        if(collector.isDeleted()) {
            return null;
        }

        LinearLayout containerLayout = new LinearLayout(context);
        containerLayout.setOrientation(LinearLayout.VERTICAL);

        ConstraintLayout collectorInfoViewLayout = buildCollectorInfoView(collector, containerLayout);
        collectorInfoViewLayout.setId(View.generateViewId());
        containerLayout.addView(collectorInfoViewLayout);

        Pair<TextView, LinearLayout.LayoutParams> chartTitlePair = buildChartTitle();
        chartTitlePair.first.setId(View.generateViewId());
        containerLayout.addView(chartTitlePair.first, chartTitlePair.second);


        Pair<TextView, LinearLayout.LayoutParams> chartYAxisLabelPair = buildChartYAxisLabels();
        chartYAxisLabelPair.first.setId(View.generateViewId());
        containerLayout.addView(chartYAxisLabelPair.first, chartYAxisLabelPair.second);

        LinearLayout lineChart = buildChart(collector);

        lineChart.setId(View.generateViewId());
        containerLayout.addView(lineChart);


        Pair<TextView, LinearLayout.LayoutParams> sampleDataTitlePair = buildSampleDataPieceTitle();
        sampleDataTitlePair.first.setId(View.generateViewId());
        containerLayout.addView(sampleDataTitlePair.first, sampleDataTitlePair.second);

        Pair<TextView, LinearLayout.LayoutParams> sampleDataPiecePair = buildSampleDataPiece(collector);
        sampleDataPiecePair.first.setId(View.generateViewId());
        containerLayout.addView(sampleDataPiecePair.first, sampleDataPiecePair.second);



        return containerLayout;
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
