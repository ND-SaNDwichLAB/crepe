package com.example.crepe.ui.main_activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

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

    public CollectorCardDetailBuilder(Context c, Collector collector) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.collector = collector;
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
        } else{
            enableSwitch.setChecked(true);
        }

        collectorDataField.setText(collector.getDescription());

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO：delete the collector?
                Toast.makeText(c, "Collector (ID:" + collector.getCollectorId() + ") is deleted", Toast.LENGTH_LONG).show();
                collector.setCollectorStatus("deleted");
                DatabaseManager dbManager = new DatabaseManager(c);
                dbManager.removeCollectorById(collector.getCollectorId());
                dialog.dismiss();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enableSwitch.isChecked()){
                    collector.setCollectorStatus("running");
                } else {
                    collector.setCollectorStatus("disabled");
                }
                dialog.dismiss();
            }
        });

        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    collector.setCollectorStatus("running");
                } else {
                    collector.setCollectorStatus("disabled");
                }
            }
        });


        return dialog;
    }

}








