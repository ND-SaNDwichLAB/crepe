package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.example.crepe.R;

public class CreateCollectorFromConfigDialogBuilder {

    private Context c;
    private AlertDialog.Builder dialogBuilder;
    public CreateCollectorFromConfigDialogBuilder(Context c) {
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);

    }

    public Dialog build(){
        final View popupView = LayoutInflater.from(c).inflate(R.layout.dialog_add_collector_from_config, null);
        dialogBuilder.setView(popupView);
        Button popupCancelBtn = (Button) popupView.findViewById(R.id.addCollectorFromConfigDialogCancelButton);
        Button popupNextBtn = (Button) popupView.findViewById(R.id.addCollectorFromConfigDialogNextButton);
        Dialog dialog = dialogBuilder.create();
        popupCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        popupNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                CreateCollectorFromConfigDialogBuilderGraphQuery newPopup = new CreateCollectorFromConfigDialogBuilderGraphQuery(c);
                Dialog newDialog = newPopup.build();
                newDialog.show();
            }
        });
        return dialog;
    }

}
