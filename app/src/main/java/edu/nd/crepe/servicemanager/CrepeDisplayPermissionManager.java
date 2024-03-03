package edu.nd.crepe.servicemanager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import edu.nd.crepe.R;

public class CrepeDisplayPermissionManager {
    private static CrepeDisplayPermissionManager instance;

    private CrepeDisplayPermissionManager() {
    }

    public static synchronized CrepeDisplayPermissionManager getInstance() {
        if (instance == null) {
            instance = new CrepeDisplayPermissionManager();
        }
        return instance;
    }

    public Dialog getEnableDisplayServiceDialog(Context context) {
        if (!Settings.canDrawOverlays(context)) {
            final View displayPermissionView = LayoutInflater.from(context).inflate(R.layout.display_permission_request, null);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setView(displayPermissionView);
            Dialog dialog = dialogBuilder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Button confirmationYesButton = displayPermissionView.findViewById(R.id.confirmationYesButton);
            confirmationYesButton.setOnClickListener(view -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                dialog.dismiss();
            });
            return dialog;
        }
        Log.i("DisplayPermissionManager", "Display service is already enabled");
        return null;
    }

}
