package edu.nd.crepe.ui.main_activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;

import java.util.List;

public class CollectorCardDetailBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private Collector collector;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;

    public CollectorCardDetailBuilder(Context c, Collector collector, Runnable refreshCollectorListRunnable) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.collector = collector;
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(c);
    }

    public Dialog build() {
        final View popupView = LayoutInflater.from(c).inflate(R.layout.collector_detail, null);
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();

        List<Datafield> datafieldsForCollector = dbManager.getAllDatafieldsForCollector(collector);

        TextView collectorDatafield = (TextView) popupView.findViewById(R.id.collectorDetailDatafield);
        if (datafieldsForCollector.size() > 0) {
            for (Datafield datafield : datafieldsForCollector) {
                collectorDatafield.append("\"" + datafield.getName() + "\"\n\n");
            }
        } else {
            collectorDatafield.setText("No datafields available");
        }

        Button collectorShareButton = (Button) popupView.findViewById(R.id.collectorShareButton);
        collectorShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // we will just share the id of the collector for now, instead of a url
                ClipboardManager clipboard = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("share URL", collector.getCollectorId());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(c,"collector ID copied to clipboard " + collector.getCollectorId(), Toast.LENGTH_LONG).show();
            }
        });


        Button closeBtn = (Button) popupView.findViewById(R.id.collectorCloseButton);
        Button deleteBtn = (Button) popupView.findViewById(R.id.collectorDeleteButton);
//        Switch enableSwitch = (Switch) popupView.findViewById(R.id.collectorStatusSwitch);
//        if(collector.getCollectorStatus().equals("disabled")){
//            enableSwitch.setChecked(false);
//            enableSwitch.setText("Disabled");
//        } else{
//            enableSwitch.setChecked(true);
//        }

        // TODO Yuwen figure out what to do here
//        collectorDatafield.setText(collector.getDatafieldsToString());


        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // new popup to confirm
                AlertDialog.Builder builder = new AlertDialog.Builder(c);
                builder.setTitle("Delete Collector");
                builder.setMessage("Are you sure you want to delete this collector?");

                builder.setPositiveButton("Yes", (dialogInterface, i) -> {
                    Toast.makeText(c, "Collector for " + collector.getAppName() + " is deleted", Toast.LENGTH_LONG).show();
                    // This will only set the status of collector to deleted,
                    // it will still be present in database but won't be displayed
                    collector.deleteCollector();
                    dbManager.updateCollectorStatus(collector);
                    // TODO Yuwen: maybe we should also delete all the datafields associated with this collector, also delete the collector from firebase?

                    // update the home fragment list
                    refreshCollectorListRunnable.run();
                    dialog.dismiss();
                });
                builder.setNegativeButton("No", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });

                builder.show();
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








