package edu.nd.crepe.ui.main_activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Firebase;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.network.FirebaseCallback;
import edu.nd.crepe.network.FirebaseCommunicationManager;

import java.util.List;

public class CollectorCardDetailBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;
    private FirebaseCommunicationManager fbManager;

    public CollectorCardDetailBuilder(Context c, Collector collector, Runnable refreshCollectorListRunnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.collector = collector;
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(c);
        this.fbManager = new FirebaseCommunicationManager(c);
    }

    public Dialog build() {
        final View popupView = LayoutInflater.from(c).inflate(R.layout.collector_detail, null);
        if (popupView.getParent() != null) {
            ((ViewGroup)popupView.getParent()).removeView(popupView); // <- remove the view from its parent
        }
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        List<Datafield> datafieldsForCollector = dbManager.getAllDatafieldsForCollector(collector);

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
                dialog.dismiss();

                // TODO Meng: finish the logic for the edit button
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
//                if(enableSwitch.isChecked()){
//                    collector.activateCollector();
//                    dbManager.updateCollectorStatus(collector);
//                } else {
//                    collector.disableCollector();
//                    dbManager.updateCollectorStatus(collector);
//                }
//                // update the home fragment list
//                refreshCollectorListRunnable.run();
                dialog.dismiss();
            }
        });

//        enableSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
//                // Commented the following block out, don't feel it's necessary because the collector status is updated at the closeBtn onclicklistener
//                if (!isChecked){
//                    enableSwitch.setText("Disabled");
//                } else {
//                    enableSwitch.setText("Enabled");
//                }
//            }
//        });


        return dialog;
    }

}








