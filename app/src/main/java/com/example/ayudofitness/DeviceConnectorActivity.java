/*package com.example.ayudofitness;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class DeviceConnectorActivity extends AppCompatActivity {


    private TextView textView;
    private String deviceName;
    private String deviceAdress;
    private BluetoothLEService bluetoothLEService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLEService = ((BluetoothLEService.LocalBinder) service).getService();
            bluetoothLEService.connect(deviceAdress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.connect_device);
        textView = findViewById(R.id.textView);
        Intent intent = getIntent();
        deviceName = intent.getStringExtra("DEVICE_NAME");
        deviceAdress = intent.getStringExtra("DEVICE_ADRESS");
        textView.setText(deviceName);

        Intent gattIntent = new Intent(this, BluetoothLEService.class);
    }
}
*/