package edu.nd.crepe.ui.main_activity;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import edu.nd.crepe.R;

public class FabModalBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ModalBottomSheet";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fab_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());

        ConstraintLayout bottomSheetContent = view.findViewById(R.id.bottom_sheet_content);
        // get the height of the bottom sheet content
        bottomSheetContent.measure(0, 0);
        int height = bottomSheetContent.getMeasuredHeight();
        // set the peek height to the height of the bottom sheet content
        bottomSheetBehavior.setPeekHeight(height);
    }


}
