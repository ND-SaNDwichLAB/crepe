package edu.nd.crepe.ui.main_activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import edu.nd.crepe.database.Collector;
import edu.nd.crepe.servicemanager.AccessibilityPermissionManager;
import edu.nd.crepe.servicemanager.CrepeAccessibilityService;
import edu.nd.crepe.R;
import edu.nd.crepe.servicemanager.CrepeDisplayPermissionManager;
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

                // check display over other apps permission
                if (!Settings.canDrawOverlays(context)) {
                    Dialog enableDisplayServiceDialog = CrepeDisplayPermissionManager.getInstance().getEnableDisplayServiceDialog(context);
                    enableDisplayServiceDialog.show();
                } else {
                    // first, collapse the fab icon
                    dismiss();
                    // then, bring up the dialog to create a new collector
                    wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithCollector(new Collector());
                    Boolean isEdit = false;
                    wrapper.show(isEdit);
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
