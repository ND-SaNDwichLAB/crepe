package com.example.crepe.ui.main_activity;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.ui.chart.CollectorDataLineChartBuilder;
import com.github.mikephil.charting.charts.LineChart;

import org.w3c.dom.Text;

import java.util.List;

public class DataFragment extends Fragment {

    private DatabaseManager dbManager;
    private List<Collector> collectorList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbManager = new DatabaseManager(this.getActivity());
        collectorList = dbManager.getAllCollectors();
        return inflater.inflate(R.layout.fragment_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("View My Data");

        // load collector information from database
        initCollectorInfoLayout();
    }

    // function to init collector information from database on data fragment
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void initCollectorInfoLayout() {
        CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(getActivity());

        LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.data_fragment_inner_linear_layout);

        for(Collector collector: collectorList) {
            // display basic info for the collector
            ConstraintLayout collectorInfoView = builder.build(collector, fragmentInnerLinearLayout, "infoLayout");

            // add extra margin to the top of the view
            LinearLayout.LayoutParams collectorInfoParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            collectorInfoParams.setMargins(0, 55, 0, 0);

            collectorInfoView.setId(View.generateViewId());
            fragmentInnerLinearLayout.addView(collectorInfoView, collectorInfoParams);

            // add the data vis for the corresponding collector
            // programmatically create a LineChart

            CollectorDataLineChartBuilder chartBuilder = new CollectorDataLineChartBuilder(this.getContext(), getActivity(), collector);

            // add y axis label and chart title
            Pair<TextView, LinearLayout.LayoutParams> lineChartTitle = chartBuilder.buildChartTitle();
            lineChartTitle.first.setId(View.generateViewId());
            fragmentInnerLinearLayout.addView(lineChartTitle.first, lineChartTitle.second);
            Pair<TextView, LinearLayout.LayoutParams> lineChartYAxisLabel = chartBuilder.buildChartYAxisLabels();
            lineChartYAxisLabel.first.setId(View.generateViewId());
            fragmentInnerLinearLayout.addView(lineChartYAxisLabel.first, lineChartYAxisLabel.second);

            // add the chart itself
            LineChart lineChart = chartBuilder.build();
            lineChart.setId(View.generateViewId());
            fragmentInnerLinearLayout.addView(lineChart);

            // add a sample data piece for the corresponding collector
            // First, add section title
            TextView sampleDataTitle = new TextView(getActivity());
            sampleDataTitle.setText("Sample of Collected Data");
            sampleDataTitle.setTextColor(Color.parseColor("#1C2B34"));
            sampleDataTitle.setTypeface(null, Typeface.BOLD);

            sampleDataTitle.setId(View.generateViewId());
            LinearLayout.LayoutParams sampleDataTitleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sampleDataTitleParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;
            sampleDataTitleParams.setMargins(0, 120, 0, 0);
            fragmentInnerLinearLayout.addView(sampleDataTitle, sampleDataTitleParams);

            // this is example hard-coded string, needs to be changed later
            String sampleData = "{\n" +
                    "\t\tu_id: 0038291,\n" +
                    "\t\ttime: 01012021, \n" +
                    "\t\tprice: 2.09, \n" +
                    "\t\tdestination: \"2098 Murray Ave\",\n" +
                    "\t\tstart: \"220 Fifth Ave\", \n" +
                    "\t\tduration: 10 min \n}";
            TextView sampleDataText = new TextView(getActivity());
            sampleDataText.setText((CharSequence) sampleData);
            sampleDataText.setTypeface(getResources().getFont(R.font.courier_prime_regular));
            sampleDataText.setId(View.generateViewId());

            LinearLayout.LayoutParams sampleDataContentParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sampleDataContentParams.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
            sampleDataContentParams.setMargins(70, 20, 0, 0);
            fragmentInnerLinearLayout.addView(sampleDataText, sampleDataContentParams);
        }
    }


}
