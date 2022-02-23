package com.example.crepe.ui.main_activity;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
    private ConstraintLayout collectorLayout;
    private TextView collectorStatusTxt;
    private ImageView collectorStatusImg;

    public CollectorCardConstraintLayoutBuilder(Context c) {
        this.c = c;
    }

    public ConstraintLayout build(Collector collector, ViewGroup rootView, String layoutType) {

        if(layoutType == "cardLayout") {
            // if for home fragment, build a card layout
            collectorLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.collector_card, rootView, false);
        } else {
            // if for data fragment, build a info layout without a card appearance
            collectorLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.collector_info, rootView, false);
        }

        // get the app name text field from the card and populate it with app name
        appNameTextView = (TextView) collectorLayout.findViewById(R.id.collectorTitle);
        appNameTextView.setText(collector.getAppName());

        collectorDescriptionTextView = (TextView) collectorLayout.findViewById(R.id.collectorDataDescription);
        collectorDescriptionTextView.setText(collector.getDescription());

        scheduleStartTextView = (TextView) collectorLayout.findViewById(R.id.startTime);
        scheduleStartTextView.setText("Started: "+collector.getCollectorStartTimeString());
        scheduleEndTextView = (TextView) collectorLayout.findViewById(R.id.endTime);
        scheduleEndTextView.setText("Scheduled end: "+collector.getCollectorEndTimeString());

        // get the app status and display it
        collectorStatusImg = (ImageView) collectorLayout.findViewById(R.id.runningLightImageView);
        collectorStatusTxt = (TextView) collectorLayout.findViewById(R.id.collectorStatusText);
        if (collector.getCollectorStatus() == "running"){
            collectorStatusTxt.setText("Running");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_24_green);
        } else if (collector.getCollectorStatus() == "disabled"){
            collectorStatusTxt.setText("Disabled");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_grey);
        } else if (collector.getCollectorStatus() == "expired"){
            collectorStatusTxt.setText("Expired");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_yellow);
        } else {
            collectorStatusTxt.setText("Not yet started");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_yellow);
        }


        Button detailBtn = (Button) collectorLayout.findViewById(R.id.detailButton);

        detailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CollectorCardDetailBuilder cardDetailBuilder = new CollectorCardDetailBuilder(c, collector);
                Dialog newDialog = cardDetailBuilder.build();
                newDialog.show();

            }
        });

        //TODO: finish customize the layout based on info from card


        return collectorLayout;
    }

}
