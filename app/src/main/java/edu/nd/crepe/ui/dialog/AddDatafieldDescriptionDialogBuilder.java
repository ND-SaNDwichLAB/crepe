package edu.nd.crepe.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import edu.nd.crepe.R;
import edu.nd.crepe.demonstration.SelectionOverlayView;
import edu.nd.crepe.demonstration.WidgetService;

public class AddDatafieldDescriptionDialogBuilder {
    private Context context;
    private AlertDialog.Builder builder;
    private View confirmationView;
    private View dimView;
    private SelectionOverlayView selectionOverlayView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    public AddDatafieldDescriptionDialogBuilder(Context context, View confirmationView, View dimView, SelectionOverlayView selectionOverlayView, WindowManager windowManager, WindowManager.LayoutParams layoutParams) {
        this.context = context;
        this.builder = new AlertDialog.Builder(c);
        this.confirmationView = confirmationView;   // the previous view to show, if going back
        this.dimView = dimView;
        this.selectionOverlayView = selectionOverlayView;
        this.windowManager = windowManager;
        this.layoutParams = layoutParams;
    }

    public View buildDialog() {
        View addDatafieldDescriptionView = LayoutInflater.from(context).inflate(R.layout.demonstration_description, null);

        Button confirmationBackBtn = addDatafieldDescriptionView.findViewById(R.id.confirmationBackButton);
        Button confirmationAddBtn = addDatafieldDescriptionView.findViewById(R.id.confirmationAddButton);

        confirmationBackBtn.setOnClickListener(v -> {
            if (addDatafieldDescriptionView != null) {
                windowManager.removeView(addDatafieldDescriptionView);
            }
            if (confirmationView != null) {
                windowManager.addView(confirmationView, layoutParams);
            }
        });

        confirmationAddBtn.setOnClickListener(v -> {

            EditText datafieldDescriptionEditText = addDatafieldDescriptionView.findViewById(R.id.datafieldDescriptionEditText)
            String datafieldDescription = datafieldDescriptionEditText.getText().toString();

            if (datafieldDescription.trim().isEmpty()) {
                Toast.makeText(context, "Datafield description cannot be blank!", Toast.LENGTH_SHORT).show();
            } else {
                if (addDatafieldDescriptionView != null) {
                    windowManager.removeView(addDatafieldDescriptionView);
                }
                if (selectionOverlayView != null) {
                    windowManager.addView(selectionOverlayView, layoutParams);
                }
                if (dimView != null) {
                    windowManager.removeView(dimView);
                }

                // TODO Yuwen 1. call the openai api here

                // TODO Yuwen 2. uncomment this and send all the data
                // set the data to the main activity
//                desiredQuery = data;
//                processCallback(finalTargetEntity.getEntityValue().getText());
//                // clear the overlay
//                disableOverlay();
//                // stop widget service
//                Intent intent = new Intent(context, WidgetService.class);
//                context.stopService(intent);
//                // go back to the main activity
//                Intent mainActivityIntent = context.getPackageManager().getLaunchIntentForPackage("edu.nd.crepe");
//                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                if (mainActivityIntent != null) {
//                    context.startActivity(mainActivityIntent);
//                } else {
//                    Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
//                }
            }



        });


        return addDatafieldDescriptionView;
    }
}
