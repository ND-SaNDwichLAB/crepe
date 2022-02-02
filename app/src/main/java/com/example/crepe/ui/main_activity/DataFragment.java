package com.example.crepe.ui.main_activity;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.ui.chart.CollectorDataLineChartBuilder;
import com.github.mikephil.charting.charts.LineChart;

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
    public void initCollectorInfoLayout() {
        CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(getActivity());

        LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.data_fragment_inner_linear_layout);

        for(Collector collector: collectorList) {
            // display basic info for the collector
            ConstraintLayout collectorInfoView = builder.build(collector, fragmentInnerLinearLayout, "infoLayout");

            // add extra margin to the top of the view
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 55, 0, 0);

            collectorInfoView.setId(View.generateViewId());
            fragmentInnerLinearLayout.addView(collectorInfoView, params);

            // add the data vis for the corresponding collector
            // programmatically create a LineChart

            // first, get the width of the page so the chart can be properly positioned
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int width = displayMetrics.widthPixels;

            CollectorDataLineChartBuilder chartBuilder = new CollectorDataLineChartBuilder(this.getContext(), getActivity(), collector);

            // add y axis label and chart title
            Pair<TextView, LinearLayout.LayoutParams> lineChartTitle = chartBuilder.buildChartTitle();
            fragmentInnerLinearLayout.addView(lineChartTitle.first, lineChartTitle.second);
            Pair<TextView, LinearLayout.LayoutParams> lineChartYAxisLabel = chartBuilder.buildChartYAxisLabels();
            fragmentInnerLinearLayout.addView(lineChartYAxisLabel.first, lineChartYAxisLabel.second);

            // add the chart itself
            LineChart lineChart = chartBuilder.build();

            fragmentInnerLinearLayout.addView(lineChart); // add the programmatically created chart
        }
    }


}
