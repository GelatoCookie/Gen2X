package com.example.newgen2xplay.ui.Singulation;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.SL_FLAG;

import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

public class SingulationViewModel extends ViewModel {
    private final MutableLiveData<Boolean> singulationResult = new MutableLiveData<>();
    public LiveData<Boolean> getSingulationResult() {
        return singulationResult;
    }

    public void setSingulation(boolean isSuccess) {
        singulationResult.setValue(isSuccess);
    }

    public void setSingulationControl(int antennaIndex, int sessionIndex, int tagPopulation, int inventoryStateIndex, int slFlagIndex) {
        new Thread(() -> {
            boolean isSuccess = false;
            try {
                Antennas.SingulationControl singulationControl = mConnectedRfidReader.Config.Antennas.getSingulationControl(antennaIndex);
                singulationControl.setSession(SESSION.GetSession(sessionIndex));
                singulationControl.setTagPopulation((short) tagPopulation);
                singulationControl.Action.setInventoryState(INVENTORY_STATE.GetInventoryState(inventoryStateIndex));
                singulationControl.Action.setSLFlag(SL_FLAG.GetSLFlag(slFlagIndex));

                mConnectedRfidReader.Config.Antennas.setSingulationControl(antennaIndex, singulationControl);
                isSuccess = true;
            } catch (InvalidUsageException | OperationFailureException e) {
                isSuccess = false;
            }
            singulationResult.postValue(isSuccess);
        }).start();
    }

    public void resetSaveResult() {
        singulationResult.setValue(null);
    }
}
