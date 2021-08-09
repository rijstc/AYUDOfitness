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
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceConnectorActivity extends AppCompatActivity {
    private final static String TAG = "debugging";
    public static final String EXTRAS_DEVICE = "DEVICE";

    private TextView textView;
    private String deviceName;
    private String deviceAddress;
    private BTLEService btleService;
    private BluetoothManager bluetoothManager;
    private boolean connected = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;



    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btleService = ((BTLEService.LocalBinder) service).getService();
            if (!btleService.initialize()) {
                finish();
            }
            connected = btleService.connect(deviceAddress);
            Log.d(TAG, "connected: "+ connected);
            device.createBond();

           //btleService.pair();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btleService = null;
        }
    };

    private final BroadcastReceiver broadcastReceiverBondStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = device.getBondState();

                switch(state){
                    case BluetoothDevice.BOND_BONDED:
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    break;
                    case BluetoothDevice.BOND_BONDING:
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                    break;
                    case BluetoothDevice.BOND_NONE:
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                    break;
                }
            }
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
        registerReceiver(broadcastReceiverBondStateChanged, filter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

}
