package edu.nd.crepe.ui.main_activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import edu.nd.crepe.CrepeAccessibilityService;
import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.network.DataLoadingEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private DatabaseManager dbManager;
    private List<Collector> collectorList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbManager = DatabaseManager.getInstance(this.getActivity().getApplicationContext());

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EventBus.getDefault().register(this);
        getActivity().setTitle("Crepe");
        try {
            initCollectorList();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadingEvent(DataLoadingEvent event){
        if(event.isCompleted()){
            try {
                initCollectorList();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void initCollectorList() throws PackageManager.NameNotFoundException {
        if (dbManager != null) {
            collectorList = dbManager.getActiveCollectors();

            TextView noCollectorTextView = getView().findViewById(R.id.empty_text_view);
            LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.fragment_home_inner_linear_layout);
            // clear the collector list
            fragmentInnerLinearLayout.removeAllViews();

            if(collectorList.size() > 0) {
                // hide the no collector textview
                noCollectorTextView.setVisibility(View.GONE);

                // get all installed apps
                Map<String, Drawable> apps = getAppImage();
                CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(getActivity(), homeFragmentRefreshCollectorListRunnable,apps);


                for (Collector collector : collectorList) {

                    ConstraintLayout collectorCardView = builder.build(collector, fragmentInnerLinearLayout, "cardLayout");

                    // if the cardView is not null, meaning the collector is not in deleted status
                    if (collectorCardView != null) {
                        collectorCardView.setId(View.generateViewId());

                        // Toast.makeText(this.getActivity(), fragmentInnerConstraintLayout.toString(), Toast.LENGTH_LONG).show();
                        fragmentInnerLinearLayout.addView(collectorCardView);
                    }
                }


                // enable accessibility service
                // check if the accessibility service is running
                Boolean accessibilityServiceRunning = false;
                ActivityManager manager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
                Class clazz = CrepeAccessibilityService.class;

                if (manager != null) {
                    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                        if (clazz.getName().equals(service.service.getClassName())) {
                            accessibilityServiceRunning = true;
                        }
                    }
                }

                // if accessibility service is not on
                if (!accessibilityServiceRunning) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                    builder1.setTitle("Service Permission Required")
                            .setMessage("The accessibility service is not enabled for " + Const.appName + ". Please enable the service in the phone settings before recording.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                                    getContext().startActivity(intent);
                                    //do nothing
                                }
                            }).show();
                }

            } else {
                noCollectorTextView.setVisibility(View.VISIBLE);
            }
        }

    }

    Runnable homeFragmentRefreshCollectorListRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                initCollectorList();

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    };


    public Map<String, Drawable> getAppImage() throws PackageManager.NameNotFoundException {
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // get list of all the apps installed
        List<ResolveInfo> ril = getContext().getPackageManager().queryIntentActivities(mainIntent, 0);
//        List<String> componentList = new ArrayList<String>();
        String name = null;
        Drawable image = null;
        String packageName = "edu.nd.crepe";

        // get size of ril and create a list
        Map<String, Drawable> apps = new HashMap<String, Drawable>();
        for (ResolveInfo ri : ril) {
            if (ri.activityInfo != null) {
                // get package
                Resources res = getContext().getPackageManager().getResourcesForApplication(ri.activityInfo.applicationInfo);
                // if activity label res is found
                if (ri.activityInfo.labelRes != 0) {
                    name = res.getString(ri.activityInfo.labelRes);
                } else {
                    name = ri.activityInfo.applicationInfo.loadLabel(
                            getContext().getPackageManager()).toString();

                }
                packageName = ri.activityInfo.packageName;
                image = getContext().getPackageManager().getApplicationIcon(packageName);
                apps.put(name,image);
            }
        }
        return apps;
    }




}
