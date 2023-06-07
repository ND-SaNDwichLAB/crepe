package com.example.crepe.ui.dialog;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.example.crepe.R;
import com.example.crepe.database.Collector;
import com.example.crepe.database.DatabaseManager;
import com.google.android.material.datepicker.MaterialDatePicker;

public class CreateCollectorFromConfigDialogBuilder {

    private Context c;
    private AlertDialog.Builder builder;
    private Runnable refreshCollectorListRunnable;

    private CollectorConfigurationDialogWrapper collectorConfigurationDialogWrapper;

    public CreateCollectorFromConfigDialogBuilder(Context c, Runnable refreshCollectorListRunnable) {
        this.c = c;
        this.builder = new AlertDialog.Builder(c);
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
    }

    public CollectorConfigurationDialogWrapper buildDialogWrapperWithNewCollector() {
        AlertDialog dialog = builder.create();

        DatabaseManager dbManager = DatabaseManager.getInstance(c);
        Integer collectorQuantity = dbManager.getAllCollectors().size();
        String collectorId = String.valueOf(collectorQuantity + 1);

        collectorConfigurationDialogWrapper = new CollectorConfigurationDialogWrapper(c, dialog,  new Collector(collectorId), refreshCollectorListRunnable);
        return collectorConfigurationDialogWrapper;
    }

    public CollectorConfigurationDialogWrapper buildDialogWrapperWithExistingCollector(Collector collector) {
        AlertDialog dialog = builder.create();
        collectorConfigurationDialogWrapper = new CollectorConfigurationDialogWrapper(c, dialog, collector, refreshCollectorListRunnable);
        return collectorConfigurationDialogWrapper;
    }


}
