package com.example.crepe.ui.main_activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

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
            collectorInfoView.setId(View.generateViewId());
            fragmentInnerLinearLayout.addView(collectorInfoView);

            // add the data vis for the corresponding collector
            // programmatically create a LineChart
            CollectorDataLineChartBuilder chartBuilder = new CollectorDataLineChartBuilder(this.getContext(), collector);
            LineChart lineChart = chartBuilder.build();

            fragmentInnerLinearLayout.addView(lineChart); // add the programmatically created chart
        }
    }


}
