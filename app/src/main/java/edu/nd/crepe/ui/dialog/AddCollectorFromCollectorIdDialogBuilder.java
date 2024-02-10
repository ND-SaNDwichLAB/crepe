package edu.nd.crepe.ui.dialog;

import static edu.nd.crepe.MainActivity.currentUser;

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

import edu.nd.crepe.CrepeAccessibilityService;
import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.network.FirebaseCallback;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is used to build a dialog for adding a collector from a Collector ID.
 * The dialog will prompt the user to enter the collector ID and then add the collector to the user's profile.
 * This happens when a user wants to participate in a study with a new collector, so we make sure the collector is already created and not yet added to this user's profile.
 * It should not be used when a user wants to create a new collector
 */

public class AddCollectorFromCollectorIdDialogBuilder {

    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;
    private FirebaseCommunicationManager firebaseCommunicationManager;

    private Collector targetCollector;
    private ArrayList<Datafield> targetDatafields;
    private Boolean retrievalStatus = true;

    public AddCollectorFromCollectorIdDialogBuilder(Context c, Runnable runnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.refreshCollectorListRunnable = runnable;
        this.firebaseCommunicationManager = new FirebaseCommunicationManager(c);
        this.dbManager = DatabaseManager.getInstance(c);
        this.targetCollector = null;
        this.targetDatafields = new ArrayList<>();
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
                // show the keyboard when edittext is clicked
                InputMethodManager imm = (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                if (!collectorIdEditText.getText().toString().isEmpty()) {

                    // check if the collector already exists in local database (i.e. already added to the user's profile)
                    // if so, do not add it again
                    if (dbManager.getCollectorById(collectorIdEditText.getText().toString()) != null) {
                        Toast.makeText(c, "Collector already exists. Is it already in your list?", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                        return;
                    }

                    // if it does not exist, retrieve the collector from firebase and add it to the user's profile
                    // 3 steps:
                    // 1. add collector to local database
                    // 2. add collector to user's userCollectors list (local and firebase)
                    // 3. add the associated datafields to the local database
                    firebaseCommunicationManager.retrieveCollector(collectorIdEditText.getText().toString(), new FirebaseCallback<Collector>() {
                        public void onResponse(Collector result) {
                            targetCollector = result;
                        }
                        public void onErrorResponse(Exception e) {
                            retrievalStatus = false;
                            try {
                                Log.e("Firebase collector", "Failed to retrieve collector with the specified collector ID from firebase." + e.getMessage());
                            } catch (NullPointerException ex) {
                                Log.e("Firebase collector", "An unknown error occurred while retrieving collector with the specified collector ID from firebase.");
                            }
                        }

                    });

                    firebaseCommunicationManager.retrieveDatafieldsWithCollectorId(collectorIdEditText.getText().toString(), new FirebaseCallback<List<Datafield>>() {
                        public void onResponse(List<Datafield> results) {
                            targetDatafields.addAll(results);
                        }
                        public void onErrorResponse(Exception e) {
                            retrievalStatus = false;
                            try {
                                Log.e("Firebase datafield", "Failed to retrieve datafields with the specified collector ID from firebase." + e.getMessage());
                            } catch (NullPointerException ex) {
                                Log.e("Firebase datafield", "An unknown error occurred while retrieving datafields with the specified collector ID from firebase.");
                            }
                        }

                    });

                    // if retrieval is successful, do following
                    if (retrievalStatus) {
                        // 1. add collector to local database
                        dbManager.addOneCollector(targetCollector);

                        // 2a. add collector to user's userCollectors list (local)
                        dbManager.addCollectorForUser(targetCollector, currentUser);

                        // 2b. add collector to user's userCollectors list (firebase)
                        HashMap<String, Object> userUpdates = new HashMap<>();
                        ArrayList<String> updatedUserCollectors = currentUser.getCollectorsForCurrentUser();
                        updatedUserCollectors.add(targetCollector.getCollectorId());
                        userUpdates.put("userCollectors", updatedUserCollectors);
                        firebaseCommunicationManager.updateUser(currentUser.getUserId(), userUpdates);


                        // 3. add the associated datafields to the local database
                        for (Datafield dfield : targetDatafields) {
                            dbManager.addOneDatafield(dfield);
                        }

                        dialog.dismiss();
                        refreshCollectorListRunnable.run();
                        Toast.makeText(c, "Collector successfully added!", Toast.LENGTH_LONG).show();


                    } else {
                        Toast.makeText(c, "Failed to retrieve collector with the specified collector ID from firebase.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    }


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
                                .setMessage("The accessibility service is not enabled for " + Const.appName + ". Please enable the service in the phone settings before recording.")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                        c.startActivity(intent);
                                        //do nothing
                                    }
                                }).show();
                    }

                }
            }

        });
        return dialog;
    }

}
