package com.example.crepe.ui.main_activity;

import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
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
import com.example.crepe.ui.chart.CollectorInfoLayoutBuilder;
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

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onStart() {
        super.onStart();
        initCollectorInfoLayout();

    }

    // function to init collector information from database on data fragment
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void initCollectorInfoLayout() {
        CollectorInfoLayoutBuilder collectorInfoLayoutBuilder = new CollectorInfoLayoutBuilder(getContext());

        LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.data_fragment_inner_linear_layout);


        for(Collector collector: collectorList) {
            // display basic info for the collector
            LinearLayout collectorDetailView = collectorInfoLayoutBuilder.build(collector);

            // if the collectorDetailView is not null, meaning it's not in deleted status
            if (collectorDetailView != null) {
                collectorDetailView.setId(View.generateViewId());
                fragmentInnerLinearLayout.addView(collectorDetailView);
            }   // otherwise don't display anything


        }
    }


}
