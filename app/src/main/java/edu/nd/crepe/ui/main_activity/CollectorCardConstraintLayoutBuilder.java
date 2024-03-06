package edu.nd.crepe.ui.main_activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;

import java.util.Map;
import java.util.Objects;

import static edu.nd.crepe.database.Collector.ACTIVE;
import static edu.nd.crepe.database.Collector.DELETED;
import static edu.nd.crepe.database.Collector.EXPIRED;
import static edu.nd.crepe.database.Collector.NOTYETSTARTED;

public class CollectorCardConstraintLayoutBuilder {
    private Context c;
    private TextView appNameTextView;
    private TextView collectorDescriptionTextView;
    private TextView scheduleStartTextView;
    private TextView scheduleEndTextView;
    private ConstraintLayout collectorLayout;
    private TextView collectorStatusTxt;
    private ImageView collectorStatusImg;
    private Runnable refreshCollectorListRunnable;
    private DatabaseManager dbManager;
    private Map<String, Drawable> apps;

    public CollectorCardConstraintLayoutBuilder(Context c, Runnable refreshCollectorListRunnable, Map<String, Drawable> apps) {
        this.c = c;
        this.refreshCollectorListRunnable = refreshCollectorListRunnable;
        this.dbManager = DatabaseManager.getInstance(c);
        this.apps = apps;
    }

    public ConstraintLayout build(Collector collector, ViewGroup rootView, String layoutType) {

        // if the collector is deleted, don't display anything.
        // We handle deletion in our app in this way so database manipulation can be easier
        if (collector.getCollectorStatus().equals(DELETED)) {
            Log.e("collector", "This collector for " + collector.getAppName() + " is deleted, thus will not be displayed.");
            return null;
        }

        // else
        if (Objects.equals(layoutType, "cardLayout")) {
            // if for home fragment, build a card layout
            collectorLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.collector_card, rootView, false);
        } else {
            // if for data fragment, build a info layout without a card appearance
            collectorLayout = (ConstraintLayout) LayoutInflater.from(c).inflate(R.layout.collector_info_for_data_fragment, rootView, false);
        }

        // get the app name text field from the card and populate it with app name
        appNameTextView = (TextView) collectorLayout.findViewById(R.id.collectorTitle);
        appNameTextView.setText(collector.getAppName());

        collectorDescriptionTextView = (TextView) collectorLayout.findViewById(R.id.collectorDataDescription);
        if (collector.getDescription() != null) {
            collectorDescriptionTextView.setText(collector.getDescription());
        }

        scheduleStartTextView = (TextView) collectorLayout.findViewById(R.id.startTime);
        scheduleStartTextView.setText("Start Time: " + collector.getCollectorStartTimeString());
        scheduleEndTextView = (TextView) collectorLayout.findViewById(R.id.endTime);
        scheduleEndTextView.setText("End Time: " + collector.getCollectorEndTimeString());

        // get the app status and display it
        collectorStatusImg = (ImageView) collectorLayout.findViewById(R.id.runningLightImageView);
        collectorStatusTxt = (TextView) collectorLayout.findViewById(R.id.collectorStatusText);

        // if the collector is not deleted, refresh its status based on current time
        collector.autoSetCollectorStatus();
        // also update in the database
        dbManager.updateCollectorStatus(collector);
        if (collector.getCollectorStatus().equals(ACTIVE)) {
            collectorStatusTxt.setText("Running");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_24_green);
        } else if (collector.getCollectorStatus().equals(EXPIRED)) {
            collectorStatusTxt.setText("Expired");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_grey);
        } else if (collector.getCollectorStatus().equals(NOTYETSTARTED)) {
            collectorStatusTxt.setText("Not Started");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_yellow);
        }

        // get App logo
        ImageView appImg = (ImageView) collectorLayout.findViewById(R.id.collectorImg);
        Drawable appImage = apps.get(collector.getAppName());
        if (appImage == null) {
            appImg.setImageResource(R.drawable.nd_logo);
            collectorStatusTxt.setText("Not Installed");
            collectorStatusImg.setImageResource(R.drawable.ic_baseline_circle_12_red);
            collectorDescriptionTextView.setText("This app is not installed on your device. Please install it to participate in this data collection.");
        } else {
            appImg.setImageDrawable(appImage);
        }


        Button detailBtn = (Button) collectorLayout.findViewById(R.id.detailButton);

        detailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CollectorCardDetailBuilder cardDetailBuilder = new CollectorCardDetailBuilder(c, collector, refreshCollectorListRunnable);
                Dialog newDialog = cardDetailBuilder.build();
                newDialog.show();
            }
        });


        return collectorLayout;
    }

//    public Drawable getAppImage(String appName) throws PackageManager.NameNotFoundException {
//        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
//        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//
//        // get list of all the apps installed
//        List<ResolveInfo> ril = c.getPackageManager().queryIntentActivities(mainIntent, 0);
////        List<String> componentList = new ArrayList<String>();
//        String name = null;
//        Drawable image = null;
//        String packageName = "edu.nd.crepe";
//
//
//        // get size of ril and create a list
//        Map<String, Drawable> apps = new HashMap<String, Drawable>();
//        for (ResolveInfo ri : ril) {
//            if (ri.activityInfo != null) {
//                // get package
//                Resources res = c.getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
//                // if activity label res is found
//                if (ri.activityInfo.labelRes != 0) {
//                    name = res.getString(ri.activityInfo.labelRes);
//                } else {
//                    name = ri.activityInfo.applicationInfo.loadLabel(
//                            c.getPackageManager()).toString();
//
//                }
//                packageName = ri.activityInfo.packageName;
//                image = c.getPackageManager().getApplicationIcon(packageName);
//                apps.put(name,image);
//            }
//        }
//        return apps.get(appName);
//    }

}
