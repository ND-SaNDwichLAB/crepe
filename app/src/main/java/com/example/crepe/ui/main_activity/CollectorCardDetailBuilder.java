package com.example.crepe.ui.main_activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;

public class CollectorCardDetailBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;

    public CollectorCardDetailBuilder(Context c, Collector collector, Runnable refreshCollectorListRunnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.collector = collector;
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = new DatabaseManager(c);
    }

    public Dialog build() {
        final View popupView = LayoutInflater.from(c).inflate(R.layout.collector_detail, null);
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();

        TextView collectorDataField = (TextView) popupView.findViewById(R.id.collectorDetailDataField);
        Button closeBtn = (Button) popupView.findViewById(R.id.collectorCloseButton);
        Button deleteBtn = (Button) popupView.findViewById(R.id.collectorDeleteButton);
        Switch enableSwitch = (Switch) popupView.findViewById(R.id.collectorStatusSwitch);
        if(collector.getCollectorStatus().equals("disabled")){
            enableSwitch.setChecked(false);
            enableSwitch.setText("Disabled");
        } else{
            enableSwitch.setChecked(true);
        }

        // TODO Yuwen figure out what to do here
//        collectorDataField.setText(collector.getDataFieldsToString());


        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(c, "Collector (ID:" + collector.getCollectorId() + ") is deleted", Toast.LENGTH_LONG).show();
                // This will only set the status of collector to deleted,
                // it will still be present in database but won't be displayed
                collector.deleteCollector();
                dbManager.updateCollectorStatus(collector);

                // update the home fragment list
                refreshCollectorListRunnable.run();
                dialog.dismiss();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enableSwitch.isChecked()){
                    collector.activateCollector();
                    dbManager.updateCollectorStatus(collector);
                } else {
                    collector.disableCollector();
                    dbManager.updateCollectorStatus(collector);
                }
                // update the home fragment list
                refreshCollectorListRunnable.run();
                dialog.dismiss();
            }
        });

        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Commented the following block out, don't feel it's necessary because the collector status is updated at the closeBtn onclicklistener
                if (!isChecked){
                    enableSwitch.setText("Disabled");
                } else {
                    enableSwitch.setText("Enabled");
                }
            }
        });


        return dialog;
    }

}








