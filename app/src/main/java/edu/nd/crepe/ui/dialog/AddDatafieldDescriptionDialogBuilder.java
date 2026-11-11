package edu.nd.crepe.ui.dialog;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import edu.nd.crepe.R;
import edu.nd.crepe.demonstration.SelectionOverlayView;

public class AddDatafieldDescriptionDialogBuilder {
    private static final String TAG = "AddDatafieldDescDialog";

    private Context context;
    private SelectionOverlayView selectionOverlayView;
    private WindowManager windowManager;
    private View confirmationView;
    private WindowManager.LayoutParams layoutParams;
    private DatafieldDescriptionCallback datafieldDescriptionCallback;

    public AddDatafieldDescriptionDialogBuilder(Context context, SelectionOverlayView selectionOverlayView, WindowManager windowManager, View confirmationView, WindowManager.LayoutParams layoutParams, DatafieldDescriptionCallback datafieldDescriptionCallback) {
        this.context = context;
        this.selectionOverlayView = selectionOverlayView;
        this.windowManager = windowManager;
        this.confirmationView = confirmationView;
        this.layoutParams = layoutParams;
        this.datafieldDescriptionCallback = datafieldDescriptionCallback;
    }

    public View buildDialog() {
        View addDatafieldDescriptionView = LayoutInflater.from(context).inflate(R.layout.demonstration_description, null);

        Button confirmationBackBtn = addDatafieldDescriptionView.findViewById(R.id.confirmationBackButton);
        Button confirmationAddBtn = addDatafieldDescriptionView.findViewById(R.id.confirmationAddButton);

        confirmationBackBtn.setOnClickListener(v -> {
            removeViewSafely(addDatafieldDescriptionView);
            if (confirmationView != null) {
                try {
                    windowManager.addView(confirmationView, layoutParams);
                } catch (RuntimeException e) {
                    // already showing, or the window manager refuses new windows
                    Log.e(TAG, "Could not show the confirmation dialog again", e);
                }
            }
        });

        confirmationAddBtn.setOnClickListener(v -> {

            EditText datafieldDescriptionEditText = addDatafieldDescriptionView.findViewById(R.id.datafieldDescriptionEditText);
            String datafieldDescription = datafieldDescriptionEditText.getText().toString();

            if (datafieldDescription.trim().isEmpty()) {
                Toast.makeText(context, "Datafield description cannot be blank!", Toast.LENGTH_SHORT).show();
            } else {
                removeViewSafely(addDatafieldDescriptionView);

                datafieldDescriptionCallback.onProcessDescriptionEditText(datafieldDescription);
            }
        });


        return addDatafieldDescriptionView;
    }

    // removing a view that is not attached to the window manager throws
    private void removeViewSafely(View view) {
        if (view == null) {
            return;
        }
        try {
            windowManager.removeView(view);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "The view was not attached to the window manager");
        }
    }



}
