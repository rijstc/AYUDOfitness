package com.example.ayudofitness;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

public class BluetoothLEService extends BluetoothGattCallback {
    private static final String TAG = "debugging";

    protected BluetoothGatt bluetoothGatt;
    NotifyListener disconnectedListener = null;
    private ActionCallback currentCallback;
    private HashMap<UUID, NotifyListener> notifyListeners = new HashMap<UUID, NotifyListener>();

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            bluetoothGatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            bluetoothGatt.close();
            if (disconnectedListener != null) {
                disconnectedListener.onNotify(null);
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onSuccess(null);
        } else {
            onFail(status, "onServicesDiscovered failed!");
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onSuccess(characteristic);
        } else {
            onFail(status, "onCharacteristicRead failed!");
        }
    }


    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onSuccess(characteristic);
        } else {
            onFail(status, "onCharacteristicWrite failed!");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (notifyListeners.containsKey(characteristic.getUuid())) {
            notifyListeners.get(characteristic.getUuid()).onNotify(characteristic.getValue());
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            onSuccess(rssi);
        } else {
            onFail(status, "onReadRemoteRssi failed!");
        }
    }


    public void connect(final Context context, BluetoothDevice device, final ActionCallback actionCallback) {
        this.currentCallback = actionCallback;
        device.connectGatt(context, false, this);
    }

    public void setDisconnectedListener(NotifyListener disconnectedListener) {
        this.disconnectedListener = disconnectedListener;
    }

    public BluetoothDevice getDevice() {
        if (bluetoothGatt == null) {
            Log.d(TAG, "Not connected to MiBand");
            return null;
        }
        return bluetoothGatt.getDevice();
    }

    public void writeReadChar(final UUID characteristicUUID, byte[] value, final ActionCallback actionCallback) {
        ActionCallback readCallback = new ActionCallback() {
            @Override
            public void onSuccess(Object data) {
                readCharacteristic(UUIDs.SERVICE_BASIC, characteristicUUID, actionCallback);
            }

            @Override
            public void onFail(int errorCode, String msg) {
                actionCallback.onFail(errorCode, msg);
            }
        };
        writeCharacteristic(UUIDs.SERVICE_BASIC, characteristicUUID, value, readCallback);
    }

    private void readCharacteristic(UUID serviceUUID, UUID characteristicUUID, ActionCallback actionCallback) {
        try {
            if (bluetoothGatt == null) {
                Log.d(TAG, "not connected to MiBand");
                throw new Exception("Not connected to MiBand. Connect first.");
            }
            currentCallback = actionCallback;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
            if (bluetoothGattCharacteristic == null) {
                onFail(-1, "BluetoothGattCharacteristic " + bluetoothGattCharacteristic + " does not exist.");
                return;
            }
            if (bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic) == false) {
                onFail(-1, "writeCharacteristic returns false.");
            }
        } catch (Throwable tr) {
            Log.d(TAG, "writeCharacteristic ", tr);
            onFail(-1, tr.getMessage());
        }
    }

    public void writeCharacteristic(UUID serviceUUID, UUID characteristicUUID, byte[] value, ActionCallback actionCallback) {
        try {
            if (bluetoothGatt == null) {
                Log.d(TAG, "not connected to MiBand");
                throw new Exception("Not connected to MiBand. Connect first.");
            }
            currentCallback = actionCallback;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
            if (bluetoothGattCharacteristic == null) {
                onFail(-1, "BluetoothGattCharacteristic " + bluetoothGattCharacteristic + " does not exist.");
                return;
            }
            bluetoothGattCharacteristic.setValue(value);
            if (bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic) == false) {
                onFail(-1, "readCharacteristic returns false.");
            }
        } catch (Throwable tr) {
            Log.d(TAG, "readCharacteristic ", tr);
            onFail(-1, tr.getMessage());
        }
    }

    public void readRssi(ActionCallback actionCallback) {
        try {
            if (bluetoothGatt == null) {
                Log.d(TAG, "not connected to MiBand");
                throw new Exception("Not connected to MiBand. Connect first.");
            }
            currentCallback = actionCallback;
            bluetoothGatt.readRemoteRssi();
        } catch (Throwable tr) {
            Log.d(TAG, "readRssi ", tr);
            onFail(-1, tr.getMessage());
        }
    }

    public void setNotifyListener(UUID serviceUUID, UUID characteristicUUID, NotifyListener notifyListener) {
        if (bluetoothGatt == null) {
            Log.d(TAG, "not connected to MiBand");
            return;
        }
        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGatt.getService(serviceUUID).getCharacteristic(characteristicUUID);
        if (bluetoothGattCharacteristic == null) {
            Log.d(TAG, "characteristicId " + characteristicUUID.toString() + " not found in service " + serviceUUID.toString());
            return;
        }
        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
        BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUIDs.DESCRIPTOR_HEART_RATE);
        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
        notifyListeners.put(characteristicUUID, notifyListener);
    }

    private void onFail(int errorCode, String msg) {
        if (this.currentCallback != null) {
            ActionCallback callback = this.currentCallback;
            this.currentCallback = null;
            callback.onFail(errorCode, msg);
        }
    }

    private void onSuccess(Object data) {
        if (this.currentCallback != null) {
            ActionCallback callback = this.currentCallback;
            this.currentCallback = null;
            callback.onSuccess(data);
        }
    }
}





