package com.example.newgen2xplay.ui.Inventory;

import static com.example.newgen2xplay.RFIDHandler.impinjExtensions;
import static com.example.newgen2xplay.RFIDHandler.isInventoryRunning;
import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TAGQUIET_MASK;
import com.zebra.rfid.api3.FILTER_ACTION;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.PreFilters;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.STATE_AWARE_ACTION;
import com.zebra.rfid.api3.TARGET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InventoryViewModel extends ViewModel {
    private static final String TAG = "InventoryViewModel";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public volatile boolean inventoryBeingStopped = false;
    private final MutableLiveData<CheckboxEvent> itemCheckboxEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> focusChecked = new MutableLiveData<>();
    private final MutableLiveData<Boolean> fastidChecked = new MutableLiveData<>();
    private final MutableLiveData<Boolean> quietChecked = new MutableLiveData<>();
    private final MutableLiveData<Boolean> unquietChecked = new MutableLiveData<>();
    private final MutableLiveData<Void> clearAllCheckedEvent = new MutableLiveData<>();
    private final MutableLiveData<ArrayList<InventoryItem>> checkedItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> preInventoryReady = new MutableLiveData<>();

    private boolean isFocusCheckedState = false;
    private boolean isFastIdCheckedState = false;
    private boolean isQuietCheckedState = false;
    private boolean isUnquietCheckedState = false;

    private final MutableLiveData<ArrayList<InventoryItem>> inventoryItems = new MutableLiveData<>(new ArrayList<>());
    public LiveData<CheckboxEvent> getItemCheckboxEvent() {
        return itemCheckboxEvent;
    }
    public LiveData<Boolean> getFocusChecked() {
        return focusChecked;
    }
    public LiveData<Boolean> getFastidChecked() {
        return fastidChecked;
    }
    public LiveData<Boolean> getQuietChecked() {
        return quietChecked;
    }
    public LiveData<Boolean> getUnquietChecked() {
        return unquietChecked;
    }
    public LiveData<Void> getClearAllCheckedEvent() {
        return clearAllCheckedEvent;
    }
    public LiveData<ArrayList<InventoryItem>> getCheckedItems() {
        return checkedItems;
    }
    public LiveData<Boolean> getPreInventoryReady() {
        return preInventoryReady;
    }
    public LiveData<ArrayList<InventoryItem>> getInventoryItems() {
        return inventoryItems;
    }
    public boolean isFocusChecked() {
        return isFocusCheckedState;
    }
    public boolean isQuietChecked() {
        return isQuietCheckedState;
    }
    public boolean isUnquietChecked() {
        return isUnquietCheckedState;
    }
    public boolean isFastIdChecked() {
        return isFastIdCheckedState;
    }

    public void onItemCheckedChanged(InventoryItem item, boolean isChecked, int position) {
        itemCheckboxEvent.setValue(new CheckboxEvent(item, isChecked));
    }
    public void onFocusCheckboxChanged(boolean isChecked) {
        if (isChecked) {
            quietChecked.setValue(false);
            isQuietCheckedState = false;
            unquietChecked.setValue(false);
            isUnquietCheckedState = false;
            fastidChecked.setValue(false);
            isFastIdCheckedState = false;
        }
        focusChecked.setValue(isChecked);
        isFocusCheckedState = isChecked;
 
    }

    public void onFastIDchecked(boolean isChecked) {
        if (isChecked) {
            quietChecked.setValue(false);
            isQuietCheckedState = false;
            unquietChecked.setValue(false);
            isUnquietCheckedState = false;
            focusChecked.setValue(false);
        }
        fastidChecked.setValue(isChecked);
        isFastIdCheckedState = isChecked;

    }

    public void onQuietCheckboxChanged(boolean isChecked) {
        if (isChecked) {
            focusChecked.setValue(false);
            isFocusCheckedState = false;
            unquietChecked.setValue(false);
            isUnquietCheckedState = false;
            fastidChecked.setValue(false);
            isFastIdCheckedState = false;
        }
        quietChecked.setValue(isChecked);
        isQuietCheckedState = isChecked;
 
    }
    public void onUnquietCheckboxChanged(boolean isChecked) {
        if (isChecked) {
            focusChecked.setValue(false);
            isFocusCheckedState = false;
            quietChecked.setValue(false);
            isQuietCheckedState = false;
            fastidChecked.setValue(false);
            isFastIdCheckedState = false;
        }
        unquietChecked.setValue(isChecked);
        isUnquietCheckedState = isChecked;

    }
    public void triggerClearAllCheckedEvent() {
        clearAllCheckedEvent.setValue(null);
    }
    public void addCheckedItem(InventoryItem item) {
        ArrayList<InventoryItem> list = checkedItems.getValue();
        if (list == null) list = new ArrayList<>();
        if (!list.contains(item)) {
            list.add(item);
            checkedItems.setValue(new ArrayList<>(list));
        }
    }
    public void removeCheckedItem(InventoryItem item) {
        ArrayList<InventoryItem> list = checkedItems.getValue();
        if (list != null && list.contains(item)) {
            list.remove(item);
            checkedItems.setValue(new ArrayList<>(list));
        }
    }
    public void clearCheckedItems() {
        checkedItems.setValue(new ArrayList<>());

    }

    public void addOrUpdateInventoryItem(InventoryItem item) {
        ArrayList<InventoryItem> list = inventoryItems.getValue();
        if (list == null) list = new ArrayList<>();
        boolean found = false;
     //   Log.d(TAG, "Processing item: " + item.getEPC() + ", count: " + item.getCount() + ", RSSI: " + item.getRSSI());
        for (InventoryItem existing : list) {
            if (existing.getEPC() != null && existing.getEPC().equals(item.getEPC())) {
       //         Log.d(TAG, "Processing item: " + item.getEPC() + ", count: " + item.getCount() + ", RSSI: " + item.getRSSI() + " found existing item: " + existing.getCount() + ", lastReaderSeenCount: " + existing.getLastReaderSeenCount());
                if (existing.getCount() == 0 && existing.getLastReaderSeenCount() == 0) {
                    existing.setCount(item.getCount());
                } else {
                    existing.setCount(existing.getCount() + item.getCount());
                }
                existing.setRSSI(item.getRSSI());
                existing.setLastReaderSeenCount(existing.getLastReaderSeenCount() + item.getCount());
                existing.setLastSeenTimestampMillis(System.currentTimeMillis());
                found = true;
                break;
            }
        }
        if (!found) {
            item.setFirstSeenTimestampMillis(System.currentTimeMillis());
            item.setLastSeenTimestampMillis(System.currentTimeMillis());
            item.setLastReaderSeenCount(item.getCount());
            list.add(item);
        }
        inventoryItems.setValue(new ArrayList<>(list));
    }

    public void clearInventoryItems() {
        inventoryItems.setValue(new ArrayList<>());
    }

    public void setTagquietOrTagFocusorFastId() throws  IllegalStateException, InvalidUsageException, OperationFailureException {
        if (mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()) {
            throw new IllegalStateException("RFID Reader not connected");
        }
        ArrayList<InventoryItem> items = checkedItems.getValue();
        boolean focus = isFocusChecked();
        boolean quiet = isQuietChecked();
        boolean unquiet = isUnquietChecked();
        boolean fastId = isFastIdChecked();

        if(focus){
            //clearInventoryItems();
            mConnectedRfidReader.Actions.PreFilters.deleteAll();
            setTagFocus(focus);
        }

       /* if(quiet){
            mConnectedRfidReader.Actions.PreFilters.deleteAll();
            for(InventoryItem item : items) {
                setPrefilter(item.getEPC());
            }
            setTagQuietOrUnquiet(quiet);
        }*/
        if(unquiet){
            mConnectedRfidReader.Actions.PreFilters.deleteAll();
            setTagQuietOrUnquiet(false);

        }

        setFastId(fastId);
    }

    public void prepareForInventory() {
            executor.execute(() -> {
                if (inventoryBeingStopped) {
                    Log.d(TAG, "onQuietButtonClicked: Inventory is being stopped, skipping operation");
                    return;
                }
                try {
                    Thread.sleep(1);
                    setTagquietOrTagFocusorFastId();
                    // Simulate heavy work, replace as needed
                } catch (OperationFailureException | IllegalStateException | InvalidUsageException | InterruptedException e) {
                    e.printStackTrace();
                    preInventoryReady.postValue(false);
                }
                Log.d(TAG, "prepareForInventory: Pre-inventory setup complete");
                preInventoryReady.postValue(true);
            });
    }

    public void resetPreInventoryReady() {
        preInventoryReady.setValue(false);
    }

    public static class CheckboxEvent {
        public final InventoryItem item;
        public final boolean isChecked;
        public CheckboxEvent(InventoryItem item, boolean isChecked) {
            this.item = item;
            this.isChecked = isChecked;
        }
    }

    private void setTagFocus(boolean isChecked) throws IllegalStateException, InvalidUsageException, OperationFailureException {
            impinjExtensions.setTagFocus(isChecked, (short) 1);
            setSingulation(SESSION.SESSION_S1, INVENTORY_STATE.INVENTORY_STATE_A, SL_FLAG.SL_ALL);
    }

    private void setFastId(boolean isChecked) {
        try {
            if(isChecked){
                mConnectedRfidReader.Actions.PreFilters.deleteAll();
                impinjExtensions.setFastID(isChecked, (short) 1);
            } else{
                impinjExtensions.setFastID(isChecked, (short) 1);
            }

        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            Log.d(TAG, "setFastId: OperationFailureException: " + e.getVendorMessage());
            e.printStackTrace();
        }
    }

    private void setSingulation(SESSION session,  INVENTORY_STATE inventoryState, SL_FLAG slFlag)
            throws InvalidUsageException, OperationFailureException {
        Antennas.SingulationControl singulationControl = mConnectedRfidReader.Config.Antennas.getSingulationControl(1);
        singulationControl.setSession(session);
        singulationControl.Action.setInventoryState(inventoryState);
        singulationControl.Action.setSLFlag(slFlag);

        mConnectedRfidReader.Config.Antennas.setSingulationControl(1, singulationControl);

    }

    public boolean setTagQuietOrUnquiet(boolean isChecked)
            throws IllegalStateException, InvalidUsageException, OperationFailureException {
            ENUM_TAGQUIET_MASK[] tagMask ={ ENUM_TAGQUIET_MASK.S3B};
            if(isChecked){
                //Quiet tags
                TARGET target = TARGET.TARGET_SL;
                STATE_AWARE_ACTION stateAwareAction = STATE_AWARE_ACTION.STATE_AWARE_ACTION_ASRT_SL;
                impinjExtensions.setTagQuiet(tagMask,target,stateAwareAction,(short) 1);

                setSingulation(SESSION.SESSION_S2, INVENTORY_STATE.INVENTORY_STATE_AB_FLIP, SL_FLAG.SL_FLAG_DEASSERTED);
            }else{
                //Unquiet tags
                TARGET target = TARGET.TARGET_SL;
                STATE_AWARE_ACTION stateAwareAction = STATE_AWARE_ACTION.STATE_AWARE_ACTION_DSRT_SL;
                impinjExtensions.setTagQuiet(tagMask,target,stateAwareAction,(short) 1);

                //2nd prefilter
                TARGET target2 = TARGET.TARGET_INVENTORIED_STATE_S3;
                STATE_AWARE_ACTION stateAwareAction2 = STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_A;
                impinjExtensions.setTagQuiet(tagMask,target2,stateAwareAction2,(short) 1);
//                setSingulation(SESSION.SESSION_S2, INVENTORY_STATE.INVENTORY_STATE_AB_FLIP, SL_FLAG.SL_FLAG_ASSERTED);
            }

        return true ;
    }

    public void onQuietButtonClicked() {
        Log.d( TAG, "onQuietButtonClicked: Quiet button clicked, isInventoryRunning: " + isInventoryRunning);
       /* executor.execute(() -> */{
            if (inventoryBeingStopped) {
                Log.d(TAG, "onQuietButtonClicked: Inventory is being stopped, skipping operation");
                return;
            }
            try {
                Thread.sleep(100);
                mConnectedRfidReader.Actions.PreFilters.deleteAll();
                Thread.sleep(50);
                ArrayList<InventoryItem> items = checkedItems.getValue();
                Log.d( TAG, "onQuietButtonClicked: Items size: " + items.size());
                if (inventoryBeingStopped) {
                    Log.d(TAG, "onQuietButtonClicked: Inventory is being stopped, skipping operation");
                    return;
                }
                if(items.size()>0) {
                    setPrefilterForQuietingTags(items);
                    setTagQuietOrUnquiet(true);
                }

                //startInventory();
            } catch (Exception e) {
                preInventoryReady.postValue(false);
                e.printStackTrace();
            }
        }
        //);
    }

    public  boolean setPrefilterForQuietingTags(ArrayList<InventoryItem> tagPatternList)
            throws IllegalStateException, InvalidUsageException, OperationFailureException {
        ArrayList<InventoryItem> totalItems = inventoryItems.getValue();

        HashMap<String, InventoryItem> epcToItem = new HashMap<>();
        if (totalItems != null) {
            for (InventoryItem invItem : totalItems) {
                if (invItem.getEPC() != null) {
                    epcToItem.put(invItem.getEPC(), invItem);
                }
            }
        }
        InventoryItem[] arrayTags = tagPatternList.toArray(new InventoryItem[0]);
        int length = arrayTags.length;
        Log.d( TAG, "setPrefilter: Length of arrayTags: " + length);
        PreFilters.PreFilter[] preFiltersArray = new PreFilters.PreFilter[length];
        boolean updated = false;
        for (int i = 0; i < length; i++) {
            if (arrayTags[i] == null) {
                return false;
            } else {
                InventoryItem match = epcToItem.get(arrayTags[i].getEPC());
                if (match != null) {
                    match.setCount(-1);
                    updated = true;
                }
                arrayTags[i].setCount(-1); // Also set for tagPatternList item
                PreFilters preFilters = new PreFilters();
                PreFilters.PreFilter preFilter = preFilters.new PreFilter();
                preFilter.setAntennaID((short) 1);
                preFilter.setBitOffset(32);
                preFilter.setTagPatternBitCount(96);
                preFilter.setTagPattern(arrayTags[i].getEPC());
                preFilter.setMemoryBank(MEMORY_BANK.MEMORY_BANK_EPC);
                preFilter.setFilterAction(FILTER_ACTION.FILTER_ACTION_STATE_AWARE);
                preFilter.StateAwareAction.setTarget(TARGET.TARGET_INVENTORIED_STATE_S3);
                preFilter.StateAwareAction.setStateAwareAction(STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_B);
                preFiltersArray[i] = preFilter;
            }
        }
        mConnectedRfidReader.Actions.PreFilters.add(preFiltersArray, null);

        if (updated && totalItems != null) {
            inventoryItems.postValue(new ArrayList<>(totalItems));
        }
        return true;
    }


    public void postInventoryItems(ArrayList<InventoryItem> items) {
        inventoryItems.postValue(items);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
