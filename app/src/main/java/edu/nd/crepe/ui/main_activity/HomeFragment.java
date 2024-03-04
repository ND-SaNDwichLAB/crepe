package edu.nd.crepe.ui.main_activity;

import static edu.nd.crepe.ui.main_activity.CollectorCardConstraintLayoutBuilder.DELETED;
import static edu.nd.crepe.ui.main_activity.CollectorCardConstraintLayoutBuilder.EXPIRED;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import edu.nd.crepe.R;
import edu.nd.crepe.database.Collector;
import edu.nd.crepe.database.DatabaseManager;
import edu.nd.crepe.database.Datafield;
import edu.nd.crepe.network.DataLoadingEvent;
import edu.nd.crepe.servicemanager.AccessibilityPermissionManager;
import edu.nd.crepe.servicemanager.CrepeAccessibilityService;
import edu.nd.crepe.servicemanager.CrepeNotificationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private DatabaseManager dbManager;
    private DatabaseReference collectorReference;
    private ChildEventListener collectorListener;
    private DatabaseReference datafieldReference;
    private ChildEventListener datafieldListener;
    private List<Collector> collectorList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbManager = DatabaseManager.getInstance(Objects.requireNonNull(this.getActivity()).getApplicationContext());
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EventBus.getDefault().register(this);
        Objects.requireNonNull(getActivity()).setTitle("Crepe");
        try {
            initCollectorList();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // if there are collectors locally, add listener to firebase to watch for remote updates to these collectors
        if (!collectorList.isEmpty()) {
            // get all collector ids
            List<String> collectorIds = collectorList.stream().map(Collector::getCollectorId).collect(Collectors.toList());
            // add listener to collector and datafield updates
            collectorReference = FirebaseDatabase.getInstance().getReference(Collector.class.getSimpleName());
            datafieldReference = FirebaseDatabase.getInstance().getReference(Datafield.class.getSimpleName());
            collectorListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // when a collector of this id is added, do not do anything
                    Log.i("collector childEventListener", "This collector with id " + snapshot.getKey() + " is somehow added. Please check.");
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // when a collector of this id is changed, update the collector in local db
                    Collector updatedCollector = snapshot.getValue(Collector.class);
                    assert updatedCollector != null;
                    String updatedCollectorId = updatedCollector.getCollectorId();
                    if (collectorIds.contains(updatedCollectorId)) {
                        // this updateCollector function can also handle when only the collector status was changed
                        dbManager.updateCollector(updatedCollector);

                        Collector currentCollector = collectorList.stream().filter(collector -> collector.getCollectorId().equals(updatedCollectorId)).findFirst().orElse(null);
                        if (currentCollector != null) {

                            // compare the current collector and the updated collector, return the difference
                            Collector.ChangeStatus changeStatus = currentCollector.compareWith(updatedCollector);

                            if (changeStatus == Collector.ChangeStatus.NO_CHANGE) {
                                // do nothing, but log because this should not happen
                                Log.i("collector childEventListener", "The collector with id " + updatedCollectorId + " is changed, but no change is detected. Please check.");
                            } else {
                                CrepeNotificationManager crepeNotificationManager = new CrepeNotificationManager(getContext(), getActivity());
                                if (changeStatus == Collector.ChangeStatus.DESCRIPTION_CHANGE) {
                                    // if the collector description is changed, we need to notify the participant and researcher
                                    crepeNotificationManager.showNotification("The description of your " + currentCollector.getAppName() + " collector is changed. See details in the app.");
                                } else if (changeStatus == Collector.ChangeStatus.COLLECTOR_START_TIME_CHANGE) {
                                    // if the collector start time is changed, we don't need to notify the participant and researcher
                                    Log.i("collector childEventListener", "The collector with id " + updatedCollectorId + " start time is changed in Firebase.");
                                } else if (changeStatus == Collector.ChangeStatus.COLLECTOR_END_TIME_CHANGE) {
                                    // if the collector end time is changed, we need to notify the participant and researcher
                                    crepeNotificationManager.showNotification("The end time of your " + currentCollector.getAppName() + " collector has changed. See details in the app.");
                                } else if (changeStatus == Collector.ChangeStatus.COLLECTOR_STATUS_CHANGE) {
                                    // if the collector status is changed, we need to notify the participant and researcher
                                    // if the status changed to deleted
                                    if (updatedCollector.getCollectorStatus().equals(DELETED)) {
                                        crepeNotificationManager.showNotification("Your " + currentCollector.getAppName() + " collector has been deleted by the researcher. No more data will be collected. See details in the app.");
                                    }
                                    // if the status changed to expired, that means the collection is complete
                                    if (updatedCollector.getCollectorStatus().equals(EXPIRED)) {
                                        crepeNotificationManager.showNotification("Your " + currentCollector.getAppName() + " collector has finished. Thank you for your participation!");
                                    }
                                    // if the status changed to active, something is wrong
                                    if (updatedCollector.getCollectorStatus().equals(CollectorCardConstraintLayoutBuilder.ACTIVE)) {
                                        Log.e("collector childEventListener", "The collector with id " + updatedCollectorId + " status has changed to active. Please check.");
                                    }
                                    Log.e("collector childEventListener", "The collector with id " + updatedCollectorId + " status is changed to " + updatedCollector.getCollectorStatus() + ", but not caught. Please check.");
                                }

                                // TODO Yuwen we need to check for collector participant list, and notify the researcher when a participant drops
                                // But doing this might require data base schema change, on hold for now

                            }

                        } else {
                            Log.e("collector childEventListener", "Cannot find the collector with id " + updatedCollectorId + " in local db. Please check.");
                        }

                    }
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    // collectors should not be removed from firebased based on our setup, but only marked as deleted in collectorStatus
                    Log.e("collector childEventListener", "This collector with id " + snapshot.getKey() + " is mistakenly removed. Please check.");
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Log.e("collector childEventListener", "This collector with id " + snapshot.getKey() + " is mistakenly moved. Please check.");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("collector childEventListener", "The read failed: " + error.getCode());
                }
            };

            datafieldListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // when a datafield of current local collector is added in firebase, add it to local db
                    // TODO Yuwen why is it added when i log in???
                    Datafield addedDatafield = snapshot.getValue(Datafield.class);
                    assert addedDatafield != null;
                    if (collectorIds.contains(addedDatafield.getCollectorId())) {
                        dbManager.addDatafield(addedDatafield);
                        String collectorId = addedDatafield.getCollectorId();
                        String collectorName = collectorList.stream().filter(collector -> collector.getCollectorId().equals(collectorId)).findFirst().orElse(null).getAppName();
                        CrepeNotificationManager crepeNotificationManager = new CrepeNotificationManager(getContext(), getActivity());
                        crepeNotificationManager.showNotification("Datafields added to your " + collectorName + " collector. See details in the app.");
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    // when a datafield of this id is changed, update the datafield in local db
                    Datafield updatedDatafield = snapshot.getValue(Datafield.class);
                    String updatedDatafieldId = updatedDatafield.getCollectorId();
                    if (collectorIds.contains(updatedDatafieldId)) {
                        dbManager.updateDatafield(updatedDatafield);
                        String collectorId = updatedDatafield.getCollectorId();
                        String collectorName = collectorList.stream().filter(collector -> collector.getCollectorId().equals(collectorId)).findFirst().orElse(null).getAppName();
                        CrepeNotificationManager crepeNotificationManager = new CrepeNotificationManager(getContext(), getActivity());
                        crepeNotificationManager.showNotification("Datafields are modified for your " + collectorName + " collector. See details in the app.");
                    }
                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    // datafields should not be removed from firebased based on our setup, but only marked as deleted in datafieldStatus
                    Datafield removedDatafield = snapshot.getValue(Datafield.class);
                    if (collectorIds.contains(removedDatafield.getCollectorId())) {
                        dbManager.removeDatafieldById(removedDatafield.getDatafieldId());
                        String collectorId = removedDatafield.getCollectorId();
                        String collectorName = collectorList.stream().filter(collector -> collector.getCollectorId().equals(collectorId)).findFirst().orElse(null).getAppName();
                        CrepeNotificationManager crepeNotificationManager = new CrepeNotificationManager(getContext(), getActivity());
                        crepeNotificationManager.showNotification("Datafields removed from your " + collectorName + " collector. See details in the app.");
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Log.e("datafield childEventListener", "This datafield with id " + snapshot.getKey() + " is somehow moved. Please check.");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("datafield childEventListener", "The read failed: " + error.getCode());
                }
            };

            collectorReference.addChildEventListener(collectorListener);
            datafieldReference.addChildEventListener(datafieldListener);

        }


        // check accessibility service permission
        if (!CrepeAccessibilityService.isAccessibilityServiceEnabled(getContext(), CrepeAccessibilityService.class)) {
            // show the accessibility service permission dialog
            Dialog dialog = AccessibilityPermissionManager.getInstance().getEnableAccessibilityServiceDialog(getContext());
            dialog.show();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (collectorListener != null) {
            collectorReference.removeEventListener(collectorListener);
        }
        if (datafieldListener != null) {
            datafieldReference.removeEventListener(datafieldListener);
        }
        EventBus.getDefault().unregister(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadingEvent(DataLoadingEvent event) {
        if (event.isCompleted()) {
            try {
                initCollectorList();
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void initCollectorList() throws PackageManager.NameNotFoundException {
        // at this time, only collectors for this user are retrieved from firebase to local db
        // see code in GoogleLoginActivity.java and function addParticipatingCollectors
        collectorList = dbManager.getActiveCollectors();

        TextView noCollectorTextView = getView().findViewById(R.id.empty_text_view);
        LinearLayout fragmentInnerLinearLayout = getView().findViewById(R.id.fragment_home_inner_linear_layout);
        // clear the collector list
        fragmentInnerLinearLayout.removeAllViews();

        if (!collectorList.isEmpty()) {
            // hide the no collector textview
            noCollectorTextView.setVisibility(View.GONE);

            // get all installed apps
            Map<String, Drawable> apps = getAppImage();
            CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(getActivity(), homeFragmentRefreshCollectorListRunnable, apps);


            for (Collector collector : collectorList) {

                ConstraintLayout collectorCardView = builder.build(collector, fragmentInnerLinearLayout, "cardLayout");

                // if the cardView is not null, meaning the collector is not in deleted status
                if (collectorCardView != null) {
                    collectorCardView.setId(View.generateViewId());

                    // Toast.makeText(this.getActivity(), fragmentInnerConstraintLayout.toString(), Toast.LENGTH_LONG).show();
                    fragmentInnerLinearLayout.addView(collectorCardView);
                }
            }

        } else {
            noCollectorTextView.setVisibility(View.VISIBLE);
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
                apps.put(name, image);
            }
        }
        return apps;
    }


}
