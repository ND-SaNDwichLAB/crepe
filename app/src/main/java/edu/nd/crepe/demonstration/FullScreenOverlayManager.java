package edu.nd.crepe.demonstration;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import edu.nd.crepe.network.ApiCallManager;
import edu.nd.crepe.servicemanager.CrepeAccessibilityService;
import edu.nd.crepe.R;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.graphquery.model.Node;
import edu.nd.crepe.graphquery.ontology.OntologyQuery;
import edu.nd.crepe.graphquery.ontology.SugiliteEntity;
import edu.nd.crepe.graphquery.ontology.UISnapshot;
import edu.nd.crepe.servicemanager.CrepeDisplayPermissionManager;
import edu.nd.crepe.ui.dialog.AddDatafieldDescriptionDialogBuilder;
import edu.nd.crepe.ui.dialog.DatafieldDescriptionCallback;
import edu.nd.crepe.ui.dialog.GraphQueryCallback;
import edu.nd.crepe.ui.dialog.PickGraphQueryDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
    private OverlayViewManager overlayViewManager;
    private View dimView;
    private List<Pair<OntologyQuery, Double>> defaultQueries;
    private SugiliteEntity<Node> targetEntity = new SugiliteEntity<>();
    private UISnapshot uiSnapshot;

    private int entityId = 0;

    public FullScreenOverlayManager(Context context, WindowManager windowManager, DisplayMetrics displayMetrics, GraphQueryCallback graphQueryCallback) {
        this.context = context;
        this.windowManager = windowManager;
        this.displayMetrics = displayMetrics;
        this.graphQueryCallback = graphQueryCallback;
        this.overlay = getRectangleOverlay(context, displayMetrics.widthPixels, displayMetrics.heightPixels, Const.RECORDING_OVERLAY_COLOR);
        this.showingOverlay = false;
        this.navigationBarUtil = new NavigationBarUtil();
        this.overlayCurrentHeight = displayMetrics.heightPixels;
        this.overlayViewManager= new OverlayViewManager(context);
        //hack -- leave 1px at the right end of the screen so the input method window becomes visible
        this.overlayCurrentWidth = displayMetrics.widthPixels - 1;
        this.overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        this.overlayViewManager = new OverlayViewManager(context);
        this.dimView = new View(context);
    }

    public void enableOverlay(WidgetDisplay widgetDisplay) {

        // make sure the overlay is not tappable
        overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        WindowManager.LayoutParams layoutParams = updateLayoutParams(overlayCurrentFlag, overlayCurrentWidth, overlayCurrentHeight);

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= 23) {
            checkDrawOverlayPermission();
            System.out.println("ADDING OVERLAY TO WINDOW MANAGER");
        }
        windowManager.addView(overlay, layoutParams);

        setOverlayOnTouchListener(true, widgetDisplay);

        // set the flag
        showingOverlay = true;
        widgetDisplay.refreshWidget();
    }

    public void disableOverlay() {
        // TODO we need to refactor this and put fullscreen overlay part of overlayViewManager
        // TODO also removing overlay is all over the place, clean up
        windowManager.removeView(overlay);
        overlayViewManager.removeAllOverlays();
        // set the flag
        showingOverlay = false;
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

    private void checkDrawOverlayPermission() {
        /* check if we already  have permission to draw over other apps */
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentApiVersion >= 23) {
            if (!Settings.canDrawOverlays(context)) {
                // if not, form up an Intent to launch the permission request
                CrepeDisplayPermissionManager.getInstance().getEnableDisplayServiceDialog(context).show();
            }
        }
    }

    private void setOverlayOnTouchListener(final boolean toConsumeEvent, WidgetDisplay widgetDisplay) {
        try {
            overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            windowManager.updateViewLayout(overlay, updateLayoutParams(overlayCurrentFlag, overlayCurrentWidth, overlayCurrentHeight));
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

                @RequiresApi(api = Build.VERSION_CODES.R)
                @Override
                public boolean onSingleTapConfirmed(MotionEvent event) {
                    //single tap up detected
                    Log.i("overlay tap", "Single tap detected");

                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    float radius = 10;

                    // account for the navigation bar height
                    float navHeight = navigationBarUtil.getStatusBarHeight(context);
                    float adjustedY = rawY - navHeight;

                    // show click position immediately
                    overlayViewManager.showDotOverlay((int) rawX, (int) adjustedY, 20,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            Const.SELECTION_INDICATOR_COLOR, false);

                    Log.i("click position", "click x: " + rawX + ", click rawY: " + rawY +
                            ", click Y without status bar: " + adjustedY);
                    Log.i("position", "screen width: " + displayMetrics.widthPixels +
                            ", screen height: " + displayMetrics.heightPixels);

                    targetEntity = DemonstrationUtil.findTargetEntityFromOverlayClick(rawX, rawY);

                    if (targetEntity == null || targetEntity.getEntityValue() == null) {
                        Toast.makeText(context,
                                "Sorry! We do not support the data you just clicked. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    // Show rect overlay immediately
                    Rect clickedItemBounds = Rect.unflattenFromString(
                            targetEntity.getEntityValue().getBoundsInScreen());
                    if (clickedItemBounds != null) {
                        Log.i("clicked item position", "clicked item x: " + clickedItemBounds.left +
                                ", clicked item y: " + clickedItemBounds.top +
                                ", clicked item width: " + clickedItemBounds.width() +
                                ", clicked item height: " + clickedItemBounds.height());
                        clickedItemBounds.offset(0, -1 * (int) navHeight);
                        // show overlay
                        overlayViewManager.showRectOverlay(clickedItemBounds,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                Const.SELECTION_INDICATOR_COLOR, 3000);
                    }

                    // Start a new thread for heavy processing operations
                    new Thread(() -> {
                        // Generate uisnapshot and default queries in background
                        Pair<UISnapshot, List<Pair<OntologyQuery, Double>>> result = DemonstrationUtil.generateDefaultQueriesFromTargetEntity(targetEntity);
                        uiSnapshot = result.first;
                        defaultQueries = result.second;

                        // Check queries on background thread first
                        if (defaultQueries.isEmpty()) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(context,
                                        "Sorry! We do not support the data you just clicked. Please try again.",
                                        Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        // After heavy processing is done, handle UI updates on main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            // inflate the demonstration_confirmation.xml layout
                            DisplayMetrics metrics = new DisplayMetrics();
                            windowManager.getDefaultDisplay().getMetrics(metrics);
                            double currentDensity = metrics.density;

                            int width = metrics.widthPixels;
                            int height = metrics.heightPixels;

                            WindowManager.LayoutParams dialogParams = new WindowManager.LayoutParams(
                                    (int) ((width / currentDensity - 48) * currentDensity),
                                    WindowManager.LayoutParams.WRAP_CONTENT,
                                    Const.OVERLAY_TYPE,
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                                    PixelFormat.TRANSLUCENT);

                            LayoutInflater layoutInflater = (LayoutInflater) context
                                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            View confirmationView = layoutInflater.inflate(R.layout.demonstration_confirmation, null);

                            TextView queryTextView = confirmationView.findViewById(R.id.confirmationInfo);
                            String collectedContent = targetEntity.getEntityValue().getEntityContent();
                            String displayText = "You tapped on \"" + collectedContent +
                                    "\". Do you want to collect this data?";
                            queryTextView.setText(displayText);

                            // Create a full-screen black view with transparency
                            dimView.setBackgroundColor(Color.parseColor("#99000000"));

                            WindowManager.LayoutParams dimParams = new WindowManager.LayoutParams(
                                    WindowManager.LayoutParams.MATCH_PARENT,
                                    WindowManager.LayoutParams.MATCH_PARENT,
                                    Const.OVERLAY_TYPE,
                                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                    PixelFormat.TRANSLUCENT);

                            // Add views to window
                            windowManager.addView(dimView, dimParams);
                            windowManager.addView(confirmationView, dialogParams);

                            // Set button click listeners
                            Button yesButton = confirmationView.findViewById(R.id.confirmationYesButton);
                            Button noButton = confirmationView.findViewById(R.id.confirmationNoButton);

                            yesButton.setOnClickListener(v -> {
                                if (confirmationView != null) {
                                    windowManager.removeView(confirmationView);
                                }
                                if (overlayViewManager != null) {
                                    overlayViewManager.removeAllOverlays();
                                }
                                PickGraphQueryDialogBuilder builder =
                                        new PickGraphQueryDialogBuilder(context, windowManager,
                                                confirmationView, dialogParams, FullScreenOverlayManager.this);
                                // TODO here, before we build the dialog, we translate the default queries to human-readable format
                                List<Pair<OntologyQuery, String>> translatedQueries = translateQueryToString(defaultQueries);
                                View pickGraphQueryDialogView = builder.buildDialog(translatedQueries);
                                windowManager.addView(pickGraphQueryDialogView, dialogParams);
                            });

                            noButton.setOnClickListener(v -> {
                                if (confirmationView != null) {
                                    windowManager.removeView(confirmationView);
                                }
                                if (overlayViewManager != null) {
                                    overlayViewManager.removeAllOverlays();
                                }
                                if (dimView != null) {
                                    windowManager.removeView(dimView);
                                }
                                Toast.makeText(context, "Please tap on the data to collect again",
                                        Toast.LENGTH_SHORT).show();
                            });
                        });
                    }).start();

                    return true;
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

    // use OpenAI API to translate the query to human-readable format
    private List<Pair<OntologyQuery, String>> translateQueryToString(List<Pair<OntologyQuery, Double>> queries) {
        if (queries == null || queries.isEmpty()) {
            return null;
        }

        // Call OpenAI API to translate the query to human-readable format
        // Use one API call to translate all queries
        // Initialize the CountDownLatch, to wait for async callback to finish
        CountDownLatch latch = new CountDownLatch(1);
        final List<Pair<OntologyQuery, String>> translatedQueries = new ArrayList<>();
        // set up API call
        ApiCallManager apiCallManager = new ApiCallManager(context);

        String allQueries = queries.stream().map(query -> query.first.toString()).collect(Collectors.joining("\n"));

        String translateQueryPrompt = "We defined a Graph Query to locate target data on mobile UI structure. It describes the unique attributes of the data we are targeting.\n" +
                "For example, the query \n(conj (HAS_CLASS_NAME android.widget.FrameLayout) (RIGHT (conj (hasText \" 6\") (HAS_CLASS_NAME android.widget.TextView) (HAS_PACKAGE_NAME com.ubercab)) ) (HAS_PACKAGE_NAME com.ubercab)) \n" +
                "stands for: the information that is located to the right of a text \"6\"\n." +
                "Below I have a few queries, can you help me translate them to human-readable format like above?\n" +
                allQueries +
                "\n Leave out UI element names (TextView, FrameLayout) and do not make any reference to \"view\". Users only care about the data and information contained in the view instead of the UI elements themselves. \"With numeric index xx\" should be translated into \"the xx in the list\" (first, second, etc.). Be as concise as possible. Make sure you return the translation in the order I presented the queries above, separated by new lines. Return nothing else.";


        apiCallManager.getResponse(translateQueryPrompt, new ApiCallManager.ApiCallback() {
            @Override
            public void onResponse(String response) {
                Log.i("ApiCallManager", "API call successful: \n" + response);
                String[] translatedQueryStrings = response.split("\n");
                if (translatedQueryStrings.length == queries.size()) {
                    for (int i = 0; i < queries.size(); i++) {
                        translatedQueries.add(new Pair<>(queries.get(i).first, translatedQueryStrings[i].trim().replaceAll("- ", "")));
                    }

                } else {
                    Log.e("ApiCallManager", "API response invalid: number of queries mismatch");
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
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return translatedQueries.isEmpty() ? null : translatedQueries;
    }

    private String selectBestQuery(String datafieldUserDescription) {
        // first, we filter out queries that cannot actually retrieve the target data
        List<Pair<OntologyQuery, Double>> correctQueries = new ArrayList<>();   // those that can actually retrieve the data
        for (Pair<OntologyQuery, Double> query : defaultQueries) {
            Set<SugiliteEntity> result = query.first.executeOn(uiSnapshot);
            if (result.contains(targetEntity)) {
                correctQueries.add(query);
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
            String promptPrefix = "We defined a Graph Query to locate target data on mobile UI structure. It describes the unique attributes of the data we are targeting.\n" +
                    "For example, the query \n(conj (HAS_CLASS_NAME android.widget.FrameLayout) (RIGHT (conj (hasText \" 6\") (HAS_CLASS_NAME android.widget.TextView) (HAS_PACKAGE_NAME com.ubercab)) ) (HAS_PACKAGE_NAME com.ubercab)) \nstands for: a FrameLayout within Uber, that is located to the right of a TextView in Uber that displays text \"6\"\n." +
                    "\n" +
                    "Here are the candidate queries:\n";
            String promptSuffix = "\nYour job is to find the best matching query to the user's input, and the best query that is generalizable to different data collection scenarios. For example, a screen location of an UI element is less generalizable than having a reference point like \"to the right of a text label that says '6'\". Which one matches the following description the best? \n [insert-user-description] \n Please only return the index number, without any additional information, even periods or comma.\n Example response format: \n0";

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
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
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

    // NOTE: the above is deprecated code when we asked the user to describe the data, then ask an LLM to match that to the best query.
    // we now use a different approach, where we ask the user to select the best query from a list of candidate queries
//    @Override
//    public void onPickBestQuery(String datafieldDescription) {
//        if (datafieldDescription.trim().isEmpty()) {
//            Toast.makeText(context, "Datafield description cannot be blank!", Toast.LENGTH_SHORT).show();
//        } else {
//
//            // deal with the Views
//            if (dimView != null) {
//                windowManager.removeView(dimView);
//            }
//            // select the correct query that can retrieve our result, using LLM
//            final String data = selectBestQuery(datafieldDescription);
//
//            // send the data to MainActivity
//            desiredQuery = data;
//            SugiliteEntity<Node> finalTargetEntity = targetEntity;
//            processCallback(targetEntity.getEntityValue().getEntityContent());
//            // clear the overlay
//            disableOverlay();
//            // stop widget service
//            Intent intent = new Intent(context, WidgetService.class);
//            context.stopService(intent);
//            // go back to the main activity
//            Intent mainActivityIntent = context.getPackageManager().getLaunchIntentForPackage("edu.nd.crepe");
//            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            if (mainActivityIntent != null) {
//                context.startActivity(mainActivityIntent);
//            } else {
//                Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
//            }
//
//
//        }
//    }

    @Override
    public void onPickBestQuery(String bestQuery) {
        if (bestQuery == null || bestQuery.isEmpty()) {
            Toast.makeText(context, "Something wrong. Please try again", Toast.LENGTH_SHORT).show();
            Log.e("FullScreenOverlayManager",
                    "onPickBestQuery: bestQuery is null");
        } else {
            // deal with the Views
            if (dimView != null) {
                windowManager.removeView(dimView);
            }
            // send the data to MainActivity
            desiredQuery = bestQuery;
            SugiliteEntity<Node> finalTargetEntity = targetEntity;
            processCallback(targetEntity.getEntityValue().getEntityContent());
            // clear the overlay
            disableOverlay();
            // stop widget service
            Intent intent = new Intent(context, WidgetService.class);
            context.stopService(intent);
            // go back to the main activity
            Intent mainActivityIntent = context.getPackageManager().getLaunchIntentForPackage("edu.nd.crepe");
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (mainActivityIntent != null) {
                context.startActivity(mainActivityIntent);
            } else {
                Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
            }


        }
    }



    private void processCallback(String targetText) {
        this.graphQueryCallback.onDataReceived(desiredQuery, targetText);
    }


    // below were the original implementation of datadescriptioncallback, when we asked the user to describe the data,
    // then ask an LLM to match that to the best query. instead, now we use the above implementation, where we ask the user
    // to select the best query from a list of candidate queries
//    @Override
//    public void onProcessDescriptionEditText(String datafieldDescription) {
//        if (datafieldDescription.trim().isEmpty()) {
//            Toast.makeText(context, "Datafield description cannot be blank!", Toast.LENGTH_SHORT).show();
//        } else {
//
//            // deal with the Views
//            if (dimView != null) {
//                windowManager.removeView(dimView);
//            }
//            // select the correct query that can retrieve our result, using LLM
//            final String data = selectBestQuery(datafieldDescription);
//
//            // send the data to MainActivity
//            desiredQuery = data;
//            SugiliteEntity<Node> finalTargetEntity = targetEntity;
//            processCallback(targetEntity.getEntityValue().getEntityContent());
//            // clear the overlay
//            disableOverlay();
//            // stop widget service
//            Intent intent = new Intent(context, WidgetService.class);
//            context.stopService(intent);
//            // go back to the main activity
//            Intent mainActivityIntent = context.getPackageManager().getLaunchIntentForPackage("edu.nd.crepe");
//            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            if (mainActivityIntent != null) {
//                context.startActivity(mainActivityIntent);
//            } else {
//                Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
//            }
//
//
//        }
//    }
//
//
//
//    private void processCallback(String targetText) {
//        this.graphQueryCallback.onDataReceived(desiredQuery, targetText);
//    }
}
