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
public class CreateCollectorFromConfigDialogBuilderDescription {
    private Context c;
    private AlertDialog.Builder dialogBuilder;

    public CreateCollectorFromConfigDialogBuilderDescription(Context c){
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
    }

    public Dialog build() {
        final View popupView = LayoutInflater.from(c).inflate(R.layout.dialog_add_collector_from_config_description, null);
        dialogBuilder.setView(popupView);
        Button popupCrtBtn = (Button) popupView.findViewById(R.id.descriptionCreateButton);
        Button popupBckBtn = (Button) popupView.findViewById(R.id.descriptionBackButton);
        ImageButton closeImg = (ImageButton) popupView.findViewById(R.id.closeDescriptionImageButton);

        Dialog dialog = dialogBuilder.create();

        popupBckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                CreateCollectorFromConfigDialogBuilderGraphQuery prevPopup = new CreateCollectorFromConfigDialogBuilderGraphQuery(c);
                prevPopup.build();
            }
        });

        popupCrtBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
