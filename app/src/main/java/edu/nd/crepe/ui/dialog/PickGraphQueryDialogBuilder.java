package edu.nd.crepe.ui.dialog;

import android.content.Context;
import android.graphics.Color;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Objects;

import edu.nd.crepe.R;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;

public class PickGraphQueryDialogBuilder {
    private Context context;
    private WindowManager windowManager;
    private View confirmationView;
    private WindowManager.LayoutParams layoutParams;
    private DatafieldDescriptionCallback datafieldDescriptionCallback;

    public PickGraphQueryDialogBuilder(Context context, WindowManager windowManager, View confirmationView, WindowManager.LayoutParams layoutParams, DatafieldDescriptionCallback datafieldDescriptionCallback) {
        this.context = context;
        this.windowManager = windowManager;
        this.confirmationView = confirmationView;
        this.layoutParams = layoutParams;
        this.datafieldDescriptionCallback = datafieldDescriptionCallback;
    }

    public View buildDialog(List<Pair<OntologyQuery, Double>> defaultQueries) {
        View addDatafieldDescriptionView = LayoutInflater.from(context).inflate(R.layout.demonstration_pick_query, null);

        Button confirmationBackBtn = addDatafieldDescriptionView.findViewById(R.id.confirmationBackButton);
        Button confirmationAddBtn = addDatafieldDescriptionView.findViewById(R.id.confirmationAddButton);
        // Keep track of selected button
        final Button[] selectedButton = {null};

        for (Pair<OntologyQuery, Double> query: defaultQueries) {
            OntologyQuery ontologyQuery = query.first;

            // Create a regular Button for each query
            Button queryButton = new Button(context);

            // Set the text and tag
            queryButton.setText(ontologyQuery.toString());
            queryButton.setTag(ontologyQuery);

            // Style the button
            queryButton.setAllCaps(false); // Keep original text case
            queryButton.setBackground(ContextCompat.getDrawable(context, R.drawable.selectable_item_background));
            queryButton.setTextColor(Color.BLACK);
            queryButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); // Left-align text


            // Set layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8); // Add vertical spacing between items
            queryButton.setLayoutParams(params);

            // Add padding within the button
            queryButton.setPadding(32, 24, 32, 24);

            // Add click listener
            queryButton.setOnClickListener(v -> {
                // Deselect previous button if exists
                if (selectedButton[0] != null) {
                    selectedButton[0].setSelected(false);
                }

                // Select current button
                queryButton.setSelected(true);
                selectedButton[0] = queryButton;

                // Handle selection
                OntologyQuery selectedQuery = (OntologyQuery) v.getTag();
                // Do something with the selected query
            });

            // Add the button to the linear layout
            LinearLayout queryContainerLinearLayout = addDatafieldDescriptionView.findViewById(R.id.queryContainerLinearLayout);
            queryContainerLinearLayout.addView(queryButton);
        }

        // If you want to ensure proper spacing of the container
        LinearLayout queryContainerLinearLayout = addDatafieldDescriptionView.findViewById(R.id.queryContainerLinearLayout);
        queryContainerLinearLayout.setPadding(16, 16, 16, 16);
        queryContainerLinearLayout.setOrientation(LinearLayout.VERTICAL);

        confirmationBackBtn.setOnClickListener(v -> {
            if (addDatafieldDescriptionView != null) {
                windowManager.removeView(addDatafieldDescriptionView);
            }
            if (confirmationView != null) {
                windowManager.addView(confirmationView, layoutParams);
            }
        });

        // TODO re-write this to use our new method
        confirmationAddBtn.setOnClickListener(v -> {

            EditText datafieldDescriptionEditText = addDatafieldDescriptionView.findViewById(R.id.datafieldDescriptionEditText);
            String datafieldDescription = datafieldDescriptionEditText.getText().toString();

            if (datafieldDescription.trim().isEmpty()) {
                Toast.makeText(context, "Datafield description cannot be blank!", Toast.LENGTH_SHORT).show();
            } else {
                if (addDatafieldDescriptionView != null) {
                    windowManager.removeView(addDatafieldDescriptionView);
                }

                datafieldDescriptionCallback.onPickBestQuery(datafieldDescription);
            }
        });


        return addDatafieldDescriptionView;
    }



}
