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
import com.example.crepe.database.DatabaseManager;

public class SetUsernameDialogBuilder {
    private Context c;
    private AlertDialog.Builder dialogBuilder;
    private String androidId;
    private Runnable runnable;

    public SetUsernameDialogBuilder(Context c, String androidId, Runnable runnable){
        this.c = c;
        this.dialogBuilder = new AlertDialog.Builder(c);
        this.androidId = androidId;
        this.runnable = runnable;
    }

    public Dialog build(){
        final View popupView = LayoutInflater.from(c).inflate(R.layout.popup_set_username, null);
        dialogBuilder.setView(popupView);
        Dialog dialog = dialogBuilder.create();
        Button popupCancelBtn = (Button) popupView.findViewById(R.id.setUsernameBackButton);
        Button popupDoneBtn = (Button) popupView.findViewById(R.id.setUsernameCreateButton);
        EditText usernameText = (EditText) popupView.findViewById(R.id.userNameEditText);

        popupCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        popupDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = usernameText.getText().toString();
                if (userName.length() > 18){
                    Toast.makeText(c, "Username max length: 18 characters.", Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseManager dbManager = new DatabaseManager(c);
                    dbManager.updateUserName(androidId,userName);
                    runnable.run();
                    dialog.dismiss();
                }

            }
        });

        return dialog;
    }

}
