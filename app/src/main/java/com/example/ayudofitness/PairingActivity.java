package com.example.ayudofitness;

import static com.example.ayudofitness.Constants.ACTION_GATT_CONNECTED;
import static com.example.ayudofitness.Constants.ACTION_GATT_DISCONNECTED;
import static com.example.ayudofitness.Constants.AUTH_OK;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import static com.example.ayudofitness.Constants.*;

public class PairingActivity extends AppCompatActivity {


    private TextView textView;
    private String deviceName;
    private String deviceAddress;
    private BTLEService btleService;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private boolean connected = false;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btleService = ((BTLEService.LocalBinder) service).getService();
            if (!btleService.initialize()) {
                finish();
            }
            device.createBond();
            btleService.connect(device);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btleService = null;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_GATT_CONNECTED:
                    connected = true;
                case ACTION_GATT_DISCONNECTED:
                    connected = false;
                case AUTH_OK:
                    if (device != null && device.getBondState() == BluetoothDevice.BOND_NONE) {
                        SharedPreferences customSharedPreferences = getSharedPreferences(MYPREF, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = customSharedPreferences.edit();
                        editor.putBoolean(PREF_KEY_FIRST_RUN, false);
                        editor.putString(PREF_KEY_ADRESS, deviceAddress);
                        editor.apply();
                    }
            }
            ;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_device);

        textView = findViewById(R.id.textView);
        final Intent intent = getIntent();
        device = intent.getExtras().getParcelable(EXTRAS_DEVICE);
        deviceName = device.getName();
        deviceAddress = device.getAddress();
        textView.setText(deviceName);


        Intent gattServiceIntent = new Intent(this, BTLEService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntenFilter());
        if (btleService != null) {
            final boolean result = btleService.connect(device);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private IntentFilter makeGattUpdateIntenFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }


}
