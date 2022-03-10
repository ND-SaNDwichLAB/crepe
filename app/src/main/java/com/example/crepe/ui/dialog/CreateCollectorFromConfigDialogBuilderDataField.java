package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.example.crepe.R;

@Deprecated
public class CreateCollectorFromConfigDialogBuilderDataField {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    public CreateCollectorFromConfigDialogBuilderDataField(Context c){
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
    }

    public Dialog build(){
        final View popupView = LayoutInflater.from(c).inflate(R.layout.dialog_add_collector_from_config_data_field, null);
        dialogBuilder.setView(popupView);
        Button popupNxtBtn = (Button) popupView.findViewById(R.id.dataFieldNextButton);
        Button popupBckBtn = (Button) popupView.findViewById(R.id.dataFieldBackButton);
        ImageButton closeImg = (ImageButton) popupView.findViewById(R.id.closeDataFieldImageButton);

        Dialog dialog = dialogBuilder.create();

        popupBckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                CreateCollectorFromConfigDialogBuilderGraphQuery prevPopup = new CreateCollectorFromConfigDialogBuilderGraphQuery(c);
                prevPopup.build();
            }
        });

        popupNxtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                CreateCollectorFromConfigDialogBuilderDescription nextPopup = new CreateCollectorFromConfigDialogBuilderDescription(c);
                Dialog newDialog = nextPopup.build();
                newDialog.show();
            }
        });

        closeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        return dialog;

    }

}
