package com.example.newgen2xplay.ui.connect;

import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import android.util.Log;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.newgen2xplay.RFIDHandler;
import com.google.android.material.snackbar.Snackbar;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.ReaderDevice;

import java.util.List;

public class ConnectViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<List<ReaderDevice>> readersLiveData = new MutableLiveData<>();
    private boolean hasRefreshed = false;

   // private final String TAG = "CONNECT_VIEW_MODEL";
    public ConnectViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is connect fragment");
       // refreshReaders();
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<List<ReaderDevice>> getReaders() {
        return readersLiveData;
    }

    public void refreshReaders(View v) {
        try {
            if(mConnectedRfidReader != null && mConnectedRfidReader.isConnected()){
                Snackbar.make(v, "Reader is connected. Please disconnect the reader before refreshing the list.", Snackbar.LENGTH_SHORT).show();
            }else{
                RFIDHandler.tryGetAvailableReaders();
                if (RFIDHandler.availableRFIDReaderList != null) {
                    Log.d(RFIDHandler.TAG, "RFIDHandler.availableRFIDReaderList size: " + RFIDHandler.availableRFIDReaderList.size());
                    if (!RFIDHandler.availableRFIDReaderList.isEmpty()) {
                        for (int i = 0; i < RFIDHandler.availableRFIDReaderList.size(); i++) {
                            Log.d(RFIDHandler.TAG, "Reader[" + i + "]: " + RFIDHandler.availableRFIDReaderList.get(i).getName());
                        }
                    }
                    readersLiveData.setValue(RFIDHandler.availableRFIDReaderList);
                } else {
                    Log.d(RFIDHandler.TAG, "RFIDHandler.availableRFIDReaderList is not initialized");
                }
            }

        } catch (InvalidUsageException e) {
            Log.d(RFIDHandler.TAG, "Error in getting available readers: " + e.getMessage() + " " + e.getVendorMessage());
            e.printStackTrace();
        }
    }
    public boolean hasRefreshed() {
        return hasRefreshed;
    }

}