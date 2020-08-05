package com.example.ayudofitness;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BluetoothLEService extends Service {
    private static final String TAG = "debugging";

    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private String connectedDeviceAdress;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback gattCallback;
    private int connectionState = DISCONNECTED;

    public BluetoothLEService(Context context) {
        this.context = context;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Check if BluetoothAdapter is available. If not, get Adapter and recheck. If available return true.
    public boolean hasBleService() {
        if (bluetoothAdapter == null) {
            bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                    .getAdapter();
            if (bluetoothAdapter == null) {
                return false;
            }
        }
        return true;
    }

    //Connect to GattServer
    public boolean connect(String deviceAddress) {
        if (bluetoothAdapter == null || deviceAddress == null) {
            return false;
        }

        if (bluetoothGatt != null && connectedDeviceAdress != null && deviceAddress.equals(connectedDeviceAdress)) {
            Log.d(TAG, "already existing connection");
            if (bluetoothGatt.connect()) {
                connectionState = CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        Log.d(TAG, deviceAddress);
        if (bluetoothDevice == null) {
            return false;
        }

        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
        Log.d(TAG, "try to connect");
        connectedDeviceAdress = deviceAddress;
        connectionState = CONNECTING;
        return true;
    }

    public class LocalBinder extends Binder {
        public BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }
}





