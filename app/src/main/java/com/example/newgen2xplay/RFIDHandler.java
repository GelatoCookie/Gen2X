package com.example.newgen2xplay;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;

import com.example.newgen2xplay.ui.Inventory.TagDataViewModel;
import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.AccessFilter;
import com.zebra.rfid.api3.AntennaInfo;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.CableLossCompensation;
import com.zebra.rfid.api3.ENUM_OPERATING_MODE;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.FILTER_ACTION;
import com.zebra.rfid.api3.FILTER_MATCH_PATTERN;
import com.zebra.rfid.api3.GPITrigger;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.IRFIDLogger;
import com.zebra.rfid.api3.ImpinjExtensions;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.LOCK_DATA_FIELD;
import com.zebra.rfid.api3.LOCK_PRIVILEGE;
import com.zebra.rfid.api3.MEMORY_BANK;
import com.zebra.rfid.api3.MultiLocateParams;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.PreFilters;
import com.zebra.rfid.api3.READER_POWER_STATE;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.RFIDResults;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATE_AWARE_ACTION;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.StartTrigger;
import com.zebra.rfid.api3.StopTrigger;
import com.zebra.rfid.api3.TARGET;
import com.zebra.rfid.api3.TagAccess;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TagPatternBase;
import com.zebra.rfid.api3.TriggerInfo;
import com.zebra.scannercontrol.DCSScannerInfo;
import com.zebra.scannercontrol.FirmwareUpdateEvent;
import com.zebra.scannercontrol.IDcsSdkApiDelegate;
import com.zebra.scannercontrol.SDKHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RFIDHandler implements IDcsSdkApiDelegate, Readers.RFIDReaderEventHandler{

    public final static String TAG = "RFID_SAMPLE";
    public static Readers readers;
    public static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    public static RFIDReader mConnectedRfidReader;
    TextView textView;
    private static EventHandler eventHandler;
    private MainActivity context;
    String readername = "RFD40+_212735201D0086";
    private SDKHandler sdkHandler;

    public static boolean isInventoryRunning = false;

    public static TagDataViewModel tagDataViewModel;

    public static ImpinjExtensions impinjExtensions;

    public static ArrayList<String> tagIDs = new ArrayList<>();


    public final MutableLiveData<Boolean> connectionStatus = new MutableLiveData<>();
    public final MutableLiveData<Boolean> isInitialized = new MutableLiveData<>(false);
    // LiveData to notify trigger press/release events
    public final MutableLiveData<Boolean> triggerPressedLiveData = new MutableLiveData<>();

    void onCreate(MainActivity activity) {
        context = activity;
        InitSDK();
    }

    private void InitSDK() {
        Log.d(TAG, "InitSDK");
        IRFIDLogger.getLogger("RFIDHandler").EnableDebugLogs(true);
        if (readers == null) {
            setTransportType();
        }
        if (eventHandler == null)
            eventHandler = new EventHandler();

    }

    private void setTransportType() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Log.d(TAG, "CreateInstanceTask");
            readers = new Readers(context, ENUM_TRANSPORT.ALL);
            readers.setTransport(ENUM_TRANSPORT.ALL);
            isInitialized.postValue(true);

        });
    }

    public static void tryGetAvailableReaders() throws InvalidUsageException {
        availableRFIDReaderList =  readers.GetAvailableRFIDReaderList();
    }

    public void selectReaderDevice(ArrayList<ReaderDevice> readerList) {
        if (readerList.size() == 1) {
                  readerDevice = readerList.get(0);
                  mConnectedRfidReader = readerDevice.getRFIDReader();
        }else{
            for (ReaderDevice device : readerList) {
                Log.d(TAG,"device: "+device.getName());
                if (device.getName().startsWith(readername)) {
                    readerDevice = device;
                    mConnectedRfidReader = readerDevice.getRFIDReader();
                    break;
                }
            }
        }
    }

    // Public method to select a single reader
    public void selectReader(ReaderDevice device) {
        ArrayList<ReaderDevice> list = new ArrayList<>();
        list.add(device);
        selectReaderDevice(list);
    }

    private void showMessage(String message) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void ConfigureReader() {
        if (mConnectedRfidReader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {

                // receive events from reader
                mConnectedRfidReader.Events.addEventsListener(eventHandler);
                // HH event
                mConnectedRfidReader.Events.setHandheldEvent(true);
                // tag event with tag data
                mConnectedRfidReader.Events.setTagReadEvent(true);
                mConnectedRfidReader.Events.setAttachTagDataWithReadEvent(false);
                mConnectedRfidReader.Events.setReaderDisconnectEvent(true);

                mConnectedRfidReader.Events.setBatteryEvent(true);
                mConnectedRfidReader.Events.setFirmwareUpdateEvent(true);

                Antennas.SingulationControl s1_singulationControl = new Antennas.SingulationControl();
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.setTagPopulation((short) 200);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_AB_FLIP);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                mConnectedRfidReader.Config.Antennas.setSingulationControl(1, s1_singulationControl);

                // set trigger mode as rfid for RFD40/90 so scanner beam will not come
                mConnectedRfidReader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, false);

                // set start and stop triggers for rfd40/90
                mConnectedRfidReader.Config.setStartTrigger(triggerInfo.StartTrigger);
                mConnectedRfidReader.Config.setStopTrigger(triggerInfo.StopTrigger);

                mConnectedRfidReader.Actions.PreFilters.deleteAll();

            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
    }

    public  synchronized void disconnect() {
        Log.d(TAG, "Disconnect");
        try {
            if (mConnectedRfidReader != null) {
                if (eventHandler != null)
                    mConnectedRfidReader.Events.removeEventsListener(eventHandler);
                connectionStatus.postValue(false);
                mConnectedRfidReader.disconnect();
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void dispose() {
        disconnect();
        try {
            if (mConnectedRfidReader != null) {
                //Toast.makeText(getApplicationContext(), "Disconnecting reader", Toast.LENGTH_LONG).show();
                mConnectedRfidReader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {

    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());

        if(mConnectedRfidReader != null && mConnectedRfidReader.isConnected()){
            if (readerDevice.getName().equals(mConnectedRfidReader.getHostName()))
                disconnect();
        }

    }

    @Override
    public void dcssdkEventScannerAppeared(DCSScannerInfo dcsScannerInfo) {

    }

    @Override
    public void dcssdkEventScannerDisappeared(int i) {

    }

    @Override
    public void dcssdkEventCommunicationSessionEstablished(DCSScannerInfo dcsScannerInfo) {

    }

    @Override
    public void dcssdkEventCommunicationSessionTerminated(int i) {

    }

    @Override
    public void dcssdkEventBarcode(byte[] bytes, int i, int i1) {

    }

    @Override
    public void dcssdkEventImage(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventVideo(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventBinaryData(byte[] bytes, int i) {

    }

    @Override
    public void dcssdkEventFirmwareUpdate(FirmwareUpdateEvent firmwareUpdateEvent) {

    }

    @Override
    public void dcssdkEventAuxScannerAppeared(DCSScannerInfo dcsScannerInfo, DCSScannerInfo dcsScannerInfo1) {

    }

    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public  class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population

            TagData[] myTags = mConnectedRfidReader.Actions.getReadTags(100);
            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                   // Log.d(TAG, "Tag ID " + myTags[index].getTagID() + " Count "+ myTags[index].getTagSeenCount());

                    if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                        }
                    }
                    if (myTags[index].isContainsLocationInfo()) {
                        short dist = myTags[index].LocationInfo.getRelativeDistance();
                        short num = myTags[index].LocationInfo.getTagNumber();
                        Log.d(TAG, "Tag relative distance " + dist + " # " + num);
                    }
                }
              //  Log.d(TAG," Total Tags Read: " + myTags);
                context.runOnUiThread(() -> tagDataViewModel.setTagItems(myTags));

            }

        }

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    Log.d(TAG, "Status trigger: trigger pressed");
                    triggerPressedLiveData.postValue(true);
                    //performInventory();
                }
                if (rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent() == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    Log.d(TAG, "Status trigger: trigger release");
                    triggerPressedLiveData.postValue(false);
                    //stopInventory();
                }
            }
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.DISCONNECTION_EVENT) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {

                        disconnect();
                        return null;
                    }
                }.execute();
            }

            if(rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.FIRMWARE_UPDATE_EVENT){
                String status = rfidStatusEvents.StatusEventData.FWEventData.getStatus();
                int imageDownloadProgress = rfidStatusEvents.StatusEventData.FWEventData.getImageDownloadProgress();
                int overallUpdateProgress = rfidStatusEvents.StatusEventData.FWEventData.getOverallUpdateProgress();
                Log.d(TAG,"FW status: "+status+", idp: "+imageDownloadProgress+", ovp: "+overallUpdateProgress );
            }
            if(rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.GPI_EVENT){
                Log.d(TAG,"GPI_EVENT " );
            }
        }
    }


    public void connect(String readerName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (mConnectedRfidReader != null) {
                Log.d(TAG, "connect " + mConnectedRfidReader.getHostName());
                try {
                    if (!mConnectedRfidReader.isConnected()) {
                        mConnectedRfidReader.connect();
                        ConfigureReader();
                        if (mConnectedRfidReader.isConnected()) {
                            impinjExtensions = new ImpinjExtensions(mConnectedRfidReader);
                            Log.d(TAG, "Connection successful: " + mConnectedRfidReader.getHostName());
                            connectionStatus.postValue(true);
                        }
                    }
                } catch (InvalidUsageException | OperationFailureException e) {
                    e.printStackTrace();
                    connectionStatus.postValue(false);
                }
            }
        });
    }

    public static STATE_AWARE_ACTION getStateAwareActionFromString(String strAction) {
        STATE_AWARE_ACTION action = null;
        if (strAction.equalsIgnoreCase("INV A NOT INV B OR ASRT SL NOT DSRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_A_NOT_INV_B;
        if (strAction.equalsIgnoreCase("INV A OR ASRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_A;
        if (strAction.equalsIgnoreCase("NOT INV B OR NOT DSRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_NOT_INV_B;
        if (strAction.equalsIgnoreCase("INV A2BB2A NOT INV A OR NEG SL NOT ASRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_A2BB2A_NOT_INV_A;
        if (strAction.equalsIgnoreCase("INV B NOT INV A OR DSRT SL NOT ASRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_B_NOT_INV_A;
        if (strAction.equalsIgnoreCase("INV B OR DSRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_INV_B;
        if (strAction.equalsIgnoreCase("NOT INV A OR NOT ASRT SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_NOT_INV_A;
        if (strAction.equalsIgnoreCase("NOT INV A2BB2A OR NOT NEG SL"))
            action = STATE_AWARE_ACTION.STATE_AWARE_ACTION_NOT_INV_A2BB2A;
        return action;
    }
}
