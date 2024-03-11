package edu.nd.crepe.ui.dialog;

import static edu.nd.crepe.MainActivity.currentUser;

import android.app.AlertDialog;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.nd.crepe.servicemanager.CrepeAccessibilityService;
import edu.nd.crepe.MainActivity;
import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.demonstration.WidgetService;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.gson.Gson;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;


public class CollectorConfigurationDialogWrapper extends AppCompatActivity {

    private static Context context;
    private static String currentScreenState;
    private static Boolean isEdit;
    private static Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;
    private FirebaseCommunicationManager firebaseCommunicationManager;
    private static View dialogMainView;
    private static AlertDialog mainDialog;

    private static HashMap<String, Object> collectorUpdates = new HashMap<>();

    private static List<Datafield> datafields = new ArrayList<>();

    private static CollectorConfigurationDialogWrapper singletonInstance = null;


    public static class GraphQueryGraphQueryCallback implements edu.nd.crepe.ui.dialog.GraphQueryCallback, Serializable {
        @Override
        public void onDataReceived(String query, String targetText) {
            // datafield id format: collectorId%[timestamp]
            String datafieldId = collector.getCollectorId() + "%" + String.valueOf(System.currentTimeMillis());
            datafields.add(new Datafield(datafieldId, collector.getCollectorId(), query, targetText, true));

            if (context != null) {
                SharedPreferences sharedPreferences = context.getSharedPreferences("prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("datafieldsList", new Gson().toJson(datafields));
                editor.apply();
            } else {
                Log.e("CollectorConfigDialog", "Context is null, cannot save datafields to shared preferences");
            }

            updateDisplayedDatafieldsFromDemonstration();
        }
    }

    private GraphQueryGraphQueryCallback graphQueryCallback = new GraphQueryGraphQueryCallback();

    CollectorConfigurationDialogWrapper(Context context, Collector collector, Runnable refreshCollectorListRunnable) {
        this.context = context;
        this.collector = collector;
        this.currentScreenState = "buildDialogFromConfig";
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(context);
        this.firebaseCommunicationManager = new FirebaseCommunicationManager(context);

        // get the datafields associated with this collector
        datafields = dbManager.getAllDatafieldsForCollector(collector);
    }

    public static Boolean isNull() {
        return singletonInstance == null;
    }

    public static CollectorConfigurationDialogWrapper getInstance() {
        if (singletonInstance == null) {
            throw new IllegalStateException("DialogWrapper is not initialized, call initializeInstance(..) method first.");
        }
        return singletonInstance;
    }

    public static void initializeInstance(Context context, Collector collector, Runnable refreshCollectorListRunnable) {
        if (singletonInstance != null) {
            Log.i("CollectorConfigDialog", "Overriding existing singleton instance...");
        }
        singletonInstance = new CollectorConfigurationDialogWrapper(context, collector, refreshCollectorListRunnable);
    }


    public void updateCurrentView() {

        switch (currentScreenState) {
            case "buildDialogFromConfig":
                // create a dialog
                Button popupCancelBtn;
                Button popupNextBtn;
                Spinner appDropDown;
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_build_collector_from_config, null);
                // buttons
                popupCancelBtn = (Button) dialogMainView.findViewById(R.id.buildCollectorFromConfigDialogCancelButton);
                popupNextBtn = (Button) dialogMainView.findViewById(R.id.buildCollectorFromConfigDialogNextButton);
                appDropDown = (Spinner) dialogMainView.findViewById(R.id.appSpinner);
                Dictionary<String, String> appPackageDict = new Hashtable<>();
                TextView configPopupTitleTextView = (TextView) dialogMainView.findViewById(R.id.configPopupTitle);

                if (isEdit) {

                    String[] singleAppItem = {collector.getAppName()};
                    TextView commentOnAppSpinner = (TextView) dialogMainView.findViewById(R.id.commentOnAppSpinner);
                    ArrayAdapter<String> singleAppAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, singleAppItem);
                    singleAppAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    // change the text to notify the user that the app is not editable
                    commentOnAppSpinner.setText("The target app cannot be changed after creation");

                    // Set the adapter to the spinner
                    appDropDown.setAdapter(singleAppAdapter);

                    // Automatically select the only available option
                    appDropDown.setSelection(0);

                    appDropDown.setEnabled(false);
                    appDropDown.setClickable(false);
                    appDropDown.setFocusable(false);

                    // Title
                    configPopupTitleTextView.setText("EDIT COLLECTOR");

                } else {
                    // app spinner
                    String[] appItems = {""};
                    try {
                        appPackageDict = getAllInstalledAppNames();
                    } catch (PackageManager.NameNotFoundException e) {
                        e.getMessage();
                    }

                    // get the keys of the dictionary and put them into an array
                    appItems = Collections.list(appPackageDict.keys()).toArray(new String[0]);
                    ArrayAdapter<String> appAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, appItems);
                    appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    appDropDown.setAdapter(appAdapter);
                    //                Spinner locationDropDown = (Spinner) dialogMainView.findViewById(R.id.locationSpinner);
                    //Title
                    configPopupTitleTextView.setText("ADD A NEW COLLECTOR");
                    // When coming back from later popups using back button, if there's previously a selection made
                    if (collector.getAppName() != null) {
                        for (int i = 0; i < appItems.length; i++) {
                            if (collector.getAppName().equals(appItems[i]))
                                appDropDown.setSelection(i);
                        }

                    }
                }

                mainDialog = createNewAlertDialog(dialogMainView);
                mainDialog.show();

                collector.setCreatorUserId(currentUser.getUserId());

//                // location spinner
//                String[] locationItems = new String[]{"Local", "Remote"};
//                ArrayAdapter<String> locationAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locationItems);
//                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                locationDropDown.setAdapter(locationAdapter);
//                // When coming back from later popups using back button, if there's previously a selection made
//                if (collector.getMode() != null) {
//                    int i;
//                    for (i = 0; i < locationItems.length; i++) {
//                        if (collector.getMode() == locationItems[i])
//                            break;
//                    }
//                    locationDropDown.setSelection(i);
//                }

                // date picker buttons and textview
                ImageButton startDateCalendarBtn = (ImageButton) dialogMainView.findViewById(R.id.startImageButton);
                ImageButton endDateCalendarBtn = (ImageButton) dialogMainView.findViewById(R.id.endImageButton);
                EditText startDateText = (EditText) dialogMainView.findViewById(R.id.startDateText);
                EditText endDateText = (EditText) dialogMainView.findViewById(R.id.endDateText);

                // update field values based on current collector information, mostly used when coming back from next dialogs
                if (Long.valueOf(collector.getCollectorStartTime()) != 0) {
                    startDateText.setText(collector.getCollectorStartTimeString());
                } else {
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    String currentDate = df.format(c.getTime());
                    startDateText.setText(currentDate);
                }
                if (Long.valueOf(collector.getCollectorEndTime()) != 0) {
                    endDateText.setText(collector.getCollectorEndTimeString());
                } else {
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.DAY_OF_YEAR, 1);
                    SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");
                    String currentDate = df.format(c.getTime());
                    endDateText.setText(currentDate);
                }

                startDateCalendarBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Use the MaterialDatePicker from Material Design
                        MaterialDatePicker.Builder collectorStartDatePickerBuilder = MaterialDatePicker.Builder.datePicker();
                        collectorStartDatePickerBuilder.setTitleText("START DATE");
                        collectorStartDatePickerBuilder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        final MaterialDatePicker collectorStartDatePicker = collectorStartDatePickerBuilder.build();

                        collectorStartDatePicker.show(((MainActivity) context).getSupportFragmentManager(), "tag");

                        collectorStartDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override
                            public void onPositiveButtonClick(Long selection) {
                                long startDateSelectionValue = selection.longValue();

                                // in either mode, we first update the collector object
                                collector.setCollectorStartTime(startDateSelectionValue);
                                // if in edit mode, we make the updates to the collectorUpdates dictionary
                                if (isEdit) {
                                    collectorUpdates.put("collectorStartTime", startDateSelectionValue);
                                    collectorUpdates.put("collectorStartTimeString", collector.getCollectorStartTimeString());
                                }
                                startDateText.setText(collector.getCollectorStartTimeString(), null);
                            }
                        });
                    }
                });

                endDateCalendarBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Use the Material DatePicker from Material Design
                        MaterialDatePicker.Builder collectorEndDatePickerBuilder = MaterialDatePicker.Builder.datePicker();
                        collectorEndDatePickerBuilder.setTitleText("END DATE");
                        Long oneDayMillisOffset = (long) (24 * 60 * 60 * 1000);   // add 24 hours to the current time to get tomorrow's time
                        collectorEndDatePickerBuilder.setSelection(MaterialDatePicker.todayInUtcMilliseconds() + oneDayMillisOffset);   // set the default selection as tomorrow
                        final MaterialDatePicker collectorEndDatePicker = collectorEndDatePickerBuilder.build();

                        collectorEndDatePicker.show(((MainActivity) context).getSupportFragmentManager(), "tag");

                        collectorEndDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override
                            public void onPositiveButtonClick(Long selection) {
                                // Validate if the end date is later than the start date,
                                // otherwise toast a message
                                long endDateSelectionValue = selection.longValue();
                                long endDateSelectionEndOfDay = endDateSelectionValue + oneDayMillisOffset - 1;

                                if (endDateSelectionEndOfDay > collector.getCollectorStartTime()) {

                                    // in either mode, we first update the collector object
                                    collector.setCollectorEndTime(endDateSelectionEndOfDay);
                                    // if in edit mode, we make the updates to the collectorUpdates dictionary
                                    if (isEdit) {
                                        collectorUpdates.put("collectorEndTime", endDateSelectionEndOfDay);
                                        collectorUpdates.put("collectorEndTimeString", collector.getCollectorEndTimeString());
                                    }
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
                        mainDialog.dismiss();
                    }
                });

                Dictionary<String, String> finalAppPackageDict = appPackageDict;
                popupNextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update app info
                        String appName = appDropDown.getSelectedItem().toString();
                        String appPackage = finalAppPackageDict.get(appName);
                        if (appName != " ") {
                            collector.setAppName(appName);
                            collector.setAppPackage(appPackage);
                        } else {
                            // set the border of spinner to red
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please select an app!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // update location info
//                        String location = locationDropDown.getSelectedItem().toString();
//                        if (location != " ") {
//                            collector.setMode(location);
//                        } else {
//                            // set the border of spinner to red
//                            Context currentContext = context.getApplicationContext();
//                            Toast.makeText(currentContext, "Please select a location!", Toast.LENGTH_LONG).show();
//                            return;
//                        }

                        // update date info
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                        ParsePosition pp1 = new ParsePosition(0);
                        Date startDate = dateFormat.parse(startDateText.getText().toString(), pp1);
                        collector.setCollectorStartTime(startDate.getTime());

                        ParsePosition pp2 = new ParsePosition(0);
                        Date endDate = dateFormat.parse(endDateText.getText().toString(), pp2);
                        collector.setCollectorEndTime(endDate.getTime());

                        // After successfully set the collector's end time, automatically set its status
                        collector.autoSetCollectorStatus();
                        // Update in database as well
                        dbManager.updateCollectorStatus(collector);

                        // set the id of the collector in create mode
                        // format: userId%appName%timestamp. We will remove any space in the appName
                        if (isEdit == false) {
                            collector.setCollectorId(currentUser.getUserId() + "%" + collector.getAppName().replaceAll(" ", "") + "%" + String.valueOf(System.currentTimeMillis()));
                        }

                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigGraphQuery";
                        mainDialog.dismiss();
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();

                    }
                });

                break;


            case "buildDialogFromConfigGraphQuery":

                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_build_collector_from_config_graph_query, null);
                mainDialog = createNewAlertDialog(dialogMainView);
                // modify content of the popup box based on current state
                updateDisplayedDatafieldsFromDemonstration();


                Button graphQueryNxtBtn = (Button) dialogMainView.findViewById(R.id.graphQueryNextButton);
                Button graphQueryBckBtn = (Button) dialogMainView.findViewById(R.id.graphQueryBackButton);
                LinearLayout datafieldContainerLinearLayout = (LinearLayout) dialogMainView.findViewById(R.id.datafieldContainerLinearLayout);
//                Button graphQueryAddBtn = (Button) dialogMainView.findViewById(R.id.graphQueryAddAnotherButton);
                Button openAppButton = (Button) dialogMainView.findViewById(R.id.openAppButton);
                EditText graphQueryInput = (EditText) dialogMainView.findViewById(R.id.graphQueryInput);
                ImageButton graphQueryAddImg = (ImageButton) dialogMainView.findViewById(R.id.graphQueryAddIcon);
                ImageButton graphQueryCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeGraphQueryPopupImageButton);

                // update interface elements based on the specified app in the previous popup box
                TextView commentOnOpenAppButton = (TextView) dialogMainView.findViewById(R.id.commentOnOpenAppButton);
                String appName = collector.getAppName();

                if (datafields != null && !datafields.isEmpty()) {
                    openAppButton.setText("Add another");
                    commentOnOpenAppButton.setText("Add another data to collect in the " + appName + " app");
                } else {
                    openAppButton.setText("Open " + collector.getAppName());
                    commentOnOpenAppButton.setText("Demonstrate in the " + appName + " app");
                }


                mainDialog.show();


                // Open App button
                openAppButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // find the package
                        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                        // get list of all the apps installed
                        // ril stands for ResolveInfoList
                        List<ResolveInfo> ril = context.getPackageManager().queryIntentActivities(mainIntent, 0);
                        String appName = collector.getAppName();
                        String nameBuffer;
                        String packageName = "";
                        for (ResolveInfo ri : ril) {
                            if (ri.activityInfo != null) {
                                // get package
                                Resources res = null;
                                try {
                                    res = context.getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }
                                // if activity label res is found
                                if (ri.activityInfo.labelRes != 0) {
                                    nameBuffer = res.getString(ri.activityInfo.labelRes);
                                } else {
                                    nameBuffer = ri.activityInfo.applicationInfo.loadLabel(
                                            context.getPackageManager()).toString();
                                }
                                if (nameBuffer.equals(appName)) {
                                    // get package
                                    packageName = ri.activityInfo.packageName;
                                    break;
                                }
                            }
                        }

                        // launch the app
                        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                        if (launchIntent != null) {
                            context.startActivity(launchIntent);
                        } else {
                            Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
                        }

                        // launch the float widget
                        Intent intent = new Intent(context, WidgetService.class);
                        intent.putExtra("graphQueryCallback", graphQueryCallback);
                        context.startService(intent);
                    }
                });

                graphQueryAddImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // add datafield to datafields list
                        String graphQueryInputResult = graphQueryInput.getText().toString();
                        if (!graphQueryInputResult.isEmpty()) {
                            // datafield id format: collectorId%[timestamp]
                            String datafieldId = collector.getCollectorId() + "%" + String.valueOf(System.currentTimeMillis());
                            datafields.add(new Datafield(datafieldId, collector.getCollectorId(), graphQueryInputResult, graphQueryInputResult, false));
                            // update dialog
                            updateDisplayedDatafieldsFromDemonstration();
                            // clear the input field
                            graphQueryInput.setText("");
                            graphQueryInput.setHint("Manually add another query");
                            Toast.makeText(context, "Datafield added", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, "Please enter a query", Toast.LENGTH_LONG).show();
                        }
                    }
                });

                graphQueryBckBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfig";
                        mainDialog.dismiss();
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();

                    }
                });

                graphQueryNxtBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String graphQueryInputResult = graphQueryInput.getText().toString();
                        if (!graphQueryInputResult.isEmpty()) {
                            // datafield id format: collectorId%[timestamp]
                            String datafieldId = collector.getCollectorId() + "%" + String.valueOf(System.currentTimeMillis());
                            datafields.add(new Datafield(datafieldId, collector.getCollectorId(), graphQueryInputResult, graphQueryInputResult, false));
                        }

                        if (datafields != null && datafields.isEmpty()) {
                            // remind user to add graph query
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please open the app to demonstrate the data to collect!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigDescription";
                        mainDialog.dismiss();
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();
                    }
                });

//                graphQueryAddBtn.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {f
//                        currentScreenState = "buildDialogFromConfigGraphQuery";
//                    }
//                });

                graphQueryCloseImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mainDialog.dismiss();
                        // clear the current datafields list, since this collector creation process is terminated
                        clearDatafields();
                    }
                });
                break;


            case "buildDialogFromConfigDescription":

                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_build_collector_from_config_description, null);


                Button descriptionCreateBtn = (Button) dialogMainView.findViewById(R.id.descriptionCreateButton);
                Button descriptionBckBtn = (Button) dialogMainView.findViewById(R.id.descriptionBackButton);
                ImageButton descriptionCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeDescriptionImageButton);
                EditText descriptionEditText = (EditText) dialogMainView.findViewById(R.id.descriptionEditText);

                // show description of the collector if available
                if (collector.getDescription() != null) {
                    descriptionEditText.setText(collector.getDescription());
                }

                if (isEdit) {
                    descriptionCreateBtn.setText("Update");
                }

                mainDialog = createNewAlertDialog(dialogMainView);
                mainDialog.show();

                descriptionBckBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigGraphQuery";
                        mainDialog.dismiss();
                        // recursively call itself with new currentScreen String value
                        updateCurrentView();
                    }
                });

                descriptionCreateBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // write description into collector
                        String descriptionText = descriptionEditText.getText().toString();

                        if (descriptionText.equals("")) {
                            // remind user to add description
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please add a description!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        collector.setDescription(descriptionText);


                        if (isEdit) {
                            collectorUpdates.put("description", descriptionText);
                            // save locally
                            dbManager.updateCollector(collector);
                            firebaseCommunicationManager.updateCollector(collector.getCollectorId(), collectorUpdates).addOnSuccessListener(suc -> {
                                Log.i("Firebase", "Successfully edited collector " + collector.getCollectorId() + " to firebase.");
                            }).addOnFailureListener(er -> {
                                Log.e("Firebase", "Failed to edited collector " + collector.getCollectorId() + " to firebase.");
                            });


                            // for all of the datafields, it is a little more tricky: we need to check if the datafield already exists in the database
                            // first, get all existing datafield ids for this collector
                            List<Datafield> prevDatafieldList = dbManager.getAllDatafieldsForCollector(collector);
                            List<String> prevDatafieldIdList = new ArrayList<>();
                            for (Datafield prevDatafield : prevDatafieldList) {
                                prevDatafieldIdList.add(prevDatafield.getDatafieldId());
                            }
                            List<String> datafieldIdList = new ArrayList<>();
                            for (Datafield currentDatafield : datafields) {
                                datafieldIdList.add(currentDatafield.getDatafieldId());
                            }
                            // then, for each datafield, check if it already exists in the database
                            for (Datafield datafield : datafields) {

                                if (prevDatafieldIdList.contains(datafield.getDatafieldId())) {
                                    // if the datafield already exists in the database, update it
                                    dbManager.updateDatafield(datafield);
                                } else {
                                    // if the datafield does not exist in the database, add it
                                    dbManager.addOneDatafield(datafield);
                                }

                                // regardless of whether the datafield already exists in the database, we can use putDatafield to update the datafield in firebase
                                firebaseCommunicationManager.putDatafield(datafield).addOnCompleteListener(task -> {
                                    Log.i("Firebase", "Successfully added/updated datafield " + datafield.getDatafieldId() + " to firebase.");
                                }).addOnFailureListener(er -> {
                                    Log.e("Firebase", "Failed to add/update datafield " + datafield.getDatafieldId() + " to firebase. Error: " + er.getMessage());
                                });

                            }
                            // also, for deleted datafields, we need to remove them from the database
                            for (String prevDatafieldId : prevDatafieldIdList) {
                                if (!datafieldIdList.contains(prevDatafieldId)) {
                                    dbManager.removeDatafieldById(prevDatafieldId);
                                    firebaseCommunicationManager.removeDatafield(prevDatafieldId).addOnCompleteListener(task -> {
                                        Log.i("Firebase", "Successfully deleted datafield " + prevDatafieldId + " from firebase.");
                                    }).addOnFailureListener(er -> {
                                        Log.e("Firebase", "Failed to delete datafield " + prevDatafieldId + " from firebase. Error: " + er.getMessage());
                                    });
                                }
                            }

                        } else {
                            // save locally
                            dbManager.addOneCollector(collector);
                            // save to Firebase
                            firebaseCommunicationManager.putCollector(collector).addOnSuccessListener(suc -> {
                                Log.i("Firebase", "Successfully added collector " + collector.getCollectorId() + " to firebase.");
                            }).addOnFailureListener(er -> {
                                Log.e("Firebase", "Failed to add collector " + collector.getCollectorId() + " to firebase.");
                            });

                            // store the data fields into database
                            for (Datafield datafield : datafields) {
                                dbManager.addOneDatafield(datafield);
                                firebaseCommunicationManager.putDatafield(datafield).addOnCompleteListener(task -> {
                                    Log.i("Firebase", "Successfully added datafield " + datafield.getDatafieldId() + " to firebase.");
                                }).addOnFailureListener(er -> {
                                    Log.e("Firebase", "Failed to add datafield " + datafield.getDatafieldId() + " to firebase. Error: " + er.getMessage());
                                });
                            }
                        }


                        // Note: we do not need to update the field "userCollectors" under User (see /database/User.java),
                        // be cause the user is the creator of this collector, and this piece of info is already stored in the Collector object (see /database/Collector.java) under the field "creatorUserId"

                        // clear the current datafields list, since this collector creation process is done
                        clearDatafields();

                        // update all collectors' status, before refreshing the collector list
                        // to make sure we only display active collectors.
                        CrepeAccessibilityService.getsSharedInstance().refreshAllCollectorStatus();
                        // a callback to refresh homepage every time
                        refreshCollectorListRunnable.run();

                        // update currentScreen String value, recursively call itself with new currentScreen String value
                        currentScreenState = "buildDialogFromConfigSuccessMessage";
                        mainDialog.dismiss();
                        updateCurrentView();
                    }
                });

                descriptionCloseImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mainDialog.dismiss();
                        // clear the current datafields list, since this collector creation process is terminated
                        clearDatafields();
                    }
                });

                break;


            case "buildDialogFromConfigSuccessMessage":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_build_collector_from_config_success_message, null);

                // update the displayed app info
                TextView successMessageTextView = (TextView) dialogMainView.findViewById(R.id.shareText);
                if (isEdit) {
                    successMessageTextView.setText("Your collector for " + collector.getAppName() + " is saved. Share with your participants");
                } else {
                    successMessageTextView.setText("Your collector for " + collector.getAppName() + " is created. Share with your participants");
                }
                mainDialog = createNewAlertDialog(dialogMainView);
                mainDialog.show();

                Button closeSuccessMessage = (Button) dialogMainView.findViewById(R.id.closeSuccessMessagePopupButton);
                ImageButton shareUrlLinkButton = (ImageButton) dialogMainView.findViewById(R.id.shareUrlImageButton);

//                ImageButton shareEmailLinkButton = (ImageButton) dialogMainView.findViewById(R.id.shareEmailImageButton);

//                shareEmailLinkButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        // share email link
//                        Intent intent = new Intent(Intent.ACTION_SENDTO);
//                        intent.setType("message/rfc822");
//                        intent.setData(Uri.parse("mailto:"+"ylu23@nd.edu"));
//                        //intent.putExtra(Intent.EXTRA_EMAIL, "ylu23@nd.edu");
//                        intent.putExtra(Intent.EXTRA_SUBJECT, "Data Collector for " + collector.getAppName());
//                        intent.putExtra(Intent.EXTRA_TEXT, collector.getCollectorId());
//
//                        if (intent.resolveActivity(getPackageManager()) != null) {
//                            startActivity(intent);
//                        } else {
//                            Toast.makeText(context,"No email app on this machine",Toast.LENGTH_LONG).show();
//                        }
//
//
//                    }
//                });


                shareUrlLinkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // we will just share the id of the collector for now, instead of a url
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("share URL", collector.getCollectorId());
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "collector ID copied. Share with your participants", Toast.LENGTH_LONG).show();
                        currentScreenState = "dismissed";
                    }
                });


                closeSuccessMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // update currentScreen String value
                        currentScreenState = "dismissed";
                        mainDialog.dismiss();

                    }
                });
                break;

            case "dismissed":
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + currentScreenState);

        }
    }

    private AlertDialog createNewAlertDialog(View dialogMainView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setView(dialogMainView);
        return dialog;
    }

    private void clearDatafields() {
        // remove all elements from datafields
        if (datafields != null) {
            datafields.clear();
        }
    }

    public static List<Datafield> getDatafields() {
        return datafields;
    }

    public static void setDatafields(List<Datafield> datafields) {
        CollectorConfigurationDialogWrapper.datafields = datafields;
    }

    public Dictionary<String, String> getAllInstalledAppNames() throws PackageManager.NameNotFoundException {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // get list of all the apps installed
        // ril stands for ResolveInfoList
        List<ResolveInfo> ril = context.getPackageManager().queryIntentActivities(mainIntent, 0);
        String name = null;
        int i = 0;

        // get size of ril and create a dictionary of app names and package names
        Dictionary<String, String> appDict = new Hashtable<String, String>();
        for (ResolveInfo ri : ril) {
            if (ri.activityInfo != null) {
                // get package
                Resources res = context.getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                // if activity label res is found
                if (ri.activityInfo.labelRes != 0) {
                    name = res.getString(ri.activityInfo.labelRes);
                } else {
                    name = ri.activityInfo.applicationInfo.loadLabel(
                            context.getPackageManager()).toString();
                }
                appDict.put(name, ri.activityInfo.packageName);
                i++;
            }
        }
//        Toast.makeText(context, ril.size() + " apps are installed on this phone", Toast.LENGTH_LONG).show();
        return appDict;
    }

    public void show(Boolean isEdit) {
        this.isEdit = isEdit;
        updateCurrentView();
    }

    public void hide() {
        try {
            if (mainDialog != null && mainDialog.isShowing()) {
                mainDialog.dismiss();
            } else {
                Log.e("CollectorConfigDialog", "mainDialog is null or not showing");
            }
        } catch (Exception e) {
            Log.e("CollectorConfigDialog", "Error dismissing mainDialog: " + e.getMessage());
        }
    }

    private static void updateDisplayedDatafieldsFromDemonstration() {
        // we only update if the current screen is the demonstration screen
        if (currentScreenState.equals("buildDialogFromConfigGraphQuery")) {
            refreshDatafieldsList();
        } else {
            Log.e("Dialog", "updateDisplayedDatafieldsFromDemonstration() called when currentScreenState is not buildDialogFromConfigGraphQuery");
        }
    }

    private static void refreshDatafieldsList() {
        // remove all subviews from the linearlayout
        LinearLayout datafieldContainerLinearLayout = (LinearLayout) dialogMainView.findViewById(R.id.datafieldContainerLinearLayout);
        datafieldContainerLinearLayout.removeAllViews();

        // inflate datafields into the dialog
        if (datafields != null) {
            for (int i = 0; i < datafields.size(); i++) {
                // inflate datafield
                View datafieldView = LayoutInflater.from(context).inflate(R.layout.datafield_card, null);
                // get datafield name
                TextView datafieldName = (TextView) datafieldView.findViewById(R.id.datafieldTextView);
                datafieldName.setText(datafields.get(i).getName());

                // set onclicklistener for datafield remove
                ImageButton deleteDatafieldButton = (ImageButton) datafieldView.findViewById(R.id.removeDatafieldButton);
                int finalI = i;
                deleteDatafieldButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // remove datafield from datafields list
                        datafields.remove(finalI);
                        // update dialog
                        updateDisplayedDatafieldsFromDemonstration();
                    }
                });

                // add datafield to dialog
                if (datafieldContainerLinearLayout != null) {
                    datafieldContainerLinearLayout.addView(datafieldView);
                } else {
                    Log.e("Dialog", "datafieldContainerLinearLayout is null");
                }
            }
        }
    }


    public void setCurrentScreenState(String currentScreenState) {
        this.currentScreenState = currentScreenState;
    }

    public String getCurrentScreenState() {
        return currentScreenState;
    }

    public Collector getCurrentCollector() {
        return collector;
    }

    public void setCurrentCollector(Collector collector) {
        this.collector = collector;
    }

    public static Boolean getIsEdit() {
        return isEdit;
    }

    public static void setIsEdit(Boolean isEdit) {
        CollectorConfigurationDialogWrapper.isEdit = isEdit;
    }

    public void setDatafields(ArrayList<Datafield> datafieldsList) {
        datafields = datafieldsList;
    }
}
