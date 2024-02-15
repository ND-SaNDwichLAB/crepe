package edu.nd.crepe.ui.dialog;

import static edu.nd.crepe.MainActivity.currentUser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.network.FirebaseCallback;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import java.util.ArrayList;
import java.util.Dictionary;
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

    public AddCollectorFromCollectorIdDialogBuilder(Context c, Runnable runnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.refreshCollectorListRunnable = runnable;
        this.firebaseCommunicationManager = new FirebaseCommunicationManager(c);
        this.dbManager = DatabaseManager.getInstance(c);
        this.targetCollector = null;
        this.targetDatafields = new ArrayList<>();
    }

    public Dialog build() {
        final View popupView = LayoutInflater.from(c).inflate(R.layout.dialog_add_collector_from_collector_id, null);
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button popupCancelBtn = (Button) popupView.findViewById(R.id.addFromUrlCancelButton);
        Button popupNextBtn = (Button) popupView.findViewById(R.id.addFromUrlAddButton);
        EditText collectorIdEditText = (EditText) popupView.findViewById(R.id.collectorIdEditText);

        // first, try to update all collector status in firebase
        firebaseCommunicationManager.updateAllCollectors();

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
                String collectorId = collectorIdEditText.getText().toString();

                // handle edge cases
                if (collectorId.isEmpty()) {
                    Toast.makeText(c, "Please enter collector ID!", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!isValidCollectorId(collectorId)) {
                    Toast.makeText(c, "Invalid collector ID!", Toast.LENGTH_LONG).show();
                    return;
                }

                // check if the collector already exists in participant's userCollectors list
                // if so, do not add it again
                if (dbManager.userParticipationStatusForCollector(currentUser.getUserId(), collectorIdEditText.getText().toString())) {
                    Toast.makeText(c, "You are already participating in this collection!", Toast.LENGTH_LONG).show();
                    return;
                }


                // now, try to retrieve the collector from firebase and add it to the user's profile
                // 3 steps:
                // 1. add collector to local database
                // 2. add collector to user's userCollectors list (local and firebase)
                // 3. add the associated datafields to the local database

                // get all collectorIds from firebase, to make sure the collector exists on firebase
                firebaseCommunicationManager.retrieveAllCollectors(new FirebaseCallback<List<Collector>>() {
                    public void onResponse(List<Collector> result) {
                        if (result.size() == 0) {
                            Log.i("Firebase", "No collectors found in Firebase.");
                        } else {
                            for (Collector collector : result) {
                                // if the collector is found in firebase
                                if (collector.getCollectorId().equals(collectorId)) {
                                    targetCollector = collector;

                                    Boolean isTargetAppInstalled = false;
                                    // 0. first, check if the collector's app exist on the participant's phone
                                    // Get a list of installed apps.
                                    PackageManager packageManager = c.getPackageManager();
                                    List<ApplicationInfo> apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

                                    // Iterate over the applications and check if any is the same as the target collector's app
                                    for (ApplicationInfo app : apps) {
                                        String appPackageName = app.packageName;
                                        if (appPackageName.equals(targetCollector.getAppPackage())) {
                                            // if the app is found, then, add the collector
                                            isTargetAppInstalled = true;
                                            break;
                                        }
                                    }
                                    if (!isTargetAppInstalled) {
                                        // if the app is not found, then, show a message
                                        Toast.makeText(c, "You need to install " + targetCollector.getAppName() + " to participate in this collector study", Toast.LENGTH_LONG).show();
                                        return;
                                    }

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
                                }
                            }
                        }

                        onComplete();
                    }

                    public void onErrorResponse(Exception e) {
                        Log.e("Firebase", "Failed to retrieve collectors from Firebase.");
                    }

                    public void onComplete() {
                        // if the collector is not found in firebase
                        if (targetCollector == null) {
                            Toast.makeText(c, "Invalid collector ID!", Toast.LENGTH_LONG).show();
//                            dialog.dismiss();
                        } else {
                            // if the collector is found, then, retrieve the data fields
                            firebaseCommunicationManager.retrieveDatafieldsWithCollectorId(collectorIdEditText.getText().toString(), new FirebaseCallback<List<Datafield>>() {
                                public void onResponse(List<Datafield> results) {
                                    targetDatafields.addAll(results);
                                    onComplete();
                                }

                                public void onErrorResponse(Exception e) {
                                    Toast.makeText(c, "Failed to retrieve datafields for the collector.", Toast.LENGTH_LONG).show();
                                    onComplete();
                                }

                                public void onComplete() {
                                    // Check if retrieval was successful
                                    if (targetDatafields.size() > 0 && targetCollector != null) {
                                        // Perform the database update and UI refresh
                                        // 3. add the associated datafields to the local database
                                        for (Datafield dfield : targetDatafields) {
                                            dbManager.addOneDatafield(dfield);
                                        }

                                        dialog.dismiss();
                                        refreshCollectorListRunnable.run();
                                        Toast.makeText(c, "Collector successfully added!", Toast.LENGTH_LONG).show();

                                    } else {
                                        Toast.makeText(c, "Failed to retrieve datafields for the collector.", Toast.LENGTH_LONG).show();
                                        dialog.dismiss();
                                    }
                                }
                            });
                        }
                    }
                });



            }
        });
        return dialog;
    }

    private boolean isValidCollectorId(String collectorId) {
        // Check if collectorId is null or contains invalid characters
        return collectorId != null && !collectorId.contains(".") && !collectorId.contains("#")
                && !collectorId.contains("$") && !collectorId.contains("[") && !collectorId.contains("]");
    }
}
