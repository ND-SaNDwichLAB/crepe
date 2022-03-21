package com.example.crepe.ui.main_activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;

import java.util.List;

public class HomeFragment extends Fragment {

    private DatabaseManager dbManager;
    private List<Collector> collectorList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbManager = new DatabaseManager(this.getActivity());

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle("Crepe");
        initCollectorList();
    }

    public void initCollectorList() {
        collectorList = dbManager.getAllCollectors();
        Toast.makeText(this.getActivity(), "Collector number: " + collectorList.size(), Toast.LENGTH_LONG).show();
        CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(getActivity(), homeFragmentRefreshCollectorListRunnable);
        LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.fragment_home_inner_linear_layout);
        fragmentInnerLinearLayout.removeAllViews();
        for (Collector collector : collectorList) {

                ConstraintLayout collectorCardView = builder.build(collector, fragmentInnerLinearLayout, "cardLayout");
                // if the cardView is not null, meaning the collector is not in deleted status
                if(collectorCardView != null) {
                    collectorCardView.setId(View.generateViewId());

                    // Toast.makeText(this.getActivity(), fragmentInnerConstraintLayout.toString(), Toast.LENGTH_LONG).show();
                    fragmentInnerLinearLayout.addView(collectorCardView);
                }


        }


    }

    Runnable homeFragmentRefreshCollectorListRunnable = new Runnable() {
        @Override
        public void run() {
            initCollectorList();
        }
    };




}
