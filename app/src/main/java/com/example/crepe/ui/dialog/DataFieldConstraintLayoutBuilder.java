package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.crepe.R;
import com.example.crepe.database.Collector;

public class DataFieldConstraintLayoutBuilder {
    private Context c;
    private TextView dataFieldTextView;
    private ImageButton removeButton;
    private ConstraintLayout dataFieldConstraintLayout;
    private  Collector collector;


    public DataFieldConstraintLayoutBuilder(Context c){
        this.c = c;
    }

    public ConstraintLayout build(String text, ViewGroup rootView, Collector collector){
        dataFieldConstraintLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.datafield_card, rootView, false);
        dataFieldTextView = (TextView) dataFieldConstraintLayout.findViewById(R.id.datafieldTextView);
        dataFieldTextView.setText(text);
        removeButton = (ImageButton) dataFieldConstraintLayout.findViewById(R.id.removeDataFieldButton);
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
                        if (!collector.removeDataField(text)){
                            Toast.makeText(c, "This data field is already removed!", Toast.LENGTH_LONG).show();
                        };
                        // refresh the list
                        rootView.removeView(dataFieldConstraintLayout);
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

        return dataFieldConstraintLayout;

    }

}
