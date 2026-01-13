package com.example.mdnsdevicediscovery.mdnsdiscoverydataManagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mdnsdevicediscovery.R;
import java.util.ArrayList;
import java.util.List;

public class DeviceDetailAdapter extends RecyclerView.Adapter<DeviceDetailAdapter.DeviceViewHolder> {

    private List<DeviceDetail> devices;
    private OnDeviceClickListener listener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceDetail device);
    }

    public DeviceDetailAdapter(OnDeviceClickListener listener) {
        this.devices = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_detail, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceDetail device = devices.get(position);
        holder.bind(device);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDevices(List<DeviceDetail> newDevices) {
        devices.clear();
        devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    public void addDevice(DeviceDetail device) {
        if (!devices.contains(device)) {
            devices.add(device);
            notifyItemInserted(devices.size() - 1);
        }
    }

    public void setDeviceOnline(String ip, boolean online) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getIp().equals(ip)) {
                devices.get(i).setOnline(online);
                notifyItemChanged(i);
                break;
            }
        }
    }

    // ViewHolder class
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView ipText;
        private ImageView statusIcon;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.device_name);
            ipText = itemView.findViewById(R.id.device_ip);
            statusIcon = itemView.findViewById(R.id.status_icon);
        }

        public void bind(DeviceDetail device) {
            nameText.setText(device.getName());
            ipText.setText(device.getIp());

            if (device.isOnline()) {
                statusIcon.setImageResource(android.R.drawable.presence_online);
                statusIcon.setColorFilter(android.graphics.Color.GREEN);
            } else {
                statusIcon.setImageResource(android.R.drawable.presence_offline);
                statusIcon.setColorFilter(android.graphics.Color.GRAY);
            }
        }
    }

}
