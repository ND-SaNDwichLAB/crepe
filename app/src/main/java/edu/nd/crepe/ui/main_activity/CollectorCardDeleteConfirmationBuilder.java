package edu.nd.crepe.ui.main_activity;

import static edu.nd.crepe.MainActivity.currentUser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.database.User;
import edu.nd.crepe.network.FirebaseCommunicationManager;

public class CollectorCardDeleteConfirmationBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Collector collector;
    private Dialog parentDialog;
    private DatabaseManager dbManager;
    private Runnable refreshCollectorListRunnable;
    private FirebaseCommunicationManager fbManager;
    private enum UserRole {ADMIN, USER};


    public CollectorCardDeleteConfirmationBuilder(Context c, Collector collector, Dialog parentDialog, Runnable refreshCollectorListRunnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.collector = collector;
        this.parentDialog = parentDialog;
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(c);
        this.fbManager = new FirebaseCommunicationManager(c);
    }

    public Dialog build() {

        UserRole userRole = getUserRole();

        // new popup to confirm
        View deleteCollectorConfirmationView = LayoutInflater.from(c).inflate(R.layout.delete_collector_confirmation, null);
        if (deleteCollectorConfirmationView.getParent() != null) {
            ((ViewGroup)deleteCollectorConfirmationView.getParent()).removeView(deleteCollectorConfirmationView); // <- remove the view from its parent
        }
        dialogBuilder.setView(deleteCollectorConfirmationView);
        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // set the content of the confirmation based on users' role
        TextView confirmationDetail = (TextView) deleteCollectorConfirmationView.findViewById(R.id.collectorDetailContent);
        if (userRole == UserRole.ADMIN) {
            confirmationDetail.setText("You are the admin of this collector and deleting it will stop the data collection on all participants' phones. You can still get existing collected data. Are you sure you want to proceed?");
        }
        if (userRole == UserRole.USER) {
            confirmationDetail.setText("By deleting this collector, you will no longer contribute data to this collector. The creator of this collector will be notified. Are you sure you want to proceed?");
        }

        Button confirmBtn = (Button) deleteCollectorConfirmationView.findViewById(R.id.confirmDeleteButton);
        Button cancelBtn = (Button) deleteCollectorConfirmationView.findViewById(R.id.confirmCancelButton);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userRole == UserRole.ADMIN) {
                    // This will set the status of collector to deleted instead of directly removing it
                    // it will still be present in database but won't be displayed
                    collector.setStatusDeleted();
                    dbManager.updateCollectorStatus(collector);
                    // we do not really remove datafields, since they are queried through collectors.
                    // once the collector status is set "deleted", the datafields will not be queried anymore

                    // also delete the collector from firebase
                    fbManager.setCollectorStatusDeleted(collector.getCollectorId());
                } else if (userRole == UserRole.USER) {
                    // remove the collector from the user's list
                    User currentUser = dbManager.getAllUsers().get(0);
                    // local database change
                    dbManager.removeCollectorForUser(collector, currentUser);
                    // update to firebase too
                    HashMap<String, Object> userUpdates = new HashMap<>();
                    ArrayList<String> updatedUserCollectors = currentUser.getCollectorsForCurrentUser();
                    updatedUserCollectors.remove(collector.getCollectorId());
                    userUpdates.put("userCollectors", updatedUserCollectors);
                    fbManager.updateUser(currentUser.getUserId(), userUpdates);
                    // update in the variable too, just in case
                    currentUser.removeCollectorForCurrentUser(collector);

                    // TODO YUWEN notify the creator of the collector that their participants dropped

                }

                // update the home fragment list
                refreshCollectorListRunnable.run();
                dialog.dismiss();
                Toast.makeText(c, "Collector for " + collector.getAppName() + " is deleted", Toast.LENGTH_LONG).show();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                parentDialog.show();
            }
        });

        return dialog;
    }

    private UserRole getUserRole() {
        List<User> allUsers = dbManager.getAllUsers();
        if (allUsers.size() > 0) {
            Log.e("CollectorCardDeleteConfirmationBuilder", "More than one user found in the database");
        } else if (allUsers.size() == 0) {
            Log.e("CollectorCardDeleteConfirmationBuilder", "No user found in the database");
            return null;
        }

        User currentUser = allUsers.get(0);
        // check if the current user has this collector in its userCollector list
        if (currentUser.getCollectorsForCurrentUser().contains(collector.getCollectorId())) {
            return UserRole.USER;
        } else if (collector.getCreatorUserId().equals(currentUser.getUserId())) {
            return UserRole.ADMIN;
        } else {
            Log.e("CollectorCardDeleteConfirmationBuilder", "User is not admin or user for this collector");
            return null;
        }

    }

}








