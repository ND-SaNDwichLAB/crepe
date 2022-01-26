package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.google.android.material.datepicker.MaterialDatePicker;

public class CreateCollectorFromConfigDialogBuilder {

    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private String currentScreen;

    //TODO: collectorID, creatorUSerID
    private Collector collector;

    private View popupView;
    private Dialog dialog;

    public CreateCollectorFromConfigDialogBuilder(Context c) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.currentScreen = "buildDialogFromConfig";

        // generate an id based on existing collector quantity for collector
        DatabaseManager dbManager = new DatabaseManager(c);
        Integer collectorQuantity = dbManager.getAllCollectors().size();
        String collectorId = String.valueOf(collectorQuantity + 1);
        this.collector = new Collector(collectorId);

    }

    public Dialog build() {
        updateCurrentView(currentScreen);
        dialogBuilder.setView(popupView);
        dialog = dialogBuilder.create();
        return dialog;
    }

    public void updateCurrentView(String currentScreen) {
        switch(currentScreen){
            case "buildDialogFromConfig":
                popupView = LayoutInflater.from(c).inflate(R.layout.dialog_add_collector_from_config, null);
                dialogBuilder.setView(popupView);
                //buttons
                Button popupCancelBtn = (Button) popupView.findViewById(R.id.addCollectorFromConfigDialogCancelButton);
                Button popupNextBtn = (Button) popupView.findViewById(R.id.addCollectorFromConfigDialogNextButton);
                //spinners
                Spinner appDropDown = (Spinner)popupView.findViewById(R.id.appSpinner);
                Spinner locationDropDown = (Spinner) popupView.findViewById(R.id.locationSpinner);
                String[] appItems = new String[]{"Uber", "Doordash", "Grubhub"};
                ArrayAdapter<String> appAdapter = new ArrayAdapter<String>(c.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, appItems);
                appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                appDropDown.setAdapter(appAdapter);
                String[] locationItems = new String[]{"1", "2", "3"};
                ArrayAdapter<String> locationAdapter = new ArrayAdapter<String>(c.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locationItems);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                locationDropDown.setAdapter(locationAdapter);
                //date picker buttons and textview
                ImageButton startImgBtn = (ImageButton)popupView.findViewById(R.id.startImageButton);
                ImageButton endImgBtn = (ImageButton)popupView.findViewById(R.id.endImageButton);
                EditText startDateText = (EditText)popupView.findViewById(R.id.startDateText);
                EditText endDateText = (EditText)popupView.findViewById(R.id.endDateText);
                //MaterialDatePicker
                MaterialDatePicker.Builder builder = MaterialDatePicker.Builder.datePicker();
                builder.setTitleText("SELECT THE START DATE");
                builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                final MaterialDatePicker startMaterialDatePicker = builder.build();

                startImgBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startMaterialDatePicker.show(startMaterialDatePicker.getParentFragmentManager(), "start_date_picker");

                    }
                });

                endImgBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });

                Dialog dialog = dialogBuilder.create();
                popupCancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                popupNextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update info for collector
//                        String appName = appDropDown.getSelectedItem().toString();
//                                if (appName != someDefaultValue) {
//                                    collector.setAppName(appName);
//                                } else {
//                                    // set the border of spinner to red
//                                }
//
                        //collector.getTimeCreated();
                        //collector.getTimeLastEdited();

                        // update currentScreen String value
                        //currentScreen = "buildDialogFromConfigGraphQuery";

                        // recursively call itself with new currentScreen String value
                        //updateCurrentView(currentScreen);
                        dialog.dismiss();
                        CreateCollectorFromConfigDialogBuilderGraphQuery nextPopup = new CreateCollectorFromConfigDialogBuilderGraphQuery(c);
                        Dialog newDialog = nextPopup.build();
                        newDialog.show();
                    }
                });



            case "buildDialogFromConfigGraphQuery":
                ;

            case "buildDialogFromConfigDataField": ;




            case "buildDialogFromConfigDescription":;



        }

    }

}
