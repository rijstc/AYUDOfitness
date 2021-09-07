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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
    private static final String TAG = "debugging";

    public byte[] AUTH_CHAR_KEY = new byte[]{
            (byte) 0xf5, (byte) 0xd2, 0x29, (byte) 0x87, 0x65, 0x0a, 0x1d, (byte) 0x82, 0x05,
            (byte) 0xab, (byte) 0x82, (byte) 0xbe, (byte) 0xb9, 0x38, 0x59, (byte) 0xcf};

    private BluetoothGattCharacteristic authChar;
    private BluetoothGattDescriptor authDesc;
    private BluetoothGattService service;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String deviceAdress;
    private BluetoothDevice device;

    private final IBinder iBinder = new LocalBinder();
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
                service = gatt.getService(UUIDS.SERVICE);
                authChar = service.getCharacteristic(UUIDS.CHAR_AUTH);
                Log.d(TAG, String.valueOf(authChar.getUuid()));
                authDesc = authChar.getDescriptor(UUIDS.NOTIFICATION_DESC);
                authorise(gatt);
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
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged char uuid: " + characteristic.getUuid().toString()
                    + " value: " + Arrays.toString(characteristic.getValue()));
            byte[] newValue = Arrays.copyOfRange(characteristic.getValue(), 0, 3);
            if (characteristic.getUuid().equals(UUIDS.CHAR_AUTH)) {
                switch (Arrays.toString(newValue)) {
                    case "[16, 1, 1]":
                        authChar.setValue(new byte[]{0x02, 0x00});
                        gatt.writeCharacteristic(authChar);
                        break;
                    case "[16, 2, 1]":
                        authenticate(gatt, characteristic);
                        break;
                    case "[16, 3, 1]":
                        Log.d(TAG, "Authentifizierung erfolgreich!");
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + characteristic.getUuid());
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (descriptor.getCharacteristic().getUuid().equals(UUIDS.CHAR_AUTH)) {
                Log.d(TAG, "ondescriptorwrite");

                byte[] authKey = ArrayUtils.addAll(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, AUTH_CHAR_KEY);
                authChar.setValue(authKey);
                gatt.writeCharacteristic(authChar);

            } else {
                throw new IllegalStateException("Unexpected value: " + descriptor.getCharacteristic().getUuid().toString());
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "ondescriptorread: " + descriptor.getUuid().toString() + " Read" + "status: " + status);
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
        gatt.setCharacteristicNotification(authChar, true);
        authDesc = authChar.getDescriptor(UUIDS.NOTIFICATION_DESC);
        authDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            gatt.writeDescriptor(authDesc);

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
        } else if (device == null) {
            return false;
        } else {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
            Log.d(TAG, "Trying to create a new connection.");
            deviceAdress = address;
            return true;
        }
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }


}

