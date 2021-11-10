package com.example.ayudofitness;

import static com.example.ayudofitness.Constants.ACTION_AUTH_OK;
import static com.example.ayudofitness.Constants.ACTION_GATT_CONNECTED;
import static com.example.ayudofitness.Constants.ACTION_GATT_DISCONNECTED;
import static com.example.ayudofitness.Constants.ACTION_HR;
import static com.example.ayudofitness.Constants.ACTION_SCAN_RESULT;
import static com.example.ayudofitness.Constants.ACTION_STEPS;
import static com.example.ayudofitness.Constants.EXTRAS_AUTH;
import static com.example.ayudofitness.Constants.EXTRAS_HR;
import static com.example.ayudofitness.Constants.EXTRAS_SCAN;
import static com.example.ayudofitness.Constants.EXTRAS_STEPS;
import static com.example.ayudofitness.Constants.MYPREF;
import static com.example.ayudofitness.Constants.PERMISSION_REQUEST_COARSE_LOCATION;
import static com.example.ayudofitness.Constants.PREF_KEY_ADRESS;
import static com.example.ayudofitness.Constants.PREF_KEY_FIRST_RUN;
import static com.example.ayudofitness.Constants.REQUEST_ENABLE_BT;
import static com.example.ayudofitness.Constants.TAG;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button buttonScan;
    private Button buttonPair;
    private Button buttonHeartRate;
    private Button buttonSteps;
    private TextView textViewScan;
    private TextView textViewPair;
    private TextView textViewHeartRate;
    private TextView textViewSteps;

    private boolean bound = false;

    private BTLEService btleService;
    private BluetoothAdapter bluetoothAdapter;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            btleService = ((BTLEService.LocalBinder) service).getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            btleService = null;
            bound = false;
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_AUTH_OK:
                    if (intent.getBooleanExtra(EXTRAS_AUTH, false)) {
                        textViewPair.setText("Mi Band 3 verbunden.");
                    }
                    break;
                case ACTION_STEPS:
                    int steps = intent.getIntExtra(EXTRAS_STEPS, 0);
                    textViewSteps.setText(steps+"");
                case ACTION_SCAN_RESULT:
                    if (intent.getBooleanExtra(EXTRAS_SCAN, false)) {
                        textViewScan.setText("Mi Band 3 gefunden.");
                    }
                    break;
                case ACTION_HR:
                    String hr = intent.getStringExtra(EXTRAS_HR);
                    String text = String.format(getResources().getString(R.string.hr_data), hr);
                    textViewHeartRate.setText(text);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btleService = new BTLEService();
        initializeView();
        initializeListeners();
        requiredPermissions();

        Intent intent = new Intent(this, BTLEService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        registerReceiver(broadcastReceiver, makeIntentFilter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
        bound = false;
    }

    private void initializeView() {
        buttonScan = findViewById(R.id.buttonScan);
        buttonPair = findViewById(R.id.buttonPair);
        buttonHeartRate = findViewById(R.id.buttonHeartRate);
        buttonSteps = findViewById(R.id.buttonSteps);
        textViewScan = findViewById(R.id.textViewScan);
        textViewPair = findViewById(R.id.textViewPair);
        textViewHeartRate = findViewById(R.id.textViewHeartRate);
        textViewSteps = findViewById(R.id.textViewSteps);
    }

    private void initializeListeners() {
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bound) {
                    btleService.scan(bluetoothAdapter);
                }
            }
        });

        buttonPair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bound) {
                    btleService.connect();
                }
            }
        });

        buttonHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonHeartRate.setClickable(false);
            }
        });

        buttonSteps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            buttonSteps.setClickable(false);
            }
        });
    }

    private void requiredPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH},
                    PackageManager.PERMISSION_GRANTED);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    PackageManager.PERMISSION_GRANTED);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PackageManager.PERMISSION_GRANTED);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PackageManager.PERMISSION_GRANTED);
        }
    }

    private IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_AUTH_OK);
        intentFilter.addAction(ACTION_STEPS);
        intentFilter.addAction(ACTION_SCAN_RESULT);
        intentFilter.addAction(ACTION_HR);
        return intentFilter;
    }
}