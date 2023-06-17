package edu.nd.crepe.ui.dialog;

import android.app.Dialog;
import android.content.Context;

import edu.nd.crepe.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CreateCollectorFromURLDialogSuccessMessage {
    private Context c;
    private MaterialAlertDialogBuilder alertDialogBuilder;

    // Constructor
    public CreateCollectorFromURLDialogSuccessMessage(Context c) {
        this.c = c;
        this.alertDialogBuilder = new MaterialAlertDialogBuilder(c);
    }

    public Dialog build() {

        alertDialogBuilder.setTitle("SUCCESSFULLY ADDED COLLECTOR")
                            .setMessage("The Uber collector is added and is scheduled to start on 01/11/2021.");
        alertDialogBuilder.setPositiveButton(c.getResources().getString(R.string.close), null);

        alertDialogBuilder.show();
        return null;
    }

}
