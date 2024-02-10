package edu.nd.crepe.ui.dialog;

import static edu.nd.crepe.CrepeAccessibilityService.isAccessibilityServiceEnabled;
import static edu.nd.crepe.MainActivity.currentUser;

import android.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import edu.nd.crepe.CrepeAccessibilityService;
import edu.nd.crepe.MainActivity;
import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.demonstration.WidgetService;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.network.FirebaseCommunicationManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;

import java.io.Serializable;
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

    private AlertDialog dialog;
    private static Context context;
    private static String currentScreenState;
    private static Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;
    private FirebaseCommunicationManager firebaseCommunicationManager;
    private static View dialogMainView;

    private static List<Datafield> datafields = new ArrayList<>();

    private static CollectorConfigurationDialogWrapper singletonInstance = null;

    public static class GraphQueryCallback implements Callback, Serializable {
        @Override
        public void onDataReceived(String query, String targetText) {
            // datafield id format: collectorId%[index]
            String datafieldId = collector.getCollectorId() + "%" + String.valueOf(datafields.size());
            datafields.add(new Datafield(datafieldId, collector.getCollectorId(),query,targetText,true));
            updateDisplayedDatafieldsFromDemonstration(dialogMainView);
        }
    }
    private GraphQueryCallback graphQueryCallback = new GraphQueryCallback();

    CollectorConfigurationDialogWrapper(Context context, AlertDialog dialog, Collector collector, Runnable refreshCollectorListRunnable) {
        this.context = context;
        this.dialog = dialog;
        this.collector = collector;
        this.currentScreenState = "buildDialogFromConfig";
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(context);
        this.firebaseCommunicationManager = new FirebaseCommunicationManager(context);;
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

    public static void initializeInstance(Context context, AlertDialog dialog, Collector collector, Runnable refreshCollectorListRunnable) {
        if (singletonInstance != null) {
            Log.i("CollectorConfigDialog", "Overriding existing singleton instance...");
        }
        singletonInstance = new CollectorConfigurationDialogWrapper(context, dialog, collector, refreshCollectorListRunnable);
    }


    public void updateCurrentView() {

        switch (currentScreenState) {
            case "buildDialogFromConfig":

                collector.setCreatorUserId(currentUser.getUserId());

                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_create_collector_from_config, null);
                dialog.setContentView(dialogMainView);
                // buttons
                Button popupCancelBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogCancelButton);
                Button popupNextBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogNextButton);
                // spinners
                Spinner appDropDown = (Spinner) dialogMainView.findViewById(R.id.appSpinner);
//                Spinner locationDropDown = (Spinner) dialogMainView.findViewById(R.id.locationSpinner);

                // app spinner
                String[] appItems = {""};
                Dictionary<String, String> appPackageDict = new Hashtable<>();
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

                // When coming back from later popups using back button, if there's previously a selection made
                if (collector.getAppName() != null) {
                    for (int i = 0; i < appItems.length; i++) {
                        if (collector.getAppName().equals(appItems[i]))
                            appDropDown.setSelection(i);
                    }

                }

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
                        collectorStartDatePickerBuilder.setTitleText("COLLECTOR START DATE");
                        collectorStartDatePickerBuilder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        final MaterialDatePicker collectorStartDatePicker = collectorStartDatePickerBuilder.build();

                        collectorStartDatePicker.show(((MainActivity) context).getSupportFragmentManager(), "tag");

                        collectorStartDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override
                            public void onPositiveButtonClick(Long selection) {
                                collector.setCollectorStartTime(selection.longValue());
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
                        collectorEndDatePickerBuilder.setTitleText("COLLECTOR END DATE");
                        collectorEndDatePickerBuilder.setSelection(MaterialDatePicker.todayInUtcMilliseconds());
                        final MaterialDatePicker collectorEndDatePicker = collectorEndDatePickerBuilder.build();

                        collectorEndDatePicker.show(((MainActivity) context).getSupportFragmentManager(), "tag");

                        collectorEndDatePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
                            @Override
                            public void onPositiveButtonClick(Long selection) {
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

                Dictionary<String, String> finalAppPackageDict = appPackageDict;
                popupNextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int blankFlag = 0;
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
                            blankFlag = 1;
                        }

                        // update location info
//                        String location = locationDropDown.getSelectedItem().toString();
//                        if (location != " ") {
//                            collector.setMode(location);
//                        } else {
//                            // set the border of spinner to red
//                            Context currentContext = context.getApplicationContext();
//                            Toast.makeText(currentContext, "Please select a location!", Toast.LENGTH_LONG).show();
//                            blankFlag = 1;
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

                        // set the id of the collector
                        // format: userId%appName%timestamp. We will remove any space in the appName
                        collector.setCollectorId(currentUser.getUserId() + "%" + collector.getAppName().replaceAll(" ", "") + "%" + String.valueOf(System.currentTimeMillis()));

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
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_create_collector_from_config_graph_query, null);
                dialog.setContentView(dialogMainView);
                Button graphQueryNxtBtn = (Button) dialogMainView.findViewById(R.id.graphQueryNextButton);
                Button graphQueryBckBtn = (Button) dialogMainView.findViewById(R.id.graphQueryBackButton);
                LinearLayout datafieldContainerLinearLayout = (LinearLayout) dialogMainView.findViewById(R.id.datafieldContainerLinearLayout);
//                Button graphQueryAddBtn = (Button) dialogMainView.findViewById(R.id.graphQueryAddAnotherButton);
                Button openAppButton = (Button) dialogMainView.findViewById(R.id.openAppButton);
                ImageButton graphQueryCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeGraphQueryPopupImageButton);

                // update interface elements based on the specified app in the previous popup box
                TextView commentOnOpenAppButton = (TextView) dialogMainView.findViewById(R.id.commentOnOpenAppButton);
                String appName = collector.getAppName();
                openAppButton.setText("Open " + appName);
                commentOnOpenAppButton.setText("Demonstrate in the " + appName +" app");

                // modify content of the popup box based on current state
                updateDisplayedDatafieldsFromDemonstration(dialogMainView);


                // Open App button
                openAppButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // check if the accessibility service is running
                        Boolean accessibilityServiceEnabled = isAccessibilityServiceEnabled(context, CrepeAccessibilityService.class);

                        // if accessibility service is not on
                        if (!accessibilityServiceEnabled) {
                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                            builder.setTitle("Service Permission Required")
                                    .setMessage(Const.appName + " needs accessibility service to function. Please enable it in the phone settings.")
                                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                            context.startActivity(intent);
                                            //do nothing
                                        }
                                    }).show();
                        } else {
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
                            if (!Settings.canDrawOverlays(context)){

                                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                                builder.setTitle("Service Permission Required")
                                        .setMessage(Const.appName + " needs the permission to display over other app for proper function. Please enable the service in the phone settings.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                context.startActivity(intent);
                                            }
                                        }).show();
                            } else {
                                WidgetService widgetService = new WidgetService();
                                Intent intent = new Intent(context, widgetService.getClass());
                                intent.putExtra("graphQueryCallback", graphQueryCallback);
                                context.startService(intent);
                                finish();
                            }
                        }
                    }
                });

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

                        if (datafields.size() == 0) {
                            // remind user to add graph query
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please demonstrate the data to collect!", Toast.LENGTH_LONG).show();
                            blankFlag = 1;
                        }
                        if (blankFlag == 0) {
                            // update currentScreen String value
                            currentScreenState = "buildDialogFromConfigDescription";
                            // recursively call itself with new currentScreen String value
                            updateCurrentView();
                        }
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
                        dialog.dismiss();
                        // clear the current datafields list, since this collector creation process is terminated
                        clearDatafields();
                    }
                });
                break;


            case "buildDialogFromConfigDescription":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_create_collector_from_config_description, null);
                dialog.setContentView(dialogMainView);

                Button descriptionCreateBtn = (Button) dialogMainView.findViewById(R.id.descriptionCreateButton);
                Button descriptionBckBtn = (Button) dialogMainView.findViewById(R.id.descriptionBackButton);
                ImageButton descriptionCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeDescriptionImageButton);
                EditText descriptionEditText = (EditText) dialogMainView.findViewById(R.id.descriptionEditText);

                // show description in the collector if available
                if (collector.getDescription() != null) {
                    descriptionEditText.setText(collector.getDescription());
                }

                descriptionBckBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigGraphQuery";
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



                        // save locally
                        dbManager.addOneCollector(collector);
                        // save to Firebase
                        firebaseCommunicationManager.putCollector(collector).addOnSuccessListener(suc->{
                            Log.i("Firebase","Successfully added collector " + collector.getCollectorId() + " to firebase.");
                        }).addOnFailureListener(er->{
                            Log.e("Firebase","Failed to add collector " + collector.getCollectorId() + " to firebase.");
                        });

                        // store the data fields into database
                        for (Datafield datafield : datafields) {
                            dbManager.addOneDatafield(datafield);
                            firebaseCommunicationManager.putDataField(datafield).addOnCompleteListener(task -> {
                                Log.i("Firebase","Successfully added datafield " + datafield.getDatafieldId() + " to firebase.");
                            }).addOnFailureListener(er->{
                                Log.e("Firebase","Failed to add datafield " + datafield.getDatafieldId() + " to firebase. Error: " + er.getMessage());
                            });
                        }

                        // Note: we do not need to update the field "userCollectors" under User (see /database/User.java),
                        // be cause the user is the creator of this collector, and this piece of info is already stored in the Collector object (see /database/Collector.java) under the field "creatorUserId"

                        // clear the current datafields list, since this collector creation process is done
                        clearDatafields();

                        // update all collectors' status, before refreshing the collector list
                        // to make sure we only display active collectors.
                        CrepeAccessibilityService.getsSharedInstance().refreshCollector();
                        // a callback to refresh homepage every time
                        refreshCollectorListRunnable.run();

                        // update currentScreen String value, recursively call itself with new currentScreen String value
                        currentScreenState = "buildDialogFromConfigSuccessMessage";
                        updateCurrentView();
                    }
                });

                descriptionCloseImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                        // clear the current datafields list, since this collector creation process is terminated
                        clearDatafields();
                    }
                });

                break;


            case "buildDialogFromConfigSuccessMessage":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_create_collector_from_config_success_message, null);
                dialog.setContentView(dialogMainView);

                // update the displayed app info
                TextView successMessageTextView = (TextView) dialogMainView.findViewById(R.id.shareText);
                successMessageTextView.setText("Your collector for " + collector.getAppName() + " is created. Share with your participants");

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
                        Toast.makeText(context,"collector ID copied to clipboard " + collector.getCollectorId(), Toast.LENGTH_LONG).show();
                        currentScreenState = "dismissed";
                    }
                });


                closeSuccessMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        // update currentScreen String value
                        currentScreenState = "dismissed";
                        dialog.dismiss();

                    }
                });
                break;

            case "dismissed":
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + currentScreenState);

        }
    }

    private void clearDatafields() {
        // remove all elements from datafields
        datafields.clear();
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
//                apps[i] = name;
                appDict.put(name, ri.activityInfo.packageName);
                i++;
            }
        }
//        Toast.makeText(context, ril.size() + " apps are installed on this phone", Toast.LENGTH_LONG).show();
        return appDict;
    }

    public void show() {
        dialog.show();
        updateCurrentView();
    }

    private static void updateDisplayedDatafieldsFromDemonstration(View dialogMainView) {
        // we only update if the current screen is the demonstration screen
        if (currentScreenState == "buildDialogFromConfigGraphQuery") {

            Button openAppButton = (Button) dialogMainView.findViewById(R.id.openAppButton);

            refreshDatafieldsList();

            if (datafields.size() == 0) {

            } else {
                openAppButton.setText("Add another");

            }
        } else {
            Log.e("Dialog", "updateDisplayedDatafieldsFromDemonstration() called when currentScreenState is not buildDialogFromConfigGraphQuery");
        }
    }

    private static void refreshDatafieldsList() {
        // remove all subviews from the linearlayout
        LinearLayout datafieldContainerLinearLayout = (LinearLayout) dialogMainView.findViewById(R.id.datafieldContainerLinearLayout);
        datafieldContainerLinearLayout.removeAllViews();

        // inflate datafields into the dialog
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
                    updateDisplayedDatafieldsFromDemonstration(dialogMainView);
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

    public String getCurrentScreenState() {
        return dialog.isShowing() ? currentScreenState : null;
    }

    public void setCurrentScreenState(String currentScreenState) {
        this.currentScreenState = currentScreenState;
    }

    public Collector getCurrentCollector() {
        return collector;
    }
}
