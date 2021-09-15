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

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static com.example.ayudofitness.Constants.*;

import org.apache.commons.lang3.ArrayUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class BTLEService extends Service {
    private static final String TAG = "debugging";

    private static final String MYPREF = "myCustomSharedPref";
    private static final String PREF_KEY_FIRST_RUN = "firstrun";

    //SharedPreferences customSharedPreferences = getApplicationContext().getSharedPreferences(MYPREF, Context.MODE_PRIVATE);

    public byte[] AUTH_CHAR_KEY = new byte[]{
            (byte) 0xf5, (byte) 0xd2, 0x29, (byte) 0x87, 0x65, 0x0a, 0x1d, (byte) 0x82, 0x05,
            (byte) 0xab, (byte) 0x82, (byte) 0xbe, (byte) 0xb9, 0x38, 0x59, (byte) 0xcf};

    private BluetoothGattService service_0;
    private BluetoothGattService service_1;
    private BluetoothGattCharacteristic authChar;
    private BluetoothGattCharacteristic stepChar;
    private BluetoothGattDescriptor authDesc;

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
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                service_1 = gatt.getService(UUIDS.SERVICE_1);
                authChar = service_1.getCharacteristic(UUIDS.UUID_CHAR_AUTH);
                Log.d(TAG, String.valueOf(authChar.getUuid()));
                authDesc = authChar.getDescriptor(UUIDS.NOTIFICATION_DESC);
                authorise(gatt);
                service_0 = gatt.getService(UUIDS.SERVICE_0);
                stepChar = service_0.getCharacteristic(UUIDS.UUID_CHAR_STEPS);
                Log.d(TAG, "stepChar: " + stepChar.getUuid() + ", " + stepChar.getValue());
            } else {
                disconnect();
                return;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == GATT_SUCCESS) {
                byte[] value = characteristic.getValue();
            }
            commandCompleted();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] value = currentWriteBytes;
            currentWriteBytes = new byte[0];
            commandCompleted();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged char uuid: " + characteristic.getUuid().toString()
                    + " value: " + Arrays.toString(characteristic.getValue()));
            byte[] newValue = Arrays.copyOfRange(characteristic.getValue(), 0, 3);
            if (characteristic.getUuid().equals(UUIDS.UUID_CHAR_AUTH)) {
                switch (Arrays.toString(newValue)) {
                    case "[16, 1, 1]":
                        authChar.setValue(new byte[]{0x02, 0x00});
                        gatt.writeCharacteristic(authChar);
                        break;
                    case "[16, 2, 1]":
                        authenticate(gatt, characteristic);
                        break;
                    case "[16, 3, 1]":
                        broadcastUpdate(AUTH_OK);
                        break;
                }
            } else {
                throw new IllegalStateException("Unexpected value: " + characteristic.getUuid());
            }

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (descriptor.getCharacteristic().getUuid().equals(UUIDS.UUID_CHAR_AUTH)) {
                if (status == GATT_SUCCESS) {
                    Log.d(TAG, "ondescriptorwrite");

                    byte[] authKey = ArrayUtils.addAll(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, AUTH_CHAR_KEY);
                    authChar.setValue(authKey);
                    gatt.writeCharacteristic(authChar);
                }

            } else {
                throw new IllegalStateException("Unexpected value: " + descriptor.getCharacteristic().getUuid().toString());
            }
            commandCompleted();
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "ondescriptorread: " + descriptor.getUuid().toString() + " Read" + "status: " + status);
            commandCompleted();
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

    public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (!isConnected() || characteristic == null || (characteristic.getProperties() & PROPERTY_READ) == 0) {
            return false;
        }

        boolean result = commandQueue.add(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    if (bluetoothGatt.readCharacteristic(characteristic)) {
                        Log.d(TAG, "reading Characteristic: " + characteristic.getUuid());
                        tries++;
                    } else {
                        commandCompleted();
                    }
                } else {
                    commandCompleted();
                }
            }
        });

        if (result) {
            nextCommand();
        }

        return result;
    }

  /*  public boolean writeCharacteristics(BluetoothGattCharacteristic characteristic, byte[] value, WriteType writeType) {
        if (!isConnected()) {
            return false;
        }

        return result;
    }*/

    private void nextCommand() {
        if (commandQueueBusy) {
            return;
        }
        if (bluetoothGatt == null) {
            commandQueue.clear();
            commandQueueBusy = false;
            return;
        }
        if (commandQueue.size() > 0) {
            final Runnable bleCommand = commandQueue.peek();
            commandQueueBusy = true;
            if (!isRetrying) {
                tries = 0;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        bleCommand.run();
                    } catch (Exception e) {
                        Log.d(TAG, "Commandqueue Error: " + e);
                        commandCompleted();
                    }
                }
            });
        }
    }

    private void commandCompleted() {
        isRetrying = false;
        commandQueueBusy = false;
        commandQueue.poll();
        nextCommand();
    }

    private void retryingCommand() {
        commandQueueBusy = false;
        Runnable currentCommand = commandQueue.peek();
        if (currentCommand != null) {
            if (tries >= MAX_TRIES) {
                commandQueue.poll();
            } else {
                isRetrying = true;
            }
        }
        nextCommand();
    }

    private boolean isConnected() {
        return bluetoothGatt != null && connectionState == STATE_CONNECTED;
    }


}

