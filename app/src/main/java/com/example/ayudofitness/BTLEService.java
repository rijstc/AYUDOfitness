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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

    private static final byte[] BYTE_LAST_HEART_RATE_SCAN = {21, 1, 1};
    private static final byte[] BYTE_NEW_HEART_RATE_SCAN = {21, 2, 1};

    private BluetoothGattService service0;
    private BluetoothGattService service1;
    private BluetoothGattService serviceHeartRate;

    private BluetoothGattCharacteristic authChar;
    private BluetoothGattCharacteristic heartRateChar;
    private BluetoothGattCharacteristic stepChar;

    private BluetoothGattDescriptor authDesc;
    private BluetoothGattDescriptor heartRateDesc;

    private Queue<Runnable> commandQueue;
    private boolean commandQueueBusy;
    private int tries = 0;
    private boolean isRetrying;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int MAX_TRIES = 2;
    private byte[] currentWriteBytes = new byte[0];


    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String deviceAdress;
    private BluetoothDevice device;
    private int connectionState;

    private final IBinder iBinder = new LocalBinder();
    private BluetoothGatt bluetoothGatt;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectionState = STATE_CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    connectionState = STATE_DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    gatt.close();
                }
            } else {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status == GATT_SUCCESS) {
                SharedPreferences preferences = getApplicationContext().getSharedPreferences(MYPREF, Context.MODE_PRIVATE);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            /*    for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(TAG, "service: " + service.getUuid());

                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.d(TAG, "characteristic: " + characteristic.getUuid() + ", properties: " + characteristic.getProperties());
                    }
                }*/
                if (preferences.getBoolean(PREF_KEY_FIRST_RUN, true)) {
                    authorise(gatt);
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setupHeartRate(gatt);
                    }
                }, 5000);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getNewHeartRate();
                    }
                }, 5000);


            } else {
                disconnect();
                return;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == GATT_SUCCESS) {
                Intent intent = new Intent();
                UUID charRead = characteristic.getUuid();
                byte[] value = characteristic.getValue();
                if (charRead.equals(UUIDS.UUID_CHAR_STEPS)) {
                    byte[] stepsValue = new byte[]{value[1], value[2]};
                    int steps = (stepsValue[0] & 0xff) | ((stepsValue[1] & 0xff) << 8);
                    intent.setAction(ACTION_STEPS).putExtra(EXTRAS_STEPS, steps);
                }
                sendBroadcast(intent);
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
            Intent intent = new Intent();
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
                        broadcastUpdate(ACTION_AUTH_OK);
                        Log.d(TAG, "auth ok");
                        break;
                }
            } else if (charChanged.equals(UUIDS.UUID_NOTIFICATION_HEARTRATE)) {
                final byte heartRate = value[1];
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "heartbeat: " + Byte.toString(heartRate));
                    }
                });
            } else if (charChanged.equals(UUIDS.UUID_CHAR_STEPS)) {
                byte[] stepsValue = new byte[]{value[1], value[2]};
                int steps = (stepsValue[0] & 0xff) | ((stepsValue[1] & 0xff) << 8);
                // int steps = value[3] << 24 | (value[2] & 0xFF) << 16 | (value[1] & 0xFF) << 8 | (value[0] & 0xFF);
                intent.setAction(ACTION_STEPS).putExtra(EXTRAS_STEPS, steps);

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
                    Log.d(TAG, "ondescriptorwrite: auth");

                    byte[] authKey = ArrayUtils.addAll(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, AUTH_CHAR_KEY);
                    authChar.setValue(authKey);
                    gatt.writeCharacteristic(authChar);
                }
                if (descChar.equals(UUIDS.HEART_RATE_MEASUREMENT_CHARACTERISTIC)) {
                    Log.d(TAG, "ondescriptorwrite: heartrate");
                }

            } else {
                throw new IllegalStateException("Unexpected value: " + descriptor.getCharacteristic().getUuid().toString());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "ondescriptorread: " + descriptor.getUuid().toString() + " Read" + "status: " + status);
            //commandCompleted();
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

    public void setupHeartRate(BluetoothGatt gatt) {
        serviceHeartRate = gatt.getService(UUIDS.HEART_RATE_SERVICE);
        heartRateChar = serviceHeartRate.getCharacteristic(UUIDS.HEART_RATE_MEASUREMENT_CHARACTERISTIC);
        heartRateDesc = heartRateChar.getDescriptor(UUIDS.NOTIFICATION_DESC);

        gatt.setCharacteristicNotification(heartRateChar, true);
        heartRateDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(heartRateDesc);
    }

    public void getNewHeartRate() {
        BluetoothGattCharacteristic charHeartControl = serviceHeartRate.getCharacteristic(UUIDS.HEART_RATE_CONTROL_POINT_CHARACTERISTIC);
        charHeartControl.setValue(BYTE_NEW_HEART_RATE_SCAN);
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

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
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
        } else if (device == null) {
            return false;
        } else {
            bluetoothGatt = device.connectGatt(this, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
            Log.d(TAG, "Trying to create a new connection.");
            deviceAdress = address;
            return true;
        }
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }

    private boolean isConnected() {
        return bluetoothGatt != null && connectionState == STATE_CONNECTED;
    }


}

