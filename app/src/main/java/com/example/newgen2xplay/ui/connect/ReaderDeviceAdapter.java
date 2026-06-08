package com.example.newgen2xplay.ui.connect;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newgen2xplay.R;
import com.example.newgen2xplay.RFIDHandler;
import com.zebra.rfid.api3.ReaderDevice;
import java.util.List;

public class ReaderDeviceAdapter extends RecyclerView.Adapter<ReaderDeviceAdapter.ReaderViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(ReaderDevice device);
    }

    private List<ReaderDevice> readerList;
    private final OnItemClickListener listener;
    private String connectedReaderName = null;

    public ReaderDeviceAdapter(List<ReaderDevice> readerList, OnItemClickListener listener) {
        this.readerList = readerList;
        this.listener = listener;
    }

    public void setReaderList(List<ReaderDevice> readerList) {
        this.readerList = readerList;
        notifyDataSetChanged();
    }

    public void setConnectedReaderName(String name) {
        connectedReaderName = name;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reader_device, parent, false);
        return new ReaderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReaderViewHolder holder, int position) {
        ReaderDevice device = readerList.get(position);
        Log.d("ReaderDeviceAdapter", "Binding device: " + device.getName() + " at position: " + position+" is connected " + device.getRFIDReader().isConnected());
        holder.textViewName.setText(device.getName());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(device));

        View box = holder.itemView.findViewById(R.id.reader_box_layout);
        if (box != null) {
            if (connectedReaderName != null && device.getName().equals(connectedReaderName)) {
                holder.textViewStatus.setText( "Connected");
                box.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_light));
            } else {
                holder.textViewStatus.setText( "Disconnected");
                box.setBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.white));
            }
        }
    }

    @Override
    public int getItemCount() {
        return readerList != null ? readerList.size() : 0;
    }

    static class ReaderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewStatus;
        CardView cardView;
        ReaderViewHolder(View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewReaderName);
            textViewStatus = itemView.findViewById(R.id.textViewReaderStatus);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
