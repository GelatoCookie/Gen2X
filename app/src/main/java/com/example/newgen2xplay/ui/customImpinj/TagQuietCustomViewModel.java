package com.example.newgen2xplay.ui.customImpinj;

import static com.example.newgen2xplay.RFIDHandler.impinjExtensions;
import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.newgen2xplay.RFIDHandler;
import com.zebra.rfid.api3.ENUM_TAGQUIET_MASK;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.STATE_AWARE_ACTION;
import com.zebra.rfid.api3.TARGET;

import java.util.ArrayList;
import java.util.List;

public class TagQuietCustomViewModel extends ViewModel {
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> removeResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteAllResult = new MutableLiveData<>();

    public LiveData<Boolean> getSaveResult() { return saveResult; }
    public LiveData<Boolean> getRemoveResult() { return removeResult; }
    public LiveData<Boolean> getDeleteAllResult() { return deleteAllResult; }

    public void saveTagQuiet(String mask1, String mask2, String mask3, int target, String action) {
        if (mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()) {
            saveResult.setValue(false);
            return;
        }
        List<ENUM_TAGQUIET_MASK> maskList = new ArrayList<>();
        try {
            ENUM_TAGQUIET_MASK m1 = ENUM_TAGQUIET_MASK.fromString(mask1);
            if (m1 != null) maskList.add(m1);
        } catch (IllegalArgumentException ignored) {}

        try {
            ENUM_TAGQUIET_MASK m2 = ENUM_TAGQUIET_MASK.fromString(mask2);
            if (m2 != null) maskList.add(m2);
        } catch (IllegalArgumentException ignored) {}

        try {
            ENUM_TAGQUIET_MASK m3 = ENUM_TAGQUIET_MASK.fromString(mask3);
            if (m3 != null) maskList.add(m3);
        } catch (IllegalArgumentException ignored) {}

        ENUM_TAGQUIET_MASK[] masks = maskList.toArray(new ENUM_TAGQUIET_MASK[0]);
        TARGET target1 = TARGET.getTarget(target);
        STATE_AWARE_ACTION stateAwareAction = RFIDHandler.getStateAwareActionFromString(action);

        try {
            impinjExtensions.setTagQuiet(masks, target1, stateAwareAction, (short) 1);
        } catch (OperationFailureException | IllegalStateException | InvalidUsageException e) {
            e.printStackTrace();
            saveResult.postValue(false);
            return;
        }
        saveResult.setValue(true);
    }

    public void deleteAllTagQuiet() {
        if (mConnectedRfidReader == null || !mConnectedRfidReader.isConnected()) {
            deleteAllResult.setValue(false);
            return;
        }
        try {
            mConnectedRfidReader.Actions.PreFilters.deleteAll();
        }catch (OperationFailureException | IllegalStateException | InvalidUsageException e) {
            e.printStackTrace();
            deleteAllResult.setValue(false);
            return;
        }
        deleteAllResult.setValue(true);
    }

    public void resetSaveResult() {
        saveResult.setValue(null);
    }
    public void resetRemoveResult() {
        removeResult.setValue(null);
    }
    public void resetDeleteAllResult() {
        deleteAllResult.setValue(null);
    }
}