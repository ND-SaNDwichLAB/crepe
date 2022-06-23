package com.example.crepe.graphquery;

import static com.example.crepe.graphquery.Const.OVERLAY_TYPE;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.widget.Toast;

//import com.example.crepe.graphquery.ontology.CombinedOntologyQuery;
//import com.example.crepe.graphquery.ontology.LeafOntologyQuery;
//import com.example.crepe.graphquery.ontology.OntologyQuery;
import com.example.crepe.CrepeAccessibilityService;
import com.example.crepe.graphquery.recording.FullScreenOverlayManager;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author toby
 * @date 1/7/19
 * @time 2:44 PM
 */
public class DemonstrationUtil {
    /**
     * initiate a demonstration recording -> need to call endRecording() when the recording ends
     * @param context
//     * @param serviceStatusManager
//     * @param sharedPreferences
//     * @param scriptName
//     * @param sugiliteData
//     * @param afterRecordingCallback
     * @param fullScreenOverlayManager
     */
//    public static void initiateDemonstration(Context context, ServiceStatusManager serviceStatusManager, SharedPreferences sharedPreferences, String scriptName, SugiliteData sugiliteData, Runnable afterRecordingCallback, FullScreenOverlayManager fullScreenOverlayManager){
    public static void initiateDemonstration(Context context, FullScreenOverlayManager fullScreenOverlayManager){

            //start demonstration
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putString("scriptName", scriptName);
//            editor.putBoolean("recording_in_process", true);
//            editor.commit();

            //
            //TODO 1 Yuwen: construct script
//            sugiliteData.initiateScriptRecording(DemonstrationUtil.addScriptExtension(scriptName), afterRecordingCallback); //add the end recording callback
//            sugiliteData.initiatedExternally = false;

            //save the newly created script to DB
            // TODO 2 Yuwen: Use our own script to save this to db
//            try {
//                sugiliteScriptDao.save(sugiliteData.getScriptHead());
//                sugiliteScriptDao.commitSave(null);
//            }
//            catch (Exception e){
//                e.printStackTrace();

            //turn on the cat overlay to prepare for demonstration
            if(fullScreenOverlayManager != null){
                // if it's not currently showing the overlay
                if(!fullScreenOverlayManager.getShowingOverlay()) {
                    fullScreenOverlayManager.enableOverlay();
                } else {
                    fullScreenOverlayManager.disableOverlay();
                }

            }


//        }
    }
//
//    /**
//     * execute a script  --> check the service status and the variable values before doing so
//     * @param activityContext
//     * @param serviceStatusManager
//     * @param script
//     * @param sugiliteData
//     * @param sharedPreferences
//     * @param dialogManager
//     */
//    public static void executeScript(Activity activityContext, ServiceStatusManager serviceStatusManager, SugiliteStartingBlock script, SugiliteData sugiliteData, SharedPreferences sharedPreferences, boolean isForReconstructing, @Nullable PumiceDialogManager dialogManager, @Nullable SugiliteBlock afterExecutionOperation, @Nullable Runnable afterExecutionRunnable){
//        if(!serviceStatusManager.isRunning()){
//            //prompt the user if the accessiblity service is not active
//            activityContext.runOnUiThread(() -> {
//                AlertDialog.Builder builder1 = new AlertDialog.Builder(activityContext);
//                builder1.setTitle("Service not running")
//                        .setMessage("The Sugilite accessiblity service is not enabled. Please enable the service in the phone settings before recording.")
//                        .setPositiveButton("OK", (dialog, which) -> {
//                            serviceStatusManager.promptEnabling();
//                            //do nothing
//                        }).show();
//            });
//        }
//        else {
//            //check if pumice dialog manager is available, create a new one if needed
//            if (dialogManager == null) {
//                if (sugiliteData.pumiceDialogManager != null) {
//                    dialogManager = sugiliteData.pumiceDialogManager;
//                } else {
//                    dialogManager = new PumiceDialogManager(activityContext, true);
//                    SugiliteVoiceRecognitionListener sugiliteVoiceRecognitionListener = null;
//                    TextToSpeech tts = sugiliteData.getTTS();
//                    if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.ANDROID) {
//                        sugiliteVoiceRecognitionListener = new SugiliteAndroidAPIVoiceRecognitionListener(activityContext, null, tts);
//                    } else if (Const.SELECTED_SPEECH_RECOGNITION_TYPE == Const.SpeechRecognitionType.GOOGLE_CLOUD) {
//                        sugiliteVoiceRecognitionListener = new SugiliteGoogleCloudVoiceRecognitionListener(activityContext, sugiliteData,  null, tts);
//                    }
//                    dialogManager.setSugiliteVoiceRecognitionListener(sugiliteVoiceRecognitionListener);
//                    sugiliteData.pumiceDialogManager = dialogManager;
//                }
//            }
//
//            final PumiceDialogManager finalDialogManager = dialogManager;
//
//            activityContext.runOnUiThread(() -> {
//                VariableSetValueDialog variableSetValueDialog = new VariableSetValueDialog(sugiliteData.getApplicationContext(), sugiliteData, script, sharedPreferences, SugiliteData.EXECUTION_STATE, finalDialogManager, isForReconstructing);
//                if(script.variableNameDefaultValueMap.size() > 0) {
//
//                    //has variable
//                    sugiliteData.variableNameVariableValueMap.putAll(script.variableNameDefaultValueMap);
//                    boolean needUserInput = false;
//
//                    //check if any of the variables needs user input
//                    for(Map.Entry<String, Variable> entry : script.variableNameVariableObjectMap.entrySet()){
//                        if(entry.getValue().getVariableType() == Variable.USER_INPUT){
//                            needUserInput = true;
//                            break;
//                        }
//                    }
//                    if(needUserInput) {
//                        //show the dialog to obtain user input
//                        variableSetValueDialog.show();
//                    }
//                    else {
//                        variableSetValueDialog.executeScript(afterExecutionOperation, finalDialogManager, afterExecutionRunnable);
//                    }
//                }
//                else{
//                    //execute the script without showing the dialog
//                    variableSetValueDialog.executeScript(afterExecutionOperation, finalDialogManager, afterExecutionRunnable);
//                }
//            });
//        }
//    }
//
//
//


//    /**
//     * end the current recording
//     * @param context
//     * @param sugiliteData
//     * @param sharedPreferences
//     * @param sugiliteScriptDao
//     */
//    public static void endRecording(Context context, SugiliteData sugiliteData, SharedPreferences sharedPreferences, SugiliteScriptDao sugiliteScriptDao) {
//        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
//        SugiliteBlockJSONProcessor jsonProcessor = new SugiliteBlockJSONProcessor(context);
//
//
//        //end recording
//        prefEditor.putBoolean("recording_in_process", false);
//        prefEditor.apply();
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //commit the script through the sugiliteScriptDao
//                try {
//                    if (sugiliteScriptDao != null) {
//                        if (sugiliteData.getScriptHead() != null) {
//                            if (sugiliteData.verbalInstructionIconManager != null) {
//                                SugiliteScreenshotManager sugiliteScreenshotManager = SugiliteScreenshotManager.getInstance(sharedPreferences, sugiliteData);
//                                UISnapshot latestUISnapshot = sugiliteData.verbalInstructionIconManager.getLatestUISnapshot();
//                                if (latestUISnapshot != null) {
//                                    latestUISnapshot.annotateStringEntitiesIfNeeded();
//                                }
//                                sugiliteData.getScriptHead().uiSnapshotOnEnd = new SerializableUISnapshot(latestUISnapshot);
//                                sugiliteData.getScriptHead().screenshotOnEnd = sugiliteScreenshotManager.takeScreenshot(SugiliteScreenshotManager.DIRECTORY_PATH, sugiliteScreenshotManager.getFileNameFromDate());
//                            }
//                            sugiliteScriptDao.save(sugiliteData.getScriptHead());
//                        }
//                        sugiliteScriptDao.commitSave(new Runnable() {
//                            @Override
//                            public void run() {
//                                //invoke the callback
//                                if (sugiliteData.initiatedExternally && sugiliteData.getScriptHead() != null) {
//                                    //return the recording to the external caller
//                                    sugiliteData.communicationController.sendRecordingFinishedSignal(sugiliteData.getScriptHead().getScriptName());
//                                    sugiliteData.sendCallbackMsg(SugiliteCommunicationHelper.FINISHED_RECORDING, jsonProcessor.scriptToJson(sugiliteData.getScriptHead()), sugiliteData.callbackString);
//                                }
//
//                                //call the after recording callback
//                                if (sugiliteData.getScriptHead() != null && sugiliteData.afterRecordingCallback != null){
//                                    //call the endRecordingCallback
//                                    Runnable r = sugiliteData.afterRecordingCallback;
//                                    sugiliteData.afterRecordingCallback = null;
//                                    r.run();
//                                }
//                            }
//                        });
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//
//
//
//
//
//        //turn off the recording overlay if any
//        // TODO: Use FullScreenRecordingOverlayManager here instead -- yuwen
////        if(sugiliteData.verbalInstructionIconManager != null){
////            sugiliteData.verbalInstructionIconManager.turnOffCatOverlay();
////        }
//
//
//        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
//        PumiceDemonstrationUtil.showSugiliteToast("end recording", Toast.LENGTH_SHORT);
//    }

//    public static void showSugiliteToast(String text, int length) {
//        SugiliteData.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(SugiliteData.getAppContext(), text, length).show();
//
//            }
//        });
//    }
//
//    public static void showSugiliteAlertDialog(String content) {
//        SugiliteData.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                AlertDialog dialog = new AlertDialog.Builder(SugiliteData.getAppContext()).setMessage(content).setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                }).create();
//                dialog.getWindow().setType(OVERLAY_TYPE);
//                dialog.setCanceledOnTouchOutside(false);
//                dialog.show();
//            }
//        });
//    }

    public static String removeScriptExtension (String scriptName) {
        if (scriptName.endsWith(".SugiliteScript")) {
            return scriptName.replace(".SugiliteScript", "");
        } else {
            return scriptName;
        }
    }

    public static String addScriptExtension (String scriptName) {
        if (scriptName.endsWith(".SugiliteScript")) {
            return scriptName;
        } else {
            return scriptName + ".SugiliteScript";
        }
    }

    public static String joinListGrammatically(final List<String> list, String lastWordSeparator) {
        if (list.size() == 0) {
            return "";
        }
        return list.size() > 1
                ? StringUtils.join(list.subList(0, list.size() - 1), ", ")
                .concat(String.format("%s %s ", list.size() > 2 ? "," : "", lastWordSeparator))
                .concat(list.get(list.size() - 1))
                : list.get(0);
    }

    public static String boldify(String string){
        return "<b>" + string + "</b>";
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }
    public static boolean isInputMethodPackageName (String packageName) {
        Set<String> inputMethodNames = new HashSet<>(Arrays.asList(Const.INPUT_METHOD_PACKAGE_NAMES));
        return inputMethodNames.contains(packageName);
    }

//    public static Drawable getScaledDrawable(Drawable drawable, double scale) {
//        return getScaledDrawable(drawable, (int) (scale * drawable.getIntrinsicWidth()), (int) (scale * drawable.getIntrinsicHeight()));
//    }

//    public static Drawable getScaledDrawable(Drawable drawable, int width, int height) {
//        //scale the drawable so it fits into the dialog
//        // Read your drawable from somewhere
//        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
//        // Scale it to 50 x 50
//        Drawable d = new BitmapDrawable(SugiliteData.getAppContext().getResources(), Bitmap.createScaledBitmap(bitmap, width, height, true));
//        // Set your new, scaled drawable "d"
//        return d;
//    }

//    public static boolean checkIfOntologyQueryContains (OntologyQuery ontologyQuery, SugiliteRelation relation, Object value) {
//        if (ontologyQuery instanceof LeafOntologyQuery) {
//            if (((LeafOntologyQuery) ontologyQuery).getR().equals(relation)) {
//                Set<SugiliteSerializableEntity> objectEntities = ((LeafOntologyQuery) ontologyQuery).getObject();
//                if (objectEntities != null) {
//                    for (SugiliteSerializableEntity entity : objectEntities) {
//                        if (value.equals(entity.getEntityValue())) {
//                            return true;
//                        }
//                    }
//                }
//            }
//        }
//        if (ontologyQuery instanceof CombinedOntologyQuery) {
//            Set<OntologyQuery> subQueries = ((CombinedOntologyQuery) ontologyQuery).getSubQueries();
//            if (subQueries != null) {
//                for (OntologyQuery subQuery : subQueries) {
//                    boolean subQueryContains = checkIfOntologyQueryContains(subQuery, relation, value);
//                    if (subQueryContains) {
//                        return true;
//                    }
//                }
//            }
//        }
//
//        return false;
//    }
}