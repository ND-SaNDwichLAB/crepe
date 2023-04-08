package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.example.crepe.database.Datafield;
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
                    // Server
//                    ServerCollectorCommunicationManager serverCollectorCommunicationManager = new ServerCollectorCommunicationManager(c);
//                    serverCollectorCommunicationManager.downloadJsonFromServer(new VolleyCallback() {
//                        @Override
//                        public void onSuccess(Collector result) {
//                           // save collector to database
//                            if (result == null) {
//                                // Toast message
//                                Toast.makeText(c, "Error Downloading Collector", Toast.LENGTH_SHORT).show();
//                            } else {
//                                // else: add to the collector database
//                                DatabaseManager dbManager = new DatabaseManager(c);
//                                dbManager.addOneCollector(result);
//                                // refresh home fragment
//                                refreshCollectorListRunnable.run();
//                            }
//                        }
//                    },urlText.getText().toString());

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
                } else {
                    Toast.makeText(c,"Please enter a valid collector ID", Toast.LENGTH_LONG).show();
                }
            }

        });
        return dialog;
    }

}
