package com.example.ayudofitness;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final String TAG = "debugging";
    private BluetoothDevice device;
    private static final String[] BUTTONS = new String[]{
            "showServicesAndCharacteristics",
            "read_rssi",
            "setHeartRateNotifyListener",
            "startHeartRateScan",
            "setNotifyListener",
            "setRealtimeStepsNotifyListener",
            "enableRealtimeStepsNotify",
            "disableRealtimeStepsNotify",
            "setSensorDataNotifyListener",
            "enableSensorDataNotify",
            "disableSensorDataNotify",
            "pair",
            "connect"};
    private MiBand miBand;
    private TextView textViewLog;
    private ListView listViewChar;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String text = (String) msg.obj;
                    textViewLog.setText(text);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        device = intent.getParcelableExtra("device");
        miBand = new MiBand(this);

        textViewLog = findViewById(R.id.textViewLog);
        listViewChar = findViewById(R.id.listViewChar);
        listViewChar.setAdapter(new ArrayAdapter<String>(this, R.layout.item, BUTTONS));
        listViewChar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch (position) {
                    case 1:
                        miBand.showServicesAndCharacteristics();
                        break;
                    case 2:
                        miBand.readRssi(new ActionCallback() {
                                            @Override
                                            public void onSuccess(Object data) {
                                                Log.d(TAG, "rssi:" + (int) data);
                                            }

                                            @Override
                                            public void onFail(int errorCode, String msg) {
                                                Log.d(TAG, "readRssi fail");
                                            }
                                        }
                        );
                        break;
                    case 3:
                        miBand.setHeartRateListener(new HeartRateListener() {
                            @Override
                            public void onNotifyHeartRate(int heartrate) {
                                Log.d(TAG, "HeartRate: " + heartrate);
                            }
                        });
                        break;
                    case 4:
                        miBand.scanHeartRate();
                        break;
                    case 5:
                        miBand.setNotifyListener(new NotifyListener() {
                            @Override
                            public void onNotify(byte[] data) {
                                Log.d(TAG, "NotifyListener:" + Arrays.toString(data));
                            }
                        });
                        break;
                    case 6:
                        miBand.setStepsListener(new StepsListener() {
                            @Override
                            public void onNotifySteps(int steps) {
                                Log.d(TAG, "StepsListener:" + steps);
                            }
                        });
                        break;
                    case 7:
                        miBand.enableStepsListener();
                        break;
                    case 8:
                        miBand.disableStepsListener();
                        break;
                    case 9:
                        miBand.setSensorDataListener(new NotifyListener() {
                            @Override
                            public void onNotify(byte[] data) {
                                ByteBuffer byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                                int i = 0;

                                int index = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
                                int data1 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
                                int data2 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;
                                int data3 = (data[i++] & 0xFF) | (data[i++] & 0xFF) << 8;

                                Message msg = new Message();
                                msg.what = 1;
                                msg.obj = index + "," + data1 + "," + data2 + "," + data3;

                                handler.sendMessage(msg);
                            }
                        });
                        break;
                    case 10:
                        miBand.enableSensorDataListener();
                        break;
                    case 11:
                        miBand.disableSensorDataListener();
                        break;
                    case 12:
                        miBand.pair(new ActionCallback() {
                            @Override
                            public void onSuccess(Object data) {
                                Log.d(TAG, "pair succ");
                            }

                            @Override
                            public void onFail(int errorCode, String msg) {
                                Log.d(TAG, "pair fail");
                            }
                        });
                    case 13:
                        miBand.connect(device, new ActionCallback() {
                            @Override
                            public void onSuccess(Object data) {
                                Log.d(TAG, "Connection successful");
                                miBand.setDisconnectListener(new NotifyListener() {
                                    @Override
                                    public void onNotify(byte[] data) {
                                        Log.d(TAG, "disconnectlistener set");
                                    }
                                });
                            }

                            @Override
                            public void onFail(int errorCode, String msg) {
                                Log.d(TAG, "connect fail, code:" + errorCode + ",mgs:" + msg);
                            }
                        });
                }
            }
        });
    }
}
