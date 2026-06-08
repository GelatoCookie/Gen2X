package com.example.newgen2xplay.ui.customImpinj;

import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.newgen2xplay.R;
import com.example.newgen2xplay.databinding.FragmentCustomTagquietBinding;
import com.google.android.material.snackbar.Snackbar;

public class TagQuietCustomFragment extends Fragment {

    private TagQuietCustomViewModel mViewModel;
    private FragmentCustomTagquietBinding binding;

    public static TagQuietCustomFragment newInstance() {
        return new TagQuietCustomFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomTagquietBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TagQuietCustomViewModel.class);

        if(mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()){
            Snackbar.make(view, "Reader Not Connected", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Setup spinners for tagquietmask1, tagquietmask2, tagquietmask3
        ArrayAdapter<CharSequence> tagMaskAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.tagmask_enum,
                android.R.layout.simple_spinner_item);
        tagMaskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.tagquietMask1Spinner.setAdapter(tagMaskAdapter);
        binding.tagquietMask2Spinner.setAdapter(tagMaskAdapter);
        binding.tagquietMask3Spinner.setAdapter(tagMaskAdapter);

        // Setup spinner for target
        ArrayAdapter<CharSequence> targetAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.pre_filter_target_options,
                R.layout.simple_spinner_item_small);
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.prefilterTarget.setAdapter(targetAdapter);

        // Setup spinner for action
        ArrayAdapter<CharSequence> actionAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.pre_filter_action_array,
                R.layout.simple_spinner_item_small);
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.prefilterAction.setAdapter(actionAdapter);

        // Save button
        binding.buttonSave.setOnClickListener(v -> {
            String mask1 = binding.tagquietMask1Spinner.getSelectedItem().toString();
            String mask2 = binding.tagquietMask2Spinner.getSelectedItem().toString();
            String mask3 = binding.tagquietMask3Spinner.getSelectedItem().toString();
            int target = binding.prefilterTarget.getSelectedItemPosition();
            String action = binding.prefilterAction.getSelectedItem().toString();
            mViewModel.saveTagQuiet(mask1, mask2, mask3, target, action);
        });

        // Delete all button
        binding.buttonDeleteAll.setOnClickListener(v -> {
            mViewModel.deleteAllTagQuiet();
        });

        // Observe results and show feedback
        mViewModel.getSaveResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result) {
                Toast.makeText(requireContext(), "TagQuiet filter saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to save TagQuiet filter", Toast.LENGTH_SHORT).show();
            }
            mViewModel.resetSaveResult();
        });
        mViewModel.getRemoveResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result) {
                Toast.makeText(requireContext(), "TagQuiet filter removed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to remove TagQuiet filter", Toast.LENGTH_SHORT).show();
            }
            mViewModel.resetRemoveResult();
        });
        mViewModel.getDeleteAllResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            if (result) {
                Toast.makeText(requireContext(), "All filters deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to delete all TagQuiet filters", Toast.LENGTH_SHORT).show();
            }
            mViewModel.resetDeleteAllResult();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}