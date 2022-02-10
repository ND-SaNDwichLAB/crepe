package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.crepe.MainActivity;
import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;


public class CollectorConfigurationDialogWrapper  {

    private AlertDialog dialog;
    private Context context;
    private String currentScreenState;
    private Collector collector;

    CollectorConfigurationDialogWrapper (Context context, AlertDialog dialog, Collector collector){
        this.context = context;
        this.dialog = dialog;
        this.collector = collector;
        this.currentScreenState = "buildDialogFromConfig";
    }

    public void updateCurrentView() {
        View dialogMainView;

        //Dialog newScreen = new Dialog(c);
        switch(currentScreenState){
            case "buildDialogFromConfig":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config, null);
                dialog.setContentView(dialogMainView);

                //buttons
                Button popupCancelBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogCancelButton);
                Button popupNextBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogNextButton);

                //spinners
                Spinner appDropDown = (Spinner)dialogMainView.findViewById(R.id.appSpinner);
                Spinner locationDropDown = (Spinner) dialogMainView.findViewById(R.id.locationSpinner);
                String[] appItems = new String[]{" ","Uber", "Doordash", "Grubhub"};
                ArrayAdapter<String> appAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, appItems);
                appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                appDropDown.setAdapter(appAdapter);

                String[] locationItems = new String[]{" ","Local", "Remote"};
                ArrayAdapter<String> locationAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locationItems);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                locationDropDown.setAdapter(locationAdapter);

                //date picker buttons and textview
                ImageButton startDateCalendarBtn = (ImageButton)dialogMainView.findViewById(R.id.startImageButton);
                ImageButton endDateCalendarBtn = (ImageButton)dialogMainView.findViewById(R.id.endImageButton);
                EditText startDateText = (EditText)dialogMainView.findViewById(R.id.startDateText);
                EditText endDateText = (EditText)dialogMainView.findViewById(R.id.endDateText);

                // TODO: set the widget value according to the collector object
                startDateCalendarBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Use the MaterialDatePicker from Material Design
                        MaterialDatePicker.Builder collectorStartDatePickerBuilder = MaterialDatePicker.Builder.datePicker();
                        collectorStartDatePickerBuilder.setTitleText("SELECT THE COLLECTOR START DATE");
                        collectorStartDatePickerBuilder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        final MaterialDatePicker collectorStartDatePicker = collectorStartDatePickerBuilder.build();

                        collectorStartDatePicker.show(((MainActivity) context).getSupportFragmentManager(), "tag");

                        collectorStartDatePicker.addOnPositiveButtonClickListener( new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override public void onPositiveButtonClick(Long selection) {
                                collector.setCollectorStartTime(selection.longValue());
                            }
                        });
                    }
                });

                // TODO: change the two fields into one
                endDateCalendarBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Use the Material DatePicker from Material Design
                        MaterialDatePicker.Builder collectorEndDatePickerBuilder = MaterialDatePicker.Builder.datePicker();
                        collectorEndDatePickerBuilder.setTitleText("SELECT THE COLLECTOR END DATE");
                        collectorEndDatePickerBuilder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        final MaterialDatePicker collectorEndDatePicker = collectorEndDatePickerBuilder.build();

                        collectorEndDatePicker.show(((MainActivity) context).getSupportFragmentManager(), "tag");

                        collectorEndDatePicker.addOnPositiveButtonClickListener( new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override public void onPositiveButtonClick(Long selection) {
                                collector.setCollectorEndTime(selection.longValue());
                            }
                        });
                    }
                });

                popupCancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                popupNextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int f = 0;
                        // update info for collector
                        String appName = appDropDown.getSelectedItem().toString();
                                if (appName != " ") {
                                    collector.setAppName(appName);
                                } else {
                                    // set the border of spinner to red
                                    Context currentContext = context.getApplicationContext();
                                    Toast.makeText(currentContext,"Please select an app!",Toast.LENGTH_LONG).show();
                                    f = 1;
                                }
                        long startDate = startDateText.getSelectionStart();
                        //collector.setCollectorId();
                        long endDate = endDateText.getSelectionEnd();
                        //collector.setCollectorId();

                        //TODO: update the collector object according to the widget values
                        if (f == 0) {
                            // update currentScreen String value
                            currentScreenState = "buildDialogFromConfigGraphQuery";
                            // recursively call itself with new currentScreen String value
                            updateCurrentView();
                        }

//                        dialog.dismiss();
//                        CreateCollectorFromConfigDialogBuilderGraphQuery nextPopup = new CreateCollectorFromConfigDialogBuilderGraphQuery(c);
//                        Dialog newDialog = nextPopup.build();
//                        newDialog.show();
                    }
                });

                break;


            case "buildDialogFromConfigGraphQuery":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_graph_query, null);
                dialog.setContentView(dialogMainView);

                Button graphQueryNxtBtn = (Button) dialogMainView.findViewById(R.id.graphQueryNextButton);
                Button graphQueryBckBtn = (Button) dialogMainView.findViewById(R.id.graphQueryBackButton);
                ImageButton graphQueryCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeGraphQueryPopupImageButton);

                graphQueryBckBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfig";
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();

                    }
                });

                graphQueryNxtBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // upadate
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigDataField";
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();

                    }
                });

                graphQueryCloseImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });


                break;

            case "buildDialogFromConfigDataField": ;
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_data_field, null);
                dialog.setContentView(dialogMainView);

                Button dataFieldNxtBtn = (Button) dialogMainView.findViewById(R.id.dataFieldNextButton);
                Button dataFieldBckBtn = (Button) dialogMainView.findViewById(R.id.dataFieldBackButton);
                ImageButton dataFieldCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeDataFieldImageButton);

                dataFieldBckBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigGraphQuery";

                        // recursively call itself with new currentScreen String value
                        updateCurrentView();
                    }
                });

                dataFieldNxtBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigDescription";

                        // recursively call itself with new currentScreen String value
                        updateCurrentView();
                    }
                });

                dataFieldCloseImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });


                break;

            case "buildDialogFromConfigDescription":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_description, null);
                dialog.setContentView(dialogMainView);

                Button descriptionCreateBtn = (Button) dialogMainView.findViewById(R.id.descriptionCreateButton);
                Button descriptionBckBtn = (Button) dialogMainView.findViewById(R.id.descriptionBackButton);
                ImageButton descriptionCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeDescriptionImageButton);
                EditText descriptionEditText = (EditText) dialogMainView.findViewById(R.id.descriptionEditText);

                descriptionBckBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigDataField";
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();
                    }
                });

                descriptionCreateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // TODO: write the object to the DB
                        String descriptionText = descriptionEditText.getText().toString();

                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigSuccessMessage";
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();
                    }
                });

                descriptionCloseImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                break;

            case "buildDialogFromConfigSuccessMessage":;
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_success_message, null);
                dialog.setContentView(dialogMainView);

                Button closeSuccessMessage = (Button) dialogMainView.findViewById(R.id.closeSuccessMessagePopupButton);
                closeSuccessMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // TODO: write the object to the DB
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigSuccessMessage";
                        // recursively call itself with new currentScreen String value
                        dialog.dismiss();
                    }
                });
                break;



            default:
                throw new IllegalStateException("Unexpected value: " + currentScreenState);

        }
    }


   public void show() {
        dialog.show();
        updateCurrentView();
   }

    public Dialog getDialog() {
        return dialog;
    }
}
