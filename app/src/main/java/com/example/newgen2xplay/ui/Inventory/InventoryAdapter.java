package com.example.newgen2xplay.ui.Inventory;



import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import com.example.newgen2xplay.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import android.widget.Toast;
public class InventoryAdapter extends ArrayAdapter<InventoryItem> {

    ArrayList<InventoryItem> items;
    private HashSet<String> checkedEPCs = new HashSet<>();

    private OnCheckedChangeListener checkedChangeListener;
    private double avgPerSec = 0.0;
    private long lastAvgCalculationTime = 0;
    private static final long AVG_CALCULATION_INTERVAL = 100;
    private boolean isProgrammaticCheck = false;
    public interface OnCheckedChangeListener {
        void onCheckedCountChanged(int checkedCount);
    }
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.checkedChangeListener = listener;
    }
    public HashSet<String> getCheckedEPCs() {
        return checkedEPCs;
    }

    public InventoryAdapter(@NonNull Context context, ArrayList<InventoryItem> items) {
        super(context, 0, items);
        this.items = items;
    }

    private String focusLockedEPC = null;
    private boolean allCheckboxesEnabled = true;

    private final java.util.Map<String, Long> itemStartTimes = new java.util.HashMap<>();
    private boolean timersActive = true;

    private long sessionStartTime = 0;

    private String determineColorCategory(InventoryItem item) {
        long now = System.currentTimeMillis();
        if (now - lastAvgCalculationTime >= AVG_CALCULATION_INTERVAL) {
            recalculateAvgPerSec();
        }
        if (item != null && item.getCount() <= 0) {
            long sinceSessionStart = now - (sessionStartTime > 0 ? sessionStartTime : item.getFirstSeenTimestampMillis());
            if (sinceSessionStart >= 100 && item.getCount() == -1) {
                return "yellow";
            } else {
                return "white";
            }
        } else if (item != null) {
            long elapsed;
            if (timersActive) {
                elapsed = now - item.getFirstSeenTimestampMillis();
            } else if (item.getLastSeenTimestampMillis() > 0) {
                elapsed = item.getLastSeenTimestampMillis() - item.getFirstSeenTimestampMillis();
            } else {
                elapsed = now - item.getFirstSeenTimestampMillis();
            }
            double perSec = 0.0;
            if (item.getFirstSeenTimestampMillis() > 0 && item.getCount() > 0 && elapsed >= 1000) {
                perSec = ((double) item.getCount() / (elapsed / 1000.0));
            }
            if (perSec >= avgPerSec) {
                return "green";
            } else {
                return "grey";
            }
        } else {
            return "white";
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.inventory_list_item, parent, false);
        }
        TextView tagDataEpc = convertView.findViewById(R.id.tag_data_epc);

        TextView tagRSSI = convertView.findViewById(R.id.tag_rssi);
        TextView tagCount = convertView.findViewById(R.id.tag_seen_count);
        CheckBox checkBox = convertView.findViewById(R.id.item_checkbox);
        TextView readPerSec = convertView.findViewById(R.id.read_per_sec);


        InventoryItem item = getItem(position);
        View rowView = convertView;
        // Use centralized color logic
        String colorCategory = determineColorCategory(item);
        switch (colorCategory) {
            case "yellow":
                rowView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.quiet_yellow));
                break;
            case "green":
                rowView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.more_lighter_green));
                break;
            case "grey":
                rowView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.lighter_grey));
                break;
            case "white":
            default:
                rowView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(getContext(), R.color.white));
                break;
        }

        if (item != null && item.getEPC() != null) {
            tagDataEpc.setVisibility(View.VISIBLE);
            tagDataEpc.setText(String.format("%s", item.getEPC()));
        } else {
            tagDataEpc.setVisibility(View.GONE);
        }

        if (item != null && item.getRSSI() != 0) {
            tagRSSI.setVisibility(View.VISIBLE);
            tagRSSI.setText(String.format("%s", item.getRSSI()));
        } else {
            tagRSSI.setVisibility(View.GONE);
        }
        // Always show count field, even if zero
        tagCount.setVisibility(View.VISIBLE);
        tagCount.setText(String.format("%s", item != null ? item.getCount() : 0));
        // Set timer/elapsed time if available
        if (item != null) {
            long now = System.currentTimeMillis();
            long elapsed;
            if (timersActive) {
                elapsed = now - item.getFirstSeenTimestampMillis();
            } else if (item.getLastSeenTimestampMillis() > 0) {
                elapsed = item.getLastSeenTimestampMillis() - item.getFirstSeenTimestampMillis();
            } else {
                elapsed = now - item.getFirstSeenTimestampMillis();
            }
            double perSec = 0.0;
            // Only calculate if elapsed >= 1000ms and count > 0
            if (item.getFirstSeenTimestampMillis() > 0 && item.getCount() > 0 && elapsed >= 1000) {
                perSec = ((double) item.getCount() / (elapsed / 1000.0));
            }
            readPerSec.setVisibility(View.VISIBLE);
            readPerSec.setText(String.format(java.util.Locale.US, "%.2f", perSec));
           // android.util.Log.d("InventoryAdapter", "[getView] EPC: " + item.getEPC() + ", count: " + item.getCount() + ", elapsed(ms): " + elapsed + ", perSec: " + perSec + ", firstSeen: " + item.getFirstSeenTimestampMillis() + ", now: " + now);
        } else {
            readPerSec.setVisibility(View.VISIBLE);
            readPerSec.setText("0.00");
        }
        // Checkbox logic
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(item != null && checkedEPCs.contains(item.getEPC()));
        // Focus mode: only the focusLockedEPC is enabled
        if (focusLockedEPC != null) {
            checkBox.setEnabled(item.getEPC().equals(focusLockedEPC));
        } else {
            checkBox.setEnabled(allCheckboxesEnabled && (checkedEPCs.contains(item.getEPC()) || checkedEPCs.size() < 32));
        }
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isProgrammaticCheck) return;
         //   Log.d("InventoryItem","Adapter Check change listener called for item: " + item.getEPC() + ", isChecked: " + isChecked + ", position: " + position);
           /* if (focusLockedEPC != null) {
                // Only allow checking/unchecking the focusLockedEPC
                if (!item.getEPC().equals(focusLockedEPC)) {
                    buttonView.setChecked(false);
                    return;
                }
                if (!isChecked) {
                    checkedEPCs.remove(item.getEPC());
                    focusLockedEPC = null;
                    allCheckboxesEnabled = true;
                    notifyDataSetChanged();
                } else {
                    checkedEPCs.clear();
                    checkedEPCs.add(item.getEPC());
                }
                if (checkedChangeListener != null) checkedChangeListener.onCheckedCountChanged(checkedEPCs.size());
                if (itemCheckedChangeListener != null) {
                    itemCheckedChangeListener.onItemCheckedChanged(item, isChecked, position);
                }
                return;
            }*/
            if (isChecked) {
                if (checkedEPCs.size() < 32) {
                    checkedEPCs.add(item.getEPC());
                } else {
                    buttonView.setChecked(false);
                    android.widget.Toast.makeText(getContext(), "You can select only 31 items.", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                checkedEPCs.remove(item.getEPC());
            }
           // notifyDataSetChanged();
            if (checkedChangeListener != null) checkedChangeListener.onCheckedCountChanged(checkedEPCs.size());
            if (itemCheckedChangeListener != null) {
                itemCheckedChangeListener.onItemCheckedChanged(item, isChecked, position);
            }
        });

        return convertView;
    }

    // Call this when inventory stops
    public void stopAllTimers() {
        timersActive = false;
        long stopTime = System.currentTimeMillis();
        for (InventoryItem item : items) {
            if (item.getFirstSeenTimestampMillis() > 0) {
                long elapsed = stopTime - item.getFirstSeenTimestampMillis();
               // android.util.Log.d("InventoryTimer", "Item: " + item.getEPC() + " appeared at: " + item.getFirstSeenTimestampMillis() + ", stopped at: " + stopTime + ", elapsed: " + elapsed + " ms");
            }
        }
    }
    // Call this when inventory starts
    public void startAllTimers() {
        timersActive = true;
    }
    // Track the start of a new session
    public void resetCountsForNewSession() {
        sessionStartTime = System.currentTimeMillis();
        for (InventoryItem item : items) {
            // Only reset count if it's not -1 (preserve quieted tags)
            if (item.getCount() != -1) {
                item.setCount(0);
            }
            item.setFirstSeenTimestampMillis(sessionStartTime);
            item.setLastSeenTimestampMillis(-1);
            item.setLastReaderSeenCount(0);
            //android.util.Log.d("Nikhil", "[resetCountsForNewSession] EPC: " + item.getEPC() + ", count reset to 0, sessionStartTime: " + sessionStartTime);
        }
        notifyDataSetChanged();
    }

    // Clear all items and reset selections
    public void clearAll() {
        items.clear();
        checkedEPCs.clear();
        focusLockedEPC = null;
        allCheckboxesEnabled = true;
        notifyDataSetChanged();
    }

    public interface FocusListener extends OnCheckedChangeListener {
        boolean isFocusMode();
    }

    public interface OnItemCheckedChangeListener {
        void onItemCheckedChanged(InventoryItem item, boolean isChecked, int position);
    }
    private OnItemCheckedChangeListener itemCheckedChangeListener;
    public void setOnItemCheckedChangeListener(OnItemCheckedChangeListener listener) {
        this.itemCheckedChangeListener = listener;
    }

    public void uncheckAllExcept(String epc) {
        checkedEPCs.clear();
        if (epc != null) checkedEPCs.add(epc);
        focusLockedEPC = epc;
        allCheckboxesEnabled = false;
        notifyDataSetChanged();
    }

    public void uncheckAll() {
        checkedEPCs.clear();
        focusLockedEPC = null;
        allCheckboxesEnabled = true;
        notifyDataSetChanged();
    }

    public void setAllCheckboxesEnabled(boolean enabled) {
        allCheckboxesEnabled = enabled;
        if (enabled) focusLockedEPC = null;
        notifyDataSetChanged();
    }

    public void setCheckboxesEnabledExcept(String epc) {
        focusLockedEPC = epc;
        allCheckboxesEnabled = false;
        notifyDataSetChanged();
    }

    public boolean checkS3Bbit(String tid) {
        if (tid == null || tid.length() < 34) return false;
        String tidHex = tid.substring(32, 34);
        Log.d("InventoryAdapter", "checkS3Bbit: tidHex = " + tidHex);
        int tidInt = Integer.parseInt(tidHex, 16);

        return ((tidInt >> 2) & 1) == 1;
    }
    // Helper to get checked count
    public int getCheckedCount() {
        return checkedEPCs.size();
    }

    // Method to calculate the average read/sec
    private void recalculateAvgPerSec() {
        double sum = 0.0;
        int count = 0;
        long now = System.currentTimeMillis();

        for (InventoryItem item : items) {
            long elapsed;
            if (timersActive) {
                elapsed = now - item.getFirstSeenTimestampMillis();
            } else if (item.getLastSeenTimestampMillis() > 0) {
                elapsed = item.getLastSeenTimestampMillis() - item.getFirstSeenTimestampMillis();
            } else {
                elapsed = now - item.getFirstSeenTimestampMillis();
            }

            if (item.getFirstSeenTimestampMillis() > 0 && item.getCount() > 0 && elapsed >= 1000) {
                double perSec = ((double) item.getCount() / (elapsed / 1000.0));
                sum += perSec;
                count++;
            }
        }

        avgPerSec = (count > 0) ? Math.round(sum / count) : 0.0; // round to whole number
        lastAvgCalculationTime = now;
      //  Log.d("InventoryAdapter", "Recalculated average read/sec: " + avgPerSec);
    }


    public double getAvgPerSec() {
        return avgPerSec;
    }

    public ColorCounts getColorCounts() {
        int yellow = 0, green = 0, grey = 0, white = 0;
        for (InventoryItem item : items) {
            String cat = determineColorCategory(item);
            switch (cat) {
                case "yellow": yellow++; break;
                case "green": green++; break;
                case "grey": grey++; break;
                case "white": white++; break;
            }
        }
        return new ColorCounts(yellow, green, grey, white);
    }


    public static class ColorCounts {
        public final int yellow, green, grey, white;
        public ColorCounts(int yellow, int green, int grey, int white) {
            this.yellow = yellow;
            this.green = green;
            this.grey = grey;
            this.white = white;
        }
    }

    public void selectUpTo30ValidItems() {
        Log.d("InventoryAdapter", "selectUpTo30ValidItems called, isProgrammaticCheck: " + isProgrammaticCheck);
        isProgrammaticCheck = true;
        //items.clear();
        checkedEPCs.clear();
        int selected = 0;
        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            if (item.getCount() > -1) {
                /*String getColor = determineColorCategory(item);
                if(getColor.equals("green"))*/{
                    checkedEPCs.add(item.getEPC());
                    if (itemCheckedChangeListener != null) {
                        itemCheckedChangeListener.onItemCheckedChanged(item, true, i);
                    }
                    selected++;
                    if (selected >= 31) break;
                }
            }
        }
        notifyDataSetChanged();
        isProgrammaticCheck = false;
    }

    // Method to calculate and log average read rate for green and grey tags
    public void logGreenGreyAvgReadRates() {
        double greenSum = 0.0;
        int greenCount = 0;
        double greySum = 0.0;
        int greyCount = 0;
        long now = System.currentTimeMillis();
        for (InventoryItem item : items) {
            long elapsed;
            if (timersActive) {
                elapsed = now - item.getFirstSeenTimestampMillis();
            } else if (item.getLastSeenTimestampMillis() > 0) {
                elapsed = item.getLastSeenTimestampMillis() - item.getFirstSeenTimestampMillis();
            } else {
                elapsed = now - item.getFirstSeenTimestampMillis();
            }
            if (item.getFirstSeenTimestampMillis() > 0 && item.getCount() > 0 && elapsed >= 1000) {
                double perSec = ((double) item.getCount() / (elapsed / 1000.0));
                String color = determineColorCategory(item);
                if ("green".equals(color)) {
                    greenSum += perSec;
                    greenCount++;
                } else if ("grey".equals(color)) {
                    greySum += perSec;
                    greyCount++;
                }
            }
        }
        double greenAvg = greenCount > 0 ? greenSum / greenCount : 0.0;
        double greyAvg = greyCount > 0 ? greySum / greyCount : 0.0;
        Log.d("InventoryAdapter", String.format("Green Avg Read/sec: %.2f, Grey Avg Read/sec: %.2f", greenAvg, greyAvg));
    }
}
