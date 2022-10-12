package com.example.crepe.graphquery.recording;

import static android.content.Context.WINDOW_SERVICE;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.example.crepe.graphquery.Const.OVERLAY_TYPE;
import static com.example.crepe.graphquery.DemonstrationUtil.findClosestSiblingNode;
import static com.example.crepe.graphquery.DemonstrationUtil.generateDefaultQueries;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import com.example.crepe.CrepeAccessibilityService;
import com.example.crepe.demonstration.WidgetDisplay;
import com.example.crepe.graphquery.Const;
import com.example.crepe.graphquery.model.Node;
import com.example.crepe.graphquery.ontology.OntologyQuery;
import com.example.crepe.graphquery.ontology.SugiliteEntity;
import com.example.crepe.graphquery.ontology.SugiliteRelation;
import com.example.crepe.graphquery.ontology.UISnapshot;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FullScreenOverlayManager {

    private Context context;
    private WindowManager windowManager;
    private View overlay;
    private DisplayMetrics displayMetrics;
    private Boolean showingOverlay;
    private NavigationBarUtil navigationBarUtil;
    private int overlayCurrentHeight;
    private int overlayCurrentWidth;
    private int overlayCurrentFlag;
    private SelectionOverlayViewManager selectionOverlayViewManager;

    private int entityId = 0;

    public FullScreenOverlayManager(Context context, WindowManager windowManager, DisplayMetrics displayMetrics) {
        this.context = context;
        this.windowManager = windowManager;
        this.displayMetrics = displayMetrics;
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

                    windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
                    // if we need to use the following code block to show clicked spot on screen, remember to refresh the overlay and widget views so we can continue to click
//                    WindowManager.LayoutParams selectionLayoutParams = updateLayoutParams(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
//                    SelectionOverlayView selectionOverlay = selectionOverlayViewManager.getCircleOverlay(rawX, adjustedY, radius);
//                    windowManager.addView(selectionOverlay, selectionLayoutParams);


                    // create uiSnapshot for current screen
                    UISnapshot uiSnapshot = CrepeAccessibilityService.getsSharedInstance().generateUISnapshot();
                    // get the matched node

                    AccessibilityNodeInfo matchedAccessibilityNode = CrepeAccessibilityService.getsSharedInstance().getMatchingNodeFromClick(rawX, rawY);
                    // this matchedAccessibilityNode is an AccessibilityNodeInfo, which is not exactly the node stored in the screen's nodeSugiliteEntityMap.
                    // We retrieve that stored node from this screen's uisnapshot

                    SugiliteEntity<Node> targetEntity = uiSnapshot.getEntityWithAccessibilityNode(matchedAccessibilityNode);

                    if(targetEntity != null) {
                        SugiliteRelation[] relationsToExclude = new SugiliteRelation[0];
                        List<Pair<OntologyQuery, Double>> defaultQueries = generateDefaultQueries(uiSnapshot, targetEntity, relationsToExclude);
                    } else {
                        Log.e("generate queries", "Cannot find the tapped entity!");
                    }
//                    for(Pair<OntologyQuery, Double> query : defaultQueries) {
//                        Set<SugiliteEntity> results = query.first.executeOn(uiSnapshot);
//                    }

                    // get the matched node text
                    if(matchedAccessibilityNode != null) {
                        String matchedNodeText = String.valueOf(matchedAccessibilityNode.getText());
                        if(!matchedNodeText.isEmpty())
                        {
                            AccessibilityNodeInfo closestSiblingNode = findClosestSiblingNode(matchedAccessibilityNode);
                            if(closestSiblingNode.getText() != null) Log.d("graphquery", "Sibling Text: " + closestSiblingNode.getText().toString());
                        } else {
                            Log.d("uisnapshot", "Sorry we do not support the collection of such information. Cannot find the matching node for your click.");
                        }
                    }

                    // TODO yuwen: store the query in database, then constantly check it in another thread

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


}
