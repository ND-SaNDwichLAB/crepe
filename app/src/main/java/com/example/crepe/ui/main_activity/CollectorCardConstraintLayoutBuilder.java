package com.example.crepe.ui.main_activity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.crepe.R;
import com.example.crepe.database.Collector;

public class CollectorCardConstraintLayoutBuilder {
    private Context c;
    private TextView appNameTextView;
    private TextView collectorDescriptionTextView;
    private TextView scheduleStartTextView;
    private TextView scheduleEndTextView;

    public CollectorCardConstraintLayoutBuilder(Context c) {
        this.c = c;
    }

    public ConstraintLayout build(Collector collector, ViewGroup rootView) {
        ConstraintLayout collectorLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.collector_card, rootView, false);

        // get the app name textfield from the card and populate it with app name
        appNameTextView = (TextView) collectorLayout.findViewById(R.id.collectorTitle);
        appNameTextView.setText(collector.getAppName());

        collectorDescriptionTextView = (TextView) collectorLayout.findViewById(R.id.collectorDataDescription);
        collectorDescriptionTextView.setText(collector.getDescription());

        //TODO: finish customize the layout based on info from card

        return collectorLayout;
    }

}
