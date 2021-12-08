package com.example.crepe.ui.main_activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;

import java.util.List;

public class HomeFragment extends Fragment {

    private DatabaseManager dbManager;
    private ConstraintLayout fragmentInnerConstraintLayout;
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

        fragmentInnerConstraintLayout = getView().findViewById(R.id.fragment_home_inner_constraint_layout);

        View collector = getLayoutInflater().inflate(R.layout.collector_card, null);
        Toast.makeText(this.getActivity(), fragmentInnerConstraintLayout.toString(), Toast.LENGTH_LONG).show();
        fragmentInnerConstraintLayout.addView(collector);


    }


}
