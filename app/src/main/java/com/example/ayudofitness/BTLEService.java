package com.example.ayudofitness;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Queue;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static com.example.ayudofitness.Constants.*;

import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class BTLEService extends Service {

    private byte[] AUTH_CHAR_KEY = new byte[]{
            (byte) 0xf5, (byte) 0xd2, 0x29, (byte) 0x87, 0x65, 0x0a, 0x1d, (byte) 0x82, 0x05,
            (byte) 0xab, (byte) 0x82, (byte) 0xbe, (byte) 0xb9, 0x38, 0x59, (byte) 0xcf};

    private static final byte[] BYTE_NEW_HEART_RATE_SCAN = {21, 2, 1};

    private BluetoothGattService service0;
    private BluetoothGattService service1;
    private BluetoothGattService serviceHeartRate;

    private BluetoothGattCharacteristic authChar;
    private BluetoothGattCharacteristic heartRateChar;
    private BluetoothGattCharacteristic stepChar;

    private BluetoothGattDescriptor authDesc;
    private BluetoothGattDescriptor heartRateDesc;
    private BluetoothGattDescriptor stepDesc;

    private boolean scanning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private byte[] currentWriteBytes = new byte[0];

    private BluetoothDevice device;

    private final IBinder iBinder = new LocalBinder();
    private Intent intent = new Intent();

    private BluetoothGatt bluetoothGatt;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close();
                }
            } else {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == GATT_SUCCESS) {
                authorise(gatt);
                setupSteps(gatt);
                setupHeartRate(gatt);
            } else {
                disconnect();
                return;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == GATT_SUCCESS) {
                UUID charRead = characteristic.getUuid();
                byte[] value = characteristic.getValue();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = currentWriteBytes;
            currentWriteBytes = new byte[0];
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID charChanged = characteristic.getUuid();
            byte[] value = characteristic.getValue();
            if (charChanged.equals(UUIDS.UUID_CHAR_AUTH)) {
                byte[] authValue = Arrays.copyOfRange(value, 0, 3);
                switch (Arrays.toString(authValue)) {
                    case "[16, 1, 1]":
                        authChar.setValue(new byte[]{0x02, 0x00});
                        gatt.writeCharacteristic(authChar);
                        break;
                    case "[16, 2, 1]":
                        authenticate(gatt, characteristic);
                        break;
                    case "[16, 3, 1]":
                        intent.setAction(ACTION_AUTH_OK).putExtra(EXTRAS_AUTH, true);
                        sendBroadcast(intent);
                        break;
                }
            } else if (charChanged.equals(UUIDS.UUID_CHAR_STEPS)) {
                byte[] newSteps = new byte[]{value[1], value[2], value[3], value[4]};
                int steps = newSteps[3] << 24 | (newSteps[2] & 0xFF) << 16 | (newSteps[1] & 0xFF) << 8 | (newSteps[0] & 0xFF);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        intent.setAction(ACTION_STEPS).putExtra(EXTRAS_STEPS, steps);
                        sendBroadcast(intent);
                    }
                });
            } else if (charChanged.equals(UUIDS.UUID_NOTIFICATION_HEARTRATE)) {
                final byte heartRate = value[1];
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        intent.setAction(ACTION_HR).putExtra(EXTRAS_HR, Byte.toString(heartRate));
                        sendBroadcast(intent);
                    }
                });
            } else {
                throw new IllegalStateException("Unexpected value: " + characteristic.getUuid());
            }
            sendBroadcast(intent);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            UUID descChar = descriptor.getCharacteristic().getUuid();
            if (status == GATT_SUCCESS) {
                if (descChar.equals(UUIDS.UUID_CHAR_AUTH)) {
                    byte[] authKey = ArrayUtils.addAll(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, AUTH_CHAR_KEY);
                    authChar.setValue(authKey);
                    gatt.writeCharacteristic(authChar);
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + descriptor.getCharacteristic().getUuid().toString());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        }
    };

    private void authenticate(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value[0] == 16 && value[1] == 2 && value[2] == 1) {
            try {
                byte[] copyValue = Arrays.copyOfRange(value, 3, 19);
                String CIPHER_TYPE = "AES/ECB/NoPadding";
                Cipher cipher = Cipher.getInstance(CIPHER_TYPE);

                String CIPHER_NAME = "AES";
                SecretKeySpec secretKeySpec = new SecretKeySpec(AUTH_CHAR_KEY, CIPHER_NAME);
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

                byte[] key = cipher.doFinal(copyValue);
                byte[] request = ArrayUtils.addAll(new byte[]{0x03, 0x00}, key);
                characteristic.setValue(request);
                gatt.writeCharacteristic(characteristic);
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            }
        }
    }

    private void authorise(BluetoothGatt gatt) {
        service1 = gatt.getService(UUIDS.SERVICE_1);
        authChar = service1.getCharacteristic(UUIDS.UUID_CHAR_AUTH);
        authDesc = authChar.getDescriptor(UUIDS.NOTIFICATION_DESC);
        gatt.setCharacteristicNotification(authChar, true);
        authDesc = authChar.getDescriptor(UUIDS.NOTIFICATION_DESC);
        authDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        gatt.writeDescriptor(authDesc);

    }

    private void setupHeartRate(BluetoothGatt gatt) {
        serviceHeartRate = gatt.getService(UUIDS.HEART_RATE_SERVICE);
        heartRateChar = serviceHeartRate.getCharacteristic(UUIDS.UUID_NOTIFICATION_HEARTRATE);
        heartRateDesc = heartRateChar.getDescriptor(UUIDS.NOTIFICATION_DESC);

        gatt.setCharacteristicNotification(heartRateChar, true);
        heartRateDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(heartRateDesc);
    }

    public void getNewHeartRate() {
        BluetoothGattCharacteristic charHeartControl = serviceHeartRate.getCharacteristic(UUIDS.HEART_RATE_CONTROL_POINT_CHARACTERISTIC);
        charHeartControl.setValue(BYTE_NEW_HEART_RATE_SCAN);
    }

    private void setupSteps(BluetoothGatt gatt) {
        service0 = gatt.getService(UUIDS.SERVICE_0);
        stepChar = service0.getCharacteristic(UUIDS.UUID_CHAR_STEPS);
        stepDesc = stepChar.getDescriptor(UUIDS.NOTIFICATION_DESC);
        gatt.setCharacteristicNotification(stepChar, true);
        stepDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(stepDesc);
    }


    public class LocalBinder extends Binder {
        BTLEService getService() {
            return BTLEService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    //Scanning 10 sec for Mi Band 3; returns true and bluetooth device, if found.
    public void scan(BluetoothAdapter btadapter) {
        final long SCAN_PERIOD = 10000;
        BluetoothLeScanner bluetoothLeScanner = btadapter.getBluetoothLeScanner();
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice foundDevice = result.getDevice();
                if (foundDevice.getName() != null && foundDevice.getName().equals("Mi Band 3")) {
                    device = foundDevice;
                    intent.setAction(ACTION_SCAN_RESULT).putExtra(EXTRAS_SCAN, true);
                }
                sendBroadcast(intent);
            }
        };

        if (!scanning) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    // connect to Mi Band 3
    public void connect() {
        device.createBond();
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }
}

