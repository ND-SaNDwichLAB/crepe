
package com.example.crepe.demonstration;

/*
@Deprecated
public class CrepeIconManager {
    private ImageView statusIcon;
    private Context context;
    private WindowManager windowManager;
    private WindowManager.LayoutParams iconParams, textViewParams;
    private Dialog crepeDialog = null;
    private AccessibilityManager accessibilityManager;
    public static final int OVERLAY_TYPE = (Build.VERSION.SDK_INT >= 26) ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

    //previous x, y coordinates before the icon is removed
    Integer prev_x = null;
    Integer prev_y = null;

    public CrepeIconManager(Context context, SharedPreferences sharedPreferences, AccessibilityManager accessibilityManager){
        this.context = context;
        windowManager = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        this.accessibilityManager = accessibilityManager;

    }

    /**
     * add the status icon using the context specified in the class
     */
/*
    public void addStatusIcon(){
        if (statusIcon == null) {
            statusIcon = new ImageView(context);
        }
        statusIcon.setImageResource(R.mipmap.ic_launcher);
        iconParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);


        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = prev_x == null ? displaymetrics.widthPixels : prev_x;
        iconParams.y = prev_y == null ? 200 : prev_y;
        addCrumpledPaperOnTouchListener(statusIcon, iconParams, displaymetrics, windowManager);


        textViewParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        textViewParams.gravity = Gravity.BOTTOM | Gravity.CENTER;

        //NEEDED TO BE CONFIGURED AT APPS->SETTINGS-DRAW OVER OTHER APPS on API>=23
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        if(currentApiVersion >= 23) {
            checkDrawOverlayPermission();
            if (Settings.canDrawOverlays(context)) {
                windowManager.addView(statusIcon, iconParams);
                //=== temporarily set the status view to invisible  ===
                windowManager.addView(View.,textViewParams);
                statusView.setVisibility(View.INVISIBLE);
            } else {
                windowManager.addView(statusIcon, iconParams);
            }
        }
        else {
            windowManager.addView(statusIcon, iconParams);

            //=== temporarily set the status view to invisible ===
            windowManager.addView(statusView, textViewParams);
            statusView.setVisibility(View.INVISIBLE);
        }

    }

    private void addCrumpledPaperOnTouchListener(final View view, final WindowManager.LayoutParams mPaperParams, DisplayMetrics displayMetrics, final WindowManager windowManager) {
        final int windowWidth = displayMetrics.widthPixels;
        view.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            GestureDetector gestureDetector = new GestureDetector(context, new SingleTapUp());

            @Override
            public boolean onTouch(final View v, MotionEvent event) {
                v.performClick();
                if (gestureDetector.onTouchEvent(event)) {
                    // gesture is clicking -> pop up the on-click menu
                    AlertDialog.Builder textDialogBuilder = new AlertDialog.Builder(context);
                    final boolean recordingInProcess = sharedPreferences.getBoolean("recording_in_process", false);
                    final SugiliteStartingBlock startingBlock = (SugiliteStartingBlock) sugiliteData.getScriptHead();
                    String scriptName = (startingBlock == null ? "" : startingBlock.getScriptName());
                    final String scriptDefinedName = PumiceDemonstrationUtil.removeScriptExtension(scriptName);
                    //set pop up title
                    if(recordingInProcess){
                        textDialogBuilder.setTitle("RECORDING: " + scriptDefinedName);

                        if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE){
                            textDialogBuilder.setTitle("PAUSED FOR A BREAKPOINT");
                        }
                    }
                    else if (sugiliteData.getScriptHead() != null){
                        boolean hasLast = false;
                        try{
                            List<String> allNames = sugiliteScriptDao.getAllNames();
                            hasLast = allNames.contains(scriptName);
                        }
                        catch(Exception e){
                            e.printStackTrace();
                        }
                        if(hasLast) {
                            textDialogBuilder.setTitle("NOT RECORDING\nLAST RECORDED: " + scriptDefinedName);
                        }
                        else{
                            textDialogBuilder.setTitle("NOT RECORDING\n");
                        }
                    }

                    else {
                        textDialogBuilder.setTitle("NOT RECORDING");
                    }

                    boolean recordingInProgress = sharedPreferences.getBoolean("recording_in_process", false);
                    final boolean runningInProgress = sugiliteData.getInstructionQueueSize() > 0;



                    //pause the execution when the duck is clicked
                    storedQueue = runningInProgress ? sugiliteData.getCopyOfInstructionQueue() : null;
                    final int previousState = sugiliteData.getCurrentSystemState();
                    if(runningInProgress) {
                        sugiliteData.clearInstructionQueue();
                        if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE
                                || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_ERROR_HANDLING_STATE
                                || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_CRUCIAL_STEP_STATE){
                            //TODO: change the icon based on the current status
                        }
                        else {
                            if(previousState == SugiliteData.REGULAR_DEBUG_STATE)
                                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE);
                            else
                                sugiliteData.setCurrentSystemState(SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE);
                        }
                    }

                    //TODO: show different menu items for different state

                    List<String> operationList = new ArrayList<>();
                    if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_BREAKPOINT_STATE){
                        operationList.add("Resume Next Step");
                        operationList.add("Quit Debugging");
                    }
                    if(sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE
                            || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE) {
                        operationList.add("Resume Running");
                        operationList.add("Clear Instruction Queue");
                    }
                    operationList.add("View Script List");




                    if(startingBlock == null){
                        operationList.add("New Recording");
                    }
                    else{
                        if(recordingInProcess){
                            operationList.add("View Current Recording");
                            operationList.add("Add GO_HOME Operation Block");
                            operationList.add("Add Running a Subscript");
                            if(Const.KEEP_ALL_TEXT_LABEL_LIST)
                                operationList.add("Get a Text Element on the Screen");
                            operationList.add("Add a Delay");
                            operationList.add("End Recording");
                        }
                        else{
                            operationList.add("View Last Recording");
                            operationList.add("Resume Last Recording");
                            operationList.add("New Recording");
                        }
                    }

                    if(verbalInstructionIconManager != null) {
                        if(verbalInstructionIconManager.isShowingIcon()) {
                            operationList.add("Turn off verbal instruction");
                        }
                        else{
                            operationList.add("Turn on verbal instruction");
                        }
                    }
                    operationList.add("Hide Duck Icon");
                    operationList.add("Quit Sugilite");
                    String[] operations = new String[operationList.size()];
                    operations = operationList.toArray(operations);
                    final String[] operationClone = operations.clone();
                    final SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                    textDialogBuilder.setItems(operationClone, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (operationClone[which]) {
                                case "View Script List":
                                    Intent scriptListIntent = new Intent(context, SugiliteMainActivity.class);
                                    scriptListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    context.startActivity(scriptListIntent);
                                    PumiceDemonstrationUtil.showSugiliteToast("view script list", Toast.LENGTH_SHORT);
                                    if(runningInProgress)
                                        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    break;
                                //bring the user to the script list activity
                                case "View Last Recording":
                                case "View Current Recording":
                                    Intent intent = new Intent(context, LocalScriptDetailActivity.class);
                                    if(startingBlock != null && startingBlock.getScriptName() != null) {
                                        intent.putExtra("scriptName", startingBlock.getScriptName());
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        context.startActivity(intent);
                                    }
                                    PumiceDemonstrationUtil.showSugiliteToast("view current script", Toast.LENGTH_SHORT);
                                    if(runningInProgress)
                                        sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    break;
                                case "End Recording":
                                    //end recording
                                    PumiceDemonstrationUtil.endRecording(context, sugiliteData, sharedPreferences, sugiliteScriptDao);
                                    break;
                                case "New Recording":
                                    //create a new script
                                    NewScriptDialog newScriptDialog = new NewScriptDialog(v.getContext(), sugiliteScriptDao, serviceStatusManager, sharedPreferences, sugiliteData, true, null, null);
                                    newScriptDialog.show();
                                    break;
                                case "Resume Last Recording":
                                    //resume the recording of an existing script
                                    sugiliteData.initiatedExternally = false;
                                    SharedPreferences.Editor prefEditor2 = sharedPreferences.edit();
                                    prefEditor2.putBoolean("recording_in_process", true);
                                    prefEditor2.apply();
                                    PumiceDemonstrationUtil.showSugiliteToast("resume recording", Toast.LENGTH_SHORT);
                                    sugiliteData.setCurrentSystemState(SugiliteData.RECORDING_STATE);
                                    break;
                                case "Hide Duck Icon":
                                    //step: remove the duck and the status view
                                    removeStatusIcon();
                                    break;
                                case "Quit Sugilite":
                                    PumiceDemonstrationUtil.showSugiliteToast("quit sugilite", Toast.LENGTH_SHORT);


                                    //step 1: end recording if one is in progress
                                    if(recordingInProgress){
                                        //end recording
                                        prefEditor.putBoolean("recording_in_process", false);
                                        prefEditor.apply();
                                        new Thread(new Runnable() {
                                            @Override
                                            public void run()
                                            {
                                                try {
                                                    sugiliteScriptDao.commitSave(null);
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                }
                                            }
                                        }).start();
                                    }

                                    //step 2: clear instruction queue if there is one
                                    sugiliteData.clearInstructionQueue();
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    if(storedQueue != null)
                                        storedQueue.clear();

                                    //step 3: remove the duck and the status view
                                    removeStatusIcon();

                                    //step 4: kill Sugilite app
                                    Intent first_activity_intent = new Intent(context, SugiliteMainActivity.class);
                                    first_activity_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    first_activity_intent.putExtra("EXIT", true);
                                    context.startActivity(first_activity_intent);

                                    break;
                                case "Clear Instruction Queue":
                                    sugiliteData.clearInstructionQueue();
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    if(storedQueue != null)
                                        storedQueue.clear();
                                    break;
                                case "Resume Running":
                                    dialog.dismiss();
                                    break;
                                case "Add GO_HOME Operation Block":
                                    //insert a GO_HOME opertion block AND go home
                                    SugiliteOperationBlock operationBlock = new SugiliteOperationBlock();
                                    SugiliteOperation operation = new SugiliteSpecialOperation(SugiliteOperation.SPECIAL_GO_HOME);
                                    operationBlock.setOperation(operation);
                                    operationBlock.setDescription(descriptionGenerator.generateReadableDescription(operationBlock));
                                    try {
                                        SugiliteBlock currentBlock = sugiliteData.getCurrentScriptBlock();
                                        if(currentBlock == null || sugiliteData.getScriptHead() == null)
                                            throw new Exception("NULL CURRENT BLOCK OR NULL SCRIPT");
                                        operationBlock.setPreviousBlock(currentBlock);
                                        if (currentBlock instanceof SugiliteOperationBlock)
                                            ((SugiliteOperationBlock) currentBlock).setNextBlock(operationBlock);
                                        else if (currentBlock instanceof SugiliteStartingBlock)
                                            ((SugiliteStartingBlock) currentBlock).setNextBlock(operationBlock);
                                        else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                                            ((SugiliteSpecialOperationBlock) currentBlock).setNextBlock(operationBlock);
                                        else
                                            throw new Exception("UNSUPPORTED BLOCK TYPE");
                                        //TODO: deal with blocks other than operation block and starting block
                                        sugiliteData.setCurrentScriptBlock(operationBlock);
                                        sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                        //go to home
                                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                                        startMain.addCategory(Intent.CATEGORY_HOME);
                                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(startMain);
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    break;
                                case "Add Running a Subscript":
                                    final SugiliteSubscriptSpecialOperationBlock subscriptBlock = new SugiliteSubscriptSpecialOperationBlock();
                                    subscriptBlock.setDescription(descriptionGenerator.generateReadableDescription(subscriptBlock));
                                    List<String> subscriptNames = new ArrayList<String>();
                                    try {
                                        subscriptNames = sugiliteScriptDao.getAllNames();
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    AlertDialog.Builder chooseSubscriptDialogBuilder = new AlertDialog.Builder(context);
                                    String[] subscripts = new String[subscriptNames.size()];
                                    subscripts = subscriptNames.toArray(subscripts);
                                    final String[] subscriptClone = subscripts.clone();

                                    chooseSubscriptDialogBuilder.setItems(subscriptClone, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            String chosenScriptName = subscriptClone[which];
                                            //add a subscript operation block with the script name "chosenScriptName"
                                            subscriptBlock.setSubscriptName(chosenScriptName);
                                            SugiliteStartingBlock script = null;
                                            try {
                                                script = sugiliteScriptDao.read(chosenScriptName);
                                            }
                                            catch (Exception e){
                                                e.printStackTrace();
                                            }
                                            if(script != null) {
                                                try {
                                                    SugiliteBlock currentBlock = sugiliteData.getCurrentScriptBlock();
                                                    if(currentBlock == null || sugiliteData.getScriptHead() == null)
                                                        throw new Exception("NULL CURRENT BLOCK OR NULL SCRIPT");
                                                    subscriptBlock.setPreviousBlock(currentBlock);
                                                    if (currentBlock instanceof SugiliteOperationBlock)
                                                        ((SugiliteOperationBlock) currentBlock).setNextBlock(subscriptBlock);
                                                    else if (currentBlock instanceof SugiliteStartingBlock)
                                                        ((SugiliteStartingBlock) currentBlock).setNextBlock(subscriptBlock);
                                                    else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                                                        ((SugiliteSpecialOperationBlock) currentBlock).setNextBlock(subscriptBlock);
                                                    else
                                                        throw new Exception("UNSUPPORTED BLOCK TYPE");

                                                    //subscriptBlock.setDescription(descriptionGenerator.generateReadableDescription(subscriptBlock));
                                                    sugiliteData.setCurrentScriptBlock(subscriptBlock);
                                                    sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                }


                                                //run the script
                                                SharedPreferences.Editor prefEditor = sharedPreferences.edit();
                                                prefEditor.putBoolean("recording_in_process", false);
                                                prefEditor.apply();

                                                try {
                                                    subscriptBlock.run(context, sugiliteData, sugiliteScriptDao, sharedPreferences);
                                                }
                                                catch (Exception e){
                                                    e.printStackTrace();
                                                }
                                            }


                                        }
                                    });

                                    Dialog chooseSubscriptDialog = chooseSubscriptDialogBuilder.create();
                                    if(chooseSubscriptDialog.getWindow() != null) {
                                        chooseSubscriptDialog.getWindow().setType(OVERLAY_TYPE);
                                    }
                                    chooseSubscriptDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
                                    chooseSubscriptDialog.show();
                                    break;


                                case "Add a Delay":
                                    SugiliteDelaySpecialOperationBlock delaySpecialOperationBlock = new SugiliteDelaySpecialOperationBlock(10000);
                                    delaySpecialOperationBlock.setDescription(new SpannableString("Delay for 10s"));

                                    try {
                                        SugiliteBlock currentBlock = sugiliteData.getCurrentScriptBlock();
                                        if(currentBlock == null || sugiliteData.getScriptHead() == null)
                                            throw new Exception("NULL CURRENT BLOCK OR NULL SCRIPT");
                                        delaySpecialOperationBlock.setPreviousBlock(currentBlock);
                                        if (currentBlock instanceof SugiliteOperationBlock)
                                            ((SugiliteOperationBlock) currentBlock).setNextBlock(delaySpecialOperationBlock);
                                        else if (currentBlock instanceof SugiliteStartingBlock)
                                            ((SugiliteStartingBlock) currentBlock).setNextBlock(delaySpecialOperationBlock);
                                        else if (currentBlock instanceof SugiliteSpecialOperationBlock)
                                            ((SugiliteSpecialOperationBlock) currentBlock).setNextBlock(delaySpecialOperationBlock);
                                        else
                                            throw new Exception("UNSUPPORTED BLOCK TYPE");

                                        //subscriptBlock.setDescription(descriptionGenerator.generateReadableDescription(subscriptBlock));
                                        sugiliteData.setCurrentScriptBlock(delaySpecialOperationBlock);
                                        sugiliteScriptDao.save(sugiliteData.getScriptHead());
                                    }
                                    catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    break;


                                case "Get a Text Element on the Screen":
                                    SelectElementWithTextDialog selectElementWithTextDialog = new SelectElementWithTextDialog(context, sugiliteData);
                                    selectElementWithTextDialog.show();
                                    break;
                                case "Resume Next Step":
                                    if(sugiliteData.storedInstructionQueueForPause.peek() != null && sugiliteData.storedInstructionQueueForPause.peek() instanceof SugiliteOperationBlock)
                                        ((SugiliteOperationBlock) sugiliteData.storedInstructionQueueForPause.peek()).isSetAsABreakPoint = false;
                                    sugiliteData.addInstructions(sugiliteData.storedInstructionQueueForPause);
                                    sugiliteData.storedInstructionQueueForPause.clear();
                                    sugiliteData.setCurrentSystemState(SugiliteData.REGULAR_DEBUG_STATE);
                                    dialog.dismiss();
                                    break;
                                case "Quit Debugging":
                                    sugiliteData.storedInstructionQueueForPause.clear();
                                    sugiliteData.setCurrentSystemState(SugiliteData.DEFAULT_STATE);
                                    dialog.dismiss();
                                    break;

                                case "Turn on verbal instruction":
                                    if(verbalInstructionIconManager != null){
                                        verbalInstructionIconManager.addStatusIcon();
                                    }
                                    break;

                                case "Turn off verbal instruction":
                                    if(verbalInstructionIconManager != null){
                                        verbalInstructionIconManager.removeStatusIcon();
                                    }
                                    break;


                                default:
                                    //do nothing
                            }
                        }
                    });
                    if (duckDialog != null && duckDialog.isShowing()){
                        duckDialog.dismiss();
                    }
                    duckDialog = textDialogBuilder.create();
                    duckDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_REGULAR_EXECUTION_STATE
                                    || sugiliteData.getCurrentSystemState() == SugiliteData.PAUSED_FOR_DUCK_MENU_IN_DEBUG_MODE) {
                                //restore execution
                                sugiliteData.addInstructions(storedQueue);
                                sugiliteData.setCurrentSystemState(previousState);
                            }
                        }
                    });
                    if(duckDialog.getWindow() != null) {
                        duckDialog.getWindow().setType(OVERLAY_TYPE);
                        duckDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_box);
                    }
                    duckDialog.show();
                    return true;

                }
                //gesture is not clicking - handle the drag & move
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = mPaperParams.x;
                        initialY = mPaperParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // move paper ImageView
                        mPaperParams.x = initialX - (int) (initialTouchX - event.getRawX());
                        mPaperParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        prev_x = mPaperParams.x;
                        prev_y = mPaperParams.y;
                        windowManager.updateViewLayout(view, mPaperParams);
                        return true;
                }
                return false;
            }

            class SingleTapUp extends GestureDetector.SimpleOnGestureListener {

                @Override
                public boolean onSingleTapUp(MotionEvent event) {
                    return true;
                }
            }

        });
    }

    public void removeCrepeIcon(){
        try{
            if(statusIcon != null && statusIcon.getWindowToken() != null) {
                windowManager.removeView(statusIcon);
            }
            if(statusView != null && statusView.getWindowToken() != null) {
                windowManager.removeView(statusView);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
*/
///*
//    public void checkDrawOverlayPermission() {
//        /** check if we already  have permission to draw over other apps */
//        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
//        if(currentApiVersion >= 23) {
//            if (!Settings.canDrawOverlays(context)) {
//                /** if not construct intent to request permission */
//                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                        Uri.parse("package:" + context.getPackageName()));
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                /** request permission via start activity for result */
//                context.startActivity(intent);
//
//            }
//        }
//    }
//
//
//
//
//
//}


