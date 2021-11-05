package com.example.ayudofitness;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.example.ayudofitness.Constants.*;

public class ScanActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private AlertDialog.Builder builder;
    private DeviceListAdapter deviceListAdapter;
    private ListView listView;
    private boolean scanning;
    private BluetoothDevice bluetoothDevice;
    private BluetoothDevice foundMiBand;
    //private BTLEService BTLEService = new BTLEService();

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    foundMiBand = result.getDevice();
                    if (foundMiBand.getName() != null && foundMiBand.getName().equals("Mi Band 3")) {
                        deviceListAdapter.addDevice(foundMiBand);
                        deviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
            listView.setAdapter(deviceListAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    bluetoothDevice = deviceListAdapter.getDevice(position);
                    if (bluetoothDevice == null) {
                        return;
                    }
                    Intent intent = new Intent(ScanActivity.this, PairingActivity.class);
                    intent.putExtra(EXTRAS_DEVICE, bluetoothDevice);
                    startActivity(intent);
                }
            });
        }

        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        deviceListAdapter = new DeviceListAdapter(this);

        listView = findViewById(R.id.list);
        bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        getPermissions();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, deviceName);
                if (deviceName.equals("Mi Band 3")){
                  //  BTLEService.getNewHeartRate();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Zugriff auf Standort erlaubt", Toast.LENGTH_LONG).show();
            } else {
                builder = new AlertDialog.Builder(this);
                builder.setTitle("Eingeschränkte Funktionen");
                builder.setMessage("Die App funktioniert erst, wenn der Zugriff auf den Standort erlaubt wurde.");
                builder.setPositiveButton(R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);
        if (!scanning) {
            menu.findItem(R.id.menuSearch).setVisible(true);
            menu.findItem(R.id.menuStop).setVisible(false);
            menu.findItem(R.id.menuDiscover).setActionView(null);
        } else {
            menu.findItem(R.id.menuSearch).setVisible(false);
            menu.findItem(R.id.menuStop).setVisible(true);
            menu.findItem(R.id.menuDiscover).setActionView(R.layout.actionbar_search_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSearch:
                deviceListAdapter.clear();
                scanDevice(true);
                break;
            case R.id.menuStop:
                scanDevice(false);
                break;
        }
        return true;
    }

    private void scanDevice(boolean enabled) {
        String name = "Mi Band 3";
        List<ScanFilter> filters = null;
        filters = new ArrayList<>();
        ScanFilter filter = new ScanFilter.Builder()
                .setDeviceName(name)
                .build();
        filters.add(filter);
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                .setReportDelay(0L)
                .build();
        if (enabled) {
            scanning = true;
            bluetoothLeScanner.startScan(filters, scanSettings, scanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
    }


    private void getPermissions() {
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_DENIED) {
                builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth wird benötigt.");
                builder.setMessage("Bitte schalten Sie Bluetooth ein.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.BLUETOOTH}, REQUEST_ENABLE_BT);
                    }
                });
                builder.show();
            }
            if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission_group.LOCATION) == PackageManager.PERMISSION_DENIED) {
                   builder = new AlertDialog.Builder(this);
                    builder.setTitle("Standortzugriff wird benötigt.");
                    builder.setMessage("Bitte geben Sie den Standortzugriff frei.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_BT);
                        }
                    });
                    builder.show();
                }
                }
            } else {
                Toast.makeText(this, "Bluetooth LE wird von diesem" +
                        "Gerät nicht unterstützt.", Toast.LENGTH_LONG).show();
                finish();
            }
        }

}
