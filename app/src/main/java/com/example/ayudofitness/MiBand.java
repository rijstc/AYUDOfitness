package com.example.ayudofitness;

import android.content.Context;

public class MiBand {
    private static final String TAG = "debugging";
    private Context context;
    private BluetoothIO bluetoothIO;

    public MiBand(final Context context){
        this.context = context;

    /*    ActionCallback actionCallback = new ActionCallback() {
            @Override
            public void onSuccess(Object data) {
                bluetoothIO = new BTLEService(context)
            }

            @Override
            public void onFail(int errorCode, String msg) {

            }
        }*/
    }
}
