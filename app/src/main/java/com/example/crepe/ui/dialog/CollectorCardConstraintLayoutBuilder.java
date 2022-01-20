package com.example.crepe.ui.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.crepe.R;
import com.example.crepe.ui.main_activity.CollectorCard;

public class CollectorCardConstraintLayoutBuilder {
    private Context c;

    public CollectorCardConstraintLayoutBuilder(Context c) {
        this.c = c;
    }

    public ConstraintLayout build (CollectorCard card, ViewGroup rootView) {
        ConstraintLayout collector = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.collector_card, rootView, false);

        // get the app name textfield from the card and populate it with app name
        TextView appNameTextView = (TextView) collector.findViewById(R.id.collectorTitle);
        appNameTextView.setText(card.getAppName());
        //TODO: finish customize the layout based on info from card

        return collector;
    }

}
