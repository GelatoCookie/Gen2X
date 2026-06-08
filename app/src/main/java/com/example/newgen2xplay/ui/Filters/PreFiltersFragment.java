package com.example.newgen2xplay.ui.Filters;


import static com.example.newgen2xplay.RFIDHandler.getStateAwareActionFromString;
import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;
import static com.example.newgen2xplay.RFIDHandler.tagIDs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.example.newgen2xplay.R;
import com.example.newgen2xplay.databinding.FragmentPrefiltersBinding;
import com.zebra.rfid.api3.FILTER_ACTION;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.PreFilters;
import com.zebra.rfid.api3.TARGET;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class PreFiltersFragment extends Fragment {
    private static final String TAG = "PREFILTER_FRAGMENT";
    private FragmentPrefiltersBinding binding;
    private PreFiltersViewModel viewModel;
    int selectedPrefilterIndex = 0;
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentPrefiltersBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(PreFiltersViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.getPreFilterResult().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess == null) return;
            if (isSuccess) {
                Snackbar.make(requireView(), "PreFilter saved successfully", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(requireView(), "Failed to save PreFilter", Snackbar.LENGTH_SHORT).show();
            }
            viewModel.resetPreFilterResult();
        });
        viewModel.getDeleteAllResult().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess == null) return;
            if (isSuccess) {
                Snackbar.make(requireView(), "All PreFilters deleted successfully", Snackbar.LENGTH_SHORT).show();
                int index = binding.prefilterIndex.getSelectedItemPosition();
                getPreFilter(requireView(), index);
                // Notify InventoryFragment to uncheck all checkboxes
                com.example.newgen2xplay.ui.Inventory.InventoryViewModel inventoryViewModel =
                        new ViewModelProvider(requireActivity()).get(com.example.newgen2xplay.ui.Inventory.InventoryViewModel.class);
                inventoryViewModel.triggerClearAllCheckedEvent();

                inventoryViewModel.onUnquietCheckboxChanged(false);

            } else {
                Snackbar.make(requireView(), "Failed to delete all PreFilter", Snackbar.LENGTH_SHORT).show();
            }
            viewModel.resetDeleteAllResult();
        });
        viewModel.getDeletePrefilter().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess == null) return;
            if (isSuccess) {
                Snackbar.make(requireView(), "PreFilter deleted successfully", Snackbar.LENGTH_SHORT).show();
                int index = binding.prefilterIndex.getSelectedItemPosition();
                getPreFilter(requireView(), index);
            } else {
                Snackbar.make(requireView(), "Failed to delete PreFilter", Snackbar.LENGTH_SHORT).show();
            }
            viewModel.resetDeletePrefilter();
        });

        if(mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()){
            Snackbar.make(view, "Reader Not Connected", Snackbar.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<CharSequence> targetAdaptor = ArrayAdapter.createFromResource(getActivity(),
                R.array.pre_filter_target_options, android.R.layout.simple_spinner_item);
        targetAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.prefilterTarget.setAdapter(targetAdaptor);

        ArrayAdapter<CharSequence> actionAdaptor = ArrayAdapter.createFromResource(getActivity(),
                R.array.pre_filter_action_array, android.R.layout.simple_spinner_item);
        actionAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.prefilterAction.setAdapter(actionAdaptor);

        ArrayAdapter<CharSequence> memBankAdaptor = ArrayAdapter.createFromResource(getActivity(),
                R.array.pre_filter_memory_bank_array, android.R.layout.simple_spinner_item);
        memBankAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.prefilterMembank.setAdapter(memBankAdaptor);

        ArrayAdapter<CharSequence> prefilterIndex = ArrayAdapter.createFromResource(getActivity(),
                R.array.pre_filter_index, android.R.layout.simple_spinner_item);
        prefilterIndex.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.prefilterIndex.setAdapter(prefilterIndex);

        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.item_tag_suggestion, tagIDs);
        ((AutoCompleteTextView) binding.etMask).setAdapter(tagAdapter);


        binding.prefilterIndex.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view1, int position, long id) {
                selectedPrefilterIndex = position;
                getPreFilter(view, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        binding.buttonSave.setOnClickListener(v -> {
            String mask = binding.etMask.getText().toString();
            String length = binding.etLength.getText().toString();
            String pointer = binding.etPointer.getText().toString();

            if (mask == null || mask.isEmpty() ||
                length == null || length.isEmpty() ||
                pointer == null || pointer.isEmpty()) {
                Snackbar.make(v, "Please fill all required fields", Snackbar.LENGTH_SHORT).show();
                return;
            }

            PreFilters filters = new PreFilters();
            PreFilters.PreFilter filter = filters.new PreFilter();
            filter.setAntennaID((short) 1);
            filter.setTagPattern(mask);
            filter.setTagPatternBitCount(Integer.parseInt(length));
            filter.setBitOffset(Integer.parseInt(pointer));
            filter.setMemoryBank(MEMORY_BANK.GetMemoryBankValue(binding.prefilterMembank.getSelectedItem().toString()));
            filter.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE);
            filter.StateAwareAction.setTarget(TARGET.getTarget(binding.prefilterTarget.getSelectedItemPosition()));
            filter.StateAwareAction.setStateAwareAction(getStateAwareActionFromString(binding.prefilterAction.getSelectedItem().toString()));

            viewModel.savePreFilter(filter);
        });

        binding.buttonDelete.setOnClickListener(v -> {
            int index = binding.prefilterIndex.getSelectedItemPosition();

            viewModel.deletePreFilter(index);
        });

        binding.buttonDeleteAll.setOnClickListener(v -> {
            viewModel.deleteAllPreFilter();
        });

    }

    private void getPreFilter(View view, int index) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicReference<PreFilters.PreFilter> preFilter = new AtomicReference<>();
        executor.execute(() -> {
            try {
                PreFilters.PreFilter filter = mConnectedRfidReader.Actions.PreFilters.getPreFilter(index);
                Log.d(TAG, "PreFilter at index " + index + ": " + filter.getMemoryBank());
                preFilter.set(mConnectedRfidReader.Actions.PreFilters.getPreFilter(index));
            } catch (InvalidUsageException e) {
                handler.post(() -> {
                    binding.etMask.setText("");
                    binding.etLength.setText("");
                    binding.etPointer.setText("");
                    binding.prefilterMembank.setSelection(0);
                    binding.prefilterTarget.setSelection(0);
                    binding.prefilterAction.setSelection(0);

                });
                //Snackbar.make(view, "No filter found in index "+ index , Snackbar.LENGTH_SHORT).show();
                if (e.getStackTrace().length > 0) {
                    Log.e(TAG, e.getStackTrace()[0].toString());
                }
            }
            handler.post(() -> {
                if(preFilter.get() != null){
                    StringBuilder tagPattern = new StringBuilder();
                    for (byte b : preFilter.get().getTagPattern()) {
                        tagPattern.append(String.format("%02X", b));
                    }
                    binding.etMask.setText(tagPattern);
                    binding.etLength.setText(String.valueOf(preFilter.get().getTagPatternBitCount()));
                    binding.etPointer.setText(String.valueOf(preFilter.get().getBitOffset()));
                    if (preFilter.get().getMemoryBank() != null) {
                        binding.prefilterMembank.setSelection(preFilter.get().getMemoryBank().getValue() - 1);
                    } else {
                        //binding.prefilterMembank.setSelection(0);
                    }
                    binding.prefilterTarget.setSelection(preFilter.get().StateAwareAction.getTarget().getValue());
                    binding.prefilterAction.setSelection(preFilter.get().StateAwareAction.getStateAwareAction().getValue());

                }
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mConnectedRfidReader != null && mConnectedRfidReader.isConnected()){
            int index = binding.prefilterIndex.getSelectedItemPosition();
            getPreFilter(requireView(), index);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}