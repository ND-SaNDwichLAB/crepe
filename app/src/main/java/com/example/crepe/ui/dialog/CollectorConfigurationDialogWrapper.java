package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.crepe.MainActivity;
import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.demosntration.WidgetService;
import com.example.crepe.network.ServerCollectorCommunicationManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.gson.Gson;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class CollectorConfigurationDialogWrapper {

    private AlertDialog dialog;
    private Context context;
    private String currentScreenState;
    private Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;

    CollectorConfigurationDialogWrapper(Context context, AlertDialog dialog, Collector collector, Runnable refreshCollectorListRunnable) {
        this.context = context;
        this.dialog = dialog;
        this.collector = collector;
        this.currentScreenState = "buildDialogFromConfig";
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = new DatabaseManager(context);
    }

    public void updateCurrentView() {
        View dialogMainView;

        switch (currentScreenState) {
            case "buildDialogFromConfig":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config, null);
                dialog.setContentView(dialogMainView);
                // buttons
                Button popupCancelBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogCancelButton);
                Button popupNextBtn = (Button) dialogMainView.findViewById(R.id.addCollectorFromConfigDialogNextButton);
                // spinners
                Spinner appDropDown = (Spinner) dialogMainView.findViewById(R.id.appSpinner);
                Spinner locationDropDown = (Spinner) dialogMainView.findViewById(R.id.locationSpinner);

                // app spinner
                String[] appItems = {""};
                try {
                    appItems = getAllInstalledAppNames();
                } catch (PackageManager.NameNotFoundException e) {
                    e.getMessage();
                }
                ArrayAdapter<String> appAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, appItems);
                appAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                appDropDown.setAdapter(appAdapter);

                // When coming back from later popups using back button, if there's previously a selection made
                if (collector.getAppName() != null) {
                    int i;
                    for (i = 0; i < appItems.length; i++) {
                        if (collector.getAppName() == appItems[i])
                            break;
                    }
                    appDropDown.setSelection(i);
                }

                // location spinner
                String[] locationItems = new String[]{" ", "Local", "Remote"};
                ArrayAdapter<String> locationAdapter = new ArrayAdapter<String>(context.getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locationItems);
                locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                locationDropDown.setAdapter(locationAdapter);
                // When coming back from later popups using back button, if there's previously a selection made
                if (collector.getMode() != null) {
                    int i;
                    for (i = 1; i < locationItems.length; i++) {
                        if (collector.getMode() == locationItems[i])
                            break;
                    }
                    locationDropDown.setSelection(i);
                }

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
                        collectorStartDatePickerBuilder.setTitleText("SELECT THE COLLECTOR START DATE");
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
                        collectorEndDatePickerBuilder.setTitleText("SELECT THE COLLECTOR END DATE");
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

                popupNextBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int blankFlag = 0;
                        // update app info
                        String appName = appDropDown.getSelectedItem().toString();
                        if (appName != " ") {
                            collector.setAppName(appName);
                        } else {
                            // set the border of spinner to red
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please select an app!", Toast.LENGTH_LONG).show();
                            blankFlag = 1;
                        }

                        // update location info
                        String location = locationDropDown.getSelectedItem().toString();
                        if (location != " ") {
                            collector.setMode(location);
                        } else {
                            // set the border of spinner to red
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please select a location!", Toast.LENGTH_LONG).show();
                            blankFlag = 1;
                        }

                        // update date info
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                        ParsePosition pp1 = new ParsePosition(0);
                        Date startDate = dateFormat.parse(startDateText.getText().toString(), pp1);
                        collector.setCollectorStartTime(startDate.getTime());

                        ParsePosition pp2 = new ParsePosition(1);
                        Date endDate = dateFormat.parse(endDateText.getText().toString(), pp2);
                        collector.setCollectorEndTime(endDate.getTime());

                        // After successfully set the collector's end time, automatically set its status
                        collector.autoSetCollectorStatus();
                        // Update in database as well
                        dbManager.updateCollectorStatus(collector);

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
                Button openAppBtn = (Button) dialogMainView.findViewById(R.id.openAppButton);
                ImageButton graphQueryCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeGraphQueryPopupImageButton);
                EditText graphQueryEditTxt = (EditText) dialogMainView.findViewById(R.id.graphQueryEditText);

                // update interface elements based on the specified app in the previous popup box
                Button openAppButton = (Button) dialogMainView.findViewById(R.id.openAppButton);
                TextView commentOnOpenAppButton = (TextView) dialogMainView.findViewById(R.id.commentOnOpenAppButton);
                String appName = collector.getAppName();
                openAppButton.setText("Open " + appName);
                commentOnOpenAppButton.setText("Demonstrate in the " + appName +" app");

                // TODO: finish graph query
                // Open App button
                openAppBtn.setOnClickListener(new View.OnClickListener() {
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
                        if (!Settings.canDrawOverlays(context)){
                            getPermission();
                        } else {
                            Intent intent = new Intent(context, WidgetService.class);
                            context.startService(intent);
                        }
                    }
                });

                // show graph query info in the collector if available
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
                        if (graphQueryContent != null) {
                            collector.setCollectorGraphQuery(graphQueryContent);
                        } else {
                            // remind user to add graph query
                            Context currentContext = context.getApplicationContext();
                            Toast.makeText(currentContext, "Please type in the graph query!", Toast.LENGTH_LONG).show();
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


            case "buildDialogFromConfigDataField":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_data_field, null);
                dialog.setContentView(dialogMainView);

                Button dataFieldNxtBtn = (Button) dialogMainView.findViewById(R.id.dataFieldNextButton);
                Button dataFieldBckBtn = (Button) dialogMainView.findViewById(R.id.dataFieldBackButton);
                ImageButton dataFieldCloseImg = (ImageButton) dialogMainView.findViewById(R.id.closeDataFieldImageButton);
                EditText dataFieldEditText = (EditText) dialogMainView.findViewById(R.id.dataFieldEditText);

                // show data fields info in the collector if available
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

                // show description in the collector if available
                if (collector.getDescription() != null) {
                    descriptionEditText.setText(collector.getDescription());
                }

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

                        // save the collector to database
                        // add a callback to refresh homepage every time
                        DatabaseManager dbManager = new DatabaseManager(context);
                        dbManager.addOneCollector(collector);

                        // update currentScreen String value
                        currentScreenState = "buildDialogFromConfigSuccessMessage";
                        // recursively call itself with new currentScreen String value
                        refreshCollectorListRunnable.run();
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


            case "buildDialogFromConfigSuccessMessage":
                dialogMainView = LayoutInflater.from(context).inflate(R.layout.dialog_add_collector_from_config_success_message, null);
                dialog.setContentView(dialogMainView);

                // TODO: QUESTION – what's the best way to encode url?
                // TODO: Create a new class to handle url generation e.g. collectorUrlManager
                //      1. create url
                //      2. get collector from url
                collector.setCollectorId("9");
                ServerCollectorCommunicationManager sccManager = new ServerCollectorCommunicationManager(context);
                try {
                    sccManager.uploadJsonToServer(collector);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Create url for current collector
                String collectorURL = "http://35.222.12.92:8000?id=" + collector.getCollectorId();

                Button closeSuccessMessage = (Button) dialogMainView.findViewById(R.id.closeSuccessMessagePopupButton);
                ImageButton shareUrlLinkButton = (ImageButton) dialogMainView.findViewById(R.id.shareUrlImageButton);
                ImageButton shareEmailLinkButton = (ImageButton) dialogMainView.findViewById(R.id.shareUrlImageButton);

                shareEmailLinkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // share email link

                    }
                });

                shareUrlLinkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // share url link
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("share URL", collectorURL);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context," Copied URL to clipboard", Toast.LENGTH_LONG).show();
                    }
                });


                closeSuccessMessage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

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

    public String[] getAllInstalledAppNames() throws PackageManager.NameNotFoundException {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // get list of all the apps installed
        // ril stands for ResolveInfoList
        List<ResolveInfo> ril = context.getPackageManager().queryIntentActivities(mainIntent, 0);
//        List<String> componentList = new ArrayList<String>();
        String name = null;
        int i = 0;

        // get size of ril and create a list
        String[] apps = new String[ril.size()];
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
                apps[i] = name;
                i++;
            }
        }
//        Toast.makeText(context, ril.size() + " apps are installed on this phone", Toast.LENGTH_LONG).show();
        return apps;
    }

    public void getPermission() {
//         if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)){
//            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+context.getPackageName()));
//            context.startActivity(intent);
//        check if we already have permission to draw over other apps
//    }
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23) {
            if (!Settings.canDrawOverlays(context)) {
                // if not construct intent to request permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // request permission via start activity for result
                context.startActivity(intent);

            }
        }
    }

    public void show() {
        dialog.show();
        updateCurrentView();
    }
}
