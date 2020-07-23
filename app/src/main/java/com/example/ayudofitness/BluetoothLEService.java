package com.example.ayudofitness;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Binder;

public class BluetoothLEService {

    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private String connectedDeviceAdress;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback gattCallback;
    private int connectionState = DISCONNECTED;

    public class LocalBinder extends Binder {
        BluetoothLEService getService() {
            return BluetoothLEService.this;
        }

        public boolean connect(Context context, String deviceAdress) {
            if (bluetoothAdapter == null || deviceAdress == null) {
                return false;
            }

            if (bluetoothGatt != null && connectedDeviceAdress != null && deviceAdress.equals(connectedDeviceAdress)) {
                if (bluetoothGatt.connect()) {
                    connectionState = CONNECTING;
                    return true;
                } else {
                    return false;
                }
            }

            bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAdress);
            if (bluetoothDevice == null) {
                return false;
            }

            // bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
            connectedDeviceAdress = deviceAdress;
            connectionState = CONNECTING;
            return true;
        }
    }
}

