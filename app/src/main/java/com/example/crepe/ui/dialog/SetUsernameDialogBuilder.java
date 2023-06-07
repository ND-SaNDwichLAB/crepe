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
import com.example.crepe.database.User;
import com.example.crepe.network.FirebaseCommunicationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                    DatabaseManager dbManager = DatabaseManager.getInstance(c);
                    dbManager.updateUserName(androidId,userName);
                    List<User> users = dbManager.getAllUsers();
                    User user = users.get(0);
                    FirebaseCommunicationManager firebaseCommunicationManager = new FirebaseCommunicationManager(c);
                    // create new user hashmap
                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("userId", user.getUserId());
                    userMap.put("name", user.getName());
                    userMap.put("timeCreated", user.getTimeCreated());
                    user.setTimeLastEdited(System.currentTimeMillis());
                    userMap.put("timeLastEdited", user.getTimeLastEdited());
                    firebaseCommunicationManager.updateUser(user.getUserId(), userMap);
                    runnable.run();
                    dialog.dismiss();
                }

            }
        });

        return dialog;
    }

}
