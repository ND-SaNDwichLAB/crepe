package edu.nd.crepe.ui.main_activity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import edu.nd.crepe.accessibilityservice.AccessibilityPermissionManager;
import edu.nd.crepe.accessibilityservice.CrepeAccessibilityService;
import edu.nd.crepe.R;
import edu.nd.crepe.ui.dialog.CollectorConfigurationDialogWrapper;
import edu.nd.crepe.ui.dialog.CreateCollectorFromConfigDialogBuilder;
import edu.nd.crepe.ui.dialog.AddCollectorFromCollectorIdDialogBuilder;

public class FabModalBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ModalBottomSheet";
    private HomeFragment currentFragment;
    private AddCollectorFromCollectorIdDialogBuilder addCollectorFromCollectorIdDialogBuilder;
    private CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder;
    private CollectorConfigurationDialogWrapper wrapper;

    private Context context;

    public FabModalBottomSheet(AddCollectorFromCollectorIdDialogBuilder addCollectorFromCollectorIdDialogBuilder, CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder) {
        this.addCollectorFromCollectorIdDialogBuilder = addCollectorFromCollectorIdDialogBuilder;
        this.createCollectorFromConfigDialogBuilder = createCollectorFromConfigDialogBuilder;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fab_bottom_sheet, container, false);

        LinearLayout addExistingBtn = view.findViewById(R.id.add_existing_collector_btn);
        LinearLayout createNewBtn = view.findViewById(R.id.create_new_collector_btn);

        this.context = getContext();

        addExistingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check accessibility service permission
                if (!CrepeAccessibilityService.isAccessibilityServiceEnabled(getContext(), CrepeAccessibilityService.class)) {
                    Dialog enableAccessibilityServiceDialog = AccessibilityPermissionManager.getInstance().getEnableAccessibilityServiceDialog(getContext());
                    enableAccessibilityServiceDialog.show();
                } else {
                    // first, collapse the fab icon
                    dismiss();
                    // then, bring up the dialog to add a collector from collector id
                    Dialog dialog = addCollectorFromCollectorIdDialogBuilder.build();
                    dialog.show();
                }


            }
        });

        createNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // check accessibility service permission
                if (!CrepeAccessibilityService.isAccessibilityServiceEnabled(getContext(), CrepeAccessibilityService.class)) {
                    Dialog enableAccessibilityServiceDialog = AccessibilityPermissionManager.getInstance().getEnableAccessibilityServiceDialog(getContext());
                    enableAccessibilityServiceDialog.show();
                    // then, check display over other apps permission
                } else if (!Settings.canDrawOverlays(context)) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                    builder.setTitle("Service Permission Required")
                            .setMessage("Please enable the permission to display over other app for proper function.")
                            .setPositiveButton("ENABLE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                }
                            }).show();
                } else {
                    // first, collapse the fab icon
                    dismiss();
                    // then, bring up the dialog to create a new collector
                    wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithNewCollector();
                    wrapper.show();
                }
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }


}
