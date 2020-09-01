package com.example.ayudofitness;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;

public class MiBand {
    private final static String TAG = "debug";
    private static final byte[] PAIR = {2};
    private static final byte[] ENABLE_SENSOR_DATA_NOTIFY = {18, 1};
    private static final byte[] DISABLE_SENSOR_DATA_NOTIFY = {18, 0};
    private static final byte[] ENABLE_STEPS_NOTIFY = {3, 1};
    private static final byte[] DISABLE_STEPS_NOTIFY = {3, 0};
    private static final byte[] SCAN_HEART_RATE = {21, 2, 1};

    private Context context;
    private BluetoothLEService service;

    public MiBand(Context context) {
        this.context = context;
        this.service = new BluetoothLEService();
    }

    public void connect(BluetoothDevice device, ActionCallback actionCallback) {
        service.connect(context, device, actionCallback);
    }

    public void setDisconnectListener(NotifyListener disconnectListener) {
        service.setDisconnectedListener(disconnectListener);
    }

    public void showServicesAndCharacteristics() {
        for (BluetoothGattService service : service.bluetoothGatt.getServices()) {
            Log.d(TAG, "onServicesDiscovered: " + service.getUuid());
            for (BluetoothGattCharacteristic chara : service.getCharacteristics()) {
                Log.d(TAG, "char: " + chara.getUuid());
                for (BluetoothGattDescriptor descriptor : chara.getDescriptors()) {
                    Log.d(TAG, "descriptor " + descriptor.getUuid());
                }
            }
        }
    }

    public void readRssi(ActionCallback actionCallback) {
        service.readRssi(actionCallback);
    }

    public void setHeartRateListener(final HeartRateListener heartRateNotifyListener) {
        service.setNotifyListener(UUIDs.SERVICE_HEART_RATE, UUIDs.CHARACTERISTIC_HEART_RATE_MEASURE, new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 2 && data[0] == 6) {
                    int heartRate = data[1] & 0xFF;
                    heartRateNotifyListener.onNotifyHeartRate(heartRate);
                }
            }
        });
    }

    public void scanHeartRate() {
        service.writeCharacteristic(UUIDs.SERVICE_HEART_RATE, UUIDs.CHARACTERISTIC_HEART_RATE_CONTROL, SCAN_HEART_RATE, null);
    }

    public void setNotifyListener(NotifyListener notifyListener) {
        service.setNotifyListener(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_NOTIFICATION, notifyListener);

    }

    public void setStepsListener(final StepsListener stepsListener) {
        service.setNotifyListener(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_STEPS, new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                Log.d(TAG, Arrays.toString(data));
                if (data.length == 4) {
                    int steps = data[3] << 24 | (data[2] & 0xFF) << 16 | (data[1] & 0xFF) << 8 | (data[0] & 0xFF);
                    stepsListener.onNotifySteps(steps);
                }
            }
        });
    }

    public void enableStepsListener() {
        service.writeCharacteristic(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_CONTROL_POINT, ENABLE_STEPS_NOTIFY, null);
    }

    public void disableStepsListener() {
        service.writeCharacteristic(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_CONTROL_POINT, DISABLE_STEPS_NOTIFY, null);
    }

    public void setSensorDataListener(final NotifyListener notifyListener) {
        service.setNotifyListener(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_SENSOR_DATA, new NotifyListener() {
            @Override
            public void onNotify(byte[] data) {
                notifyListener.onNotify(data);
            }
        });
    }

    public void enableSensorDataListener() {
        service.writeCharacteristic(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_CONTROL_POINT, ENABLE_SENSOR_DATA_NOTIFY, null);
    }

    public void disableSensorDataListener() {
        service.writeCharacteristic(UUIDs.SERVICE_BASIC, UUIDs.CHARACTERISTIC_CONTROL_POINT, DISABLE_SENSOR_DATA_NOTIFY, null);
    }

    public void pair(final ActionCallback actionCallback) {
        ActionCallback serviceCallBack = new ActionCallback() {
            @Override
            public void onSuccess(Object data) {
                BluetoothGattCharacteristic bluetoothGattCharacteristic = (BluetoothGattCharacteristic) data;
                Log.d(TAG, "pair result " + Arrays.toString(bluetoothGattCharacteristic.getValue()));
                if (bluetoothGattCharacteristic.getValue().length == 1 && bluetoothGattCharacteristic.getValue()[0] == 2) {
                    actionCallback.onSuccess(null);
                } else {
                    actionCallback.onFail(-1, "pairing not successful");
                }
            }

            @Override
            public void onFail(int errorCode, String msg) {
                actionCallback.onFail(errorCode, msg);
            }
        };
        service.writeReadChar(UUIDs.CHARACTERISTIC_PAIR, PAIR, serviceCallBack);
    }

    public BluetoothDevice getDevice() {
        return service.getDevice();
    }
}
