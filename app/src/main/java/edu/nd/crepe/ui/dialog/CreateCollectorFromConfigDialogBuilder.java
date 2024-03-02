package edu.nd.crepe.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;

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

    public CollectorConfigurationDialogWrapper buildDialogWrapperWithCollector(Collector collector) {
        CollectorConfigurationDialogWrapper.initializeInstance(c, collector, refreshCollectorListRunnable);
        collectorConfigurationDialogWrapper = CollectorConfigurationDialogWrapper.getInstance();
        return collectorConfigurationDialogWrapper;
    }

}
