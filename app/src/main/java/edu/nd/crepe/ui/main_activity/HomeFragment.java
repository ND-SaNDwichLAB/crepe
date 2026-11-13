package edu.nd.crepe.ui.main_activity;

import static edu.nd.crepe.database.Collector.DELETED;
import static edu.nd.crepe.database.Collector.EXPIRED;
import static edu.nd.crepe.database.Collector.ACTIVE;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
import com.google.firebase.database.DatabaseException;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private Long BUFFER_GAP = 500L; // a gap to avoid triggering the onChildAdded Firebase listener when we attach it
    private DatabaseManager dbManager;
    private DatabaseReference collectorReference;
    private ChildEventListener collectorListener;
    private DatabaseReference datafieldReference;
    private ChildEventListener datafieldListener;
    private List<Collector> collectorList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        dbManager = DatabaseManager.getInstance(Objects.requireNonNull(this.getActivity()).getApplicationContext());
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            getActivity().setTitle("Crepe");
        }
        try {
            initCollectorList();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not build the collector list", e);
        }

        // check accessibility service permission
        Context context = getContext();
        if (context != null && !CrepeAccessibilityService.isAccessibilityServiceEnabled(context, CrepeAccessibilityService.class)) {
            // show the accessibility service permission dialog
            Dialog dialog = AccessibilityPermissionManager.getInstance().getEnableAccessibilityServiceDialog(context);
            if (dialog != null) {
                dialog.show();
            }
        }
    }

    // The event bus and the firebase listeners are tied to the started state: they are released in onStop,
    // so they have to be (re-)acquired in onStart, not just once when the view is created. Otherwise the
    // screen stops updating after the app has been in the background, for example during a demonstration.
    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        attachFirebaseListeners();
    }

    @Override
    public void onStop() {
        super.onStop();
        detachFirebaseListeners();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    private void attachFirebaseListeners() {
        // if there are collectors locally, add listener to firebase to watch for remote updates to these collectors
        if (collectorList == null || collectorList.isEmpty() || collectorListener != null || datafieldListener != null) {
            return;
        }

        try {
            // record the current timestamp
            Long attachTimestamp = System.currentTimeMillis();

            // add listener to collector and datafield updates
            collectorReference = FirebaseDatabase.getInstance().getReference(Collector.class.getSimpleName());
            datafieldReference = FirebaseDatabase.getInstance().getReference(Datafield.class.getSimpleName());

            collectorListener = createNewCollectorListener(attachTimestamp);
            collectorReference.addChildEventListener(collectorListener);

            datafieldListener = createNewDatafieldListener(attachTimestamp);
            datafieldReference.addChildEventListener(datafieldListener);
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not listen to remote collector updates", e);
            detachFirebaseListeners();
        }
    }

    private void detachFirebaseListeners() {
        if (collectorListener != null && collectorReference != null) {
            collectorReference.removeEventListener(collectorListener);
        }
        if (datafieldListener != null && datafieldReference != null) {
            datafieldReference.removeEventListener(datafieldListener);
        }
        collectorListener = null;
        datafieldListener = null;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDataLoadingEvent(DataLoadingEvent event) {
        if (event.isCompleted()) {
            try {
                initCollectorList();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not refresh the collector list", e);
            }
        }
    }

    public void initCollectorList() throws PackageManager.NameNotFoundException {
        // This can be reached after the fragment's view is gone (a late firebase result, or a refresh
        // requested by a dialog that outlived the screen). There is nothing to draw on then.
        View root = getView();
        Activity activity = getActivity();
        if (root == null || activity == null || dbManager == null) {
            Log.w(TAG, "The home screen is not showing, skipping the collector list refresh");
            return;
        }

        TextView noCollectorTextView = root.findViewById(R.id.empty_text_view);
        LinearLayout fragmentInnerLinearLayout = root.findViewById(R.id.fragment_home_inner_linear_layout);
        if (noCollectorTextView == null || fragmentInnerLinearLayout == null) {
            Log.e(TAG, "The home screen layout is missing its collector list views");
            return;
        }

        // at this time, only active collectors for this user are retrieved from firebase to local db
        // see code in GoogleLoginActivity.java and function addParticipatingCollectors
        collectorList = dbManager.getActiveCollectors();

        // clear the collector list
        fragmentInnerLinearLayout.removeAllViews();

        if (!collectorList.isEmpty()) {
            // hide the no collector textview
            noCollectorTextView.setVisibility(View.GONE);

            // get all installed apps
            Map<String, Drawable> apps = getAppImage();
            CollectorCardConstraintLayoutBuilder builder = new CollectorCardConstraintLayoutBuilder(activity, homeFragmentRefreshCollectorListRunnable, apps);


            for (Collector collector : collectorList) {

                // one collector that cannot be drawn must not take the whole list down with it
                try {
                    ConstraintLayout collectorCardView = builder.build(collector, fragmentInnerLinearLayout, "cardLayout");

                    // if the cardView is not null, meaning the collector is not in deleted status
                    if (collectorCardView != null) {
                        collectorCardView.setId(View.generateViewId());

                        // Toast.makeText(this.getActivity(), fragmentInnerConstraintLayout.toString(), Toast.LENGTH_LONG).show();
                        fragmentInnerLinearLayout.addView(collectorCardView);
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "Could not show the collector " + collector.getCollectorId(), e);
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
                Log.e(TAG, "Could not refresh the collector list", e);
            }
        }
    };


    public Map<String, Drawable> getAppImage() throws PackageManager.NameNotFoundException {
        Map<String, Drawable> apps = new HashMap<String, Drawable>();
        Context context = getContext();
        if (context == null) {
            return apps;
        }

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        // get list of all the apps installed
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> ril = packageManager.queryIntentActivities(mainIntent, 0);

        for (ResolveInfo ri : ril) {
            if (ri.activityInfo != null) {
                // an app that was uninstalled while we are looking, or one with a broken label, is skipped
                try {
                    String name;
                    // get package
                    Resources res = packageManager.getResourcesForApplication(ri.activityInfo.applicationInfo);
                    // if activity label res is found
                    if (ri.activityInfo.labelRes != 0) {
                        name = res.getString(ri.activityInfo.labelRes);
                    } else {
                        name = ri.activityInfo.applicationInfo.loadLabel(packageManager).toString();

                    }
                    Drawable image = packageManager.getApplicationIcon(ri.activityInfo.packageName);
                    apps.put(name, image);
                } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                    Log.w(TAG, "Skipping app " + ri.activityInfo.packageName + ": " + e.getMessage());
                }
            }
        }
        return apps;
    }

    // the name of the app of one of the collectors that are shown, or a generic name if it is unknown
    private String appNameOf(String collectorId) {
        List<Collector> currentCollectors = collectorList;
        if (currentCollectors != null) {
            for (Collector collector : currentCollectors) {
                if (Objects.equals(collector.getCollectorId(), collectorId) && collector.getAppName() != null) {
                    return collector.getAppName();
                }
            }
        }
        return "";
    }

    private void showNotification(String message) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        new CrepeNotificationManager(context, getActivity()).showNotification(message);
    }

    private ChildEventListener createNewCollectorListener(Long attachTimestamp) {

        // get all collector ids
        List<String> collectorIds = collectorList.stream().map(Collector::getCollectorId).collect(Collectors.toList());

        return new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                // note that this will get triggered when we attach the listener, so we need to check the timestamp
                if (snapshot.exists() && snapshot.getValue() != null) {
                    Long currentTimestamp = System.currentTimeMillis();
                    if (currentTimestamp > attachTimestamp + BUFFER_GAP) { // add a small buffer to avoid duplicate
                        // when a collector of this id is added, do not do anything, this actually should not happen
                        Log.i("collector childEventListener", "This collector with id " + snapshot.getKey() + " is somehow added. Please check.");
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Collector updatedCollector;
                try {
                    updatedCollector = snapshot.getValue(Collector.class);
                } catch (DatabaseException e) {
                    Log.e("collector childEventListener", "Could not read the changed collector " + snapshot.getKey(), e);
                    return;
                }
                if (updatedCollector == null) {
                    Log.e("collector childEventListener", "The changed collector " + snapshot.getKey() + " is empty. Please check.");
                    return;
                }

                // when a collector of this id is changed, update the collector in local db
                String updatedCollectorId = updatedCollector.getCollectorId();
                if (updatedCollectorId != null && collectorIds.contains(updatedCollectorId)) {
                    // this updateCollector function can also handle when only the collector status was changed
                    dbManager.updateCollector(updatedCollector);

                    Collector currentCollector = null;
                    for (Collector collector : collectorList) {
                        if (updatedCollectorId.equals(collector.getCollectorId())) {
                            currentCollector = collector;
                            break;
                        }
                    }
                    if (currentCollector != null) {

                        // compare the current collector and the updated collector, return the difference
                        Collector.ChangeStatus changeStatus = currentCollector.compareWith(updatedCollector);

                        if (changeStatus == Collector.ChangeStatus.NO_CHANGE) {
                            // do nothing, but log because this should not happen
                            Log.i("collector childEventListener", "The collector with id " + updatedCollectorId + " is changed, but no change is detected. Please check.");
                        } else if (changeStatus != null) {
                            if (changeStatus == Collector.ChangeStatus.DESCRIPTION_CHANGE) {
                                // if the collector description is changed, we need to notify the participant and researcher
                                showNotification("The description of your " + currentCollector.getAppName() + " collector is changed. See details in the app.");
                            } else if (changeStatus == Collector.ChangeStatus.COLLECTOR_START_TIME_CHANGE) {
                                // if the collector start time is changed, we don't need to notify the participant and researcher
                                Log.i("collector childEventListener", "The collector with id " + updatedCollectorId + " start time is changed in Firebase.");
                            } else if (changeStatus == Collector.ChangeStatus.COLLECTOR_END_TIME_CHANGE) {
                                // if the collector end time is changed, we need to notify the participant and researcher
                                showNotification("The end time of your " + currentCollector.getAppName() + " collector has changed. See details in the app.");
                            } else if (changeStatus == Collector.ChangeStatus.COLLECTOR_STATUS_CHANGE) {
                                // if the collector status is changed, we need to notify the participant and researcher
                                // if the status changed to deleted
                                if (DELETED.equals(updatedCollector.getCollectorStatus())) {
                                    showNotification("Your " + currentCollector.getAppName() + " collector has been deleted by the researcher. No more data will be collected. See details in the app.");
                                }
                                // if the status changed to expired, that means the collection is complete
                                if (EXPIRED.equals(updatedCollector.getCollectorStatus())) {
                                    showNotification("Your " + currentCollector.getAppName() + " collector has finished. Thank you for your participation!");
                                }
                                // if the status changed to active, something is wrong
                                if (ACTIVE.equals(updatedCollector.getCollectorStatus())) {
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
    }


    private ChildEventListener createNewDatafieldListener(Long attachTimestamp) {

        // get all collector ids
        List<String> collectorIds = collectorList.stream().map(Collector::getCollectorId).collect(Collectors.toList());
        return new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                // note that this will get triggered when we attach the listener, so we need to check the timestamp
                if (snapshot.exists() && snapshot.getValue() != null) {
                    Long currentTimestamp = System.currentTimeMillis();
                    if (currentTimestamp > attachTimestamp + BUFFER_GAP) { // add a small buffer to avoid duplicate
                        // when a datafield of current local collector is added in firebase, add it to local db
                        Datafield addedDatafield = readDatafield(snapshot);
                        if (addedDatafield != null && collectorIds.contains(addedDatafield.getCollectorId())) {
                            dbManager.addDatafield(addedDatafield);
                            showNotification("Datafields added to your " + appNameOf(addedDatafield.getCollectorId()) + " collector. See details in the app.");
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // when a datafield of this id is changed, update the datafield in local db
                Datafield updatedDatafield = readDatafield(snapshot);
                if (updatedDatafield != null && collectorIds.contains(updatedDatafield.getCollectorId())) {
                    dbManager.updateDatafield(updatedDatafield);
                    showNotification("Datafields are modified for your " + appNameOf(updatedDatafield.getCollectorId()) + " collector. See details in the app.");
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // datafields should not be removed from firebased based on our setup, but only marked as deleted in datafieldStatus
                Datafield removedDatafield = readDatafield(snapshot);
                if (removedDatafield != null && collectorIds.contains(removedDatafield.getCollectorId())) {
                    dbManager.removeDatafieldById(removedDatafield.getDatafieldId());
                    showNotification("Datafields removed from your " + appNameOf(removedDatafield.getCollectorId()) + " collector. See details in the app.");
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
    }

    // null if the node cannot be converted, instead of an exception inside a firebase listener
    @Nullable
    private static Datafield readDatafield(DataSnapshot snapshot) {
        try {
            Datafield datafield = snapshot.getValue(Datafield.class);
            if (datafield == null) {
                Log.e("datafield childEventListener", "The datafield " + snapshot.getKey() + " is empty. Please check.");
            }
            return datafield;
        } catch (DatabaseException e) {
            Log.e("datafield childEventListener", "Could not read the datafield " + snapshot.getKey(), e);
            return null;
        }
    }


}
