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

import static com.example.ayudofitness.Constants.*;

public class BTLEService extends Service {
    private static final String TAG = "debugging";

    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private String deviceAdress;
    private int connectionState = DISCONNECTED;

    private NotifyListener disconnectedListener = null;
    private ActionCallback callback;
    private final IBinder iBinder = new LocalBinder();
    private Context context;
    private BluetoothGatt bluetoothGatt;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = CONNECTED;
                broadCastUpdate(intentAction);
                Log.d(TAG, "Connected to Gatt.");
                Log.d(TAG, "Attempting to start service discovery: " + bluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = DISCONNECTED;
                Log.d(TAG, "Disconnected to Gatt.");
                broadCastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadCastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                pair();
            } else {
                Log.d(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadCastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
           super.onCharacteristicWrite(gatt, characteristic,status);
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
            switch (action){
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
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        return super.onUnbind(intent);
    }

    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.d(TAG, "Unable to initialize Bluetoothmanager.");
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "unable to obtain a bluetoothadapter.");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.d(TAG, "Bluetoothadapter or address is null.");
            return false;
        }

        if (deviceAdress != null && address.equals(deviceAdress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use existing Gatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "Device not found. Unable to connect.");
            return false;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        deviceAdress = address;
        connectionState = CONNECTING;
        return true;
    }

    public void pair() {
        if (bluetoothGatt != null) {
            BluetoothGattService miliService = bluetoothGatt.getService(UUIDS.UUID_SERVICE_MILI_SERVICE);
            BluetoothGattCharacteristic characteristic = miliService.getCharacteristic(UUIDS.UUID_CHARACTERISTIC_PAIR);
            Log.d(TAG, "characteristics value: "+characteristic.getValue());
            characteristic.setValue(new byte[]{2});
            bluetoothGatt.writeCharacteristic(characteristic);
            Log.d(TAG, "pair sent");
        }
    }



   /* public void connect(final Context context, BluetoothDevice bluetoothDevice,
                        final ActionCallback actionCallback) {
        this.callback = actionCallback;
        Log.d(TAG, "uuids ----------- "+String.valueOf(bluetoothDevice.getUuids()));
        bluetoothDevice.connectGatt(context, true, BTLEService.this);
        Log.d("debugging", "connect okay");
        actionCallback.onSuccess(callback);
    }

    public void writeAndRead(final UUID uuid, byte[] valueToWrite, final ActionCallback actionCallback) {
        writeChar(UUIDS.UUID_SERVICE_MILI_SERVICE, uuid, valueToWrite, new ActionCallback() {

            @Override
            public void onSuccess(Object data) {
                readChar(UUIDS.UUID_SERVICE_MILI_SERVICE, uuid, actionCallback);
            }

            @Override
            public void onFail(int errorCode, String msg) {
                callback.onFail(errorCode, msg);
            }
        });
    }

    private void readChar(UUID serviceUUID, UUID charUUID, ActionCallback actionCallback) {
        try {
            if (bluetoothGatt == null) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("Bitte zuerst mit dem Mi Band verbinden!");
            }
            this.callback = actionCallback;
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(serviceUUID)
                    .getCharacteristic(charUUID);
            if (characteristic == null) {
                callback.onFail(-1, "BluetoothGattCharacteristic " + charUUID + " existiert nicht!");
                return;
            }
            if (this.bluetoothGatt.readCharacteristic(characteristic) == false) {
                callback.onFail(-1, "Characteristic konnte nicht gelesen werden.");
            }
        } catch (Exception e) {
            Log.d(TAG, "readChar " + e);
            callback.onFail(-1, e.getMessage());
        }
    }

    public void writeChar(UUID serviceUUID, UUID charUUID, byte[] value, ActionCallback actionCallback) {
        try {
            if (bluetoothGatt == null) {
                Log.e(TAG, "connect to miband first");
                throw new Exception("Bitte zuerst mit dem Mi Band verbinden!");
            }
            this.callback = actionCallback;
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(serviceUUID)
                    .getCharacteristic(charUUID);
            if (characteristic == null) {
                callback.onFail(-1, "BluetoothGattCharacteristic " + charUUID + " existiert nicht!");
                return;
            }
            characteristic.setValue(value);
            if (this.bluetoothGatt.writeCharacteristic(characteristic) == false) {
                callback.onFail(-1, "Characteristic konnte nicht geschrieben werden.");
            }
        } catch (Exception e) {
            Log.d(TAG, "writeChar " + e);
            callback.onFail(-1, e.getMessage());
        }
    }

    public void setDisconnectedListener(NotifyListener disconnectedListener) {
        this.disconnectedListener = disconnectedListener;
    }*/
}

