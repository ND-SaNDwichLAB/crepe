package edu.nd.crepe.ui.main_activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import edu.nd.crepe.MainActivity;
import edu.nd.crepe.R;
import edu.nd.crepe.graphquery.Const;
import edu.nd.crepe.ui.dialog.CollectorConfigurationDialogWrapper;
import edu.nd.crepe.ui.dialog.CreateCollectorFromConfigDialogBuilder;
import edu.nd.crepe.ui.dialog.CreateCollectorFromURLDialogBuilder;

public class FabModalBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ModalBottomSheet";
    private HomeFragment currentFragment;
    private CreateCollectorFromURLDialogBuilder createCollectorFromURLDialogBuilder;
    private CreateCollectorFromConfigDialogBuilder createCollectorFromConfigDialogBuilder;
    private CollectorConfigurationDialogWrapper wrapper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fab_bottom_sheet, container, false);

        currentFragment = (HomeFragment) getParentFragment();
        createCollectorFromURLDialogBuilder = new CreateCollectorFromURLDialogBuilder(getContext(), refreshCollectorListRunnable);
        createCollectorFromConfigDialogBuilder = new CreateCollectorFromConfigDialogBuilder(getContext(), refreshCollectorListRunnable);


        LinearLayout addExistingBtn = view.findViewById(R.id.add_existing_collector_btn);
        LinearLayout createNewBtn = view.findViewById(R.id.create_new_collector_btn);


        addExistingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // collapse the fab icon

                dismiss();

                Dialog dialog = createCollectorFromURLDialogBuilder.build();
                dialog.show();
            }
        });

        createNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dismiss();

                wrapper = createCollectorFromConfigDialogBuilder.buildDialogWrapperWithNewCollector();
                if (!Settings.canDrawOverlays(getContext())){

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
                    builder.setTitle("Service Permission Required")
                            .setMessage(Const.appName + " needs the permission to display over other app for proper function. Please enable the service in the phone settings.")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    getContext().startActivity(intent);
                                }
                            }).show();
                }
                else {
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
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme );
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    Runnable refreshCollectorListRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentFragment instanceof HomeFragment) {
                try {
                    ((HomeFragment) currentFragment).initCollectorList();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    };

}
