package com.example.ayudofitness;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String MYPREF = "myCustomSharedPref";
    private static final String PREF_KEY_FIRST_RUN = "firstrun";

    SharedPreferences customSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        customSharedPreferences = getSharedPreferences(MYPREF, Context.MODE_PRIVATE);
        customSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        boolean firstrun = customSharedPreferences.getBoolean(PREF_KEY_FIRST_RUN, true);
        Log.d(Constants.TAG, "sharedPref: "+firstrun);
        if(firstrun){
            startActivity(new Intent(MainActivity.this, ScanActivity.class));
        }
        initializeView();
    }

    private void initializeView() {
        //TextView textView = findViewById(R.id.textView2);
        //textView.setText("Hallo!");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }
}