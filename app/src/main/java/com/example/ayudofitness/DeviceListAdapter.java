package com.example.ayudofitness;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

class DeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> deviceList;
    private Context context;
    private BluetoothDevice device;
    private String deviceName;

    public DeviceListAdapter(Context context) {
        super();
        deviceList = new ArrayList<>();
        this.context = context;
    }

    @Override
    public int getCount() {
        return deviceList.size();
    }

    @Override
    public Object getItem(int position) {
        return deviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Device device;
        if (convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.devicelist, null);
            device = new Device();
            device.deviceName = convertView.findViewById(R.id.deviceName);
            device.deviceAdress = convertView.findViewById(R.id.deviceAdress);
            convertView.setTag(device);
        } else {
            device = (Device) convertView.getTag();
        }
        this.device = deviceList.get(position);
        deviceName = this.device.getName();
        if (deviceName!=null && deviceName.length()>0){
            device.deviceName.setText(deviceName);
        } else {
            device.deviceName.setText(R.string.unknownDevice);
        }
        device.deviceAdress.setText(this.device.getAddress());
        return convertView;
    }

    public void addDevice(BluetoothDevice device) {
        if (!deviceList.contains(device)) {
            deviceList.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return deviceList.get(position);
    }

    public void clear() {
        deviceList.clear();
    }
}
