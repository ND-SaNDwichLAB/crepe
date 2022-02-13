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

import java.text.SimpleDateFormat;
import java.util.Calendar;


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

        switch(currentScreenState){
            case "buildDialogFromConfig":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config, null);
                dialog.setContentView(dialogMainView);

                // buttons
                Button popupCancelBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogCancelButton);
                Button popupNextBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogNextButton);

                // spinners
                Spinner appDropDown = (Spinner)dialogMainView.findViewById(R.id.appSpinner);
                Spinner locationDropDown = (Spinner) dialogMainView.findViewById(R.id.locationSpinner);

                // app spinner
                String[] appItems = new String[]{" ","Uber", "Doordash", "Grubhub"};
                ArrayAdapter<String> appAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, appItems);
                appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                appDropDown.setAdapter(appAdapter);
                if (collector.getAppName() != null) {
                    int i;
                    for (i = 1; i < appItems.length; i++) {
                        if (collector.getAppName() == appItems[i])
                            break;
                    }
                    appDropDown.setSelection(i);
                }

                // location spinner
                String[] locationItems = new String[]{" ","Local", "Remote"};
                ArrayAdapter<String> locationAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locationItems);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                locationDropDown.setAdapter(locationAdapter);
                if (collector.getMode() != null) {
                    int i;
                    for (i = 1; i < locationItems.length; i++) {
                        if (collector.getMode() == locationItems[i])
                            break;
                    }
                    locationDropDown.setSelection(i);
                }

                // date picker buttons and textview
                ImageButton startDateCalendarBtn = (ImageButton)dialogMainView.findViewById(R.id.startImageButton);
                ImageButton endDateCalendarBtn = (ImageButton)dialogMainView.findViewById(R.id.endImageButton);
                EditText startDateText = (EditText)dialogMainView.findViewById(R.id.startDateText);
                EditText endDateText = (EditText)dialogMainView.findViewById(R.id.endDateText);

                // update field values based on current collector information, mostly used when coming back from next dialogs
                if(Long.valueOf(collector.getCollectorStartTime()) != 0) {
                    startDateText.setText(collector.getCollectorStartTimeString());
                }
                else {
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    String currentDate = df.format(c.getTime());
                    startDateText.setText(currentDate);
                }
                if(Long.valueOf(collector.getCollectorEndTime()) != 0) {
                    endDateText.setText(collector.getCollectorEndTimeString());
                }
                else {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.DAY_OF_YEAR,1);
                    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    String currentDate = df.format(c.getTime());
                    endDateText.setText(currentDate);
                }

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
                                startDateText.setText(collector.getCollectorStartTimeString(), null);
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
                                // Validate if the end date is later than the start date,
                                // otherwise toast a message
                                long endDateSelectionValue = selection.longValue();

                                if (endDateSelectionValue > collector.getCollectorStartTime()) {
                                    collector.setCollectorEndTime(endDateSelectionValue);
                                    endDateText.setText(collector.getCollectorEndTimeString(), null);
                                } else {
                                    Toast.makeText(context, "Please select a date later than your start date (" + collector.getCollectorStartTimeString() + ")", Toast.LENGTH_LONG).show();
                                }
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
                        int blankFlag = 0;
                        // update info for collector
                        String appName = appDropDown.getSelectedItem().toString();
                        if (appName != " ") {
                            collector.setAppName(appName);
                        } else {
                            // set the border of spinner to red
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext,"Please select an app!",Toast.LENGTH_LONG).show();
                            blankFlag = 1;
                        }

                        String location = locationDropDown.getSelectedItem().toString();
                        if (location != " ") {
                            collector.setMode(location);
                        } else {
                            // set the border of spinner to red
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext,"Please select a location!",Toast.LENGTH_LONG).show();
                            blankFlag = 1;
                        }


                        //TODO: update the collector object according to the widget values
                        if (blankFlag == 0) {
                            // update currentScreen String value
                            currentScreenState = "buildDialogFromConfigGraphQuery";
                            // recursively call itself with new currentScreen String value
                            updateCurrentView();
                        }

                    }
                });
                break;




            case "buildDialogFromConfigGraphQuery":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_graph_query, null);
                dialog.setContentView(dialogMainView);

                Button graphQueryNxtBtn = (Button) dialogMainView.findViewById(R.id.graphQueryNextButton);
                Button graphQueryBckBtn = (Button) dialogMainView.findViewById(R.id.graphQueryBackButton);
                ImageButton graphQueryCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeGraphQueryPopupImageButton);
                EditText graphQueryEditTxt = (EditText) dialogMainView.findViewById(R.id.graphQueryEditText);

                if (collector.getCollectorGraphQuery() != null) {
                    graphQueryEditTxt.setText(collector.getCollectorGraphQuery());
                }

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
                        int blankFlag = 0;
                        // get graph query
                        String graphQueryContent = graphQueryEditTxt.getText().toString();
                        if (graphQueryContent != null){
                            // TODO: finish graph query
                            collector.setCollectorGraphQuery(graphQueryContent);
                        } else {
                            // remind user to add graph query
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext,"Please type in the graph query!",Toast.LENGTH_LONG).show();
                            blankFlag = 1;
                        }
                        if (blankFlag == 0) {
                            // update currentScreen String value
                            currentScreenState = "buildDialogFromConfigDataField";
                            // recursively call itself with new currentScreen String value
                            updateCurrentView();
                        }
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
                EditText dataFieldEditText = (EditText) dialogMainView.findViewById(R.id.dataFieldEditText);

                if (collector.getCollectorAppDataFields() != null) {
                    dataFieldEditText.setText(collector.getCollectorAppDataFields());
                }

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
                        // write data field into collector
                        String dataFieldContent = dataFieldEditText.getText().toString();
                        collector.setCollectorAppDataFields(dataFieldContent);
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

                        // write description into collector
                        String descriptionText = descriptionEditText.getText().toString();
                        collector.setDescription(descriptionText);

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
