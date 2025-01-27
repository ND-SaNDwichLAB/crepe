package edu.nd.crepe.ui.main_activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import edu.nd.crepe.R;

public class SettingsFragment extends Fragment {
    private SwitchMaterial recordingSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Settings");
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireActivity().getSharedPreferences("eval_settings", Context.MODE_PRIVATE);
        recordingSwitch = view.findViewById(R.id.recording_switch);

        recordingSwitch.setChecked(sharedPreferences.getBoolean("is_recording", false));
        recordingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("is_recording", isChecked).apply();
        });

        return view;
    }
}