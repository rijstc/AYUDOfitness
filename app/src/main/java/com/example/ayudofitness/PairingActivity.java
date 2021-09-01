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
import android.util.Log;
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
    private boolean connected = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private boolean isPairing;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btleService = ((BTLEService.LocalBinder) service).getService();
            if (!btleService.initialize()) {
                finish();
            }
            connected = btleService.connect(device);
            Log.d(TAG, "connected: " + connected);
            device.createBond();

            //btleService.pair();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btleService = null;
        }
    };


    private final BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_DEVICE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            }
        }
    };

    private void firstConnect(BluetoothDevice device) {
        Looper mainLooper = Looper.getMainLooper();
        new Handler(mainLooper).postDelayed(new Runnable() {
            @Override
            public void run() {
                btleService.disconnect();
                btleService.connect(device);
            }
        }, 1000);
    }

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

        SharedPreferences sharedPreferences = getSharedPreferences("MiBandSettings", Context.MODE_PRIVATE);
        String authKey = sharedPreferences.getString("authKey", null);
        if (authKey == null || authKey.isEmpty()){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            String randomAuthKey = randomString(16);
            editor.putString("authKey", randomAuthKey);
            editor.apply();
        }


        Intent gattIntent = new Intent(this, BTLEService.class);
        bindService(gattIntent, serviceConnection, BIND_AUTO_CREATE);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private String randomString(int i) {
        byte[] array = new byte[i];
        new Random().nextBytes(array);
        return new String(array, Charset.forName("UTF-8"));
    }

}
