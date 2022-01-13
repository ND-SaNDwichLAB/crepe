package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.example.crepe.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CreateCollectorFromConfigDialogBuilderSpecifyData {
    private Context c;
    private MaterialAlertDialogBuilder alertDialogBuilder;

    // Constructor
    public CreateCollectorFromConfigDialogBuilderSpecifyData (Context c) {
        this.c = c;
        this.alertDialogBuilder = new MaterialAlertDialogBuilder(c);
    }

    public void build() {

        alertDialogBuilder.setTitle("SPECIFY DATA TO COLLECT")
                            .setMessage("Some Supporting Message");

        alertDialogBuilder.setNegativeButton(c.getResources().getString(R.string.back), null);
        alertDialogBuilder.setPositiveButton(c.getResources().getString(R.string.next), null);

        alertDialogBuilder.show();
    }

}
