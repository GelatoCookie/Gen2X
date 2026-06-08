package com.example.newgen2xplay.ui.Filters;

import static com.example.newgen2xplay.RFIDHandler.mConnectedRfidReader;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zebra.rfid.api3.PreFilters;

public class PreFiltersViewModel extends ViewModel {
    private final MutableLiveData<Boolean> preFilterResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteAllResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deletePrefilter = new MutableLiveData<>();

    public LiveData<Boolean> getPreFilterResult() {
        return preFilterResult;
    }
    public LiveData<Boolean> getDeleteAllResult() {
        return deleteAllResult;
    }

    public MutableLiveData<Boolean> getDeletePrefilter() {
        return deletePrefilter;
    }

    public void savePreFilter(PreFilters.PreFilter filter) {
        new Thread(() -> {
            boolean isSuccess = false;
            try {
                mConnectedRfidReader.Actions.PreFilters.add(filter);
                isSuccess = true;
            } catch (Exception e) {
                isSuccess = false;
            }
            preFilterResult.postValue(isSuccess);
        }).start();
    }

    public void deletePreFilter(int index){
        new Thread(() -> {
            boolean isSuccess = false;
            try {
                int length = mConnectedRfidReader.Actions.PreFilters.length();
                if (length > 0 && index <= length) {
                    PreFilters.PreFilter filter = mConnectedRfidReader.Actions.PreFilters.getPreFilter(index);
                    mConnectedRfidReader.Actions.PreFilters.delete(filter);
                    isSuccess = true;
                }
            } catch (Exception e) {
                isSuccess = false;
            }
            deletePrefilter.postValue(isSuccess);
        }).start();
    }

    public void deleteAllPreFilter() {
        new Thread(() -> {
            boolean isSuccess = false;
            try {
                mConnectedRfidReader.Actions.PreFilters.deleteAll();
                isSuccess = true;
            } catch (Exception e) {
                isSuccess = false;
            }
            deleteAllResult.postValue(isSuccess);
        }).start();
    }

    public void resetPreFilterResult() {
        preFilterResult.setValue(null);
    }
    public void resetDeleteAllResult() {
        deleteAllResult.setValue(null);
    }
    public void resetDeletePrefilter() {
        deletePrefilter.setValue(null);
    }
}
