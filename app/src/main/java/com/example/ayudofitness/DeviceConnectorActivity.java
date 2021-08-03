package com.example.ayudofitness;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceConnectorActivity extends AppCompatActivity {
    private final static String TAG = "debugging";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView textView;
    private String deviceName;
    private String deviceAddress;
    private BTLEService btleService;
    private boolean connected = false;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btleService = ((BTLEService.LocalBinder) service).getService();
            if (!btleService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.d(TAG, "deviceAddress: " + deviceAddress);
            btleService.connect(deviceAddress);
           // btleService.pair();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btleService = null;
        }
    };

   /* private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BTLEService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                Log.d(TAG, "Broadcastreceiver Connected.");
            } else if (BTLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                Log.d(TAG, "Broadcastreceiver disconnected.");
            } else if (BTLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "Broadcastreceiver services discoverd.");
            } else if (BTLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "Broadcastreceiver data available.");
            }
        }
    };*/



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_device);
        textView = findViewById(R.id.textView);
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra("DEVICE_NAME");
        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS");
        Log.d(TAG, "on Create device address: "+ deviceAddress);
        textView.setText(deviceName);

        Intent gattIntent = new Intent(this, BTLEService.class);
        bindService(gattIntent, serviceConnection, BIND_AUTO_CREATE);


    }
}
