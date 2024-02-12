package edu.nd.crepe.accessibilityservice;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import edu.nd.crepe.R;

/*
This class is used to manage the accessibility service permission.
It is a singleton class that provides methods to check if the accessibility service is enabled,
 and to prompt the user to enable it if it is not.
*/

public class AccessibilityPermissionManager {

    private static AccessibilityPermissionManager instance;
    private static boolean accessibilityServiceRunning = false;

    private AccessibilityPermissionManager() {
    }

    public static synchronized AccessibilityPermissionManager getInstance() {
        if (instance == null) {
            instance = new AccessibilityPermissionManager();
        }
        return instance;
    }

    // if accessibility service is not on, request permission from user
    // return a dialog to be shown in the UI
    public Dialog getEnableAccessibilityServiceDialog(Context context) {
        if (!accessibilityServiceRunning) {
            final View accessibilityPermissionView = LayoutInflater.from(context).inflate(R.layout.accessibility_permission_request, null);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setView(accessibilityPermissionView);
            Dialog dialog = dialogBuilder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Button accessibilityEnableButton = (Button) accessibilityPermissionView.findViewById(R.id.accessibilityEnableButton);
            accessibilityEnableButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    context.startActivity(intent);
                    dialog.dismiss();
                }
            });
            return dialog;
        }
        Log.i("AccessibilityPermissionManager", "Accessibility service is already enabled");
        return null;
    }
}





