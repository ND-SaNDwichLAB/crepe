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
