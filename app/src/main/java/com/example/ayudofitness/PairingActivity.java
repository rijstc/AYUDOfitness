package com.example.ayudofitness;

import static com.example.ayudofitness.Constants.ACTION_GATT_CONNECTED;
import static com.example.ayudofitness.Constants.ACTION_GATT_DISCONNECTED;
import static com.example.ayudofitness.Constants.ACTION_AUTH_OK;

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

import java.util.UUID;

public class PairingActivity extends AppCompatActivity {


    private TextView heartRate;
    private String deviceName;
    private String deviceAddress;
    private BTLEService btleService;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private boolean connected = false;
    //SharedPreferences customSharedPreferences = getSharedPreferences(MYPREF, Context.MODE_PRIVATE);

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
                case ACTION_AUTH_OK:
                        SharedPreferences customSharedPreferences = getSharedPreferences(MYPREF, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = customSharedPreferences.edit();
                        editor.putBoolean(PREF_KEY_FIRST_RUN, true); //todo set false
                        editor.putString(PREF_KEY_ADRESS, deviceAddress);
                        editor.apply();
                        Log.d(TAG, "new address set: "+customSharedPreferences.getString(PREF_KEY_ADRESS,"test"));
                case ACTION_STEPS:
                    int steps = intent.getIntExtra(EXTRAS_STEPS, 0);
                   // String characteristic = intent.getExtras().getString("char");
                    Log.d(TAG, "steps: "+steps);

            }
            ;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_device);

        heartRate = findViewById(R.id.tVHeartRate);
        final Intent intent = getIntent();
        device = intent.getExtras().getParcelable(EXTRAS_DEVICE);
        deviceName = device.getName();
        deviceAddress = device.getAddress();
//        heartRate.setText(btleService.heartRate);
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());


        Intent gattServiceIntent = new Intent(this, BTLEService.class);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (btleService != null) {
            final boolean result = btleService.connect(device);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_AUTH_OK);
        intentFilter.addAction(ACTION_STEPS);
        return intentFilter;
    }


}
