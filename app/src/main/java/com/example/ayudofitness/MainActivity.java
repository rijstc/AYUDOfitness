package com.example.ayudofitness;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String TAG = "debugging";


    byte[] data;
    private TextView textView;
    private TextView textViewState;
    private TextView textViewCharacteristics;
    private Button buttonConnect;
    private BluetoothAdapter bluetoothAdapter;
    private boolean connected;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice device;
    private BluetoothGattCharacteristic gattCharacteristic;
    private BluetoothGattDescriptor descriptor;
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bluetoothGatt.discoverServices();
                textViewState.setText("Verbunden");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bluetoothGatt.disconnect();
                textViewState.setText("Nicht verbunden");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            gattCharacteristic = bluetoothGatt.getService(UUIDList.SERVICE_HEART_RATE)
                    .getCharacteristic(UUIDList.CHARACTERISTIC_HEART_RATE_MEASURE);
            bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
            descriptor = gattCharacteristic.getDescriptor(UUIDList.DESCRIPTOR_HEART_RATE);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "on CharacteristicRead");
            data = characteristic.getValue();
            textViewCharacteristics.setText(Arrays.toString(data));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCaracteristicWrite");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged");
            data = characteristic.getValue();
            textViewCharacteristics.setText(Arrays.toString(data));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite");
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d(TAG, "onReadRemoteRssi");
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.d(TAG, "onMtuChanged");
        }
    };
    private String deviceName;
    private String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView2);
        textViewState = findViewById(R.id.textViewState);
        textViewCharacteristics = findViewById(R.id.textViewCharacteristics);
        buttonConnect = findViewById(R.id.buttonConnect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });


        Intent intent = this.getIntent();
        device = intent.getParcelableExtra("device");
        deviceName = device.getName();
        deviceAddress = device.getAddress();
        textView.setText(deviceAddress);

        Set<BluetoothDevice> bondedDevice = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bd : bondedDevice) {
            if (bd.getName().contains("Mi Band 3")) {
                textView.setText(bd.getAddress());
            }
        }

    }

    private void connect() {
        String address = textView.getText().toString();
        device = bluetoothAdapter.getRemoteDevice(address);

        bluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback);
    }
}
