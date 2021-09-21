package com.example.ayudofitness;

public class Constants {
    public final static String ACTION_GATT_CONNECTED =
            "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_AUTH_OK = "AUTH_OK";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTED = 1;

    public static final int REQUEST_ENABLE_BT = 2;
    public static final int PERMISSION_REQUEST_COARSE_LOCATION = 3;

    public final static String TAG = "debugging";

    public static final String EXTRAS_DEVICE = "DEVICE";
    public static final String EXTRAS_STEPS = "EXTRAS_STEPS";

    public static final String ACTION_STEPS = "ACTION_STEPS";

    public static final String MYPREF = "myCustomSharedPref";
    public static final String PREF_KEY_FIRST_RUN = "firstrun";
    public static final String PREF_KEY_ADRESS = "deviceAdress";


}
