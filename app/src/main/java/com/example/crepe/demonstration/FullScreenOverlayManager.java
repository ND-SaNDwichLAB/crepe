package com.example.crepe.demonstration;

import static android.content.Context.WINDOW_SERVICE;
import static com.example.crepe.demonstration.DemonstrationUtil.processOverlayClick;
import static com.example.crepe.graphquery.Const.OVERLAY_TYPE;
import static com.example.crepe.demonstration.DemonstrationUtil.generateDefaultQueries;
import static com.example.crepe.graphquery.Const.appName;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
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

import androidx.annotation.RequiresApi;

import com.example.crepe.CrepeAccessibilityService;
import com.example.crepe.R;
import com.example.crepe.graphquery.Const;
import com.example.crepe.graphquery.model.Node;
import com.example.crepe.graphquery.ontology.OntologyQuery;
import com.example.crepe.graphquery.ontology.SugiliteEntity;
import com.example.crepe.graphquery.ontology.SugiliteRelation;
import com.example.crepe.graphquery.ontology.UISnapshot;
import com.example.crepe.ui.dialog.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FullScreenOverlayManager {

    private Context context;
    private WindowManager windowManager;
    private View overlay;
    private DisplayMetrics displayMetrics;
    private Callback callback;
    private Boolean showingOverlay;
    private NavigationBarUtil navigationBarUtil;
    private int overlayCurrentHeight;
    private int overlayCurrentWidth;
    private int overlayCurrentFlag;
    private SelectionOverlayViewManager selectionOverlayViewManager;

    private int entityId = 0;

    private String desiredQuery = "";

    public FullScreenOverlayManager(Context context, WindowManager windowManager, DisplayMetrics displayMetrics, Callback callback) {
        this.context = context;
        this.windowManager = windowManager;
        this.displayMetrics = displayMetrics;
        this.callback = callback;
        this.overlay = getRectangleOverlay(context, displayMetrics.widthPixels, displayMetrics.heightPixels, Const.RECORDING_OVERLAY_COLOR);
        this.showingOverlay = false;
        this.navigationBarUtil = new NavigationBarUtil();
        this.overlayCurrentHeight = displayMetrics.heightPixels;
        //hack -- leave 1px at the right end of the screen so the input method window becomes visible
        this.overlayCurrentWidth = displayMetrics.widthPixels - 1;
        this.overlayCurrentFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        this.selectionOverlayViewManager = new SelectionOverlayViewManager(context);
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
        windowManager.removeView(overlay);
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
                OVERLAY_TYPE,
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
                OVERLAY_TYPE,
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
                /* if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                /* request permission via start activity for result */
                context.startActivity(intent);

            }
        }
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
                    //single tap up detected
                    System.out.println("Single tap detected");

                    float rawX = event.getRawX();
                    float rawY = event.getRawY();
                    float radius = 10;

                    // account for the navigation bar height
                    float navHeight = navigationBarUtil.getStatusBarHeight(context);
                    float adjustedY = rawY - navHeight;

                    // show the matched item on screen
                    windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
                    WindowManager.LayoutParams selectionLayoutParams = updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
                    Rect clickedItemBounds = DemonstrationUtil.getBoundingBoxOfClickedItem(rawX, rawY);
                    // move the clickedItemBounds up by the navHeight
                    if (clickedItemBounds != null) {
                        clickedItemBounds.offset(0, -1 * (int) navHeight);
                        this.selectionOverlay = selectionOverlayViewManager.getRectOverlay(clickedItemBounds);
                        windowManager.addView(this.selectionOverlay, selectionLayoutParams);
                    }

                    List<Pair<OntologyQuery, Double>> defaultQueries = processOverlayClick(rawX, rawY);

                    // get the current uisnapshot
                    UISnapshot uiSnapshot = CrepeAccessibilityService.getsSharedInstance().generateUISnapshot();

                    List<AccessibilityNodeInfo> matchedAccessibilityNodeList = CrepeAccessibilityService.getsSharedInstance().getMatchingNodeFromClickWithText(rawX, rawY);
                    // this matchedAccessibilityNode is an AccessibilityNodeInfo, which is not exactly the node stored in the screen's nodeSugiliteEntityMap.
                    // We retrieved that stored node from this screen's uisnapshot

                    SugiliteEntity<Node> targetEntity = new SugiliteEntity<>();
                    AccessibilityNodeInfo matchedNode;

                    if (matchedAccessibilityNodeList != null) {
                        if(matchedAccessibilityNodeList.size() == 1) {
                            matchedNode = matchedAccessibilityNodeList.get(0);
                            targetEntity = uiSnapshot.getEntityWithAccessibilityNode(matchedNode);
                        } else {
                            // TODO: Find the node that we actually need
                            matchedNode = matchedAccessibilityNodeList.get(0);
                            targetEntity = uiSnapshot.getEntityWithAccessibilityNode(matchedNode);
                        }
                    }


                    if(targetEntity != null) {
                        SugiliteRelation[] relationsToExclude = new SugiliteRelation[1];
                        relationsToExclude[0] = SugiliteRelation.HAS_TEXT;
                        defaultQueries = generateDefaultQueries(uiSnapshot, targetEntity, relationsToExclude);
                    } else {
                        Log.e("generate queries", "Cannot find the tapped entity!");
                        return false;
                    }

                    if (targetEntity.getEntityValue() == null || defaultQueries.size() == 0) {
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
                            OVERLAY_TYPE,
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                            PixelFormat.TRANSLUCENT);
                    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View confirmationView = layoutInflater.inflate(R.layout.demonstration_confirmation, null);

                    // Set the elevation of the view
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        confirmationView.setElevation(10.0f); // Set the elevation to 10dp
                    }
                    TextView queryTextView = confirmationView.findViewById(R.id.confirmationInfo);
                    // set the text of the dialog window
                    String displayText = "";

                    displayText = "You clicked on \"" + targetEntity.getEntityValue().getText() + "\". Do you want to collect this data?";
                    queryTextView.setText(displayText);
                    windowManager.addView(confirmationView, dialogParams);



                    // set the onclick listener for the buttons
                    Button yesButton = confirmationView.findViewById(R.id.confirmationYesButton);
                    Button noButton = confirmationView.findViewById(R.id.confirmationNoButton);

                    // select the correct query that can retrieve our data, and can retrieve least other unrelated data
                    final String data = selectBestQuery(defaultQueries, targetEntity, uiSnapshot);

                    SugiliteEntity<Node> finalTargetEntity = targetEntity;
                    yesButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //confirm the selection
                            // remove the confirmation dialog
                            if (confirmationView != null) {
                                windowManager.removeView(confirmationView);
                            }
                            // remove the selection overlay
                            if (selectionOverlay != null) {
                                windowManager.removeView(selectionOverlay);
                            }

                            // set the data to the main activity
                            desiredQuery = data;
                            processCallback(finalTargetEntity.getEntityValue().getText());
                            // clear the overlay
                            disableOverlay();
                            // stop widget service
                            Intent intent = new Intent(context, WidgetService.class);
                            context.stopService(intent);
                            // go back to the main activity
                            Intent mainActivityIntent = context.getPackageManager().getLaunchIntentForPackage("com.example.crepe");
                            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            if (mainActivityIntent != null) {
                                context.startActivity(mainActivityIntent);
                            } else {
                                Toast.makeText(context, "There is no package available in android", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    noButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // remove the confirmation dialog
                            if(confirmationView != null) {
                                windowManager.removeView(confirmationView);
                            }
                            // remove the selection overlay
                            if(selectionOverlay != null) {
                                windowManager.removeView(selectionOverlay);
                            }
                            Toast.makeText(context, "Please click on the data to collect again", Toast.LENGTH_SHORT).show();
                        }
                    });
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

    private String selectBestQuery(List<Pair<OntologyQuery, Double>> defaultQueries, SugiliteEntity<Node> targetEntity, UISnapshot currentUISnapshot) {
        // first, we filter out queries that cannot actually retrieve the targe data
        List<Pair<OntologyQuery, Double>> correctQueries = new ArrayList<>();   // those that can actually retrieve the data
        for (Pair<OntologyQuery, Double> query : defaultQueries) {
            Set<SugiliteEntity> result = query.first.executeOn(currentUISnapshot);
            if (result.contains(targetEntity)) {
                correctQueries.add(query);
            }
        }

        Log.i("graphquery", "correctQueries.size() = " + correctQueries.size());

        // second, we select the query that can retrieve the least unrelated data, then rank by the heuristics score
        if (correctQueries.size() == 0) {
            return null;
        }
        else if (correctQueries.size() == 1) {
            return correctQueries.get(0).first.toString();
        }
        else {
            // sort the queries by the heuristics score
            Collections.sort(correctQueries, new Comparator<Pair<OntologyQuery, Double>>() {
                @Override
                public int compare(Pair<OntologyQuery, Double> o1, Pair<OntologyQuery, Double> o2) {
                    return o2.second.compareTo(o1.second);
                }
            });
            // select the query that can retrieve the least unrelated data
            Pair<OntologyQuery, Double> bestQuery = correctQueries.get(0);
            int minSize = bestQuery.first.executeOn(currentUISnapshot).size();
            for (int i = 1; i < correctQueries.size(); i++) {
                int size = correctQueries.get(i).first.executeOn(currentUISnapshot).size();
                if (size < minSize && size >= 1) {
                    minSize = size;
                    bestQuery = correctQueries.get(i);
                }

            }
            return bestQuery.first.toString();
        }

    }

    private void processCallback(String targetText) {
        this.callback.onDataReceived(desiredQuery, targetText);
    }


}
