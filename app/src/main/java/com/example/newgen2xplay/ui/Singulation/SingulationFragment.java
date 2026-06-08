package com.example.newgen2xplay.ui.Singulation;


import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.example.newgen2xplay.R;
import com.example.newgen2xplay.databinding.FragmentSingulationBinding;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class SingulationFragment extends Fragment {
    private static final String TAG = "SINGULATION_FRAGMENT";
    private FragmentSingulationBinding binding;
    private SingulationViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentSingulationBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(SingulationViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()){
            Snackbar.make(view, "Reader Not Connected", Snackbar.LENGTH_SHORT).show();
            return;
        }

        viewModel.getSingulationResult().observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess == null) return;
            if (isSuccess) {
                Snackbar.make(requireView(), "Setting singulation success", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(requireView(), "Setting singulation Failure", Snackbar.LENGTH_SHORT).show();
            }
            viewModel.resetSaveResult();
        });



        ArrayList<String> antennaArray = new ArrayList<>();
        if(mConnectedRfidReader.isConnected()) {
            for (int i = 1; i <= mConnectedRfidReader.ReaderCapabilities.getNumAntennaSupported(); i++) {
                antennaArray.add("Antenna " + i);
            }
        }

        getSingulation();

        ArrayAdapter<CharSequence> sessionAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.session_array, android.R.layout.simple_spinner_item);
        sessionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSession.setAdapter(sessionAdapter);

        ArrayAdapter<CharSequence> inventoryAdapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.inventory_state_array, android.R.layout.simple_spinner_item);
        inventoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerInventoryState.setAdapter(inventoryAdapter);

        ArrayAdapter<CharSequence> sLFlagAdaptor = ArrayAdapter.createFromResource(getActivity(),
                R.array.sl_flags_array, android.R.layout.simple_spinner_item);
        sLFlagAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerSlFlag.setAdapter(sLFlagAdaptor);

        binding.buttonSave.setOnClickListener(v -> {
            setSingulationControlViaViewModel();
        });
    }

    private void getSingulation() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicReference<Antennas.SingulationControl> singulationControl = new AtomicReference<>();
        executor.execute(() -> {
            try {
                singulationControl.set(mConnectedRfidReader.Config.Antennas.getSingulationControl(1));
            } catch (InvalidUsageException | OperationFailureException e) {
                if (e.getStackTrace().length > 0) {
                    Log.e(TAG, e.getStackTrace()[0].toString());
                }
            }
            handler.post(() -> {
                if (singulationControl.get() != null) {
                    if(singulationControl.get().getSession() != null){
                        binding.spinnerSession.setSelection(singulationControl.get().getSession().getValue());
                    }
                    binding.etTagPopulation.setText(String.valueOf(singulationControl.get().getTagPopulation()));
                    if(singulationControl.get().Action.getInventoryState() != null) {
                        binding.spinnerInventoryState.setSelection(singulationControl.get().Action.getInventoryState().getValue());
                    }
                    if(singulationControl.get().Action.getSLFlag() != null){
                        binding.spinnerSlFlag.setSelection(singulationControl.get().Action.getSLFlag().getValue());
                    }
                }
            });
        });
    }

    private void setSingulationControlViaViewModel() {
        int antennaIndex = 1;
        int sessionIndex = binding.spinnerSession.getSelectedItemPosition();
        int tagPopulation = Integer.parseInt(binding.etTagPopulation.getText().toString());
        int inventoryStateIndex = binding.spinnerInventoryState.getSelectedItemPosition();
        int slFlagIndex = binding.spinnerSlFlag.getSelectedItemPosition();
        viewModel.setSingulationControl(antennaIndex, sessionIndex, tagPopulation, inventoryStateIndex, slFlagIndex);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mConnectedRfidReader != null && mConnectedRfidReader.isConnected()){
            getSingulation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
