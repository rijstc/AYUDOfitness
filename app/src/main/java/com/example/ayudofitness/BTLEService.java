package com.example.ayudofitness;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.example.ayudofitness.Constants.*;

public class BTLEService extends Service {
    private static final String TAG = "debugging";

    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String deviceAdress;
    private BluetoothDevice device;

    private final IBinder iBinder = new LocalBinder();
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService miliService;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction = null;
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = ACTION_GATT_CONNECTED;
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = ACTION_GATT_DISCONNECTED;
                    gatt.close();
                }
                broadCastUpdate(intentAction);
            } else {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        gatt.readCharacteristic(characteristic);
                        if (characteristic.getUuid().equals(UUIDS.UUID_CHARACTERISTIC_PAIR)) {
                            characteristic.setValue(new byte[]{2});
                            gatt.writeCharacteristic(characteristic);
                        }
                        if (characteristic.getUuid().equals(UUIDS.UUID_CHARACTERISTIC_USER_INFO)) {
                            Log.d(TAG, "user info found");
                        }
                    }
                }
                miliService = gatt.getService(UUIDS.UUID_SERVICE_MILI_SERVICE);
            } else {
                disconnect();
                return;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            byte[] value = characteristic.getValue();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID characteristicUUID = characteristic.getUuid();
            Log.d(TAG, "durch pair aufgerufen");
            if (UUIDS.UUID_CHARACTERISTIC_PAIR.equals(characteristicUUID)) {
                handlePair(characteristic.getValue(), status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadCastUpdate(ACTION_DATA_AVAILABLE, characteristic);

        }
    };


    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case MI_BAND_CONNECT:
                    //  connect();
                    break;
            }
        }
    };

    protected BroadcastReceiver stopServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(BTLEService.this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.ACTION_MIBAND));

        registerReceiver(stopServiceReceiver, new IntentFilter("myStoppingFilter"));
    }


    private void broadCastUpdate(String actionDataAvailable, BluetoothGattCharacteristic characteristic) {
//TODO
    }

    private void broadCastUpdate(String intentAction) {
        Log.d(TAG, "broadcastupdate intentAction: " + intentAction);
        final Intent intent = new Intent(intentAction);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BTLEService getService() {
            return BTLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (bluetoothGatt != null) {
            Log.d(TAG, "onUnbind");
            // bluetoothGatt.close();
            // bluetoothGatt = null;
        }
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public boolean connect(final BluetoothDevice device) {
        this.device = device;
        String address = device.getAddress();
        if (bluetoothAdapter == null || address == null) {
            return false;
        }

        if (deviceAdress != null && address.equals(deviceAdress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use existing Gatt for connection.");
            if (bluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        // final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        deviceAdress = address;
        if (bluetoothGatt.connect()) {
        }
        return true;
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }

    public void pair() {
        Log.d(TAG, "pair started");
        if (bluetoothGatt != null) {
            BluetoothGattCharacteristic characteristic = miliService.getCharacteristic(UUIDS.UUID_CHARACTERISTIC_PAIR);
            Log.d(TAG, "pair");

            characteristic.setValue(new byte[]{2});
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            bluetoothGatt.writeCharacteristic(characteristic);
            Log.d(TAG, "pair sent");
        }
    }

    private void handlePair(byte[] value, int status) {
        if (status != GATT_SUCCESS) {
            Log.d(TAG, "pairing failed");
            return;
        }

        if (value != null) {
            if (value.length == 1) {
                try {
                    if (value[0] == 2) {
                        Log.d(TAG, "successfully paired");
                        return;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error in pairing: " + e);
                    return;
                }
            }
        }
        Log.d(TAG, Arrays.toString(value));
    }

}

