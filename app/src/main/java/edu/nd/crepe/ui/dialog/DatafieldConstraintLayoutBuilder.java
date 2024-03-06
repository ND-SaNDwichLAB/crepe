package edu.nd.crepe.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.constraintlayout.widget.ConstraintLayout;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;

public class DatafieldConstraintLayoutBuilder {
    private Context c;
    private TextView datafieldTextView;
    private ImageButton removeButton;
    private ConstraintLayout datafieldConstraintLayout;
    private Collector collector;


    public DatafieldConstraintLayoutBuilder(Context c){
        this.c = c;
    }

    public ConstraintLayout build(String text, ViewGroup rootView, Collector collector){
        datafieldConstraintLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.datafield_card, rootView, false);
        datafieldTextView = (TextView) datafieldConstraintLayout.findViewById(R.id.datafieldTextView);
        datafieldTextView.setText(text);
        removeButton = (ImageButton) datafieldConstraintLayout.findViewById(R.id.removeDatafieldButton);
        this.collector = collector;
        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(c);


        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // confirmation
                alertDialogBuilder.setTitle("DELETE DATA FIELD")
                        .setMessage("Are you sure to delete " + text + "?");
                alertDialogBuilder.setPositiveButton(c.getResources().getString(R.string.positive), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // remove this graph query
                        // TODO potentially add a confirmation dialog
//                        if (!collector.removeDatafield(text)){
//                            Toast.makeText(c, "This data field is already removed!", Toast.LENGTH_LONG).show();
//                        };
                        // refresh the list
                        rootView.removeView(datafieldConstraintLayout);
                    }
                });
                alertDialogBuilder.setNegativeButton(c.getResources().getString(R.string.negative),new DialogInterface.OnClickListener(){
                    @Override public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialogBuilder.show();
            }
        });

        return datafieldConstraintLayout;

    }

}
