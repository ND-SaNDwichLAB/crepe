package com.example.crepe.ui.dialog;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.crepe.CrepeAccessibilityService;
import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.database.Datafield;
import com.example.crepe.graphquery.Const;
import com.example.crepe.network.FirebaseCallback;
import com.example.crepe.network.FirebaseCommunicationManager;
import com.google.gson.Gson;

import java.util.List;

public class CreateCollectorFromURLDialogBuilder {

    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Runnable refreshCollectorListRunnable;

    public CreateCollectorFromURLDialogBuilder(Context c, Runnable runnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.refreshCollectorListRunnable = runnable;

    }

    public Dialog build(){
        final View popupView = LayoutInflater.from(c).inflate(R.layout.dialog_add_collector_from_url, null);
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();
        Button popupCancelBtn = (Button) popupView.findViewById(R.id.addFromUrlCancelButton);
        Button popupNextBtn = (Button) popupView.findViewById(R.id.addFromUrlAddButton);
        EditText collectorIdEditText = (EditText) popupView.findViewById(R.id.collectorIdEditText);

        popupCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        popupNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // download collector from URL
                Gson gson = new Gson();
                // show the keyboard when edittext is clicked
                InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                if (!collectorIdEditText.getText().toString().isEmpty()) {
                    // Firebase
                    FirebaseCommunicationManager firebaseCommunicationManager = new FirebaseCommunicationManager(c);
//                    firebaseCommunicationManager.retrieveCollector(urlText.getText().toString(), new FirebaseCallback() {
//                        @Override
//                        public void onResponse(Collector result) {
//                            DatabaseManager dbManager = new DatabaseManager(c);
//                            dbManager.addOneCollector(result);
//                            refreshCollectorListRunnable.run();
//                            List<Collector> collectors = dbManager.getAllCollectors();
//                            for (Collector collector : collectors) {
//                                System.out.println(collector.toString());
//                            }
//                        }
//                    });

                    DatabaseManager dbManager = new DatabaseManager(c);
                    firebaseCommunicationManager.retrieveCollector(collectorIdEditText.getText().toString(), new FirebaseCallback<Collector>() {
                        public void onResponse(Collector result) {
                            dbManager.addOneCollector(result);
                            refreshCollectorListRunnable.run();
                        }
                        public void onErrorResponse(Exception e) {
                            try {
                                Log.e("Firebase collector", e.getMessage());
                            } catch (NullPointerException ex) {
                                Log.e("Firebase collector", "An unknown error occurred.");
                            }
                        }

                    });

                    firebaseCommunicationManager.retrieveDatafieldswithCollectorId(collectorIdEditText.getText().toString(), new FirebaseCallback<List<Datafield>>() {
                        public void onResponse(List<Datafield> results) {
                            for (Datafield result : results) {
                                dbManager.addOneDatafield(result);
                            }
                        }
                        public void onErrorResponse(Exception e) {
                            try {
                                Log.e("Firebase datafield", e.getMessage());
                            } catch (NullPointerException ex) {
                                Log.e("Firebase datafield", "An unknown error occurred.");
                            }
                        }

                    });


                    // next popup
                    dialog.dismiss();
//                    CreateCollectorFromURLDialogSuccessMessage nextPopup = new CreateCollectorFromURLDialogSuccessMessage(c);
//                    nextPopup.build();
                    Toast.makeText(c, "Collector successfully added!", Toast.LENGTH_LONG).show();

                    // enable accessibility service
                    // check if the accessibility service is running
                    Boolean accessibilityServiceRunning = false;
                    ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
                    Class clazz = CrepeAccessibilityService.class;

                    if (manager != null) {
                        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                            if (clazz.getName().equals(service.service.getClassName())) {
                                accessibilityServiceRunning = true;
                            }
                        }
                    }

                    // if accessibility service is not on
                    if (!accessibilityServiceRunning) {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(c);
                        builder1.setTitle("Service Permission Required")
                                .setMessage("The accessibility service is not enabled for " + Const.appNameUpperCase + ". Please enable the service in the phone settings before recording.")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                        c.startActivity(intent);
                                        //do nothing
                                    }
                                }).show();
                    }

                } else {
                    Toast.makeText(c,"Please enter a valid collector ID", Toast.LENGTH_LONG).show();
                }
            }

        });
        return dialog;
    }

}
