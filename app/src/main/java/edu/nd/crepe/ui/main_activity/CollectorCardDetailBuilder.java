package edu.nd.crepe.ui.main_activity;

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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.database.User;
import edu.nd.crepe.network.FirebaseCallback;
import edu.nd.crepe.network.FirebaseCommunicationManager;
import edu.nd.crepe.ui.dialog.CollectorConfigurationDialogWrapper;
import edu.nd.crepe.ui.dialog.CreateCollectorFromConfigDialogBuilder;

import java.util.List;

public class CollectorCardDetailBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;
    private FirebaseCommunicationManager fbManager;

    // add for editing function 02/25 qi
    private CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder;
    private CollectorConfigurationDialogWrapper wrapper;

    public CollectorCardDetailBuilder(Context c, Collector collector, Runnable refreshCollectorListRunnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.collector = collector;
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(c);
        this.fbManager = new FirebaseCommunicationManager(c);
        this.createCollectorFromConfigDialogBuilder = new CreateCollectorFromConfigDialogBuilder(c, refreshCollectorListRunnable);
    }

    public Dialog build() {
        final View popupView = LayoutInflater.from(c).inflate(R.layout.collector_detail, null);
        if (popupView.getParent() != null) {
            ((ViewGroup)popupView.getParent()).removeView(popupView); // <- remove the view from its parent
        }
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // populate information of the collector
        TextView creatorTitleTextView = (TextView) popupView.findViewById(R.id.collectorCreatorTitle);
        TextView creatorNameTextView = (TextView) popupView.findViewById(R.id.collectorCreatorName);
        String creatorUserId = collector.getCreatorUserId();
        List<User> allUsers = dbManager.getAllUsers();
        // if it is created by current user, change the title to "Created By You" and show the participant count
        // otherwise, show the creator's name
        Boolean createdByCurrentUser = false;
        for (User user : allUsers) {
            if (user.getUserId().equals(creatorUserId)) {
                creatorNameTextView.setText(user.getName() + " (you)");
                createdByCurrentUser = true;
                break;
            }
        }
        if (!createdByCurrentUser) {
            fbManager.retrieveUser(creatorUserId, new FirebaseCallback() {
                @Override
                public void onResponse (Object user) {
                    User creator = (User) user;
                    creatorNameTextView.setText(creator.getName());
                }

                @Override
                public void onErrorResponse(Exception e) {
                    Log.e("CollectorCardDetailBuilder", "Error retrieving creator user: " + e.getMessage());
                    creatorTitleTextView.setVisibility(View.GONE);
                    creatorNameTextView.setVisibility(View.GONE);
                }
            });
        }


        // populate the datafield information
        List<Datafield> datafieldsForCollector = dbManager.getAllDatafieldsForCollector(collector);
        // TODO Yuwen - retrieve datafields from firebase
//        List<Datafield> datafieldsForCollectorFromFirebase = fbManager.retrieveDatafieldsWithCollectorId(collector.getCollectorId());

        TextView collectorDatafield = (TextView) popupView.findViewById(R.id.collectorDetailDatafield);
        if (datafieldsForCollector.size() > 0) {
            for (Datafield datafield : datafieldsForCollector) {
                collectorDatafield.append("\"" + datafield.getName() + "\"\n\n");
            }
        } else {
            collectorDatafield.setText("No datafields available");
        }

        ImageButton collectorShareButton = (ImageButton) popupView.findViewById(R.id.collectorShareButton);
        collectorShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // we will just share the id of the collector for now, instead of a url
                ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("share URL", collector.getCollectorId());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(c,"collector ID copied. Share with your participants!", Toast.LENGTH_LONG).show();
            }
        });

        ImageButton collectorEditButton = (ImageButton) popupView.findViewById(R.id.collectorEditButton);
        collectorEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // dismiss current dialog

                // TODO Qi Zhao: finish the logic for the edit button
                // check display over other apps permission
//                if (!Settings.canDrawOverlays(c)) {
//                    Dialog enableDisplayServiceDialog = DisplayPermissionManager.getInstance().getEnableDisplayServiceDialog(c);
//                    enableDisplayServiceDialog.show();
//                } else {
                    dialog.dismiss();
                    // first, collapse the fab icon
                    // then, bring up the dialog to edit the existing collector
                    wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithCollector(collector);
                    boolean isEdit = true;
                    wrapper.show(isEdit);
//                }


            }
        });


        Button closeBtn = (Button) popupView.findViewById(R.id.collectorCloseButton);
        ImageButton deleteBtn = (ImageButton) popupView.findViewById(R.id.collectorDeleteButton);

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // dismiss current dialog
                dialog.dismiss();

                CollectorCardDeleteConfirmationBuilder deleteConfirmationBuilder = new CollectorCardDeleteConfirmationBuilder(c, collector, dialog, refreshCollectorListRunnable);
                Dialog deleteConfirmationDialog = deleteConfirmationBuilder.build();
                deleteConfirmationDialog.show();
            }
        });

        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });



        return dialog;
    }

}








