package com.example.ayudofitness;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.Charset;
import java.util.Random;

public class PairingActivity extends AppCompatActivity {
    private final static String TAG = "debugging";
    public static final String EXTRAS_DEVICE = "DEVICE";
    public static final String ACTION_DEVICE_CHANGED = "ACTION_DEVICE_CHANGED";

    private TextView textView;
    private String deviceName;
    private String deviceAddress;
    private BTLEService btleService;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;


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



        Intent gattIntent = new Intent(this, BTLEService.class);
        bindService(gattIntent, serviceConnection, BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


}
