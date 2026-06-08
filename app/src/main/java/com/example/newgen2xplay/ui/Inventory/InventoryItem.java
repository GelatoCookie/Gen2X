package com.example.newgen2xplay.ui.Inventory;

public class InventoryItem {

    private String EPC;
    private String TID;
    private String User;
    private int PC;
    private String CRC;
    private short Phase;
    private short Antenna;
    private String Channel;
    private int count;
    private int RSSI;
    private int lastReaderSeenCount = 0;

    private long firstSeenTimestampMillis = -1;
    private long lastSeenTimestampMillis = -1;

    public String getMemBankData() {
        return memBankData;
    }

    private String memBankData;

    public InventoryItem(String EPC,int count, int RSSI, String memBankData) {
        this.EPC = EPC;
        this.count = count;
        this.RSSI = RSSI;
        this.memBankData = memBankData;
        this.lastReaderSeenCount = count;
    }

    public String getEPC() {
        return EPC;
    }

    public String getTID() {
        return TID;
    }

    public String getUser() {
        return User;
    }

    public int getPC() {
        return PC;
    }

    public String getCRC() {
        return CRC;
    }

    public short getPhase() {
        return Phase;
    }

    public short getAntenna() {
        return Antenna;
    }

    public String getChannel() {
        return Channel;
    }

    public int getCount() {
        return count;
    }

    public int getRSSI() {
        return RSSI;
    }

    public int getLastReaderSeenCount() {
        return lastReaderSeenCount;
    }

    public void setFirstSeenTimestampMillis(long millis) {
        this.firstSeenTimestampMillis = millis;
    }
    public long getFirstSeenTimestampMillis() {
        return firstSeenTimestampMillis;
    }
    public void setLastSeenTimestampMillis(long millis) {
        this.lastSeenTimestampMillis = millis;
    }
    public long getLastSeenTimestampMillis() {
        return lastSeenTimestampMillis;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public void setRSSI(int rssi) {
        this.RSSI = rssi;
    }
    public void setLastReaderSeenCount(int count) {
        this.lastReaderSeenCount = count;
    }
    public void setTagSeenCount(int count) {
        this.count = count;
        this.lastReaderSeenCount = count;
    }
    public void setPeakRSSI(int rssi) {
        this.RSSI = rssi;
    }


}
