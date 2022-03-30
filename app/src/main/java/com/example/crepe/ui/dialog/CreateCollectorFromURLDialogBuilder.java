package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.crepe.MainActivity;
import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.network.ServerCollectorCommunicationManager;
import com.example.crepe.network.VolleyCallback;
import com.google.gson.Gson;

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
        EditText urlText = (EditText) popupView.findViewById(R.id.urlEditText);

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
                if (urlText.getText() != null) {
                    ServerCollectorCommunicationManager serverCollectorCommunicationManager = new ServerCollectorCommunicationManager(c);
                    serverCollectorCommunicationManager.downloadJsonFromServer(new VolleyCallback() {
                        @Override
                        public void onSuccess(Collector result) {
                            // TODO: check if the return object is null
                            if (result == null) {
                                // Toast message
                                Toast.makeText(c, "Error Downloading Collector", Toast.LENGTH_SHORT).show();
                            } else {
                                // else: add to the collector database
                                DatabaseManager dbManager = new DatabaseManager(c);
                                dbManager.addOneCollector(result);
                                // refresh home fragment
                                refreshCollectorListRunnable.run();
                            }
                        }
                    },urlText.getText().toString());

                    // next popup
                    dialog.dismiss();
                    CreateCollectorFromURLDialogSuccessMessage nextPopup = new CreateCollectorFromURLDialogSuccessMessage(c);
                    nextPopup.build();
                } else {
                    Toast.makeText(c,"Please enter a valid URL", Toast.LENGTH_LONG).show();
                }

            }
        });
        return dialog;
    }

}
