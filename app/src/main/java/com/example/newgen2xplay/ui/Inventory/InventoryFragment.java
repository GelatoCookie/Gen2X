package com.example.newgen2xplay.ui.Inventory;


import static com.example.newgen2xplay.RFIDHandler.isInventoryRunning;
import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;
import static com.example.newgen2xplay.RFIDHandler.tagDataViewModel;

import static  com.example.newgen2xplay.RFIDHandler.tagIDs;

import android.media.tv.TableRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.newgen2xplay.RFIDHandler;
import com.google.android.material.snackbar.Snackbar;
import com.example.newgen2xplay.databinding.FragmentInventoryBinding;
import com.zebra.rfid.api3.AntennaInfo;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.TagData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryFragment extends Fragment {
    private static final String TAG = "INVENTORY_FRAGMENT";
    private FragmentInventoryBinding binding;
    InventoryAdapter inventoryAdapter;

    AntennaInfo antennaInfo;
    private int tagSeenCount = 0;
    private int totalUniquetags = 0;
    private InventoryViewModel inventoryViewModel;

    // Add variables for tracking unique tags per 10 seconds
    private long lastUniqueTagsCountTime = 0;
    private int uniqueTagsPer10Sec = 0;
    private int previousTotalUniqueTags = 0; // Track previous total for comparison
    private HashSet<String> tagsInLast10Sec = new HashSet<>();
    private static final long UNIQUE_TAGS_INTERVAL_MS = 10000; // 10 seconds

   // public volatile boolean  inventoryViewModel.inventoryBeingStopped = false;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long inventoryStartTime = 0; // Track inventory timer start

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (inventoryAdapter != null) inventoryAdapter.notifyDataSetChanged();

            long currentTime = System.currentTimeMillis();


            // Check if 10 seconds have passed since last unique tags count update
            if (currentTime - lastUniqueTagsCountTime >= UNIQUE_TAGS_INTERVAL_MS) {
                // Update the unique tags per 10 sec value
                uniqueTagsPer10Sec = totalUniquetags - previousTotalUniqueTags;
                // Update the UI
                if (binding != null && binding.uniqueTagPer10Sec != null) {
                    binding.uniqueTagPer10Sec.setText(String.format("New Unique Tags every 10s: %d", uniqueTagsPer10Sec));
                }
                // Reset for next 10-second window
                tagsInLast10Sec.clear();
                previousTotalUniqueTags = totalUniquetags; // Update previous total
                lastUniqueTagsCountTime = currentTime;
            }

            if (inventoryStartTime > 0 && binding != null && binding.timerValue != null) {
                long elapsed = (currentTime - inventoryStartTime) / 1000;
                long minutes = elapsed / 60;
                long seconds = elapsed % 60;
                String timeStr = String.format("%02d:%02d", minutes, seconds);
                String timerLabel = String.format("Timer : %s", timeStr);
                binding.timerValue.setText(timerLabel);

                // Check if we've reached the time limit
                String inputTime = binding.inventoryStopTimer.getText().toString();
                if (!inputTime.isEmpty()) {
                    try {
                        int limitSeconds = Integer.parseInt(inputTime);
                        Log.d(TAG, "Time limit set to: " + limitSeconds + " seconds");
                        if (limitSeconds > 0 && elapsed >= limitSeconds) {
                            Log.d(TAG, "Time limit reached, stopping inventory.");
                            inventoryViewModel.inventoryBeingStopped = true;
                            quietHandler.removeCallbacks(quietRunnable);
                            timerHandler.removeCallbacks(timerRunnable);
                            inventoryStartTime = 0;
                            stopInventoryProcess();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        // Invalid input, continue as normal
                    }
                }

            }
             // update every second
            timerHandler.postDelayed(this, 1000);
        }
    };

    // Handler and Runnable for 2-second quiet operation
    private Handler quietHandler = new Handler(Looper.getMainLooper());
    private final Runnable quietRunnable = new Runnable() {
        @Override
        public void run() {
            if(inventoryViewModel.inventoryBeingStopped){
                quietHandler.removeCallbacks(this);
                Log.d(TAG, "Quiet operation stopped due to inventory being stopped.");
                stopInventoryProcess();
                return;
            }
            else{
                if (binding != null && binding.selectAll != null && binding.selectAll.isChecked()) {
                    Log.d(TAG, "quietRunnable running: isInventoryRunning=" + isInventoryRunning);
                    String quietTimer = binding.quietTimer.getText().toString();
                    int quietTimeInMilliSeconds = 2000; // Default quiet time
                    if(!quietTimer.isEmpty()) {
                        try {
                            int seconds = Integer.parseInt(quietTimer);
                            if(seconds > 0) {
                                quietTimeInMilliSeconds = seconds * 1000;
                            }
                            Log.d(TAG,"Quiet time set to: " + quietTimeInMilliSeconds + " milliseconds");
                        } catch (NumberFormatException e) {
                            // Invalid input, use default
                        }
                    }


                    if (isInventoryRunning) {
                        isInventoryRunning = false;
//                    if (inventoryAdapter != null) {
//                        inventoryAdapter.stopAllTimers();
//                    }
                        stopInventory();
                        inventoryAdapter.uncheckAll();
                        inventoryViewModel.clearCheckedItems();
                        inventoryViewModel.resetPreInventoryReady();
                    }

                    // Select items and perform quiet operation
                    if (inventoryAdapter != null) {
                        inventoryAdapter.selectUpTo30ValidItems();
                    }
                    if (inventoryViewModel != null) {
                        inventoryViewModel.onQuietButtonClicked();
                    }
                    Log.d(TAG, "Quiet operation started, preparing for inventory.");
                    inventoryViewModel.prepareForInventory();

                    if (!inventoryViewModel.inventoryBeingStopped && binding != null && binding.selectAll != null && binding.selectAll.isChecked()) {
                        quietHandler.postDelayed(this, quietTimeInMilliSeconds);
                    }
                }
            }

        }
    };

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentInventoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inventoryViewModel = new ViewModelProvider(requireActivity()).get(InventoryViewModel.class);

        if(mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()){
            Snackbar.make(view, "Reader Not Connected", Snackbar.LENGTH_SHORT).show();
            //return;
        }

        if (isInventoryRunning) {
            binding.fabInventory.setImageResource(android.R.drawable.ic_media_pause);
        }

        antennaInfo = new AntennaInfo();
        ArrayList<Short> antennaList = new ArrayList<>();

        binding.fabInventory.setOnClickListener(v -> {
            if (mConnectedRfidReader != null && mConnectedRfidReader.isConnected()) {
                if (!isInventoryRunning) {
                    inventoryViewModel.inventoryBeingStopped = false; // Reset flag for next inventory
                    inventoryViewModel.prepareForInventory();

                    if (inventoryAdapter != null) inventoryAdapter.startAllTimers();

                    resetUniqueTagCounters();
                    startInventoryTimer();
                } else {
                    stopInventoryProcess();
                }
            } else {
                Snackbar.make(view, "Reader Not Connected", Snackbar.LENGTH_SHORT).show();
            }
        });


        ArrayList<InventoryItem> arrayOfItems = new ArrayList<>();
        inventoryAdapter = new InventoryAdapter(requireActivity(), arrayOfItems);
        binding.inventoryList.setAdapter(inventoryAdapter);


        inventoryAdapter = new InventoryAdapter(requireActivity(), new ArrayList<>());
        binding.inventoryList.setAdapter(inventoryAdapter);


        inventoryViewModel.getInventoryItems().observe(getViewLifecycleOwner(), items -> {

            inventoryAdapter.clear();
            if (items != null) {
                totalUniquetags = items.size();
                String tagCountText = String.format("Unique tags : %5s", String.format("%d", items.size()));
                if (binding != null && binding.uniqueTagCount != null) {
                    binding.uniqueTagCount.setText(tagCountText);
                }
                for (InventoryItem item : items) {
                    inventoryAdapter.add(item);
                }

                if (binding != null) {
                    InventoryAdapter.ColorCounts colorCounts = inventoryAdapter.getColorCounts();
                        if (binding.yellowCount != null)
                            binding.yellowCount.setText("Y: " + colorCounts.yellow);
                        if (binding.greenCount != null)
                            binding.greenCount.setText("G: " + colorCounts.green);
                        if (binding.lgreyCount != null)
                            binding.lgreyCount.setText("GREY: " + colorCounts.grey);
                        if (binding.whiteCount != null)
                            binding.whiteCount.setText("W: " + colorCounts.white);

                }
            } else {
                if (binding != null && binding.uniqueTagCount != null) {
                    binding.uniqueTagCount.setText("Unique tags : 00");
                }
                // Reset color counts to zero
                if (binding != null) {
                    if (binding.yellowCount != null)
                        binding.yellowCount.setText("Y: 0");
                    if (binding.greenCount != null)
                        binding.greenCount.setText("G: 0");
                    if (binding.lgreyCount != null)
                        binding.lgreyCount.setText("GREY: 0");
                    if (binding.whiteCount != null)
                        binding.whiteCount.setText("W: 0");
                }
            }

        });

        tagDataViewModel.getInventoryItem().observe(getViewLifecycleOwner(), tagItems -> {
            ArrayList<InventoryItem> batchUpdates = new ArrayList<>();

            for(TagData data : tagItems) {
                batchUpdates.add(new InventoryItem(data.getTagID(), data.getTagSeenCount(), data.getPeakRSSI(), data.getMemoryBankData()));
                try {
                    if (!tagIDs.contains(data.getTagID())) {
                        tagIDs.add(data.getTagID());
                    }
                } catch(Exception e) {}
            }

            if (!batchUpdates.isEmpty()) {
                for (InventoryItem item : batchUpdates) {
                    inventoryViewModel.addOrUpdateInventoryItem(item);
                }
            }
        });

        binding.checkboxFocus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
//                binding.checkboxQuiet.setChecked(false);
                inventoryViewModel.clearInventoryItems();
                inventoryAdapter.clearAll();
                inventoryViewModel.clearCheckedItems();
                binding.checkboxUnquiet.setChecked(false);
                if (inventoryAdapter != null) {
                    inventoryAdapter.uncheckAll();
                }
                binding.checkboxFastId.setChecked(false);
                binding.checkboxFocus.setChecked(true);

            }
            inventoryViewModel.onFocusCheckboxChanged(isChecked);
        });

        binding.checkboxFastId.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
//                binding.checkboxQuiet.setChecked(false);
                inventoryViewModel.clearInventoryItems();
                inventoryAdapter.clearAll();
                inventoryViewModel.clearCheckedItems();
                binding.checkboxUnquiet.setChecked(false);
                binding.checkboxFocus.setChecked(false);
                if (inventoryAdapter != null) {
                    inventoryAdapter.uncheckAll();
                }
                binding.checkboxFastId.setChecked(true);
            }
            inventoryViewModel.onFastIDchecked(isChecked);
        });


        binding.checkboxUnquiet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.checkboxFocus.setChecked(false);
            //    binding.checkboxQuiet.setChecked(false);
            }
            inventoryViewModel.onUnquietCheckboxChanged(isChecked);
        });

        inventoryAdapter.setOnItemCheckedChangeListener((item, isChecked, position) -> {
            int checkedCount = inventoryAdapter.getCheckedCount();
           // Log.d("InventoryItem","Check change listener called for item: " + item.getEPC() + ", isChecked: " + isChecked + ", position: " + position);
            if (isChecked) {
                inventoryViewModel.addCheckedItem(item);
            } else {
                inventoryViewModel.removeCheckedItem(item);
            }
            inventoryViewModel.onItemCheckedChanged(item, isChecked, position);

            int selectableCount = 0;
            for (InventoryItem invItem : inventoryAdapter.items) {
                if (invItem.getCount() > -1) selectableCount++;
            }
            if (binding != null && binding.selectAll != null) {
                binding.selectAll.setOnCheckedChangeListener(null);
                binding.selectAll.setChecked(checkedCount == Math.min(31, selectableCount) && checkedCount > 0);

                binding.selectAll.setOnCheckedChangeListener((buttonView, selectAllChecked) -> {
                    if (selectAllChecked) {
                        inventoryAdapter.selectUpTo30ValidItems();
                    } else {
                        inventoryAdapter.uncheckAll();
                    }
                });
            }
        });

        // Observe trigger events from RFIDHandler using MainActivity's instance
        RFIDHandler handler = ((com.example.newgen2xplay.MainActivity) requireActivity()).getRfidHandler();
        handler.triggerPressedLiveData.observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean pressed) {
                if (pressed != null && pressed) {
                    if (!isInventoryRunning) {
                        inventoryViewModel.inventoryBeingStopped = false; // Reset flag for next inventory
                        inventoryViewModel.prepareForInventory();

                        if (inventoryAdapter != null) inventoryAdapter.startAllTimers();

                        resetUniqueTagCounters();
                        startInventoryTimer();
                    }
                } else {
                    stopInventoryProcess();
                }
            }
        });

        inventoryViewModel.getClearAllCheckedEvent().observe(getViewLifecycleOwner(), unused -> {
            if (inventoryAdapter != null) {
                inventoryAdapter.uncheckAll();
            }
            inventoryViewModel.clearCheckedItems();
            inventoryViewModel.onQuietCheckboxChanged(false);
            binding.checkboxUnquiet.setChecked(false);
            inventoryViewModel.onUnquietCheckboxChanged(false);
        });

        // Observe pre-inventory ready event
        inventoryViewModel.getPreInventoryReady().observe(getViewLifecycleOwner(), isReady -> {
            if (Boolean.TRUE.equals(isReady) && !inventoryViewModel.inventoryBeingStopped) {
                Log.d(TAG, "Pre-inventory is ready, starting inventory.");
                isInventoryRunning = true;
                binding.fabInventory.setImageResource(android.R.drawable.ic_media_pause);

                inventoryAdapter.resetCountsForNewSession();
                tagSeenCount = 0;

                startInventory();
            }
        });

        // Observe ViewModel for business logic results (example)
        inventoryViewModel.getItemCheckboxEvent().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                // TODO: React to item checkbox changes here (UI update, business logic, etc.)
            }
        });
        inventoryViewModel.getFocusChecked().observe(getViewLifecycleOwner(), isChecked -> {
            if (isChecked != null) {
                // TODO: React to Focus checkbox changes here
            }
        });
        inventoryViewModel.getQuietChecked().observe(getViewLifecycleOwner(), isChecked -> {
            if (isChecked != null) {
                // TODO: React to Quiet checkbox changes here
            }
        });


        binding.buttonClearScreen.setOnClickListener(v -> {
            inventoryViewModel.clearInventoryItems();
            inventoryAdapter.clearAll(); // Ensures UI is cleared immediately
            inventoryViewModel.clearCheckedItems();
            binding.checkboxFocus.setChecked(false);
            binding.checkboxUnquiet.setChecked(false);
            binding.selectAll.setChecked(false);
            tagIDs.clear();
            inventoryViewModel.onUnquietCheckboxChanged(false);
        });

        binding.buttonQuiet.setOnClickListener(v -> {
            if (mConnectedRfidReader != null && mConnectedRfidReader.isConnected()) {

                resetUniqueTagCounters();
                inventoryViewModel.inventoryBeingStopped = false;
                if ( binding.selectAll.isChecked()) {
                    quietHandler.removeCallbacks(quietRunnable);
                    quietRunnable.run(); // Start immediately
                }
                if (isInventoryRunning) {
                    isInventoryRunning = false;
                    binding.fabInventory.setImageResource(android.R.drawable.ic_media_play);
                    if (inventoryAdapter != null) {
                        inventoryAdapter.stopAllTimers();
                    }
                    stopInventory();
                    //inventoryViewModel.clearCheckedItems();
                    // inventoryViewModel.onQuietCheckboxChanged(false);
                    inventoryViewModel.resetPreInventoryReady();


                    inventoryViewModel.prepareForInventory();
                    if (inventoryAdapter != null) inventoryAdapter.startAllTimers();
                    inventoryAdapter.setAllCheckboxesEnabled(true);
                    // inventoryViewModel.prepareForInventory();

                } else {

                    inventoryViewModel.resetPreInventoryReady();

                    if (inventoryViewModel != null) {
                        inventoryViewModel.onQuietButtonClicked();
                    }
                    inventoryViewModel.prepareForInventory();
                    if (inventoryAdapter != null) inventoryAdapter.startAllTimers();
                }

                startInventoryTimer();
            } else {
                Snackbar.make(view, "Reader Not Connected", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "selectAll.setOnCheckedChangeListener: isChecked=" + isChecked + ", programmatic=" + (buttonView.isPressed() ? "false" : "true"));
            if (isChecked) {
                inventoryAdapter.selectUpTo30ValidItems();

                if (isInventoryRunning) {
                    quietHandler.removeCallbacks(quietRunnable);
                    quietRunnable.run();
                }
            } else {
                Log.d(TAG, "selectAll.setOnCheckedChangeListener: isChecked=" + isChecked + ", programmatic=" + (buttonView.isPressed() ? "false" : "true"));
                inventoryAdapter.uncheckAll();
                quietHandler.removeCallbacks(quietRunnable);
            }
        });

    }

    private synchronized void startInventory(){
        // ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting inventory process");
                mConnectedRfidReader.Actions.Inventory.perform();
            } catch (InvalidUsageException | OperationFailureException e) {
                if( e.getStackTrace().length > 0) {
                    Log.e(TAG, e.getStackTrace()[0].toString());
                }
            }
        });
    }

    private synchronized void stopInventory(){
     //   ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Log.d(TAG, "stopInventory called");
                mConnectedRfidReader.Actions.Inventory.stop();
            } catch (InvalidUsageException | OperationFailureException e) {
                if (e.getStackTrace().length > 0) {
                    Log.e(TAG, e.getStackTrace()[0].toString());
                }
            }
        });
    }


    private void startInventoryTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        inventoryStartTime = System.currentTimeMillis();
        if (binding != null && binding.timerValue != null) {
            String timeStr = String.format("%02d:%02d", 0, 0);
            String timerLabel = String.format("Timer : %s", timeStr);
            binding.timerValue.setText(timerLabel);
        }

        timerHandler.post(timerRunnable);
    }

    private void resetUniqueTagCounters() {
        uniqueTagsPer10Sec = 0;
        previousTotalUniqueTags = 0;
        tagsInLast10Sec.clear();
        lastUniqueTagsCountTime = System.currentTimeMillis();

        // Update UI for unique tags per 10 seconds
        if (binding != null && binding.uniqueTagPer10Sec != null) {
            binding.uniqueTagPer10Sec.setText(String.format("New Unique Tags in every 10s: %d", uniqueTagsPer10Sec));
        }
    }

    private void stopInventoryProcess(){
        Log.d(TAG, "stopInventoryProcess called");
        inventoryViewModel.inventoryBeingStopped = true;
        isInventoryRunning = false;

        timerHandler.removeCallbacks(timerRunnable);
        quietHandler.removeCallbacks(quietRunnable);

        inventoryStartTime = 0;
        binding.fabInventory.setImageResource(android.R.drawable.ic_media_play);
        if (inventoryAdapter != null) {
            inventoryAdapter.uncheckAll();
            inventoryAdapter.stopAllTimers();
        }

        binding.fabInventory.setEnabled(true);
        binding.selectAll.setChecked(false);
        inventoryViewModel.clearCheckedItems();
        // Uncheck and disable Quiet checkbox, update ViewModel
//                    binding.checkboxQuiet.setChecked(false);
//                    binding.checkboxQuiet.setEnabled(false);
        inventoryViewModel.onUnquietCheckboxChanged(false);
        binding.checkboxUnquiet.setChecked(false);
        inventoryViewModel.resetPreInventoryReady();
        inventoryAdapter.setAllCheckboxesEnabled(true);

        stopInventory();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(timerRunnable);
        quietHandler.removeCallbacks(quietRunnable);

        inventoryStartTime = 0;
        if (binding != null && binding.timerValue != null) {
            String timeStr = String.format("%02d:%02d", 0, 0);
            String timerLabel = String.format("Timer : %s", timeStr);
            binding.timerValue.setText(timerLabel);
        }
        Log.d("onDestroyView", "InventoryFragment destroyed, timer stopped and reset.");
        binding = null;
        executor.shutdown();
    }
}
