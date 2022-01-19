package com.example.crepe.ui.main_activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.ui.dialog.CollectorCardConstraintLayoutBuilder;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private DatabaseManager dbManager;
    private List<Collector> collectorList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // TODO: get all elements from database, use them to create collectorCard
        dbManager = new DatabaseManager(this.getActivity());
        collectorList = dbManager.getAllCollectors();
        Toast.makeText(this.getActivity(), "Collector number: " + collectorList.size(), Toast.LENGTH_LONG).show();

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initCollectorList();

    }

    private void initCollectorList() {
        CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(getActivity());
        LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.fragment_home_inner_linear_layout);

        List<CollectorCard> cards = new ArrayList<>();
        //TODO: First get them from the DB

        CollectorCard collectorCard1 = new CollectorCard("test1", "Test 1", 1, 0);
        CollectorCard collectorCard2 = new CollectorCard("test2", "Test 2", 1, 0);
        cards.add(collectorCard1);
        cards.add(collectorCard2);

        for (CollectorCard collectorCard : cards) {
            ConstraintLayout collectorCardView = builder.build(collectorCard, fragmentInnerLinearLayout);
            collectorCardView.setId(View.generateViewId());

            // Toast.makeText(this.getActivity(), fragmentInnerConstraintLayout.toString(), Toast.LENGTH_LONG).show();
            fragmentInnerLinearLayout.addView(collectorCardView);
            // TODO: add proper constraints programmatically (https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintSet)

        }

    }




}
