package edu.nd.crepe.ui.main_activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.network.FirebaseCommunicationManager;

public class CollectorCardDeleteConfirmationBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Collector collector;
    private Dialog parentDialog;
    private DatabaseManager dbManager;
    private Runnable refreshCollectorListRunnable;
    private FirebaseCommunicationManager fbManager;

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
        // new popup to confirm
        View deleteCollectorConfirmationView = LayoutInflater.from(c).inflate(R.layout.delete_collector_confirmation, null);
        if (deleteCollectorConfirmationView.getParent() != null) {
            ((ViewGroup)deleteCollectorConfirmationView.getParent()).removeView(deleteCollectorConfirmationView); // <- remove the view from its parent
        }
        dialogBuilder.setView(deleteCollectorConfirmationView);
        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button confirmBtn = (Button) deleteCollectorConfirmationView.findViewById(R.id.confirmDeleteButton);
        Button cancelBtn = (Button) deleteCollectorConfirmationView.findViewById(R.id.confirmCancelButton);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(c, "Collector for " + collector.getAppName() + " is deleted", Toast.LENGTH_LONG).show();
                // This will set the status of collector to deleted instead of directly removing it
                // it will still be present in database but won't be displayed
                collector.setStatusDeleted();
                dbManager.updateCollectorStatus(collector);
                // we do not really remove datafields, since they are queried through collectors.
                // once the collector status is set "deleted", the datafields will not be queried anymore

                // also delete the collector from firebase
                fbManager.setCollectorStatusDeleted(collector.getCollectorId());

                // update the home fragment list
                refreshCollectorListRunnable.run();
                dialog.dismiss();
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

}








