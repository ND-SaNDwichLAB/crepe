package edu.nd.crepe.ui.chart;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
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

import static edu.nd.crepe.database.Collector.ACTIVE;
import static edu.nd.crepe.database.Collector.EXPIRED;

// TODO:
//  1. calculate the size of all collected data of all time
//  2. sum the size of all collected data of one day
//  3. display the latest collected data

public class CollectorInfoLayoutBuilder {
    Context context;
    Map<String, Drawable> apps;
    private int textColor;
    private int gridColor;
    private int lineColor;
    private int cardTextColor;  // New color specifically for text inside the card


    public CollectorInfoLayoutBuilder(Context context, Map<String, Drawable> apps) {
        this.context = context;
        this.apps = apps;

        // Get system colors based on current theme
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        textColor = ContextCompat.getColor(context, typedValue.resourceId);

        // Always keep card text dark since card background is white
        cardTextColor = Color.parseColor("#1C2B34");  // Dark text color for card content

        // Get grid color based on theme
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        gridColor = isDarkMode ? Color.parseColor("#444444") : Color.parseColor("#DADADA");
        lineColor = isDarkMode ? Color.parseColor("#81D4FA") : Color.parseColor("#223651");
    }

    public ConstraintLayout buildCollectorInfoView(Collector collector, ViewGroup containerLayout) {
        ConstraintLayout collectorInfoLayout = (ConstraintLayout) LayoutInflater.from(context)
                .inflate(R.layout.collector_info_for_data_fragment, containerLayout, false);

        // get the app name text field from the card and populate it with app name
        TextView appNameTextView = collectorInfoLayout.findViewById(R.id.collectorTitle);
        appNameTextView.setText(collector.getAppName());
        appNameTextView.setTextColor(cardTextColor);

        TextView collectorDescriptionTextView = collectorInfoLayout.findViewById(R.id.collectorDataDescription);
        collectorDescriptionTextView.setText(collector.getDescription());
        collectorDescriptionTextView.setTextColor(cardTextColor);

        TextView scheduleStartTextView = collectorInfoLayout.findViewById(R.id.startTime);
        scheduleStartTextView.setText("Started: " + collector.getCollectorStartTimeString());
        scheduleStartTextView.setTextColor(cardTextColor);

        TextView scheduleEndTextView = collectorInfoLayout.findViewById(R.id.endTime);
        scheduleEndTextView.setText("Scheduled end: " + collector.getCollectorEndTimeString());
        scheduleEndTextView.setTextColor(cardTextColor);

        ImageView collectorStatusImg = collectorInfoLayout.findViewById(R.id.statusImageView);
        TextView collectorStatusTxt = collectorInfoLayout.findViewById(R.id.statusText);
        collectorStatusTxt.setTextColor(cardTextColor);

        if (collector.getCollectorStatus().equals(ACTIVE)) {
            collectorStatusTxt.setText("Running");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_24_green);
        } else if (collector.getCollectorStatus().equals(EXPIRED)) {
            collectorStatusTxt.setText("Expired");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_grey);
        } else {
            collectorStatusTxt.setText("Not yet started");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_yellow);
        }

        ImageView appImg = collectorInfoLayout.findViewById(R.id.collectorImg);
        Drawable appImage = apps.get(collector.getAppName());
        if (appImage == null) {
            appImg.setImageResource(R.drawable.nd_logo);
        } else {
            appImg.setImageDrawable(appImage);
        }

        return collectorInfoLayout;
    }


    public LinearLayout buildChart(Collector collector) {
        LineChart lineChart = new LineChart(context);

        // fake some data
        Integer dataPointNum = 10;
        List<Entry> entries = new ArrayList<>();

        float max = 5;
        float min = 0.5F;

        for (Integer i = 0; i < dataPointNum; i++) {
            Random rand = new Random();
            float randomValue = rand.nextFloat() * (max - min) + min;
            entries.add(new Entry((float) i, randomValue));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Label");
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);
        dataSet.setColor(lineColor);

        // set attributes for x axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setLabelCount(5);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(textColor);
        xAxis.setDrawGridLines(false);
        xAxis.setGridLineWidth(0);
        xAxis.setGranularity(1f);

        ValueFormatter xAxisFormatter = new DayAxisValueFormatter(lineChart);
        xAxis.setValueFormatter(xAxisFormatter);

        // set attributes for y axis
        YAxis yAxisLeft = lineChart.getAxisLeft();
        YAxis yAxisRight = lineChart.getAxisRight();
        yAxisLeft.setGridLineWidth(1f);
        yAxisLeft.setGridColor(gridColor);
        yAxisRight.setDrawLabels(false);
        yAxisLeft.setDrawAxisLine(false);
        yAxisRight.setDrawAxisLine(false);
        yAxisLeft.setTextSize(10f);
        yAxisLeft.setTextColor(textColor);

        lineChart.setMinimumHeight(500);
        lineChart.setDescription(null);
        lineChart.setTouchEnabled(false);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setNoDataText("There's no data to be displayed currently.");
        lineChart.setNoDataTextColor(textColor);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.setExtraOffsets(40, 0, 40, 20);
        lineChart.getLegend().setEnabled(false);
        lineChart.invalidate();

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        if (lineChart.getParent() != null) {
            ((ViewGroup) lineChart.getParent()).removeView(lineChart);
        }
        linearLayout.addView(lineChart);

        return linearLayout;
    }

    public Pair<TextView, LinearLayout.LayoutParams> buildChartYAxisLabels() {
        TextView yAxisLabel = new TextView(context);
        yAxisLabel.setText("Daily Data \n (MB)");
        yAxisLabel.setTextSize(10f);
        yAxisLabel.setTextColor(textColor);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        params.setMargins(70, 100, 0, 0);

        return new Pair<>(yAxisLabel, params);
    }

    public Pair<TextView, LinearLayout.LayoutParams> buildChartTitle() {
        TextView chartTitle = new TextView(context);
        chartTitle.setText("Daily Volume of Collected Data");
        chartTitle.setTextSize(14f);
        chartTitle.setTypeface(null, Typeface.BOLD);
        chartTitle.setTextColor(textColor);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        params.setMargins(0, 120, 0, -50);

        return new Pair<>(chartTitle, params);
    }

    public Pair<TextView, LinearLayout.LayoutParams> buildSampleDataPieceTitle() {
        TextView sampleDataTitle = new TextView(context);
        sampleDataTitle.setText("Sample of Collected Data");
        sampleDataTitle.setTextColor(textColor);
        sampleDataTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
        params.setMargins(0, 120, 0, 0);
        return new Pair<>(sampleDataTitle, params);
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
        sampleDataText.setText(sampleData);
        sampleDataText.setTypeface(context.getResources().getFont(R.font.courier_prime_regular));
        sampleDataText.setTextColor(textColor);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        params.setMargins(70, 20, 0, 0);

        return new Pair<>(sampleDataText, params);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public LinearLayout build(Collector collector) {
        // if the collector is in deleted status, don't display anything
        if (collector.isDeleted()) {
            return null;
        }

        // set the layout params for the linear layout
        LinearLayout containerLayout = new LinearLayout(context);
        containerLayout.setOrientation(LinearLayout.VERTICAL);

        // build collector view
        ConstraintLayout collectorInfoViewLayout = buildCollectorInfoView(collector, containerLayout);
        collectorInfoViewLayout.setId(View.generateViewId());
        containerLayout.addView(collectorInfoViewLayout);

        // chart title
        Pair<TextView, LinearLayout.LayoutParams> chartTitlePair = buildChartTitle();
        chartTitlePair.first.setId(View.generateViewId());
        containerLayout.addView(chartTitlePair.first, chartTitlePair.second);

        // pair y axis label with its title
        Pair<TextView, LinearLayout.LayoutParams> chartYAxisLabelPair = buildChartYAxisLabels();
        chartYAxisLabelPair.first.setId(View.generateViewId());
        containerLayout.addView(chartYAxisLabelPair.first, chartYAxisLabelPair.second);

        // build chart
        LinearLayout lineChart = buildChart(collector);
        lineChart.setId(View.generateViewId());
        containerLayout.addView(lineChart);

        // sample data piece title
        Pair<TextView, LinearLayout.LayoutParams> sampleDataTitlePair = buildSampleDataPieceTitle();
        sampleDataTitlePair.first.setId(View.generateViewId());
        containerLayout.addView(sampleDataTitlePair.first, sampleDataTitlePair.second);

        // sample data piece content
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

    // calculate collected data size (in GB) for each day
    public List<Entry> getCollectedDataSizeByDay(Collector collector) {
        List<Entry> collectedDataSizeByDay = new ArrayList<>();
        return collectedDataSizeByDay;
    }
}