package edu.nd.crepe.demonstration;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import edu.nd.crepe.network.ApiCallManager;
import edu.nd.crepe.servicemanager.CrepeAccessibilityService;
import edu.nd.crepe.R;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.graphquery.model.Node;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import edu.nd.crepe.graphquery.ontology.SugiliteEntity;
import edu.nd.crepe.graphquery.ontology.SugiliteRelation;
import edu.nd.crepe.graphquery.ontology.UISnapshot;
import edu.nd.crepe.servicemanager.CrepeDisplayPermissionManager;
import edu.nd.crepe.ui.dialog.AddDatafieldDescriptionDialogBuilder;
import edu.nd.crepe.ui.dialog.DatafieldDescriptionCallback;
import edu.nd.crepe.ui.dialog.GraphQueryCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FullScreenOverlayManager implements DatafieldDescriptionCallback {

    private Context context;
    private WindowManager windowManager;
    private String desiredQuery = "";
    private View overlay;
    private DisplayMetrics displayMetrics;
    private GraphQueryCallback graphQueryCallback;
    private Boolean showingOverlay;
    private NavigationBarUtil navigationBarUtil;
    private int overlayCurrentHeight;
    private int overlayCurrentWidth;
    private int overlayCurrentFlag;
    private SelectionOverlayViewManager selectionOverlayViewManager;
    private View dimView;
    private List<Pair<OntologyQuery, Double>> defaultQueries;
    private SugiliteEntity<Node> targetEntity = new SugiliteEntity<>();
    private UISnapshot uiSnapshot;

    private int entityId = 0;

    // true while the best query is being selected on the worker thread. The tap handler overwrites the fields that
    // selection reads (target entity, candidate queries, snapshot), so taps are ignored until it has finished.
    private volatile boolean selectingQuery = false;

    public FullScreenOverlayManager(Context context, WindowManager windowManager, DisplayMetrics displayMetrics, GraphQueryCallback graphQueryCallback) {
        this.context = context;
        this.windowManager = windowManager;
        this.displayMetrics = displayMetrics;
        this.graphQueryCallback = graphQueryCallback;
        this.overlay = getRectangleOverlay(context, displayMetrics.widthPixels, displayMetrics.heightPixels, Const.RECORDING_OVERLAY_COLOR);
        this.showingOverlay = false;
        this.navigationBarUtil = new NavigationBarUtil();
        this.overlayCurrentHeight = displayMetrics.heightPixels;
        //hack -- leave 1px at the right end of the screen so the input method window becomes visible
        this.overlayCurrentWidth = displayMetrics.widthPixels - 1;
        this.overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        this.selectionOverlayViewManager = new SelectionOverlayViewManager(context);
        this.dimView = new View(context);
    }

    public void enableOverlay(WidgetDisplay widgetDisplay) {

        // adding the same view twice throws
        if (showingOverlay) {
            return;
        }

        // make sure the overlay is not tappable
        overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        WindowManager.LayoutParams layoutParams = updateLayoutParams(overlayCurrentFlag, overlayCurrentWidth, overlayCurrentHeight);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        // Without the permission the window manager rejects the overlay, so ask for it and stop here.
        if (!checkDrawOverlayPermission()) {
            return;
        }
        try {
            windowManager.addView(overlay, layoutParams);
        } catch (RuntimeException e) {
            Log.e("FullScreenOverlay", "Could not add the overlay to the window manager", e);
            Toast.makeText(context, "Could not start the demonstration overlay.", Toast.LENGTH_LONG).show();
            return;
        }

        setOverlayOnTouchListener(true, widgetDisplay);

        // set the flag
        showingOverlay = true;
        widgetDisplay.refreshWidget();
    }

    public void disableOverlay() {
        // removing a view that is not attached throws
        if (showingOverlay) {
            removeViewSafely(overlay);
        }
        // set the flag
        showingOverlay = false;
    }

    // Removes a view from the window manager, tolerating that it was never added or was already removed.
    private void removeViewSafely(@Nullable View view) {
        if (view == null) {
            return;
        }
        try {
            windowManager.removeView(view);
        } catch (IllegalArgumentException e) {
            Log.w("FullScreenOverlay", "The view was not attached to the window manager");
        }
    }

    public void refreshOverlay(WidgetDisplay widgetDisplay) {
        disableOverlay();
        enableOverlay(widgetDisplay);
    }

    public Boolean getShowingOverlay() {
        return showingOverlay;
    }

    private WindowManager.LayoutParams updateLayoutParams(int flag, int width, int height) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Const.OVERLAY_TYPE,
                flag,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        int real_y = 0;
        int statusBarHeight = navigationBarUtil.getStatusBarHeight(context);
        real_y -= statusBarHeight;

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.x = 0;
        layoutParams.y = real_y;
        layoutParams.width = width;
        layoutParams.height = height;
        return layoutParams;
    }

    private View getRectangleOverlay(Context context, int width, int height, int color) {
        View overlay = new View(context);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Const.OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        layoutParams.width = width;
        layoutParams.height = height;
        overlay.setLayoutParams(layoutParams);
        overlay.setBackgroundColor(color);
        return overlay;
    }

    // returns true if we may draw over other apps; otherwise asks the user for the permission and returns false
    private boolean checkDrawOverlayPermission() {
        /* check if we already  have permission to draw over other apps */
        if (Settings.canDrawOverlays(context)) {
            return true;
        }
        // if not, form up an Intent to launch the permission request
        try {
            android.app.Dialog dialog = CrepeDisplayPermissionManager.getInstance().getEnableDisplayServiceDialog(context);
            if (dialog != null) {
                dialog.show();
            }
        } catch (RuntimeException e) {
            // a dialog cannot always be shown from a service context
            Log.e("FullScreenOverlay", "Could not ask for the overlay permission", e);
            Toast.makeText(context, "Please allow Crepe to display over other apps in your device settings.", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void setOverlayOnTouchListener(final boolean toConsumeEvent, WidgetDisplay widgetDisplay) {
        try {
            overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowManager.updateViewLayout(overlay, updateLayoutParams(overlayCurrentFlag, overlayCurrentWidth, overlayCurrentHeight));
            overlay.setBackgroundColor(Const.RECORDING_OVERLAY_COLOR);
            overlay.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        overlay.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector myGestureDetector = new GestureDetector(context, new MyGestureDetector());

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                return myGestureDetector.onTouchEvent(event);
            }

            class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

                SelectionOverlayView selectionOverlay = null;

                @RequiresApi(api = Build.VERSION_CODES.R)
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    if (selectingQuery) {
                        Toast.makeText(context, "Still working on your previous selection, please wait a moment.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    try {
                        //single tap up detected
                        System.out.println("Single tap detected");

                        float rawX = event.getRawX();
                        float rawY = event.getRawY();
                        float radius = 10;

                        // account for the navigation bar height
                        float navHeight = navigationBarUtil.getStatusBarHeight(context);
                        float adjustedY = rawY - navHeight;

                        // show the matched item on screen
    //                    windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
    //                    WindowManager.LayoutParams selectionLayoutParams = updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    //                    Rect clickedItemBounds = DemonstrationUtil.getBoundingBoxOfClickedItem(rawX, rawY);
    //                    // move the clickedItemBounds up by the navHeight
    //                    if (clickedItemBounds != null) {
    //                        clickedItemBounds.offset(0, -1 * (int) navHeight);
    //                        this.selectionOverlay = selectionOverlayViewManager.getRectOverlay(clickedItemBounds);
    //                        windowManager.addView(this.selectionOverlay, selectionLayoutParams);
    //                    }

                        // forget what was tapped before, otherwise a tap on an empty area would reuse the previous target
                        targetEntity = new SugiliteEntity<>();

                        CrepeAccessibilityService accessibilityService = CrepeAccessibilityService.getsSharedInstance();
                        if (accessibilityService == null) {
                            Toast.makeText(context, "The Crepe accessibility service is not running. Please enable it and try again.", Toast.LENGTH_LONG).show();
                            return false;
                        }

                        // get the current uisnapshot
                        uiSnapshot = accessibilityService.generateUISnapshot();

                        List<AccessibilityNodeInfo> matchedAccessibilityNodeList = accessibilityService.getMatchingNodeFromClickWithText(rawX, rawY);
                        // this matchedAccessibilityNode is an AccessibilityNodeInfo, which is not exactly the node stored in the screen's nodeSugiliteEntityMap.
                        // We retrieved that stored node from this screen's uisnapshot
                        AccessibilityNodeInfo matchedNode;

                        if (matchedAccessibilityNodeList != null) {
                            if (matchedAccessibilityNodeList.size() == 1) {
                                matchedNode = matchedAccessibilityNodeList.get(0);
                                targetEntity = uiSnapshot.getEntityWithAccessibilityNode(matchedNode);
                            } else {
                                // TODO: Find the node that we actually need
                                matchedNode = matchedAccessibilityNodeList.get(0);
                                targetEntity = uiSnapshot.getEntityWithAccessibilityNode(matchedNode);
                            }
                        }


                        if (targetEntity != null) {
                            SugiliteRelation[] relationsToExclude = new SugiliteRelation[1];
                            relationsToExclude[0] = SugiliteRelation.HAS_TEXT;
                            defaultQueries = DemonstrationUtil.generateDefaultQueries(uiSnapshot, targetEntity, relationsToExclude);
                        } else {
                            Log.e("generate queries", "Cannot find the tapped entity!");
                            return false;
                        }

                        if (targetEntity.getEntityValue() == null || defaultQueries == null || defaultQueries.size() == 0) {
                            Toast.makeText(context, "Sorry! We do not support the data you just clicked. Please try again.", Toast.LENGTH_SHORT).show();
                            return false;
                        }

                        // inflate the demonstration_confirmation.xml layout
                        // Specify a layoutparams to display the dialog at the center of the screen

                        DisplayMetrics metrics = new DisplayMetrics();
                        windowManager.getDefaultDisplay().getMetrics(metrics);
                        double currentDensity = metrics.density;

                        int width = metrics.widthPixels;
                        int height = metrics.heightPixels;

                        WindowManager.LayoutParams dialogParams = new WindowManager.LayoutParams(
                                (int) ((width / currentDensity - 48) * currentDensity),   // leave 24 dp margin on both sides
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                Const.OVERLAY_TYPE,
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                                PixelFormat.TRANSLUCENT);
                        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        View confirmationView = layoutInflater.inflate(R.layout.demonstration_confirmation, null);

                        TextView queryTextView = confirmationView.findViewById(R.id.confirmationInfo);
                        // set the text of the dialog window
                        String displayText = "";

                        displayText = "You clicked on \"" + targetEntity.getEntityValue().getText() + "\". Do you want to collect this data?";
                        queryTextView.setText(displayText);

                        // Create a full-screen black view with a certain transparency
                        dimView.setBackgroundColor(Color.parseColor("#99000000")); // change the alpha value to adjust transparency

                        WindowManager.LayoutParams dimParams = new WindowManager.LayoutParams(
                                WindowManager.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.MATCH_PARENT,
                                Const.OVERLAY_TYPE,
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                PixelFormat.TRANSLUCENT);

                        // Add the dim view to the window
                        windowManager.addView(dimView, dimParams);
                        // Then add the confirmation dialog to the window
                        try {
                            windowManager.addView(confirmationView, dialogParams);
                        } catch (RuntimeException e) {
                            removeViewSafely(dimView);
                            throw e;
                        }


                        // set the onclick listener for the buttons
                        Button yesButton = confirmationView.findViewById(R.id.confirmationYesButton);
                        Button noButton = confirmationView.findViewById(R.id.confirmationNoButton);

                        yesButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //confirm the selection
                                // remove the confirmation dialog
                                if (confirmationView != null) {
                                    removeViewSafely(confirmationView);
                                }
                                // show the new view here
                                AddDatafieldDescriptionDialogBuilder addDatafieldDescriptionDialogBuilder = new AddDatafieldDescriptionDialogBuilder(context, selectionOverlay, windowManager, confirmationView, dialogParams, FullScreenOverlayManager.this);
                                View addDatafieldDescriptionView = addDatafieldDescriptionDialogBuilder.buildDialog();
                                try {
                                    windowManager.addView(addDatafieldDescriptionView, dialogParams);
                                } catch (RuntimeException e) {
                                    // do not leave the screen dimmed behind a dialog that never appeared
                                    Log.e("FullScreenOverlay", "Could not show the description dialog", e);
                                    removeViewSafely(dimView);
                                    Toast.makeText(context, "Sorry! Something went wrong. Please tap on the data to collect again.", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });

                        noButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // remove the confirmation dialog
                                if (confirmationView != null) {
                                    removeViewSafely(confirmationView);
                                }
                                // remove the selection overlay
                                if (selectionOverlay != null) {
                                    removeViewSafely(selectionOverlay);
                                }
                                // remove the dim view
                                if (dimView != null) {
                                    removeViewSafely(dimView);
                                }

                                Toast.makeText(context, "Please tap on the data to collect again", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return true;
                
                    } catch (RuntimeException e) {
                        Log.e("FullScreenOverlay", "Could not process the tap", e);
                        Toast.makeText(context, "Sorry! Something went wrong. Please tap on the data to collect again.", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                @Override
                public void onLongPress(MotionEvent event) {
                    System.out.println("Context click detected");
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
//                    handleContextClick(rawX, rawY, tts);
                    return;
                }

                @Override
                public boolean onContextClick(MotionEvent e) {
                    System.out.println("Context click detected");
                    return super.onContextClick(e);
                }


//                @Override
//                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//                    if (Const.ROOT_ENABLED) {
//                        recordingOverlayManager.setPassThroughOnTouchListener();
//                        try {
//                            recordingOverlayManager.performFlingWithRootPermission(e1, e2, new Runnable() {
//                                @Override
//                                public void run() {
//                                    //allow the overlay to get touch event after finishing the simulated gesture
//                                    recordingOverlayManager.setOverlayOnTouchListener(true);
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    return true;
//                }
            }

            /*
            class Scroll extends GestureDetector.SimpleOnGestureListener {
                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    if (Const.ROOT_ENABLED) {
                        recordingOverlayManager.setPassThroughOnTouchListener(overlay);
                        try {
                            recordingOverlayManager.performFlingWithRootPermission(e1, e2, new Runnable() {
                                @Override
                                public void run() {
                                    //allow the overlay to get touch event after finishing the simulated gesture
                                    recordingOverlayManager.setOverlayOnTouchListener(overlay, true);
                                }
                            });
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    return true;
                }
            }
            */

        });
    }

    private String selectBestQuery(String datafieldUserDescription) {
        // first, we filter out queries that cannot actually retrieve the target data
        List<Pair<OntologyQuery, Double>> correctQueries = new ArrayList<>();   // those that can actually retrieve the data
        if (defaultQueries == null || uiSnapshot == null) {
            return null;
        }
        for (Pair<OntologyQuery, Double> query : defaultQueries) {
            try {
                Set<SugiliteEntity> result = query.first.executeOn(uiSnapshot);
                if (result.contains(targetEntity)) {
                    correctQueries.add(query);
                }
            } catch (RuntimeException e) {
                // a candidate query that cannot even run is simply not a candidate
                Log.e("FullScreenOverlay", "Skipping a candidate query that failed to run", e);
            }
        }

        // second, we select the query with LLM
        if (correctQueries.size() == 0) {
            return null;
        } else if (correctQueries.size() == 1) {
            return correctQueries.get(0).first.toString();
        } else {

            // Call OpenAI API to select the best query based on user description and candidate queries
            // Initialize the CountDownLatch, to wait for async callback to finish
            CountDownLatch latch = new CountDownLatch(1);
            String correctQueryAggregated = IntStream.range(0, correctQueries.size())
                    .mapToObj(index -> index + ": " + correctQueries.get(index).first.toString())
                    .collect(Collectors.joining("\n"));
            String promptPrefix = "We defined this new graph query to locate relevant data on mobile UI structure. Now, we have generated the following potential queries:\n";
            String promptSuffix = "\nWhich one matches the following description the best? \n [insert-user-description] \n Please only return the index number, without any additional information, even periods or comma.\n Example response format: \n0";

            final String[] finalPrompt = {promptPrefix + correctQueryAggregated + promptSuffix.replace("[insert-user-description]", datafieldUserDescription)};

            final Pair<OntologyQuery, Double>[] bestQuery = new Pair[1];
            // set up API call
            ApiCallManager apiCallManager = new ApiCallManager(context);
            apiCallManager.getResponse(finalPrompt[0], new ApiCallManager.ApiCallback() {
                @Override
                public void onResponse(String response) {
                    Log.i("ApiCallManager", "API call successful: \n" + response);
                    int index = 0;  // by default, the index value would be 0
                    try {
                        index = Integer.parseInt(response.replaceAll("^\"|\"$", "").trim());
                    } catch (NumberFormatException e) {
                        Log.e("ApiCallManager", "API response is not a number, using index 0 instead: " + e.getMessage());
                        // by default, the index value would be 0
                    }
                    if (index >= 0 && index < correctQueries.size()) {
                        bestQuery[0] = correctQueries.get(index);
                    } else {
                        Log.e("ApiCallManager", "API response invalid: invalid index");
                    }
                    latch.countDown();
                }

                @Override
                public void onErrorResponse(Exception e) {
                    Log.e("ApiCallManager", "API call failed: " + e.getMessage());
                    latch.countDown();
                }
            });

            try {
                // the server normally answers well within the timeouts of the http client, this is only a safety net
                if (!latch.await(60, TimeUnit.SECONDS)) {
                    Log.e("ApiCallManager", "Timed out waiting for the API response, using the default query");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e("ApiCallManager", "Interrupted while waiting for the API response", e);
            } finally {
                apiCallManager.shutdown();
            }

            return bestQuery[0] == null ? correctQueries.get(0).first.toString() : bestQuery[0].first.toString();
        }


//        else {
//            // sort the queries by the heuristics score
//            Collections.sort(correctQueries, new Comparator<Pair<OntologyQuery, Double>>() {
//                @Override
//                public int compare(Pair<OntologyQuery, Double> o1, Pair<OntologyQuery, Double> o2) {
//                    if(o1.second > o2.second) return 1;
//                    else if (o1.second.equals(o2.second)) return 0;
//                    else return -1;
//                }
//            });
//            // select the query that can retrieve the least unrelated data
//            Pair<OntologyQuery, Double> bestQuery = correctQueries.get(0);
//            int minSize = bestQuery.first.executeOn(uiSnapshot).size();
//            for (int i = 1; i < correctQueries.size(); i++) {
//                int size = correctQueries.get(i).first.executeOn(uiSnapshot).size();
//                if (size < minSize && size >= 1) {
//                    minSize = size;
//                    bestQuery = correctQueries.get(i);
//                }
//
//            }
//            return bestQuery.first.toString();
//        }

    }

    @Override
    public void onProcessDescriptionEditText(String datafieldDescription) {
        if (datafieldDescription == null || datafieldDescription.trim().isEmpty()) {
            Toast.makeText(context, "Datafield description cannot be blank!", Toast.LENGTH_SHORT).show();
        } else {

            // deal with the Views
            removeViewSafely(dimView);

            // Selecting the best query may involve a call to a server, and this method runs on the UI thread,
            // so the selection happens on a worker thread and the result is handed back to the UI thread.
            final String description = datafieldDescription;
            // the text of what was tapped, read now while it is certainly still the entity that this description belongs to
            final String targetText = (targetEntity != null && targetEntity.getEntityValue() != null) ? targetEntity.getEntityValue().getText() : null;
            selectingQuery = true;
            Toast.makeText(context, "Working out how to find that data...", Toast.LENGTH_SHORT).show();
            Thread selectionThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String bestQuery = null;
                    try {
                        // select the correct query that can retrieve our result, using LLM
                        bestQuery = selectBestQuery(description);
                    } catch (RuntimeException e) {
                        Log.e("FullScreenOverlay", "Could not select a query for the demonstrated data", e);
                    }
                    final String selectedQuery = bestQuery;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finishDemonstration(selectedQuery, targetText);
                        }
                    });
                }
            }, "crepe-select-query");
            selectionThread.start();
        }
    }

    private void finishDemonstration(@Nullable String query, @Nullable String targetText) {
        selectingQuery = false;
        if (query == null) {
            // no candidate query is able to find the data again, so there is nothing sensible to save
            Toast.makeText(context, "Sorry! We could not work out how to find that data. Please tap on the data to collect again.", Toast.LENGTH_LONG).show();
            return;
        }

        // send the data to MainActivity
        desiredQuery = query;
        processCallback(targetText);
        // clear the overlay
        disableOverlay();
        // stop widget service
        Intent intent = new Intent(context, WidgetService.class);
        context.stopService(intent);
        // go back to the main activity
        Intent mainActivityIntent = context.getPackageManager().getLaunchIntentForPackage("edu.nd.crepe");
        if (mainActivityIntent != null) {
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(mainActivityIntent);
        } else {
            Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
        }
    }



    private void processCallback(String targetText) {
        if (this.graphQueryCallback == null) {
            Log.e("FullScreenOverlay", "There is no callback to hand the demonstrated data to");
            return;
        }
        this.graphQueryCallback.onDataReceived(desiredQuery, targetText);
    }
}
