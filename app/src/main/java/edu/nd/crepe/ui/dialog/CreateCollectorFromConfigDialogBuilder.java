package edu.nd.crepe.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;

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

    public CollectorConfigurationDialogWrapper buildDialogWrapperWithNewCollector() {
        AlertDialog dialog = builder.create();

        CollectorConfigurationDialogWrapper.initializeInstance(c, dialog,  new Collector(), refreshCollectorListRunnable);
        collectorConfigurationDialogWrapper = CollectorConfigurationDialogWrapper.getInstance();
        return collectorConfigurationDialogWrapper;
    }

    public CollectorConfigurationDialogWrapper buildDialogWrapperWithExistingCollector(Collector collector) {
        AlertDialog dialog = builder.create();

        CollectorConfigurationDialogWrapper.initializeInstance(c, dialog, collector, refreshCollectorListRunnable);
        collectorConfigurationDialogWrapper = CollectorConfigurationDialogWrapper.getInstance();
        return collectorConfigurationDialogWrapper;
    }


}
